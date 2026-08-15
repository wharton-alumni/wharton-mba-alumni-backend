package edu.wharton.alumni.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wharton.alumni.model.AlumniEvent;
import edu.wharton.alumni.model.CohortCampus;
import edu.wharton.alumni.model.EventCategory;
import edu.wharton.alumni.model.EventStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WhartonEventScraperService {
    private static final ZoneId EASTERN_TIME = ZoneId.of("America/New_York");
    private static final UUID EXTERNAL_POSTER_ID = new UUID(0L, 52L);
    private static final Pattern JSON_LD_SCRIPT = Pattern.compile(
            "<script[^>]+type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final List<String> SOURCES = List.of(
            "https://events.wharton.upenn.edu/",
            "https://alumni.wharton.upenn.edu/clubs-network/wharton-global-club-network-events/",
            "https://alumni.wharton.upenn.edu/clubs-network/global-clubs-directory/",
            "https://alumni.wharton.upenn.edu/clubs-network/affinity-clubs/",
            "https://alumni.wharton.upenn.edu/",
            "https://ai-analytics.wharton.upenn.edu/events/",
            "https://ai-analytics.wharton.upenn.edu/",
            "https://ai-analytics.wharton.upenn.edu/for-students/student-clubs/",
            "https://ai-analytics.wharton.upenn.edu/centers-labs/",
            "https://ai-analytics.wharton.upenn.edu/for-researchers/funded-research/",
            "https://groups.wharton.upenn.edu/club_signup?order=name_desc&view=all",
            "https://research.wharton.upenn.edu/",
            "https://knowledge.wharton.upenn.edu/",
            "https://entrepreneurship.wharton.upenn.edu/",
            "https://healthcare.wharton.upenn.edu/",
            "https://wrds-www.wharton.upenn.edu/"
    );

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean enabled;
    private volatile Instant cachedAt = Instant.EPOCH;
    private volatile List<AlumniEvent> cachedEvents = List.of();

    public WhartonEventScraperService(ObjectMapper objectMapper,
                                      @Value("${app.events.external.enabled:true}") boolean enabled) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public List<AlumniEvent> upcomingEvents() {
        if (!enabled) {
            return List.of();
        }
        if (cachedAt.plus(Duration.ofMinutes(30)).isAfter(Instant.now())) {
            return cachedEvents;
        }
        Map<UUID, AlumniEvent> eventsById = new LinkedHashMap<>();
        for (String sourceUrl : SOURCES) {
            for (AlumniEvent event : scrapeSource(sourceUrl)) {
                eventsById.putIfAbsent(event.id(), event);
            }
        }
        cachedEvents = eventsById.values().stream()
                .sorted(Comparator.comparing(AlumniEvent::eventDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        cachedAt = Instant.now();
        return cachedEvents;
    }

    private List<AlumniEvent> scrapeSource(String sourceUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(sourceUrl))
                    .timeout(Duration.ofSeconds(12))
                    .header("user-agent", "Wharton Alumni Portal event indexer")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }
            return structuredEvents(response.body(), sourceUrl);
        } catch (IOException exception) {
            return List.of();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private List<AlumniEvent> structuredEvents(String html, String sourceUrl) {
        List<AlumniEvent> events = new ArrayList<>();
        Matcher matcher = JSON_LD_SCRIPT.matcher(html);
        while (matcher.find()) {
            try {
                collectEvents(objectMapper.readTree(matcher.group(1).trim()), sourceUrl, events);
            } catch (IOException ignored) {
                // Ignore malformed embedded structured data from source pages.
            }
        }
        return events;
    }

    private void collectEvents(JsonNode node, String sourceUrl, List<AlumniEvent> events) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectEvents(child, sourceUrl, events));
            return;
        }
        JsonNode graph = node.get("@graph");
        if (graph != null) {
            collectEvents(graph, sourceUrl, events);
        }
        if (isEventNode(node)) {
            toEvent(node, sourceUrl).ifPresent(events::add);
        }
    }

    private boolean isEventNode(JsonNode node) {
        JsonNode type = node.get("@type");
        if (type == null) {
            return false;
        }
        if (type.isArray()) {
            for (JsonNode item : type) {
                if ("Event".equalsIgnoreCase(item.asText())) {
                    return true;
                }
            }
            return false;
        }
        return "Event".equalsIgnoreCase(type.asText());
    }

    private java.util.Optional<AlumniEvent> toEvent(JsonNode node, String sourceUrl) {
        String title = text(node, "name");
        Instant eventDate = parseDate(text(node, "startDate"));
        if (title.isBlank() || eventDate == null || eventDate.isBefore(Instant.now())) {
            return java.util.Optional.empty();
        }
        String url = firstNonBlank(text(node, "url"), sourceUrl);
        String description = firstNonBlank(text(node, "description"), "Wharton event from " + host(sourceUrl));
        String location = location(node.get("location"));
        String imageUrl = image(node.get("image"));
        UUID id = UUID.nameUUIDFromBytes((title + "|" + eventDate + "|" + url).getBytes(StandardCharsets.UTF_8));
        return java.util.Optional.of(new AlumniEvent(
                id,
                title,
                description,
                categoryFor(sourceUrl, title, description),
                eventDate,
                location,
                url,
                imageUrl,
                EXTERNAL_POSTER_ID,
                host(sourceUrl),
                CohortCampus.Global,
                false,
                null,
                EventStatus.APPROVED,
                Instant.now()
        ));
    }

    private EventCategory categoryFor(String sourceUrl, String title, String description) {
        String haystack = (sourceUrl + " " + title + " " + description).toLowerCase();
        if (haystack.contains("career")) {
            return EventCategory.Career_Opportunity;
        }
        if (haystack.contains("reunion")) {
            return EventCategory.Reunion;
        }
        if (haystack.contains("research") || haystack.contains("analytics") || haystack.contains("ai")) {
            return EventCategory.Industry_Insights;
        }
        return EventCategory.Networking;
    }

    private String location(JsonNode location) {
        if (location == null || location.isNull()) {
            return "";
        }
        if (location.isTextual()) {
            return location.asText();
        }
        String name = text(location, "name");
        JsonNode address = location.get("address");
        if (address != null && address.isObject()) {
            return firstNonBlank(name, text(address, "addressLocality"), text(address, "addressRegion"), text(address, "streetAddress"));
        }
        return name;
    }

    private String image(JsonNode image) {
        if (image == null || image.isNull()) {
            return "";
        }
        if (image.isTextual()) {
            return image.asText();
        }
        if (image.isArray() && !image.isEmpty()) {
            return image(image.get(0));
        }
        return firstNonBlank(text(image, "url"), text(image, "contentUrl"));
    }

    private Instant parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            // Try less specific ISO formats below.
        }
        try {
            return java.time.OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
            // Try less specific ISO formats below.
        }
        try {
            return LocalDateTime.parse(value).atZone(EASTERN_TIME).toInstant();
        } catch (DateTimeParseException ignored) {
            // Try date-only structured values below.
        }
        try {
            return LocalDate.parse(value).atStartOfDay(EASTERN_TIME).toInstant();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String host(String sourceUrl) {
        return URI.create(sourceUrl).getHost();
    }
}
