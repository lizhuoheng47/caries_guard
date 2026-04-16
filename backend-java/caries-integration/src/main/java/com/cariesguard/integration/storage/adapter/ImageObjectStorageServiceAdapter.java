package com.cariesguard.integration.storage.adapter;

import com.cariesguard.image.domain.model.ObjectStoreCommand;
import com.cariesguard.image.domain.model.StoredObject;
import com.cariesguard.image.domain.model.StoredObjectResource;
import com.cariesguard.image.domain.service.ObjectStorageService;
import com.cariesguard.integration.storage.ObjectContent;
import com.cariesguard.integration.storage.ObjectKeyGenerator;
import com.cariesguard.integration.storage.ObjectKeyRequest;
import com.cariesguard.integration.storage.ObjectStorageClient;
import com.cariesguard.integration.storage.StorageProperties;
import com.cariesguard.integration.storage.UploadObjectCommand;
import com.cariesguard.integration.storage.UploadObjectResult;
import java.io.IOException;
import java.time.LocalDate;

public class ImageObjectStorageServiceAdapter implements ObjectStorageService {

    private final ObjectStorageClient objectStorageClient;
    private final StorageProperties storageProperties;
    private final ObjectKeyGenerator objectKeyGenerator;

    public ImageObjectStorageServiceAdapter(ObjectStorageClient objectStorageClient,
                                            StorageProperties storageProperties,
                                            ObjectKeyGenerator objectKeyGenerator) {
        this.objectStorageClient = objectStorageClient;
        this.storageProperties = storageProperties;
        this.objectKeyGenerator = objectKeyGenerator;
    }

    @Override
    public StoredObject store(ObjectStoreCommand command) throws IOException {
        String bucketName = storageProperties.bucketName(command.bucketCode());
        String objectKey = objectKeyGenerator.generate(toKeyRequest(command));
        UploadObjectResult result = objectStorageClient.upload(new UploadObjectCommand(
                bucketName,
                objectKey,
                fileName(objectKey),
                command.contentType(),
                command.inputStream(),
                command.fileSizeBytes(),
                command.md5()));
        return new StoredObject(
                result.bucketName(),
                result.objectKey(),
                result.fileName(),
                result.contentType(),
                result.fileSizeBytes(),
                result.md5(),
                result.providerCode());
    }

    @Override
    public StoredObjectResource load(String bucketName,
                                     String objectKey,
                                     String originalFileName,
                                     String contentType) throws IOException {
        ObjectContent content = objectStorageClient.download(bucketName, objectKey, originalFileName, contentType);
        return new StoredObjectResource(
                content.resource(),
                content.contentType(),
                content.originalFileName(),
                content.contentLength());
    }

    @Override
    public void delete(String bucketName, String objectKey) throws IOException {
        objectStorageClient.delete(bucketName, objectKey);
    }

    @Override
    public String presignGetObject(String bucketName, String objectKey) throws IOException {
        return objectStorageClient.presignGetObject(bucketName, objectKey, storageProperties.defaultPresignExpireDuration());
    }

    @Override
    public long defaultPresignExpireSeconds() {
        return storageProperties.getDefaultPresignExpireSeconds();
    }

    @Override
    public String proxyAccessSecret() {
        return storageProperties.getProxyAccessSecret();
    }

    private ObjectKeyRequest toKeyRequest(ObjectStoreCommand command) {
        return new ObjectKeyRequest(
                command.objectKindCode(),
                command.orgId(),
                command.caseNo(),
                command.imageTypeCode(),
                command.attachmentId(),
                command.taskNo(),
                command.modelVersion(),
                command.assetTypeCode(),
                command.relatedImageId(),
                command.toothCode(),
                command.reportTypeCode(),
                command.versionNo(),
                command.operatorId(),
                command.exportLogId(),
                command.reportNo(),
                command.originalFileName(),
                LocalDate.now());
    }

    private String fileName(String objectKey) {
        int index = objectKey.lastIndexOf('/');
        return index >= 0 ? objectKey.substring(index + 1) : objectKey;
    }
}
