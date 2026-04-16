package com.cariesguard.image.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.image.domain.model.AttachmentObjectRegistrationModel;
import com.cariesguard.image.domain.model.AttachmentOwnerCaseModel;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentAppService {

    private static final String BIZ_CASE = "CASE";
    private static final String BIZ_ANALYSIS = "ANALYSIS";
    private static final String CATEGORY_RAW_IMAGE = "RAW_IMAGE";
    private static final String CATEGORY_VISUAL = "VISUAL";
    private static final String RETENTION_LONG_TERM = "LONG_TERM";
    private static final String RETENTION_VISUAL_MID_TERM = "VISUAL_180D";
    private static final Set<String> VISUAL_ASSET_TYPES = Set.of("HEATMAP", "MASK", "OVERLAY");

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
        return upload(file, null, null);
    }

    @Transactional
    public AttachmentUploadVO upload(MultipartFile file, Long caseId, String imageTypeCode) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "File is required");
        }
        if (caseId == null) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "caseId is required for raw image upload");
        }
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        AttachmentOwnerCaseModel medicalCase = imageCommandRepository.findCase(caseId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));
        ensureOrgAccess(operator, medicalCase.orgId());

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR.code(), "Read file failed");
        }
        String md5 = md5(bytes);
        String normalizedImageType = normalizeImageType(imageTypeCode);
        return imageCommandRepository.findAttachmentByMd5(
                        medicalCase.orgId(), md5, BIZ_CASE, medicalCase.caseId(), CATEGORY_RAW_IMAGE)
                .map(item -> new AttachmentUploadVO(
                        item.attachmentId(),
                        item.fileName(),
                        item.bucketName(),
                        item.objectKey(),
                        item.md5()))
                .orElseGet(() -> createCaseImageAttachment(operator, medicalCase, normalizedImageType, file, bytes.length, md5));
    }

    @Transactional
    public AttachmentUploadVO registerExternalObject(AttachmentObjectRegistrationModel model) {
        validateExternalObject(model);
        String sourceBucketName = model.bucketName().trim();
        String sourceObjectKey = model.objectKey().trim();
        return imageCommandRepository.findAttachmentByObject(sourceBucketName, sourceObjectKey)
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
                                                        AttachmentOwnerCaseModel medicalCase,
                                                        String imageTypeCode,
                                                        MultipartFile file,
                                                        long fileSizeBytes,
                                                        String md5) {
        long attachmentId = IdWorker.getId();
        StoredObject storedObject;
        try (InputStream inputStream = file.getInputStream()) {
            storedObject = objectStorageService.store(ObjectStoreCommand.rawImage(
                    "IMAGE",
                    medicalCase.orgId(),
                    medicalCase.caseNo(),
                    imageTypeCode,
                    attachmentId,
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
                    BIZ_CASE,
                    medicalCase.caseId(),
                    CATEGORY_RAW_IMAGE,
                    imageTypeCode,
                    null,
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
                    RETENTION_LONG_TERM,
                    null,
                    operator.getUserId(),
                    medicalCase.orgId(),
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
        String fileCategoryCode = normalizeFileCategory(model.fileCategoryCode());
        String assetTypeCode = normalizeAssetType(fileCategoryCode, model.assetTypeCode());
        String originalName = StringUtils.hasText(model.originalName()) ? model.originalName().trim() : fileNameFromObjectKey(model.objectKey());
        String contentType = defaultContentType(model.contentType());
        StoredObject storedObject = null;
        String bucketName = model.bucketName().trim();
        String objectKey = model.objectKey().trim();
        String fileName = fileNameFromObjectKey(objectKey);
        Long fileSizeBytes = model.fileSizeBytes();
        String md5 = trimToNull(model.md5());
        String storageProviderCode = "MINIO";

        if (CATEGORY_VISUAL.equals(fileCategoryCode)) {
            storedObject = storeCanonicalVisualObject(attachmentId, model, assetTypeCode, originalName, contentType);
            bucketName = storedObject.bucketName();
            objectKey = storedObject.objectKey();
            fileName = storedObject.fileName();
            fileSizeBytes = storedObject.fileSizeBytes();
            md5 = storedObject.md5();
            storageProviderCode = storedObject.providerCode();
        }

        try {
            imageCommandRepository.createAttachment(new AttachmentUploadModel(
                    attachmentId,
                    trimOrDefault(model.bizModuleCode(), BIZ_ANALYSIS),
                    model.bizId(),
                    fileCategoryCode,
                    assetTypeCode,
                    model.sourceAttachmentId(),
                    fileName,
                    originalName,
                    bucketName,
                    objectKey,
                    contentType,
                    fileExtension(originalName),
                    fileSizeBytes,
                    md5,
                    storageProviderCode,
                    trimOrDefault(model.visibilityCode(), "PRIVATE"),
                    defaultRetentionPolicy(fileCategoryCode, model.retentionPolicyCode()),
                    defaultExpiredAt(fileCategoryCode, model.expiredAt()),
                    model.uploadUserId(),
                    model.orgId(),
                    trimOrDefault(model.status(), "ACTIVE"),
                    trimToNull(model.remark()),
                    model.operatorUserId()));
        } catch (RuntimeException exception) {
            deleteStoredObjectQuietly(storedObject);
            throw exception;
        }
        return new AttachmentUploadVO(attachmentId, fileName, bucketName, objectKey, md5);
    }

    private StoredObject storeCanonicalVisualObject(Long attachmentId,
                                                    AttachmentObjectRegistrationModel model,
                                                    String assetTypeCode,
                                                    String originalName,
                                                    String contentType) {
        try {
            StoredObjectResource source = objectStorageService.load(
                    model.bucketName().trim(),
                    model.objectKey().trim(),
                    originalName,
                    contentType);
            byte[] bytes;
            try (InputStream inputStream = source.resource().getInputStream()) {
                bytes = inputStream.readAllBytes();
            }
            return objectStorageService.store(ObjectStoreCommand.visualAsset(
                    "VISUAL",
                    model.orgId(),
                    model.caseNo(),
                    model.taskNo(),
                    model.modelVersion(),
                    assetTypeCode,
                    model.relatedImageId(),
                    model.toothCode(),
                    attachmentId,
                    originalName,
                    source.contentType(),
                    new ByteArrayInputStream(bytes),
                    bytes.length,
                    trimOrDefault(model.md5(), md5(bytes))));
        } catch (IOException exception) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR.code(), "Register visual asset object failed");
        }
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
        if (CATEGORY_VISUAL.equals(normalizeFileCategory(model.fileCategoryCode()))
                && (!StringUtils.hasText(model.caseNo())
                || !StringUtils.hasText(model.taskNo())
                || !StringUtils.hasText(model.modelVersion())
                || !StringUtils.hasText(model.assetTypeCode()))) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "Visual object key context is incomplete");
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

    private String normalizeFileCategory(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : CATEGORY_VISUAL;
    }

    private String normalizeImageType(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "PANORAMIC";
    }

    private String normalizeAssetType(String fileCategoryCode, String value) {
        if (CATEGORY_VISUAL.equals(fileCategoryCode)) {
            String assetType = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
            if (!VISUAL_ASSET_TYPES.contains(assetType)) {
                throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "Visual assetTypeCode is invalid");
            }
            return assetType;
        }
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String defaultRetentionPolicy(String fileCategoryCode, String value) {
        if (StringUtils.hasText(value)) {
            return value.trim().toUpperCase(Locale.ROOT);
        }
        return CATEGORY_VISUAL.equals(fileCategoryCode) ? RETENTION_VISUAL_MID_TERM : RETENTION_LONG_TERM;
    }

    private LocalDateTime defaultExpiredAt(String fileCategoryCode, LocalDateTime expiredAt) {
        if (expiredAt != null) {
            return expiredAt;
        }
        return CATEGORY_VISUAL.equals(fileCategoryCode) ? LocalDateTime.now().plusDays(180) : null;
    }

    private String trimOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void deleteStoredObjectQuietly(StoredObject storedObject) {
        if (storedObject == null) {
            return;
        }
        try {
            objectStorageService.delete(storedObject.bucketName(), storedObject.objectKey());
        } catch (IOException ignored) {
            // Keep the original failure cause from metadata persistence.
        }
    }
}
