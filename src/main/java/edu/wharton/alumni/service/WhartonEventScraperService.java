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
import java.time.LocalTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.util.HtmlUtils;

@Service
public class WhartonEventScraperService {
    private static final ZoneId EASTERN_TIME = ZoneId.of("America/New_York");
    private static final UUID EXTERNAL_POSTER_ID = new UUID(0L, 52L);
    private static final Pattern JSON_LD_SCRIPT = Pattern.compile(
            "<script[^>]+type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern MARTECH_EVENT_ITEM = Pattern.compile(
            "<div class=\"martech-wevent-list--item\">.*?<div class=\"month\">(.*?)</div>.*?<div class=\"day\">(.*?)</div>.*?<a\\s+href=\"(.*?)\"[^>]*>(.*?)</a>.*?<div class=\"info\"><span>(.*?)</span></div>.*?<!--\\s*\\.martech-wevent-list--item\\s*-->",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}:\\d{2}\\s*[AP]M)", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter CLOCK_TIME = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter NATION_BUILDER_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
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
        for (AlumniEvent event : scrapeNationBuilderCalendarEvents()) {
            eventsById.putIfAbsent(event.id(), event);
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
            List<AlumniEvent> events = new ArrayList<>(structuredEvents(response.body(), sourceUrl));
            events.addAll(martechEvents(response.body(), sourceUrl));
            return events;
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

    private List<AlumniEvent> martechEvents(String html, String sourceUrl) {
        List<AlumniEvent> events = new ArrayList<>();
        Matcher matcher = MARTECH_EVENT_ITEM.matcher(html);
        while (matcher.find()) {
            String month = cleanText(matcher.group(1));
            String day = cleanText(matcher.group(2));
            String url = resolveUrl(matcher.group(3), sourceUrl);
            String title = cleanText(matcher.group(4));
            String infoHtml = matcher.group(5).replaceAll("(?i)<br\\s*/?>", "\n");
            String info = cleanText(infoHtml);
            Instant eventDate = parseMartechDate(month, day, info);
            if (title.isBlank() || eventDate == null || eventDate.isBefore(Instant.now())) {
                continue;
            }
            String location = martechLocation(infoHtml);
            events.add(externalEvent(
                    title,
                    "Wharton event from " + host(sourceUrl),
                    categoryFor(sourceUrl, title, info),
                    eventDate,
                    location,
                    url,
                    "",
                    host(sourceUrl)
            ));
        }
        return events;
    }

    private List<AlumniEvent> scrapeNationBuilderCalendarEvents() {
        List<AlumniEvent> events = new ArrayList<>();
        YearMonth cursor = YearMonth.now(EASTERN_TIME);
        for (int index = 0; index < 6; index++) {
            YearMonth month = cursor.plusMonths(index);
            String apiUrl = "https://wharton.herokuapp.com/get_month_year.json?month="
                    + month.getMonth().name().charAt(0)
                    + month.getMonth().name().substring(1).toLowerCase()
                    + "&year=" + month.getYear()
                    + "&zone=-4&site_slug=wharton";
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .timeout(Duration.ofSeconds(12))
                        .header("user-agent", "Wharton Alumni Portal event indexer")
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    collectNationBuilderEvents(objectMapper.readTree(response.body()), events);
                }
            } catch (IOException exception) {
                // Skip unavailable calendar months.
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return events;
            } catch (RuntimeException exception) {
                // Skip malformed calendar months.
            }
        }
        return events;
    }

    private void collectNationBuilderEvents(JsonNode node, List<AlumniEvent> events) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectNationBuilderEvents(child, events));
            return;
        }
        if (node.isObject()) {
            if (node.hasNonNull("headline") && node.hasNonNull("start_time")) {
                toNationBuilderEvent(node).ifPresent(events::add);
                return;
            }
            node.fields().forEachRemaining(entry -> collectNationBuilderEvents(entry.getValue(), events));
        }
    }

    private java.util.Optional<AlumniEvent> toNationBuilderEvent(JsonNode node) {
        String title = cleanText(text(node, "headline"));
        Instant eventDate = parseNationBuilderDate(text(node, "start_time"));
        if (title.isBlank() || eventDate == null || eventDate.isBefore(Instant.now())) {
            return java.util.Optional.empty();
        }
        String label = cleanText(text(node, "label"));
        String subnation = cleanText(text(node, "subnation"));
        String url = resolveUrl(text(node, "url"), "https://alumni.wharton.upenn.edu/clubs-network/wharton-global-club-network-events/");
        String description = firstNonBlank("Wharton Global Club Network event" + (subnation.isBlank() ? "" : " from " + subnation), "Wharton Global Club Network event");
        return java.util.Optional.of(externalEvent(
                title,
                description,
                categoryFor(url, title, label),
                eventDate,
                label,
                url,
                "",
                "Wharton Global Club Network"
        ));
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
                id, title, description, categoryFor(sourceUrl, title, description), eventDate, location, url,
                imageUrl, EXTERNAL_POSTER_ID, host(sourceUrl), CohortCampus.Global, false, null,
                EventStatus.APPROVED, Instant.now()
        ));
    }

    private AlumniEvent externalEvent(String title, String description, EventCategory category, Instant eventDate,
                                      String location, String url, String imageUrl, String sourceName) {
        UUID id = UUID.nameUUIDFromBytes((title + "|" + eventDate + "|" + url).getBytes(StandardCharsets.UTF_8));
        return new AlumniEvent(
                id,
                title,
                description,
                category,
                eventDate,
                location,
                url,
                imageUrl,
                EXTERNAL_POSTER_ID,
                sourceName,
                CohortCampus.Global,
                false,
                null,
                EventStatus.APPROVED,
                Instant.now()
        );
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

    private Instant parseMartechDate(String monthText, String dayText, String info) {
        try {
            Month month = parseMonth(monthText);
            int day = Integer.parseInt(dayText.trim());
            LocalTime time = LocalTime.NOON;
            Matcher matcher = TIME_PATTERN.matcher(info);
            if (matcher.find()) {
                time = LocalTime.parse(matcher.group(1).trim().toUpperCase(), CLOCK_TIME);
            }
            int year = LocalDate.now(EASTERN_TIME).getYear();
            LocalDateTime dateTime = LocalDateTime.of(year, month, day, time.getHour(), time.getMinute());
            if (dateTime.atZone(EASTERN_TIME).toInstant().isBefore(Instant.now())) {
                dateTime = dateTime.plusYears(1);
            }
            return dateTime.atZone(EASTERN_TIME).toInstant();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private Month parseMonth(String monthText) {
        String normalized = monthText.trim().toLowerCase();
        return switch (normalized.substring(0, Math.min(3, normalized.length()))) {
            case "jan" -> Month.JANUARY;
            case "feb" -> Month.FEBRUARY;
            case "mar" -> Month.MARCH;
            case "apr" -> Month.APRIL;
            case "may" -> Month.MAY;
            case "jun" -> Month.JUNE;
            case "jul" -> Month.JULY;
            case "aug" -> Month.AUGUST;
            case "sep" -> Month.SEPTEMBER;
            case "oct" -> Month.OCTOBER;
            case "nov" -> Month.NOVEMBER;
            case "dec" -> Month.DECEMBER;
            default -> throw new IllegalArgumentException("Unsupported month: " + monthText);
        };
    }

    private Instant parseNationBuilderDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ZonedDateTime.parse(value, NATION_BUILDER_TIME).toInstant();
        } catch (DateTimeParseException exception) {
            return parseDate(value);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private String cleanText(String value) {
        if (value == null) {
            return "";
        }
        return HtmlUtils.htmlUnescape(value.replaceAll("<[^>]+>", " "))
                .replace('\u00a0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String martechLocation(String info) {
        String[] lines = info.split("\\R");
        if (lines.length > 1) {
            return cleanText(lines[1]);
        }
        int pipeIndex = info.indexOf('|');
        return pipeIndex >= 0 ? cleanText(info.substring(pipeIndex + 1).replaceFirst("(?i).*?[AP]M\\s*(-\\s*\\d{1,2}:\\d{2}\\s*[AP]M)?", "")) : "";
    }

    private String resolveUrl(String url, String sourceUrl) {
        if (url == null || url.isBlank()) {
            return sourceUrl;
        }
        String cleanUrl = HtmlUtils.htmlUnescape(url.trim());
        if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            return cleanUrl;
        }
        if (cleanUrl.startsWith("//")) {
            return "https:" + cleanUrl;
        }
        if (!cleanUrl.startsWith("/") && cleanUrl.contains(".")) {
            return "https://" + cleanUrl;
        }
        URI source = URI.create(sourceUrl);
        return source.resolve(cleanUrl).toString();
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
