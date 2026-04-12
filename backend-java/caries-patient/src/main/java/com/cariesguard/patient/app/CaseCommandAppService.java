package com.cariesguard.patient.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.patient.domain.model.CaseCreateModel;
import com.cariesguard.patient.domain.model.CaseManagedModel;
import com.cariesguard.patient.domain.model.CaseStatusLogCreateModel;
import com.cariesguard.patient.domain.model.CaseStatusUpdateModel;
import com.cariesguard.patient.domain.model.VisitOwnedModel;
import com.cariesguard.patient.domain.repository.VisitCaseCommandRepository;
import com.cariesguard.patient.interfaces.command.CaseStatusTransitionCommand;
import com.cariesguard.patient.interfaces.command.CreateCaseCommand;
import com.cariesguard.patient.interfaces.vo.CaseMutationVO;
import com.cariesguard.patient.interfaces.vo.CaseStatusTransitionVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CaseCommandAppService {

    private static final DateTimeFormatter CASE_NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final VisitCaseCommandRepository visitCaseCommandRepository;
    private final CaseStatusMachine caseStatusMachine;

    public CaseCommandAppService(VisitCaseCommandRepository visitCaseCommandRepository,
                                 CaseStatusMachine caseStatusMachine) {
        this.visitCaseCommandRepository = visitCaseCommandRepository;
        this.caseStatusMachine = caseStatusMachine;
    }

    @Transactional
    public CaseMutationVO createCase(CreateCaseCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        VisitOwnedModel visit = visitCaseCommandRepository.findVisit(command.visitId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Visit does not exist"));
        ensureOrgAccess(operator, visit.orgId());
        if (!visit.patientId().equals(command.patientId())) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Visit does not belong to patient");
        }

        long caseId = IdWorker.getId();
        String caseStatusCode = "CREATED";
        CaseCreateModel model = new CaseCreateModel(
                caseId,
                buildCaseNo(caseId),
                visit.visitId(),
                visit.patientId(),
                command.caseTitle(),
                defaultCaseType(command.caseTypeCode()),
                caseStatusCode,
                defaultPriority(command.priorityCode()),
                command.chiefComplaint(),
                command.clinicalNotes(),
                command.onsetDate(),
                visit.doctorUserId(),
                operator.getUserId(),
                "0",
                "0",
                visit.orgId(),
                defaultStatus(command.status()),
                command.remark(),
                operator.getUserId());
        CaseStatusLogCreateModel statusLog = new CaseStatusLogCreateModel(
                IdWorker.getId(),
                caseId,
                null,
                caseStatusCode,
                operator.getUserId(),
                "CASE_CREATED",
                defaultReasonRemark(command.remark(), "Case created"),
                LocalDateTime.now(),
                visit.orgId());
        visitCaseCommandRepository.createCase(model, statusLog);
        return new CaseMutationVO(model.caseId(), model.caseNo(), model.caseStatusCode());
    }

    @Transactional
    public CaseStatusTransitionVO transitionStatus(Long caseId, CaseStatusTransitionCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        CaseManagedModel managedCase = visitCaseCommandRepository.findManagedCase(caseId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));
        ensureOrgAccess(operator, managedCase.orgId());
        validateTransitionCommand(command);
        String targetStatus = command.targetStatusCode().trim();

        caseStatusMachine.ensureTransitionAllowed(managedCase.caseStatusCode(), targetStatus);
        validateTransitionPreconditions(operator, managedCase, targetStatus);

        LocalDateTime operationTime = LocalDateTime.now();
        visitCaseCommandRepository.updateCaseStatus(new CaseStatusUpdateModel(
                managedCase.caseId(),
                targetStatus,
                resolveReportReadyFlag(managedCase.reportReadyFlag(), targetStatus),
                resolveFollowupFlag(managedCase.followupRequiredFlag(), targetStatus),
                resolveClosedAt(targetStatus, operationTime),
                operator.getUserId()));
        visitCaseCommandRepository.appendCaseStatusLog(new CaseStatusLogCreateModel(
                IdWorker.getId(),
                managedCase.caseId(),
                managedCase.caseStatusCode(),
                targetStatus,
                operator.getUserId(),
                command.reasonCode().trim(),
                command.reasonRemark(),
                operationTime,
                managedCase.orgId()));
        return new CaseStatusTransitionVO(managedCase.caseId(), managedCase.caseStatusCode(), targetStatus);
    }

    private void validateTransitionPreconditions(AuthenticatedUser operator, CaseManagedModel managedCase, String targetStatus) {
        if ("ANALYZING".equals(targetStatus) && !visitCaseCommandRepository.hasActiveImage(managedCase.caseId())) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case has no active image for analyzing");
        }
        if ("REVIEW_PENDING".equals(targetStatus) && !visitCaseCommandRepository.hasAiSummary(managedCase.caseId())) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case has no AI summary for review");
        }
        if (Set.of("REPORT_READY", "FOLLOWUP_REQUIRED", "CLOSED").contains(targetStatus)
                && !operator.hasAnyRole("DOCTOR", "ADMIN", "SYS_ADMIN")) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    private void validateTransitionCommand(CaseStatusTransitionCommand command) {
        if (!StringUtils.hasText(command.reasonCode())) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case transition reason code is required");
        }
    }

    private String resolveReportReadyFlag(String currentFlag, String targetStatus) {
        if ("REPORT_READY".equals(targetStatus)) {
            return "1";
        }
        return currentFlag;
    }

    private String resolveFollowupFlag(String currentFlag, String targetStatus) {
        if ("FOLLOWUP_REQUIRED".equals(targetStatus)) {
            return "1";
        }
        return currentFlag;
    }

    private LocalDateTime resolveClosedAt(String targetStatus, LocalDateTime operationTime) {
        return Set.of("CLOSED", "CANCELLED").contains(targetStatus) ? operationTime : null;
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    private String buildCaseNo(long caseId) {
        return "CASE" + LocalDate.now().format(CASE_NO_DATE_FORMATTER) + String.format("%06d", caseId % 1_000_000);
    }

    private String defaultCaseType(String caseTypeCode) {
        return StringUtils.hasText(caseTypeCode) ? caseTypeCode.trim() : "CARIES_SCREENING";
    }

    private String defaultPriority(String priorityCode) {
        return StringUtils.hasText(priorityCode) ? priorityCode.trim() : "NORMAL";
    }

    private String defaultStatus(String status) {
        return StringUtils.hasText(status) ? status.trim() : "ACTIVE";
    }

    private String defaultReasonRemark(String remark, String fallback) {
        return StringUtils.hasText(remark) ? remark.trim() : fallback;
    }
}
