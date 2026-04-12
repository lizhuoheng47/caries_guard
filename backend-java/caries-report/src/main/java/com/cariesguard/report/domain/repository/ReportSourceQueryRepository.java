package com.cariesguard.report.domain.repository;

import com.cariesguard.report.domain.model.ReportAnalysisSummaryModel;
import com.cariesguard.report.domain.model.ReportCaseModel;
import com.cariesguard.report.domain.model.ReportCorrectionModel;
import com.cariesguard.report.domain.model.ReportImageModel;
import com.cariesguard.report.domain.model.ReportRiskAssessmentModel;
import java.util.List;
import java.util.Optional;

public interface ReportSourceQueryRepository {

    Optional<ReportCaseModel> findCase(Long caseId);

    List<ReportImageModel> listCaseImages(Long caseId);

    Optional<ReportAnalysisSummaryModel> findLatestSummary(Long caseId);

    Optional<ReportRiskAssessmentModel> findLatestRiskAssessment(Long caseId);

    Optional<ReportCorrectionModel> findLatestCorrection(Long caseId);
}

