package com.cariesguard.report.domain.repository;

import com.cariesguard.report.domain.model.ReportAttachmentCreateModel;
import com.cariesguard.report.domain.model.ReportAttachmentModel;
import com.cariesguard.report.domain.model.ReportGenerateModel;
import com.cariesguard.report.domain.model.ReportRecordModel;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportRecordRepository {

    int nextVersionNo(Long caseId, String reportTypeCode);

    void create(ReportGenerateModel model);

    void updateArchiveInfo(Long reportId,
                           Long attachmentId,
                           String reportStatusCode,
                           LocalDateTime generatedAt,
                           Long operatorUserId);

    Optional<ReportRecordModel> findById(Long reportId);

    List<ReportRecordModel> listByCaseId(Long caseId);

    void createAttachment(ReportAttachmentCreateModel model);

    Optional<ReportAttachmentModel> findAttachment(Long attachmentId);
}

