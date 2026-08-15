package edu.wharton.alumni.controller;

import edu.wharton.alumni.service.HeadshotStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class HeadshotController {
    private static final String PREFIX = "/api/headshots/";

    private final HeadshotStorageService headshotStorageService;

    public HeadshotController(HeadshotStorageService headshotStorageService) {
        this.headshotStorageService = headshotStorageService;
    }

    @GetMapping("/api/headshots/**")
    public ResponseEntity<byte[]> getHeadshot(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int prefixIndex = uri.indexOf(PREFIX);
        String key = prefixIndex >= 0
                ? URLDecoder.decode(uri.substring(prefixIndex + PREFIX.length()), StandardCharsets.UTF_8)
                : "";

        HeadshotStorageService.StoredHeadshot headshot = headshotStorageService.find(key)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Headshot not found."));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(headshot.contentType()));
        headers.setCacheControl("public, max-age=2592000, immutable");
        return ResponseEntity.ok()
                .headers(headers)
                .body(headshot.bytes());
    }
}
