package com.cariesguard.image.infrastructure.storage;

import com.cariesguard.image.config.ImageStorageProperties;
import com.cariesguard.image.domain.model.StoredObject;
import com.cariesguard.image.domain.model.StoredObjectResource;
import com.cariesguard.image.domain.service.ObjectStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "caries.image.storage", name = "provider-code", havingValue = "MINIO", matchIfMissing = true)
public class MinioObjectStorageService implements ObjectStorageService {

    private static final long DEFAULT_PART_SIZE = 10L * 1024L * 1024L;

    private final ImageStorageProperties properties;
    private final MinioClient minioClient;
    private volatile boolean bucketChecked;

    public MinioObjectStorageService(ImageStorageProperties properties) {
        this.properties = properties;
        this.minioClient = MinioClient.builder()
                .endpoint(properties.getMinio().getEndpoint())
                .credentials(properties.getMinio().getAccessKey(), properties.getMinio().getSecretKey())
                .build();
    }

    @Override
    public StoredObject store(String originalFileName,
                              String contentType,
                              InputStream inputStream,
                              long fileSizeBytes,
                              String md5) throws IOException {
        String objectKey = buildObjectKey(originalFileName, md5);
        String normalizedContentType = StringUtils.hasText(contentType) ? contentType : "application/octet-stream";
        try {
            ensureBucketExists();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucketName())
                    .object(objectKey)
                    .contentType(normalizedContentType)
                    .stream(inputStream, fileSizeBytes, DEFAULT_PART_SIZE)
                    .build());
            return new StoredObject(
                    properties.getBucketName(),
                    objectKey,
                    objectKey.substring(objectKey.lastIndexOf('/') + 1),
                    normalizedContentType,
                    fileSizeBytes,
                    md5,
                    "MINIO");
        } catch (Exception exception) {
            throw new IOException("Store object to MinIO failed", exception);
        }
    }

    @Override
    public StoredObjectResource load(String bucketName,
                                     String objectKey,
                                     String originalFileName,
                                     String contentType) throws IOException {
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
            GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
            String responseContentType = StringUtils.hasText(contentType)
                    ? contentType
                    : (StringUtils.hasText(stat.contentType()) ? stat.contentType() : "application/octet-stream");
            String responseFileName = StringUtils.hasText(originalFileName)
                    ? originalFileName
                    : objectKey.substring(objectKey.lastIndexOf('/') + 1);
            return new StoredObjectResource(
                    new InputStreamResource(response),
                    responseContentType,
                    responseFileName,
                    stat.size());
        } catch (Exception exception) {
            throw new IOException("Load object from MinIO failed", exception);
        }
    }

    @Override
    public void delete(String bucketName, String objectKey) throws IOException {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
        } catch (Exception exception) {
            throw new IOException("Delete object from MinIO failed", exception);
        }
    }

    private void ensureBucketExists() throws Exception {
        if (bucketChecked) {
            return;
        }
        synchronized (this) {
            if (bucketChecked) {
                return;
            }
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(properties.getBucketName())
                    .build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(properties.getBucketName())
                        .build());
            }
            bucketChecked = true;
        }
    }

    private String buildObjectKey(String originalFileName, String md5) {
        LocalDate now = LocalDate.now();
        return "attachments/%s/%s/%s/%s%s".formatted(
                now.getYear(),
                pad(now.getMonthValue()),
                pad(now.getDayOfMonth()),
                md5,
                fileExtension(originalFileName));
    }

    private String fileExtension(String originalFileName) {
        if (!StringUtils.hasText(originalFileName) || !originalFileName.contains(".")) {
            return "";
        }
        return originalFileName.substring(originalFileName.lastIndexOf('.'));
    }

    private String pad(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }
}
