package com.cariesguard.patient.app;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.patient.domain.model.CaseDetailModel;
import com.cariesguard.patient.domain.model.CaseDiagnosisModel;
import com.cariesguard.patient.domain.model.CaseImageModel;
import com.cariesguard.patient.domain.model.CaseSummaryModel;
import com.cariesguard.patient.domain.model.PageQueryResult;
import com.cariesguard.patient.domain.repository.CaseQueryRepository;
import com.cariesguard.patient.interfaces.vo.CaseDetailVO;
import com.cariesguard.patient.interfaces.vo.CaseDiagnosisVO;
import com.cariesguard.patient.interfaces.vo.CaseImageVO;
import com.cariesguard.patient.interfaces.vo.CaseListItemVO;
import com.cariesguard.patient.interfaces.vo.PageResultVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class CaseQueryAppService {

    private final CaseQueryRepository caseQueryRepository;
    private final ObjectMapper objectMapper;

    public CaseQueryAppService(CaseQueryRepository caseQueryRepository, ObjectMapper objectMapper) {
        this.caseQueryRepository = caseQueryRepository;
        this.objectMapper = objectMapper;
    }

    public CaseDetailVO getCase(Long caseId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        CaseDetailModel caseDetail = caseQueryRepository.findCaseDetail(caseId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !caseDetail.orgId().equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        return new CaseDetailVO(
                caseDetail.caseId(),
                caseDetail.caseNo(),
                caseDetail.patientId(),
                caseDetail.visitId(),
                caseDetail.caseStatusCode(),
                caseDetail.reportReadyFlag(),
                caseDetail.followupRequiredFlag(),
                caseDetail.images().stream().map(this::toImageVO).toList(),
                caseDetail.diagnoses().stream().map(this::toDiagnosisVO).toList(),
                readSummary(caseDetail.latestAiSummaryRaw()));
    }

    private CaseImageVO toImageVO(CaseImageModel model) {
        return new CaseImageVO(model.imageId(), model.imageTypeCode(), model.qualityStatusCode(), model.primaryFlag());
    }

    private CaseDiagnosisVO toDiagnosisVO(CaseDiagnosisModel model) {
        return new CaseDiagnosisVO(model.diagnosisName(), model.severityCode(), model.finalFlag());
    }

    private JsonNode readSummary(String rawSummary) {
        if (rawSummary == null) {
            return null;
        }
        try {
            return objectMapper.readTree(rawSummary);
        } catch (Exception exception) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "AI summary payload is invalid");
        }
    }

    public PageResultVO<CaseListItemVO> pageCases(int pageNo,
                                                  int pageSize,
                                                  Long patientId,
                                                  String caseStatusCode,
                                                  Long attendingDoctorId) {
        PageQueryResult<CaseSummaryModel> result = caseQueryRepository.pageCases(
                currentOrgScope(),
                pageNo,
                pageSize,
                patientId,
                caseStatusCode,
                attendingDoctorId);
        return new PageResultVO<>(
                result.records().stream().map(this::toCaseListItemVO).toList(),
                result.total(),
                result.pageNo(),
                result.pageSize());
    }

    private CaseListItemVO toCaseListItemVO(CaseSummaryModel model) {
        return new CaseListItemVO(
                model.caseId(),
                model.caseNo(),
                model.patientId(),
                model.visitId(),
                model.caseTitle(),
                model.caseStatusCode(),
                model.priorityCode(),
                model.attendingDoctorId(),
                model.reportReadyFlag(),
                model.followupRequiredFlag(),
                model.createdAt());
    }

    private Long currentOrgScope() {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        return operator.hasAnyRole("ADMIN", "SYS_ADMIN") ? null : operator.getOrgId();
    }
}
