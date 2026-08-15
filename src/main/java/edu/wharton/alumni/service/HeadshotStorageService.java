package edu.wharton.alumni.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;
import java.util.Optional;

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

    public record StoredHeadshot(byte[] bytes, String contentType) {
    }
}
