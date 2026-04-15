package com.cariesguard.image.domain.service;

import com.cariesguard.image.domain.model.ObjectStoreCommand;
import com.cariesguard.image.domain.model.StoredObject;
import com.cariesguard.image.domain.model.StoredObjectResource;
import java.io.IOException;
import java.io.InputStream;

public interface ObjectStorageService {

    StoredObject store(ObjectStoreCommand command) throws IOException;

    default StoredObject store(String originalFileName,
                               String contentType,
                               InputStream inputStream,
                               long fileSizeBytes,
                               String md5) throws IOException {
        return store(new ObjectStoreCommand(
                "IMAGE",
                "case-image",
                md5 == null ? "general" : md5,
                originalFileName,
                contentType,
                inputStream,
                fileSizeBytes,
                md5));
    }

    StoredObjectResource load(String bucketName,
                              String objectKey,
                              String originalFileName,
                              String contentType) throws IOException;

    void delete(String bucketName, String objectKey) throws IOException;

    String presignGetObject(String bucketName, String objectKey) throws IOException;

    long defaultPresignExpireSeconds();

    String proxyAccessSecret();
}
