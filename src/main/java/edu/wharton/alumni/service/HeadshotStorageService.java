package edu.wharton.alumni.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Service
public class HeadshotStorageService {
    private final String bucket;
    private final S3Client s3Client;

    public HeadshotStorageService(@Value("${app.headshots.bucket}") String bucket,
                                  @Value("${app.headshots.endpoint}") String endpoint,
                                  @Value("${app.headshots.access-key-id}") String accessKeyId,
                                  @Value("${app.headshots.secret-access-key}") String secretAccessKey,
                                  @Value("${app.headshots.region}") String region,
                                  @Value("${app.headshots.url-style}") String urlStyle) {
        this.bucket = bucket;
        this.s3Client = isBlank(bucket) || isBlank(endpoint) || isBlank(accessKeyId) || isBlank(secretAccessKey)
                ? null
                : S3Client.builder()
                        .endpointOverride(URI.create(endpoint))
                        .region(Region.of(isBlank(region) ? "auto" : region))
                        .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                        ))
                        .serviceConfiguration(S3Configuration.builder()
                                .pathStyleAccessEnabled(usesPathStyle(urlStyle))
                                .build())
                        .build();
    }

    public Optional<StoredHeadshot> find(String key) {
        if (s3Client == null || !isAllowedHeadshotKey(key)) {
            return Optional.empty();
        }

        try {
            ResponseBytes<GetObjectResponse> object = s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build(),
                    ResponseTransformer.toBytes()
            );
            String contentType = object.response().contentType();
            return Optional.of(new StoredHeadshot(
                    object.asByteArray(),
                    isBlank(contentType) ? "application/octet-stream" : contentType
            ));
        } catch (NoSuchKeyException exception) {
            return Optional.empty();
        } catch (S3Exception exception) {
            throw new IllegalStateException("Unable to fetch headshot from bucket.", exception);
        }
    }

    public StoredHeadshotUpload upload(UUID userId, String originalFilename, String contentType, byte[] bytes) {
        if (s3Client == null) {
            throw new IllegalStateException("Headshot bucket is not configured.");
        }
        if (!isAllowedUpload(contentType, bytes)) {
            throw new IllegalArgumentException("Please upload a JPEG, PNG, or WebP image under 2 MB.");
        }

        String extension = extensionFor(contentType, originalFilename);
        String key = "headshots/uploads/" + userId + "-" + UUID.randomUUID() + extension;
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .cacheControl("public, max-age=2592000, immutable")
                        .build(),
                RequestBody.fromBytes(bytes)
        );

        return new StoredHeadshotUpload(
                key,
                "/api/headshots/" + key.substring("headshots/".length())
        );
    }

    private boolean isAllowedHeadshotKey(String key) {
        return key != null
                && !key.isBlank()
                && !key.startsWith("/")
                && !key.contains("..")
                && key.startsWith("headshots/")
                && (key.endsWith(".jpg") || key.endsWith(".jpeg") || key.endsWith(".png") || key.endsWith(".webp"));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean usesPathStyle(String value) {
        return "path".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
    }

    private boolean isAllowedUpload(String contentType, byte[] bytes) {
        return bytes != null
                && bytes.length > 0
                && bytes.length <= 2_000_000
                && ("image/jpeg".equalsIgnoreCase(contentType)
                || "image/png".equalsIgnoreCase(contentType)
                || "image/webp".equalsIgnoreCase(contentType));
    }

    private String extensionFor(String contentType, String filename) {
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        if (filename != null && filename.toLowerCase().endsWith(".jpeg")) {
            return ".jpeg";
        }
        return ".jpg";
    }

    public record StoredHeadshot(byte[] bytes, String contentType) {
    }

    public record StoredHeadshotUpload(String key, String url) {
    }
}
