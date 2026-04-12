package com.cariesguard.patient.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.patient.domain.model.CaseDiagnosisCreateModel;
import com.cariesguard.patient.domain.model.CaseManagedModel;
import com.cariesguard.patient.domain.model.CaseToothRecordCreateModel;
import com.cariesguard.patient.domain.repository.VisitCaseCommandRepository;
import com.cariesguard.patient.interfaces.command.DiagnosisItemCommand;
import com.cariesguard.patient.interfaces.command.SaveCaseDiagnosesCommand;
import com.cariesguard.patient.interfaces.command.SaveCaseToothRecordsCommand;
import com.cariesguard.patient.interfaces.command.ToothRecordItemCommand;
import com.cariesguard.patient.interfaces.vo.CaseDiagnosisMutationVO;
import com.cariesguard.patient.interfaces.vo.CaseToothRecordMutationVO;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CaseClinicalRecordAppService {

    private final VisitCaseCommandRepository visitCaseCommandRepository;

    public CaseClinicalRecordAppService(VisitCaseCommandRepository visitCaseCommandRepository) {
        this.visitCaseCommandRepository = visitCaseCommandRepository;
    }

    @Transactional
    public CaseDiagnosisMutationVO saveDiagnoses(Long caseId, SaveCaseDiagnosesCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        CaseManagedModel managedCase = loadManagedCase(caseId);
        ensureOrgAccess(operator, managedCase.orgId());
        LocalDateTime reviewTime = LocalDateTime.now();
        List<CaseDiagnosisCreateModel> diagnoses = command.diagnoses().stream()
                .map(item -> toDiagnosisModel(caseId, managedCase.orgId(), operator, reviewTime, item))
                .toList();
        visitCaseCommandRepository.replaceDiagnoses(caseId, operator.getUserId(), reviewTime, diagnoses);
        return new CaseDiagnosisMutationVO(caseId, diagnoses.size());
    }

    @Transactional
    public CaseToothRecordMutationVO saveToothRecords(Long caseId, SaveCaseToothRecordsCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        CaseManagedModel managedCase = loadManagedCase(caseId);
        ensureOrgAccess(operator, managedCase.orgId());
        validateDuplicateToothRecords(command);
        LocalDateTime reviewTime = LocalDateTime.now();
        List<CaseToothRecordCreateModel> toothRecords = command.toothRecords().stream()
                .map(item -> toToothRecordModel(caseId, managedCase.orgId(), operator, reviewTime, item))
                .toList();
        visitCaseCommandRepository.replaceToothRecords(caseId, operator.getUserId(), toothRecords);
        return new CaseToothRecordMutationVO(caseId, toothRecords.size());
    }

    private CaseManagedModel loadManagedCase(Long caseId) {
        return visitCaseCommandRepository.findManagedCase(caseId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));
    }

    private CaseDiagnosisCreateModel toDiagnosisModel(Long caseId,
                                                      Long orgId,
                                                      AuthenticatedUser operator,
                                                      LocalDateTime reviewTime,
                                                      DiagnosisItemCommand item) {
        return new CaseDiagnosisCreateModel(
                IdWorker.getId(),
                caseId,
                defaultDiagnosisType(item.diagnosisTypeCode()),
                item.diagnosisName().trim(),
                trimToNull(item.severityCode()),
                trimToNull(item.diagnosisBasis()),
                trimToNull(item.diagnosisDesc()),
                trimToNull(item.treatmentAdvice()),
                operator.getUserId(),
                reviewTime,
                defaultFinalFlag(item.finalFlag()),
                orgId,
                "ACTIVE",
                trimToNull(item.remark()),
                operator.getUserId());
    }

    private CaseToothRecordCreateModel toToothRecordModel(Long caseId,
                                                          Long orgId,
                                                          AuthenticatedUser operator,
                                                          LocalDateTime reviewTime,
                                                          ToothRecordItemCommand item) {
        return new CaseToothRecordCreateModel(
                IdWorker.getId(),
                caseId,
                item.sourceImageId(),
                item.toothCode().trim(),
                trimToNull(item.toothSurfaceCode()),
                defaultIssueType(item.issueTypeCode()),
                trimToNull(item.severityCode()),
                trimToNull(item.findingDesc()),
                trimToNull(item.suggestion()),
                item.sortOrder() == null ? 0 : item.sortOrder(),
                operator.getUserId(),
                reviewTime,
                orgId,
                "ACTIVE",
                trimToNull(item.remark()),
                operator.getUserId());
    }

    private void validateDuplicateToothRecords(SaveCaseToothRecordsCommand command) {
        Set<String> uniqueKeys = new HashSet<>();
        for (ToothRecordItemCommand item : command.toothRecords()) {
            String key = item.toothCode().trim() + "|" + normalizeSurface(item.toothSurfaceCode()) + "|" + defaultIssueType(item.issueTypeCode());
            if (!uniqueKeys.add(key)) {
                throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Duplicate tooth record in request");
            }
        }
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    private String defaultDiagnosisType(String diagnosisTypeCode) {
        return StringUtils.hasText(diagnosisTypeCode) ? diagnosisTypeCode.trim() : "CARIES";
    }

    private String defaultFinalFlag(String finalFlag) {
        return StringUtils.hasText(finalFlag) ? finalFlag.trim() : "1";
    }

    private String defaultIssueType(String issueTypeCode) {
        return StringUtils.hasText(issueTypeCode) ? issueTypeCode.trim() : "CARIES";
    }

    private String normalizeSurface(String toothSurfaceCode) {
        return StringUtils.hasText(toothSurfaceCode) ? toothSurfaceCode.trim() : "";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
