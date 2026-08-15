package edu.wharton.alumni.controller;

import edu.wharton.alumni.service.HeadshotStorageService;
import jakarta.servlet.http.HttpServletRequest;
import edu.wharton.alumni.security.JwtUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

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
        String requestedPath = prefixIndex >= 0
                ? URLDecoder.decode(uri.substring(prefixIndex + PREFIX.length()), StandardCharsets.UTF_8)
                : "";
        String key = "headshots/" + requestedPath;

        HeadshotStorageService.StoredHeadshot headshot = headshotStorageService.find(key)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Headshot not found."));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(headshot.contentType()));
        headers.setCacheControl("public, max-age=2592000, immutable");
        return ResponseEntity.ok()
                .headers(headers)
                .body(headshot.bytes());
    }

    @PostMapping("/api/headshots/me")
    public HeadshotUploadResponse uploadHeadshot(@AuthenticationPrincipal JwtUser user,
                                                 @RequestParam("file") MultipartFile file) throws IOException {
        HeadshotStorageService.StoredHeadshotUpload upload = headshotStorageService.upload(
                user.id(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes()
        );
        return new HeadshotUploadResponse(upload.key(), upload.url());
    }

    public record HeadshotUploadResponse(String key, String url) {
    }
}
