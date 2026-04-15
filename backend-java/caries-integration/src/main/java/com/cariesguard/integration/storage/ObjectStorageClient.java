package com.cariesguard.integration.storage;

import java.io.IOException;
import java.time.Duration;

public interface ObjectStorageClient {

    UploadObjectResult upload(UploadObjectCommand command) throws IOException;

    ObjectContent download(String bucketName, String objectKey, String originalFileName, String contentType) throws IOException;

    void delete(String bucketName, String objectKey) throws IOException;

    String presignGetObject(String bucketName, String objectKey, Duration expiry) throws IOException;

    String presignPutObject(String bucketName, String objectKey, Duration expiry) throws IOException;
}
