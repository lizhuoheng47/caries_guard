package com.cariesguard.integration.storage;

import com.cariesguard.image.domain.service.ObjectStorageService;
import com.cariesguard.integration.storage.adapter.ImageObjectStorageServiceAdapter;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class ObjectStorageAutoConfig {

    @Bean
    @ConditionalOnMissingBean
    public ObjectKeyGenerator objectKeyGenerator() {
        return new DefaultObjectKeyGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "caries.storage", name = "provider", havingValue = "MINIO", matchIfMissing = true)
    public MinioClient minioClient(StorageProperties properties) {
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey());
        if (StringUtils.hasText(properties.getRegion())) {
            builder.region(properties.getRegion());
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "caries.storage", name = "provider", havingValue = "MINIO", matchIfMissing = true)
    public ObjectStorageClient objectStorageClient(MinioClient minioClient, StorageProperties storageProperties) {
        return new MinioObjectStorageClient(minioClient, storageProperties);
    }

    @Bean
    @ConditionalOnMissingBean(ObjectStorageService.class)
    public ObjectStorageService objectStorageService(ObjectStorageClient objectStorageClient,
                                                     StorageProperties storageProperties,
                                                     ObjectKeyGenerator objectKeyGenerator) {
        return new ImageObjectStorageServiceAdapter(objectStorageClient, storageProperties, objectKeyGenerator);
    }
}
