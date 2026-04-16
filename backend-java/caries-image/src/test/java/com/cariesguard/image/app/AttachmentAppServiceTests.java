package com.cariesguard.image.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.image.domain.model.AttachmentDuplicateModel;
import com.cariesguard.image.domain.model.AttachmentOwnerCaseModel;
import com.cariesguard.image.domain.model.AttachmentViewModel;
import com.cariesguard.image.domain.model.ObjectStoreCommand;
import com.cariesguard.image.domain.model.StoredObject;
import com.cariesguard.image.domain.model.StoredObjectResource;
import com.cariesguard.image.domain.repository.ImageCommandRepository;
import com.cariesguard.image.domain.repository.ImageQueryRepository;
import com.cariesguard.image.domain.service.ObjectStorageService;
import com.cariesguard.image.interfaces.vo.AttachmentAccessVO;
import com.cariesguard.image.interfaces.vo.AttachmentUploadVO;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AttachmentAppServiceTests {

    @Mock
    private ImageCommandRepository imageCommandRepository;

    @Mock
    private ImageQueryRepository imageQueryRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadShouldReuseAttachmentByMd5() {
        AttachmentAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(imageCommandRepository.findCase(3001L)).thenReturn(Optional.of(
                new AttachmentOwnerCaseModel(3001L, "CASE202604160001", 5001L, 4001L, 2001L, "CREATED")));
        when(imageCommandRepository.findAttachmentByMd5(any(), any(), any(), any(), any())).thenReturn(Optional.of(
                new AttachmentDuplicateModel(3001L, "abc.jpg", "a.jpg", "caries-image", "case-image/1", "md5", "image/jpeg", 10L)));

        AttachmentUploadVO result = appService.upload(new MockMultipartFile(
                "file", "a.jpg", "image/jpeg", "abc".getBytes(StandardCharsets.UTF_8)), 3001L, "PANORAMIC");

        assertThat(result.attachmentId()).isEqualTo(3001L);
        verify(imageCommandRepository).findAttachmentByMd5(any(), any(), any(), any(), any());
    }

    @Test
    void uploadShouldStoreAndPersistNewAttachment() throws Exception {
        AttachmentAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(imageCommandRepository.findCase(3001L)).thenReturn(Optional.of(
                new AttachmentOwnerCaseModel(3001L, "CASE202604160001", 5001L, 4001L, 2001L, "CREATED")));
        when(imageCommandRepository.findAttachmentByMd5(any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        when(objectStorageService.store(any(ObjectStoreCommand.class))).thenReturn(
                new StoredObject("caries-image", "case-image/2026/04/12/3001/x.jpg", "x.jpg", "image/jpeg", 3L, "md5", "MINIO"));

        AttachmentUploadVO result = appService.upload(new MockMultipartFile(
                "file", "a.jpg", "image/jpeg", "abc".getBytes(StandardCharsets.UTF_8)), 3001L, "PANORAMIC");

        assertThat(result.bucketName()).isEqualTo("caries-image");
        verify(imageCommandRepository).createAttachment(any());
    }

    @Test
    void uploadShouldDeleteStoredObjectWhenPersistenceFails() throws Exception {
        AttachmentAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(imageCommandRepository.findCase(3001L)).thenReturn(Optional.of(
                new AttachmentOwnerCaseModel(3001L, "CASE202604160001", 5001L, 4001L, 2001L, "CREATED")));
        when(imageCommandRepository.findAttachmentByMd5(any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        when(objectStorageService.store(any(ObjectStoreCommand.class))).thenReturn(
                new StoredObject("caries-image", "case-image/2026/04/12/3001/x.jpg", "x.jpg", "image/jpeg", 3L, "md5", "MINIO"));
        doThrow(new IllegalStateException("db failed")).when(imageCommandRepository).createAttachment(any());

        assertThatThrownBy(() -> appService.upload(new MockMultipartFile(
                "file", "a.jpg", "image/jpeg", "abc".getBytes(StandardCharsets.UTF_8)), 3001L, "PANORAMIC"))
                .isInstanceOf(IllegalStateException.class);

        verify(objectStorageService).delete("caries-image", "case-image/2026/04/12/3001/x.jpg");
    }

    @Test
    void createAccessUrlShouldReturnMinioPresignedUrl() throws Exception {
        AttachmentAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(imageQueryRepository.findAttachment(3001L)).thenReturn(Optional.of(
                new AttachmentViewModel(3001L, "x.jpg", "orig.jpg", "caries-image", "case-image/x.jpg", "image/jpeg", "md5", 10L, "MINIO", 2001L)));
        when(objectStorageService.presignGetObject("caries-image", "case-image/x.jpg")).thenReturn("http://127.0.0.1:9000/caries-image/case-image/x.jpg?X-Amz-Signature=test");
        when(objectStorageService.defaultPresignExpireSeconds()).thenReturn(900L);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/files/3001/access-url");
        AttachmentAccessVO result = appService.createAccessUrl(3001L, request);

        assertThat(result.accessUrl()).contains("X-Amz-Signature=test");
        assertThat(result.expireAt()).isGreaterThan(Instant.now().getEpochSecond());
    }

    @Test
    void loadContentShouldRejectInvalidSignature() {
        AttachmentAppService appService = createService();
        when(imageQueryRepository.findAttachment(3001L)).thenReturn(Optional.of(
                new AttachmentViewModel(3001L, "x.jpg", "orig.jpg", "caries-image", "case-image/x.jpg", "image/jpeg", "md5", 10L, "MINIO", 2001L)));
        when(objectStorageService.proxyAccessSecret()).thenReturn("unit-test-image-secret");

        assertThatThrownBy(() -> appService.loadContent(3001L, Instant.now().plusSeconds(60).getEpochSecond(), "bad-signature"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void loadContentShouldReturnStoredResourceForLegacyProxyUrl() throws Exception {
        AttachmentAppService appService = createService();
        AttachmentViewModel attachment = new AttachmentViewModel(
                3001L, "x.jpg", "orig.jpg", "caries-image", "case-image/x.jpg", "image/jpeg", "md5", 10L, "MINIO", 2001L);
        long expireAt = Instant.now().plusSeconds(60).getEpochSecond();
        when(imageQueryRepository.findAttachment(3001L)).thenReturn(Optional.of(attachment));
        when(objectStorageService.proxyAccessSecret()).thenReturn("unit-test-image-secret");
        when(objectStorageService.load("caries-image", "case-image/x.jpg", "orig.jpg", "image/jpeg")).thenReturn(
                new StoredObjectResource(new ByteArrayResource("abc".getBytes(StandardCharsets.UTF_8)), "image/jpeg", "orig.jpg", 3L));

        StoredObjectResource result = appService.loadContent(3001L, expireAt, sign(3001L, "caries-image", "case-image/x.jpg", expireAt));

        assertThat(result.contentLength()).isEqualTo(3L);
        verify(objectStorageService).load("caries-image", "case-image/x.jpg", "orig.jpg", "image/jpeg");
    }

    private AttachmentAppService createService() {
        return new AttachmentAppService(imageCommandRepository, imageQueryRepository, objectStorageService);
    }

    private String sign(Long attachmentId, String bucketName, String objectKey, long expireAt) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("unit-test-image-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String payload = attachmentId + ":" + bucketName + ":" + objectKey + ":" + expireAt;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }
}
