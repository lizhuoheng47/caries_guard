package com.cariesguard.image.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.image.config.ImageStorageProperties;
import com.cariesguard.image.domain.model.AttachmentUploadModel;
import com.cariesguard.image.domain.model.AttachmentViewModel;
import com.cariesguard.image.domain.model.StoredObject;
import com.cariesguard.image.domain.model.StoredObjectResource;
import com.cariesguard.image.domain.repository.ImageCommandRepository;
import com.cariesguard.image.domain.repository.ImageQueryRepository;
import com.cariesguard.image.domain.service.ObjectStorageService;
import com.cariesguard.image.interfaces.vo.AttachmentAccessVO;
import com.cariesguard.image.interfaces.vo.AttachmentUploadVO;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AttachmentAppService {

    private final ImageCommandRepository imageCommandRepository;
    private final ImageQueryRepository imageQueryRepository;
    private final ObjectStorageService objectStorageService;
    private final ImageStorageProperties imageStorageProperties;

    public AttachmentAppService(ImageCommandRepository imageCommandRepository,
                                ImageQueryRepository imageQueryRepository,
                                ObjectStorageService objectStorageService,
                                ImageStorageProperties imageStorageProperties) {
        this.imageCommandRepository = imageCommandRepository;
        this.imageQueryRepository = imageQueryRepository;
        this.objectStorageService = objectStorageService;
        this.imageStorageProperties = imageStorageProperties;
    }

    @Transactional
    public AttachmentUploadVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "File is required");
        }
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR.code(), "Read file failed");
        }
        String md5 = md5(bytes);
        return imageCommandRepository.findAttachmentByMd5(operator.getOrgId(), md5)
                .map(item -> new AttachmentUploadVO(
                        item.attachmentId(),
                        item.fileName(),
                        item.bucketName(),
                        item.objectKey(),
                        item.md5()))
                .orElseGet(() -> createAttachment(operator, file, bytes.length, md5));
    }

    public AttachmentAccessVO createAccessUrl(Long attachmentId, HttpServletRequest request) {
        return createAccessUrl(attachmentId, requestBaseUrl(request));
    }

    public AttachmentAccessVO createAccessUrl(Long attachmentId, String baseUrl) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        AttachmentViewModel attachment = loadAttachment(attachmentId);
        ensureOrgAccess(operator, attachment.orgId());
        return buildAccessUrl(attachment, baseUrl);
    }

    public AttachmentAccessVO createInternalAccessUrl(Long attachmentId) {
        return buildAccessUrl(loadAttachment(attachmentId), imageStorageProperties.getPublicBaseUrl());
    }

    public String resolveLocalStoragePath(String bucketName, String objectKey) {
        if (!StringUtils.hasText(imageStorageProperties.getLocalRoot())
                || !StringUtils.hasText(bucketName)
                || !StringUtils.hasText(objectKey)) {
            return null;
        }
        return Path.of(imageStorageProperties.getLocalRoot(), bucketName).resolve(objectKey).toAbsolutePath().normalize().toString();
    }

    public StoredObjectResource loadContent(Long attachmentId, Long expireAt, String signature) {
        if (expireAt == null || expireAt <= 0 || !StringUtils.hasText(signature)) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "Signed access params are required");
        }
        if (System.currentTimeMillis() / 1000 > expireAt) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN.code(), "Signed access has expired");
        }
        AttachmentViewModel attachment = loadAttachment(attachmentId);
        String expectedSignature = sign(attachmentId, attachment.bucketName(), attachment.objectKey(), expireAt);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN.code(), "Signed access is invalid");
        }
        try {
            return objectStorageService.load(
                    attachment.bucketName(),
                    attachment.objectKey(),
                    attachment.originalName(),
                    attachment.contentType());
        } catch (IOException exception) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Attachment content does not exist");
        }
    }

    private AttachmentUploadVO createAttachment(AuthenticatedUser operator,
                                                MultipartFile file,
                                                long fileSizeBytes,
                                                String md5) {
        StoredObject storedObject;
        try (InputStream inputStream = file.getInputStream()) {
            storedObject = objectStorageService.store(
                    file.getOriginalFilename(),
                    defaultContentType(file.getContentType()),
                    inputStream,
                    fileSizeBytes,
                    md5);
        } catch (IOException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR.code(), "Store attachment failed");
        }
        long attachmentId = IdWorker.getId();
        try {
            imageCommandRepository.createAttachment(new AttachmentUploadModel(
                    attachmentId,
                    "CASE",
                    null,
                    "IMAGE",
                    storedObject.fileName(),
                    file.getOriginalFilename(),
                    storedObject.bucketName(),
                    storedObject.objectKey(),
                    storedObject.contentType(),
                    fileExtension(file.getOriginalFilename()),
                    storedObject.fileSizeBytes(),
                    storedObject.md5(),
                    storedObject.providerCode(),
                    "PRIVATE",
                    operator.getUserId(),
                    operator.getOrgId(),
                    "ACTIVE",
                    null,
                    operator.getUserId()));
        } catch (RuntimeException exception) {
            deleteStoredObjectQuietly(storedObject);
            throw exception;
        }
        return new AttachmentUploadVO(
                attachmentId,
                storedObject.fileName(),
                storedObject.bucketName(),
                storedObject.objectKey(),
                storedObject.md5());
    }

    private AttachmentAccessVO buildAccessUrl(AttachmentViewModel attachment, String baseUrl) {
        String resolvedBaseUrl = StringUtils.hasText(baseUrl) ? baseUrl : imageStorageProperties.getPublicBaseUrl();
        if (!StringUtils.hasText(resolvedBaseUrl)) {
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR.code(), "Image public base URL is not configured");
        }
        long expireAt = System.currentTimeMillis() / 1000 + imageStorageProperties.getAccessUrlExpireSeconds();
        String signature = sign(attachment.attachmentId(), attachment.bucketName(), attachment.objectKey(), expireAt);
        String accessUrl = UriComponentsBuilder.fromUriString(resolvedBaseUrl)
                .replacePath("/api/v1/files/" + attachment.attachmentId() + "/content")
                .replaceQuery(null)
                .queryParam("expireAt", expireAt)
                .queryParam("signature", signature)
                .build()
                .toUriString();
        return new AttachmentAccessVO(accessUrl, expireAt);
    }

    private String requestBaseUrl(HttpServletRequest request) {
        return UriComponentsBuilder.newInstance()
                .scheme(request.getScheme())
                .host(request.getServerName())
                .port(request.getServerPort())
                .build()
                .toUriString();
    }

    private AttachmentViewModel loadAttachment(Long attachmentId) {
        return imageQueryRepository.findAttachment(attachmentId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Attachment does not exist"));
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    private String sign(Long attachmentId, String bucketName, String objectKey, long expireAt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(imageStorageProperties.getAccessUrlSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String payload = attachmentId + ":" + bucketName + ":" + objectKey + ":" + expireAt;
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR);
        }
    }

    private String md5(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(bytes));
        } catch (Exception exception) {
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR);
        }
    }

    private String defaultContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType : "application/octet-stream";
    }

    private String fileExtension(String originalFileName) {
        if (!StringUtils.hasText(originalFileName) || !originalFileName.contains(".")) {
            return null;
        }
        return originalFileName.substring(originalFileName.lastIndexOf('.') + 1);
    }

    private void deleteStoredObjectQuietly(StoredObject storedObject) {
        try {
            objectStorageService.delete(storedObject.bucketName(), storedObject.objectKey());
        } catch (IOException ignored) {
            // Keep the original failure cause from attachment persistence.
        }
    }
}
