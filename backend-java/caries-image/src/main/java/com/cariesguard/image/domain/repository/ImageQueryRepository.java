package com.cariesguard.image.domain.repository;

import com.cariesguard.image.domain.model.AttachmentViewModel;
import com.cariesguard.image.domain.model.ImageQualityCheckModel;
import com.cariesguard.image.domain.model.ImageViewModel;
import java.util.List;
import java.util.Optional;

public interface ImageQueryRepository {

    Optional<AttachmentViewModel> findAttachment(Long attachmentId);

    Optional<ImageViewModel> findImage(Long imageId);

    List<ImageViewModel> listImagesByCaseId(Long caseId);

    Optional<ImageQualityCheckModel> findCurrentQualityCheck(Long imageId);
}
