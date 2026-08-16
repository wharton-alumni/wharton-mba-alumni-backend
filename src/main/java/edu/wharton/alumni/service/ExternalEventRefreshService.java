package edu.wharton.alumni.service;

import edu.wharton.alumni.model.AlumniEvent;
import edu.wharton.alumni.model.ExternalEventSyncState;
import edu.wharton.alumni.repository.AlumniEventRepository;
import edu.wharton.alumni.repository.ExternalEventSyncStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class ExternalEventRefreshService {
    private static final Logger log = LoggerFactory.getLogger(ExternalEventRefreshService.class);
    private static final String STATE_ID = "wharton-external-events";

    private final WhartonEventScraperService scraperService;
    private final AlumniEventRepository eventRepository;
    private final ExternalEventSyncStateRepository syncStateRepository;
    private final TransactionTemplate transactionTemplate;
    private final Duration freshnessWindow;
    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);

    public ExternalEventRefreshService(WhartonEventScraperService scraperService,
                                       AlumniEventRepository eventRepository,
                                       ExternalEventSyncStateRepository syncStateRepository,
                                       TransactionTemplate transactionTemplate,
                                       @Value("${app.events.external.refresh-hours:24}") long refreshHours) {
        this.scraperService = scraperService;
        this.eventRepository = eventRepository;
        this.syncStateRepository = syncStateRepository;
        this.transactionTemplate = transactionTemplate;
        this.freshnessWindow = Duration.ofHours(refreshHours);
    }

    @Scheduled(
            initialDelayString = "${app.events.external.startup-delay-ms:15000}",
            fixedDelayString = "${app.events.external.refresh-check-ms:3600000}"
    )
    public void scheduledRefresh() {
        refreshIfStaleInBackground();
    }

    public void refreshIfStaleInBackground() {
        if (!scraperService.enabled() || !safeIsStale() || !refreshInProgress.compareAndSet(false, true)) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                refreshNow();
            } finally {
                refreshInProgress.set(false);
            }
        });
    }

    private boolean safeIsStale() {
        try {
            return isStale();
        } catch (RuntimeException exception) {
            log.warn("external_events_staleness_check_failed error={}", exception.toString());
            return false;
        }
    }

    private boolean isStale() {
        return transactionTemplate.execute(status -> {
            ExternalEventSyncState state = syncStateRepository.findById(STATE_ID).orElse(null);
            return state == null
                    || state.lastScrapedAt() == null
                    || state.lastScrapedAt().plus(freshnessWindow).isBefore(Instant.now());
        });
    }

    private void refreshNow() {
        Instant startedAt = Instant.now();
        try {
            List<AlumniEvent> events = scraperService.scrapeUpcomingEvents();
            transactionTemplate.executeWithoutResult(status -> persistRefresh(events, startedAt, null));
            log.info("external_events_refreshed count={} scrapedAt={}", events.size(), startedAt);
        } catch (RuntimeException exception) {
            transactionTemplate.executeWithoutResult(status ->
                    persistState(startedAt, null, exception.getClass().getSimpleName() + ": " + exception.getMessage()));
            log.warn("external_events_refresh_failed scrapedAt={} error={}", startedAt, exception.toString());
        }
    }

    private void persistRefresh(List<AlumniEvent> events, Instant scrapedAt, String error) {
        Instant dbUpdatedAt = null;
        if (!events.isEmpty()) {
            eventRepository.saveAll(events);
            Set<UUID> currentExternalIds = events.stream().map(AlumniEvent::id).collect(Collectors.toSet());
            eventRepository.deleteByExternalManagedTrueAndIdNotIn(currentExternalIds);
            dbUpdatedAt = Instant.now();
        } else {
            error = "Scrape completed with no upcoming external events; existing DB events were retained.";
        }
        persistState(scrapedAt, dbUpdatedAt, error);
    }

    private void persistState(Instant scrapedAt, Instant dbUpdatedAt, String error) {
        ExternalEventSyncState current = syncStateRepository.findById(STATE_ID).orElse(null);
        syncStateRepository.save(new ExternalEventSyncState(
                STATE_ID,
                scrapedAt,
                dbUpdatedAt == null && current != null ? current.lastDbUpdatedAt() : dbUpdatedAt,
                error
        ));
    }
}
