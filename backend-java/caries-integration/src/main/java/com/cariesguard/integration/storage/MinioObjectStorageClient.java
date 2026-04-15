package com.cariesguard.integration.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.InputStreamResource;
import org.springframework.util.StringUtils;

public class MinioObjectStorageClient implements ObjectStorageClient {

    private static final long DEFAULT_PART_SIZE = 10L * 1024L * 1024L;

    private final MinioClient minioClient;
    private final Set<String> checkedBuckets = ConcurrentHashMap.newKeySet();

    public MinioObjectStorageClient(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public UploadObjectResult upload(UploadObjectCommand command) throws IOException {
        validate(command.bucketName(), command.objectKey());
        String contentType = StringUtils.hasText(command.contentType()) ? command.contentType() : "application/octet-stream";
        try {
            ensureBucketExists(command.bucketName());
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(command.bucketName())
                    .object(command.objectKey())
                    .contentType(contentType)
                    .stream(command.inputStream(), command.fileSizeBytes(), DEFAULT_PART_SIZE)
                    .build());
            return new UploadObjectResult(
                    command.bucketName(),
                    command.objectKey(),
                    StringUtils.hasText(command.fileName()) ? command.fileName() : fileName(command.objectKey()),
                    contentType,
                    command.fileSizeBytes(),
                    command.md5(),
                    "MINIO");
        } catch (Exception exception) {
            throw new IOException("Store object to MinIO failed", exception);
        }
    }

    @Override
    public ObjectContent download(String bucketName, String objectKey, String originalFileName, String contentType) throws IOException {
        validate(bucketName, objectKey);
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
            GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
            return new ObjectContent(
                    new InputStreamResource(response),
                    StringUtils.hasText(contentType) ? contentType : defaultContentType(stat.contentType()),
                    StringUtils.hasText(originalFileName) ? originalFileName : fileName(objectKey),
                    stat.size());
        } catch (Exception exception) {
            throw new IOException("Load object from MinIO failed", exception);
        }
    }

    @Override
    public void delete(String bucketName, String objectKey) throws IOException {
        validate(bucketName, objectKey);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
        } catch (Exception exception) {
            throw new IOException("Delete object from MinIO failed", exception);
        }
    }

    @Override
    public String presignGetObject(String bucketName, String objectKey, Duration expiry) throws IOException {
        return presign(Method.GET, bucketName, objectKey, expiry);
    }

    @Override
    public String presignPutObject(String bucketName, String objectKey, Duration expiry) throws IOException {
        return presign(Method.PUT, bucketName, objectKey, expiry);
    }

    private String presign(Method method, String bucketName, String objectKey, Duration expiry) throws IOException {
        validate(bucketName, objectKey);
        try {
            ensureBucketExists(bucketName);
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(method)
                    .bucket(bucketName)
                    .object(objectKey)
                    .expiry((int) Math.max(60, expiry == null ? 900 : expiry.toSeconds()))
                    .build());
        } catch (Exception exception) {
            throw new IOException("Create MinIO presigned URL failed", exception);
        }
    }

    private void ensureBucketExists(String bucketName) throws Exception {
        if (checkedBuckets.contains(bucketName)) {
            return;
        }
        synchronized (checkedBuckets) {
            if (checkedBuckets.contains(bucketName)) {
                return;
            }
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
            }
            checkedBuckets.add(bucketName);
        }
    }

    private void validate(String bucketName, String objectKey) throws IOException {
        if (!StringUtils.hasText(bucketName) || !StringUtils.hasText(objectKey)) {
            throw new IOException("bucketName and objectKey are required");
        }
    }

    private String defaultContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType : "application/octet-stream";
    }

    private String fileName(String objectKey) {
        int index = objectKey.lastIndexOf('/');
        return index >= 0 ? objectKey.substring(index + 1) : objectKey;
    }
}
