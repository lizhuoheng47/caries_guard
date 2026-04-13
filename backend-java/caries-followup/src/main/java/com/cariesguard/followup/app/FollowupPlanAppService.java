package com.cariesguard.followup.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.followup.domain.model.FollowupCaseModel;
import com.cariesguard.followup.domain.model.FupPlanCreateModel;
import com.cariesguard.followup.domain.model.FupPlanModel;
import com.cariesguard.followup.domain.model.FupTaskCreateModel;
import com.cariesguard.followup.domain.repository.FollowupCaseRepository;
import com.cariesguard.followup.domain.repository.FupPlanRepository;
import com.cariesguard.followup.domain.repository.FupTaskRepository;
import com.cariesguard.followup.domain.service.FollowupDomainService;
import com.cariesguard.followup.interfaces.command.CreateFollowupPlanCommand;
import com.cariesguard.followup.interfaces.vo.FollowupPlanVO;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowupPlanAppService {

    private final FupPlanRepository fupPlanRepository;
    private final FupTaskRepository fupTaskRepository;
    private final FollowupDomainService followupDomainService;
    private final FollowupCaseRepository followupCaseRepository;

    public FollowupPlanAppService(FupPlanRepository fupPlanRepository,
                                  FupTaskRepository fupTaskRepository,
                                  FollowupDomainService followupDomainService,
                                  FollowupCaseRepository followupCaseRepository) {
        this.fupPlanRepository = fupPlanRepository;
        this.fupTaskRepository = fupTaskRepository;
        this.followupDomainService = followupDomainService;
        this.followupCaseRepository = followupCaseRepository;
    }

    @Transactional
    public FollowupPlanVO createPlan(Long caseId, CreateFollowupPlanCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        FollowupCaseModel medicalCase = followupCaseRepository.findCase(caseId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case not found"));
        ensureOrgAccess(operator, medicalCase.orgId());

        int intervalDays = command.intervalDays() != null && command.intervalDays() > 0
                ? command.intervalDays() : 30;
        String planTypeCode = command.planTypeCode() != null ? command.planTypeCode() : "ROUTINE";

        long planId = IdWorker.getId();
        String planNo = followupDomainService.buildPlanNo(planId);

        fupPlanRepository.create(new FupPlanCreateModel(
                planId,
                planNo,
                caseId,
                medicalCase.patientId(),
                planTypeCode,
                "ACTIVE",
                followupDomainService.resolveFirstTaskDueDate(intervalDays),
                intervalDays,
                command.ownerUserId(),
                "DOCTOR_MANUAL",
                null,
                medicalCase.orgId(),
                command.remark(),
                operator.getUserId()));

        // 派生首个随访任务
        long taskId = IdWorker.getId();
        fupTaskRepository.create(new FupTaskCreateModel(
                taskId,
                followupDomainService.buildTaskNo(taskId),
                planId,
                caseId,
                medicalCase.patientId(),
                "FOLLOW_CONTACT",
                "TODO",
                command.ownerUserId(),
                followupDomainService.resolveFirstTaskDueDate(intervalDays),
                medicalCase.orgId(),
                null,
                operator.getUserId()));

        return fupPlanRepository.findById(planId)
                .map(this::toVO)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.SYSTEM_ERROR));
    }

    @Transactional
    public void cancelPlan(Long planId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        FupPlanModel plan = fupPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Followup plan not found"));
        ensureOrgAccess(operator, plan.orgId());
        if ("DONE".equals(plan.planStatusCode()) || "CANCELLED".equals(plan.planStatusCode())) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Plan is already in terminal status");
        }
        fupPlanRepository.updateStatus(planId, "CANCELLED", operator.getUserId());
    }

    @Transactional
    public void closePlan(Long planId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        FupPlanModel plan = fupPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Followup plan not found"));
        ensureOrgAccess(operator, plan.orgId());
        fupPlanRepository.updateStatus(planId, "DONE", operator.getUserId());
    }

    public FollowupPlanVO getPlan(Long planId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        FupPlanModel plan = fupPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Followup plan not found"));
        ensureOrgAccess(operator, plan.orgId());
        return toVO(plan);
    }

    public List<FollowupPlanVO> listCasePlans(Long caseId) {
        return fupPlanRepository.listByCaseId(caseId).stream().map(this::toVO).toList();
    }

    private FollowupPlanVO toVO(FupPlanModel model) {
        return new FollowupPlanVO(
                model.planId(),
                model.planNo(),
                model.caseId(),
                model.patientId(),
                model.planTypeCode(),
                model.planStatusCode(),
                model.nextFollowupDate(),
                model.intervalDays(),
                model.ownerUserId(),
                model.triggerSourceCode(),
                model.triggerRefId(),
                model.remark(),
                model.createdAt());
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
