package edu.wharton.alumni.service;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@Service
public class RateLimitService {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void check(String key, int maxAttempts, long windowSeconds) {
        long now = Instant.now().getEpochSecond();
        Bucket bucket = buckets.compute(key, (ignored, current) -> {
            if (current == null || current.windowStart + windowSeconds <= now) {
                return new Bucket(now, 1);
            }
            return new Bucket(current.windowStart, current.attempts + 1);
        });
        if (bucket.attempts > maxAttempts) {
            throw new ResponseStatusException(TOO_MANY_REQUESTS, "Too many attempts. Please try again later.");
        }
    }

    private record Bucket(long windowStart, int attempts) {
    }
}
