package com.cariesguard.image.infrastructure.storage;

import com.cariesguard.image.config.ImageStorageProperties;
import com.cariesguard.image.domain.model.StoredObject;
import com.cariesguard.image.domain.model.StoredObjectResource;
import com.cariesguard.image.domain.service.ObjectStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "caries.image.storage", name = "provider-code", havingValue = "LOCAL_FS")
public class LocalObjectStorageService implements ObjectStorageService {

    private final ImageStorageProperties properties;

    public LocalObjectStorageService(ImageStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StoredObject store(String originalFileName,
                              String contentType,
                              InputStream inputStream,
                              long fileSizeBytes,
                              String md5) throws IOException {
        String fileExt = fileExtension(originalFileName);
        LocalDate now = LocalDate.now();
        String objectKey = "attachments/%s/%s/%s/%s%s".formatted(
                now.getYear(),
                pad(now.getMonthValue()),
                pad(now.getDayOfMonth()),
                md5,
                fileExt);
        Path basePath = Path.of(properties.getLocalRoot(), properties.getBucketName()).toAbsolutePath().normalize();
        Path targetPath = basePath.resolve(objectKey).normalize();
        if (!targetPath.startsWith(basePath)) {
            throw new IOException("Invalid local object key");
        }
        Files.createDirectories(targetPath.getParent());
        if (Files.notExists(targetPath)) {
            Files.copy(inputStream, targetPath);
        }
        return new StoredObject(
                properties.getBucketName(),
                objectKey.replace('\\', '/'),
                targetPath.getFileName().toString(),
                contentType,
                fileSizeBytes,
                md5,
                "LOCAL_FS");
    }

    @Override
    public StoredObjectResource load(String bucketName,
                                     String objectKey,
                                     String originalFileName,
                                     String contentType) throws IOException {
        Path basePath = Path.of(properties.getLocalRoot(), bucketName).toAbsolutePath().normalize();
        Path targetPath = basePath.resolve(objectKey).normalize();
        if (!targetPath.startsWith(basePath) || Files.notExists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new IOException("Stored object does not exist");
        }
        return new StoredObjectResource(
                new FileSystemResource(targetPath),
                StringUtils.hasText(contentType) ? contentType : "application/octet-stream",
                StringUtils.hasText(originalFileName) ? originalFileName : targetPath.getFileName().toString(),
                Files.size(targetPath));
    }

    @Override
    public void delete(String bucketName, String objectKey) throws IOException {
        Path basePath = Path.of(properties.getLocalRoot(), bucketName).toAbsolutePath().normalize();
        Path targetPath = basePath.resolve(objectKey).normalize();
        if (!targetPath.startsWith(basePath) || Files.notExists(targetPath)) {
            return;
        }
        Files.deleteIfExists(targetPath);
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
