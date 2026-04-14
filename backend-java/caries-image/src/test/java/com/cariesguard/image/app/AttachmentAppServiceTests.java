package com.cariesguard.image.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.image.config.ImageStorageProperties;
import com.cariesguard.image.domain.model.AttachmentDuplicateModel;
import com.cariesguard.image.domain.model.AttachmentViewModel;
import com.cariesguard.image.domain.model.StoredObject;
import com.cariesguard.image.domain.model.StoredObjectResource;
import com.cariesguard.image.domain.repository.ImageCommandRepository;
import com.cariesguard.image.domain.repository.ImageQueryRepository;
import com.cariesguard.image.domain.service.ObjectStorageService;
import com.cariesguard.image.interfaces.vo.AttachmentAccessVO;
import com.cariesguard.image.interfaces.vo.AttachmentUploadVO;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

    private ImageStorageProperties imageStorageProperties;

    @BeforeEach
    void setUp() {
        imageStorageProperties = new ImageStorageProperties();
        imageStorageProperties.setAccessUrlSecret("unit-test-image-secret");
        imageStorageProperties.setAccessUrlExpireSeconds(900);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadShouldReuseAttachmentByMd5() {
        AttachmentAppService appService = new AttachmentAppService(
                imageCommandRepository,
                imageQueryRepository,
                objectStorageService,
                imageStorageProperties);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(imageCommandRepository.findAttachmentByMd5(any(), any())).thenReturn(Optional.of(
                new AttachmentDuplicateModel(3001L, "abc.jpg", "a.jpg", "caries-image", "attachments/1", "md5", "image/jpeg", 10L)));

        AttachmentUploadVO result = appService.upload(new MockMultipartFile(
                "file", "a.jpg", "image/jpeg", "abc".getBytes(StandardCharsets.UTF_8)));

        assertThat(result.attachmentId()).isEqualTo(3001L);
        verify(imageCommandRepository).findAttachmentByMd5(any(), any());
    }

    @Test
    void uploadShouldStoreAndPersistNewAttachment() throws Exception {
        AttachmentAppService appService = new AttachmentAppService(
                imageCommandRepository,
                imageQueryRepository,
                objectStorageService,
                imageStorageProperties);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(imageCommandRepository.findAttachmentByMd5(any(), any())).thenReturn(Optional.empty());
        when(objectStorageService.store(any(), any(), any(), any(Long.class), any())).thenReturn(
                new StoredObject("caries-image", "attachments/2026/04/12/x.jpg", "x.jpg", "image/jpeg", 3L, "md5", "MINIO"));

        AttachmentUploadVO result = appService.upload(new MockMultipartFile(
                "file", "a.jpg", "image/jpeg", "abc".getBytes(StandardCharsets.UTF_8)));

        assertThat(result.bucketName()).isEqualTo("caries-image");
        verify(imageCommandRepository).createAttachment(any());
    }

    @Test
    void uploadShouldDeleteStoredObjectWhenPersistenceFails() throws Exception {
        AttachmentAppService appService = new AttachmentAppService(
                imageCommandRepository,
                imageQueryRepository,
                objectStorageService,
                imageStorageProperties);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(imageCommandRepository.findAttachmentByMd5(any(), any())).thenReturn(Optional.empty());
        when(objectStorageService.store(any(), any(), any(), any(Long.class), any())).thenReturn(
                new StoredObject("caries-image", "attachments/2026/04/12/x.jpg", "x.jpg", "image/jpeg", 3L, "md5", "MINIO"));
        doThrow(new IllegalStateException("db failed")).when(imageCommandRepository).createAttachment(any());

        assertThatThrownBy(() -> appService.upload(new MockMultipartFile(
                "file", "a.jpg", "image/jpeg", "abc".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IllegalStateException.class);

        verify(objectStorageService).delete("caries-image", "attachments/2026/04/12/x.jpg");
    }

    @Test
    void createAccessUrlShouldReturnSignedUrl() {
        AttachmentAppService appService = new AttachmentAppService(
                imageCommandRepository,
                imageQueryRepository,
                objectStorageService,
                imageStorageProperties);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(imageQueryRepository.findAttachment(3001L)).thenReturn(Optional.of(
                new AttachmentViewModel(3001L, "x.jpg", "orig.jpg", "caries-image", "attachments/x.jpg", "image/jpeg", "md5", 10L, "MINIO", 2001L)));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/files/3001/access-url");
        request.setScheme("http");
        request.setServerName("127.0.0.1");
        request.setServerPort(8080);
        AttachmentAccessVO result = appService.createAccessUrl(3001L, request);

        assertThat(result.accessUrl()).contains("/api/v1/files/3001/content");
        assertThat(result.accessUrl()).contains("signature=");
        assertThat(result.expireAt()).isGreaterThan(Instant.now().getEpochSecond());
    }

    @Test
    void loadContentShouldRejectInvalidSignature() {
        AttachmentAppService appService = new AttachmentAppService(
                imageCommandRepository,
                imageQueryRepository,
                objectStorageService,
                imageStorageProperties);
        when(imageQueryRepository.findAttachment(3001L)).thenReturn(Optional.of(
                new AttachmentViewModel(3001L, "x.jpg", "orig.jpg", "caries-image", "attachments/x.jpg", "image/jpeg", "md5", 10L, "MINIO", 2001L)));

        assertThatThrownBy(() -> appService.loadContent(3001L, Instant.now().plusSeconds(60).getEpochSecond(), "bad-signature"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void loadContentShouldReturnStoredResource() throws Exception {
        AttachmentAppService appService = new AttachmentAppService(
                imageCommandRepository,
                imageQueryRepository,
                objectStorageService,
                imageStorageProperties);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        AttachmentViewModel attachment = new AttachmentViewModel(
                3001L, "x.jpg", "orig.jpg", "caries-image", "attachments/x.jpg", "image/jpeg", "md5", 10L, "MINIO", 2001L);
        when(imageQueryRepository.findAttachment(3001L)).thenReturn(Optional.of(attachment));
        when(objectStorageService.load("caries-image", "attachments/x.jpg", "orig.jpg", "image/jpeg")).thenReturn(
                new StoredObjectResource(new ByteArrayResource("abc".getBytes(StandardCharsets.UTF_8)), "image/jpeg", "orig.jpg", 3L));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/files/3001/access-url");
        request.setScheme("http");
        request.setServerName("127.0.0.1");
        request.setServerPort(8080);
        AttachmentAccessVO access = appService.createAccessUrl(3001L, request);
        String query = access.accessUrl().substring(access.accessUrl().indexOf('?') + 1);
        String[] parts = query.split("&");
        long expireAt = Long.parseLong(parts[0].split("=")[1]);
        String signature = parts[1].split("=")[1];

        StoredObjectResource result = appService.loadContent(3001L, expireAt, signature);

        assertThat(result.contentLength()).isEqualTo(3L);
        verify(objectStorageService).load("caries-image", "attachments/x.jpg", "orig.jpg", "image/jpeg");
    }

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }
}

