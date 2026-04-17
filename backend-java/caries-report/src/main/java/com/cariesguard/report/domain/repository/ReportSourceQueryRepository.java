package com.cariesguard.report.domain.repository;

import com.cariesguard.report.domain.model.ReportAnalysisSummaryModel;
import com.cariesguard.report.domain.model.ReportCaseModel;
import com.cariesguard.report.domain.model.ReportCorrectionModel;
import com.cariesguard.report.domain.model.ReportImageModel;
import com.cariesguard.report.domain.model.ReportRiskAssessmentModel;
import com.cariesguard.report.domain.model.ReportToothRecordModel;
import com.cariesguard.report.domain.model.ReportVisualAssetModel;
import java.util.List;
import java.util.Optional;

public interface ReportSourceQueryRepository {

    Optional<ReportCaseModel> findCase(Long caseId);

    List<ReportImageModel> listCaseImages(Long caseId);

    List<ReportToothRecordModel> listToothRecords(Long caseId);

    Optional<ReportAnalysisSummaryModel> findLatestSummary(Long caseId);

    List<ReportVisualAssetModel> listVisualAssetsByTaskId(Long taskId);

    Optional<ReportRiskAssessmentModel> findLatestRiskAssessment(Long caseId);

    Optional<ReportCorrectionModel> findLatestCorrection(Long caseId);

    List<ReportCorrectionModel> listCorrections(Long caseId);
}
