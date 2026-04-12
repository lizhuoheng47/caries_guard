package com.cariesguard.image.domain.service;

import com.cariesguard.image.domain.model.StoredObject;
import com.cariesguard.image.domain.model.StoredObjectResource;
import java.io.IOException;
import java.io.InputStream;

public interface ObjectStorageService {

    StoredObject store(String originalFileName,
                       String contentType,
                       InputStream inputStream,
                       long fileSizeBytes,
                       String md5) throws IOException;

    StoredObjectResource load(String bucketName,
                              String objectKey,
                              String originalFileName,
                              String contentType) throws IOException;

    void delete(String bucketName, String objectKey) throws IOException;
}
