package com.cariesguard.image.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.image.domain.model.AttachmentOwnerCaseModel;
import com.cariesguard.image.domain.model.AttachmentViewModel;
import com.cariesguard.image.domain.model.ImageManagedModel;
import com.cariesguard.image.domain.model.ImageQualityCheckModel;
import com.cariesguard.image.domain.model.ImageViewModel;
import com.cariesguard.image.domain.repository.ImageCommandRepository;
import com.cariesguard.image.domain.repository.ImageQueryRepository;
import com.cariesguard.image.interfaces.command.CreateCaseImageCommand;
import com.cariesguard.image.interfaces.command.SaveImageQualityCheckCommand;
import com.cariesguard.image.interfaces.vo.CaseImageMutationVO;
import com.cariesguard.image.interfaces.vo.ImageQualityCheckVO;
import com.cariesguard.image.interfaces.vo.ImageVO;
import com.cariesguard.patient.app.CaseCommandAppService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CaseImageAppServiceTests {

    @Mock
    private ImageCommandRepository imageCommandRepository;

    @Mock
    private ImageQueryRepository imageQueryRepository;

    @Mock
    private CaseCommandAppService caseCommandAppService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCaseImageShouldClearPrimaryForSameType() {
        CaseImageAppService appService = new CaseImageAppService(imageCommandRepository, imageQueryRepository, new ObjectMapper(), caseCommandAppService);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(imageCommandRepository.findCase(6001L)).thenReturn(Optional.of(new AttachmentOwnerCaseModel(6001L, "CASE1", 4001L, 3001L, 2001L, "CREATED")));
        when(imageCommandRepository.findAttachment(7001L)).thenReturn(Optional.of(new AttachmentViewModel(
                7001L, "x.jpg", "orig.jpg", "caries-image", "attachments/x.jpg", "image/jpeg", "md5", 10L, "MINIO", 2001L)));
        when(imageCommandRepository.nextImageIndexNo(6001L, "PANORAMIC")).thenReturn(2);

        CaseImageMutationVO result = appService.createCaseImage(6001L, new CreateCaseImageCommand(
                7001L, 4001L, 3001L, "PANORAMIC", "UPLOAD", LocalDateTime.now(), "FULL_MOUTH", "1", null));

        assertThat(result.qualityStatusCode()).isEqualTo("PENDING");
        verify(imageCommandRepository).clearPrimaryFlag(6001L, "PANORAMIC", 1001L);
        verify(imageCommandRepository).createImage(any());
        verify(caseCommandAppService).transitionStatus(eq(6001L), any());
    }

    @Test
    void createCaseImageShouldRejectCaseMismatch() {
        CaseImageAppService appService = new CaseImageAppService(imageCommandRepository, imageQueryRepository, new ObjectMapper(), caseCommandAppService);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(imageCommandRepository.findCase(6001L)).thenReturn(Optional.of(new AttachmentOwnerCaseModel(6001L, "CASE1", 4001L, 3001L, 2001L, "QC_PENDING")));
        when(imageCommandRepository.findAttachment(7001L)).thenReturn(Optional.of(new AttachmentViewModel(
                7001L, "x.jpg", "orig.jpg", "caries-image", "attachments/x.jpg", "image/jpeg", "md5", 10L, "MINIO", 2001L)));

        assertThatThrownBy(() -> appService.createCaseImage(6001L, new CreateCaseImageCommand(
                7001L, 9999L, 3001L, "PANORAMIC", "UPLOAD", null, null, "0", null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void listCaseImagesShouldReturnViews() {
        CaseImageAppService appService = new CaseImageAppService(imageCommandRepository, imageQueryRepository, new ObjectMapper(), caseCommandAppService);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(imageCommandRepository.findCase(6001L)).thenReturn(Optional.of(new AttachmentOwnerCaseModel(6001L, "CASE1", 4001L, 3001L, 2001L, "QC_PENDING")));
        when(imageQueryRepository.listImagesByCaseId(6001L)).thenReturn(List.of(
                new ImageViewModel(8001L, 7001L, "orig.jpg", "caries-image", "attachments/x.jpg", "PANORAMIC", "UPLOAD", "PENDING", "1", null, "FULL_MOUTH")));
        when(imageQueryRepository.findCurrentQualityCheck(8001L)).thenReturn(Optional.of(new ImageQualityCheckModel(
                8001L, "AUTO", "PASS", 88, 85, 90, 92, 80, "[\"BLUR\"]", "ok", LocalDateTime.now())));

        List<ImageVO> result = appService.listCaseImages(6001L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).imageId()).isEqualTo(8001L);
        assertThat(result.get(0).currentQualityCheck()).isNotNull();
        assertThat(result.get(0).currentQualityCheck().checkResultCode()).isEqualTo("PASS");
    }

    @Test
    void getImageShouldReturnEnrichedView() {
        CaseImageAppService appService = new CaseImageAppService(imageCommandRepository, imageQueryRepository, new ObjectMapper(), caseCommandAppService);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(imageCommandRepository.findImage(8001L)).thenReturn(Optional.of(new ImageManagedModel(8001L, 6001L, 3001L, 7001L, 2001L)));
        when(imageQueryRepository.findImage(8001L)).thenReturn(Optional.of(
                new ImageViewModel(8001L, 7001L, "orig.jpg", "caries-image", "attachments/x.jpg", "PANORAMIC", "UPLOAD", "PASS", "1", null, "FULL_MOUTH")));
        when(imageQueryRepository.findCurrentQualityCheck(8001L)).thenReturn(Optional.of(new ImageQualityCheckModel(
                8001L, "AUTO", "PASS", 88, 85, 90, 92, 80, "[\"BLUR\"]", "ok", LocalDateTime.now())));

        ImageVO result = appService.getImage(8001L);

        assertThat(result.imageId()).isEqualTo(8001L);
        assertThat(result.currentQualityCheck()).isNotNull();
        assertThat(result.currentQualityCheck().issueCodes()).containsExactly("BLUR");
    }

    @Test
    void saveQualityCheckShouldPersistAndReturnCurrentCheck() {
        CaseImageAppService appService = new CaseImageAppService(imageCommandRepository, imageQueryRepository, new ObjectMapper(), caseCommandAppService);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(imageCommandRepository.findImage(8001L)).thenReturn(Optional.of(new ImageManagedModel(8001L, 6001L, 3001L, 7001L, 2001L)));
        when(imageQueryRepository.findCurrentQualityCheck(8001L)).thenReturn(Optional.of(new ImageQualityCheckModel(
                8001L, "AUTO", "PASS", 88, 85, 90, 92, 80, "[\"BLUR\"]", "ok", LocalDateTime.now())));

        ImageQualityCheckVO result = appService.saveQualityCheck(8001L, new SaveImageQualityCheckCommand(
                "AUTO", "PASS", 88, 85, 90, 92, 80, List.of("BLUR"), "ok", null));

        assertThat(result.checkResultCode()).isEqualTo("PASS");
        assertThat(result.issueCodes()).containsExactly("BLUR");
        verify(imageCommandRepository).saveQualityCheck(any());
    }

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }
}

