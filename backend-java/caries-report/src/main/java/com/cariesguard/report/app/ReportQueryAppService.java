package com.cariesguard.report.app;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.report.domain.model.ReportCaseModel;
import com.cariesguard.report.domain.model.ReportRecordModel;
import com.cariesguard.report.domain.repository.ReportRecordRepository;
import com.cariesguard.report.domain.repository.ReportSourceQueryRepository;
import com.cariesguard.report.interfaces.vo.ReportDetailVO;
import com.cariesguard.report.interfaces.vo.ReportListItemVO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReportQueryAppService {

    private final ReportRecordRepository reportRecordRepository;
    private final ReportSourceQueryRepository reportSourceQueryRepository;

    public ReportQueryAppService(ReportRecordRepository reportRecordRepository,
                                 ReportSourceQueryRepository reportSourceQueryRepository) {
        this.reportRecordRepository = reportRecordRepository;
        this.reportSourceQueryRepository = reportSourceQueryRepository;
    }

    public List<ReportListItemVO> listCaseReports(Long caseId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        ReportCaseModel medicalCase = reportSourceQueryRepository.findCase(caseId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));
        ensureOrgAccess(operator, medicalCase.orgId());
        return reportRecordRepository.listByCaseId(caseId).stream()
                .map(this::toListItemVO)
                .toList();
    }

    public ReportDetailVO getReport(Long reportId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        ReportRecordModel report = reportRecordRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Report does not exist"));
        ensureOrgAccess(operator, report.orgId());
        return new ReportDetailVO(
                report.reportId(),
                report.reportNo(),
                report.caseId(),
                report.patientId(),
                report.attachmentId(),
                report.reportTypeCode(),
                report.reportStatusCode(),
                report.versionNo(),
                report.summaryText(),
                report.generatedAt(),
                report.signedAt(),
                report.createdAt());
    }

    private ReportListItemVO toListItemVO(ReportRecordModel report) {
        return new ReportListItemVO(
                report.reportId(),
                report.reportNo(),
                report.reportTypeCode(),
                report.versionNo(),
                report.reportStatusCode(),
                report.attachmentId(),
                report.generatedAt(),
                report.createdAt());
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}

