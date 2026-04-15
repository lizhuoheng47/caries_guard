package com.cariesguard.image.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.image.domain.model.AttachmentObjectRegistrationModel;
import com.cariesguard.image.domain.model.AttachmentUploadModel;
import com.cariesguard.image.domain.model.AttachmentViewModel;
import com.cariesguard.image.domain.model.ObjectStoreCommand;
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
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentAppService {

    private final ImageCommandRepository imageCommandRepository;
    private final ImageQueryRepository imageQueryRepository;
    private final ObjectStorageService objectStorageService;

    public AttachmentAppService(ImageCommandRepository imageCommandRepository,
                                ImageQueryRepository imageQueryRepository,
                                ObjectStorageService objectStorageService) {
        this.imageCommandRepository = imageCommandRepository;
        this.imageQueryRepository = imageQueryRepository;
        this.objectStorageService = objectStorageService;
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
                .orElseGet(() -> createCaseImageAttachment(operator, file, bytes.length, md5));
    }

    @Transactional
    public AttachmentUploadVO registerExternalObject(AttachmentObjectRegistrationModel model) {
        validateExternalObject(model);
        return imageCommandRepository.findAttachmentByObject(model.bucketName().trim(), model.objectKey().trim())
                .map(item -> new AttachmentUploadVO(
                        item.attachmentId(),
                        item.fileName(),
                        item.bucketName(),
                        item.objectKey(),
                        item.md5()))
                .orElseGet(() -> createExternalAttachment(model));
    }

    public AttachmentAccessVO createAccessUrl(Long attachmentId, HttpServletRequest request) {
        return createAccessUrl(attachmentId, (String) null);
    }

    public AttachmentAccessVO createAccessUrl(Long attachmentId, String ignoredBaseUrl) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        AttachmentViewModel attachment = loadAttachment(attachmentId);
        ensureOrgAccess(operator, attachment.orgId());
        return buildPresignedAccessUrl(attachment);
    }

    public AttachmentAccessVO createInternalAccessUrl(Long attachmentId) {
        return buildPresignedAccessUrl(loadAttachment(attachmentId));
    }

    public String resolveLocalStoragePath(String bucketName, String objectKey) {
        return null;
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

    private AttachmentUploadVO createCaseImageAttachment(AuthenticatedUser operator,
                                                        MultipartFile file,
                                                        long fileSizeBytes,
                                                        String md5) {
        long attachmentId = IdWorker.getId();
        StoredObject storedObject;
        try (InputStream inputStream = file.getInputStream()) {
            storedObject = objectStorageService.store(new ObjectStoreCommand(
                    "IMAGE",
                    "case-image",
                    String.valueOf(attachmentId),
                    file.getOriginalFilename(),
                    defaultContentType(file.getContentType()),
                    inputStream,
                    fileSizeBytes,
                    md5));
        } catch (IOException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR.code(), "Store attachment failed");
        }
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

    private AttachmentUploadVO createExternalAttachment(AttachmentObjectRegistrationModel model) {
        long attachmentId = model.attachmentId() == null ? IdWorker.getId() : model.attachmentId();
        String originalName = StringUtils.hasText(model.originalName()) ? model.originalName().trim() : fileNameFromObjectKey(model.objectKey());
        String fileName = fileNameFromObjectKey(model.objectKey());
        imageCommandRepository.createAttachment(new AttachmentUploadModel(
                attachmentId,
                trimOrDefault(model.bizModuleCode(), "ANALYSIS"),
                model.bizId(),
                trimOrDefault(model.fileCategoryCode(), "IMAGE"),
                fileName,
                originalName,
                model.bucketName().trim(),
                model.objectKey().trim(),
                defaultContentType(model.contentType()),
                fileExtension(originalName),
                model.fileSizeBytes(),
                trimToNull(model.md5()),
                "MINIO",
                trimOrDefault(model.visibilityCode(), "PRIVATE"),
                model.uploadUserId(),
                model.orgId(),
                trimOrDefault(model.status(), "ACTIVE"),
                trimToNull(model.remark()),
                model.operatorUserId()));
        return new AttachmentUploadVO(attachmentId, fileName, model.bucketName().trim(), model.objectKey().trim(), trimToNull(model.md5()));
    }

    private AttachmentAccessVO buildPresignedAccessUrl(AttachmentViewModel attachment) {
        try {
            String accessUrl = objectStorageService.presignGetObject(attachment.bucketName(), attachment.objectKey());
            long expireAt = System.currentTimeMillis() / 1000 + objectStorageService.defaultPresignExpireSeconds();
            return new AttachmentAccessVO(accessUrl, expireAt);
        } catch (IOException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR.code(), "Create object access URL failed");
        }
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

    private void validateExternalObject(AttachmentObjectRegistrationModel model) {
        if (model == null
                || !StringUtils.hasText(model.bucketName())
                || !StringUtils.hasText(model.objectKey())
                || model.orgId() == null) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "External object metadata is incomplete");
        }
    }

    private String sign(Long attachmentId, String bucketName, String objectKey, long expireAt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(objectStorageService.proxyAccessSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
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
        return StringUtils.hasText(contentType) ? contentType.trim() : "application/octet-stream";
    }

    private String fileExtension(String originalFileName) {
        if (!StringUtils.hasText(originalFileName) || !originalFileName.contains(".")) {
            return null;
        }
        return originalFileName.substring(originalFileName.lastIndexOf('.') + 1);
    }

    private String fileNameFromObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return "object.bin";
        }
        String normalized = objectKey.replace('\\', '/');
        int index = normalized.lastIndexOf('/');
        return index >= 0 ? normalized.substring(index + 1) : normalized;
    }

    private String trimOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void deleteStoredObjectQuietly(StoredObject storedObject) {
        try {
            objectStorageService.delete(storedObject.bucketName(), storedObject.objectKey());
        } catch (IOException ignored) {
            // Keep the original failure cause from attachment persistence.
        }
    }
}
