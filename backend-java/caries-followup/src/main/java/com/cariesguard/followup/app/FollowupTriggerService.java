package com.cariesguard.followup.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.followup.domain.model.FupPlanCreateModel;
import com.cariesguard.followup.domain.model.FupTaskCreateModel;
import com.cariesguard.followup.domain.model.MsgNotifyCreateModel;
import com.cariesguard.followup.domain.repository.FupPlanRepository;
import com.cariesguard.followup.domain.repository.FupTaskRepository;
import com.cariesguard.followup.domain.repository.MsgNotifyRepository;
import com.cariesguard.followup.domain.service.FollowupDomainService;
import com.cariesguard.followup.interfaces.vo.FollowupTriggerResultVO;
import com.cariesguard.patient.app.CaseCommandAppService;
import com.cariesguard.patient.interfaces.command.CaseStatusTransitionCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 随访触发服务 — 统一承接来自 report/risk 的触发逻辑。
 * <p>
 * 所有自动触发必须经过此服务，不允许各模块自行建计划，以保证：
 * 1. 触发规则集中可维护；
 * 2. 幂等保护统一生效；
 * 3. 触发来源可解释。
 */
@Service
public class FollowupTriggerService {

    private static final Logger log = LoggerFactory.getLogger(FollowupTriggerService.class);

    private final FupPlanRepository fupPlanRepository;
    private final FupTaskRepository fupTaskRepository;
    private final MsgNotifyRepository msgNotifyRepository;
    private final FollowupDomainService followupDomainService;
    private final CaseCommandAppService caseCommandAppService;

    public FollowupTriggerService(FupPlanRepository fupPlanRepository,
                                  FupTaskRepository fupTaskRepository,
                                  MsgNotifyRepository msgNotifyRepository,
                                  FollowupDomainService followupDomainService,
                                  CaseCommandAppService caseCommandAppService) {
        this.fupPlanRepository = fupPlanRepository;
        this.fupTaskRepository = fupTaskRepository;
        this.msgNotifyRepository = msgNotifyRepository;
        this.followupDomainService = followupDomainService;
        this.caseCommandAppService = caseCommandAppService;
    }

    /**
     * 从报告触发随访。
     *
     * @param caseId              病例ID
     * @param patientId           患者ID
     * @param orgId               机构ID
     * @param reportId            报告ID（作为触发来源参考ID）
     * @param riskLevelCode       风险等级（HIGH 时触发）
     * @param reviewSuggestedFlag 复查建议标记（"1" 时触发）
     * @param recommendedCycleDays 推荐随访周期天数（可为 null）
     * @param operatorUserId      操作人ID
     * @return 触发结果
     */
    @Transactional
    public FollowupTriggerResultVO triggerFromReport(Long caseId,
                                                     Long patientId,
                                                     Long orgId,
                                                     Long reportId,
                                                     String riskLevelCode,
                                                     String reviewSuggestedFlag,
                                                     Integer recommendedCycleDays,
                                                     Long operatorUserId) {
        if (!followupDomainService.shouldTriggerFollowup(riskLevelCode, reviewSuggestedFlag)) {
            log.debug("Followup not triggered for caseId={}: riskLevel={}, reviewSuggested={}",
                    caseId, riskLevelCode, reviewSuggestedFlag);
            return FollowupTriggerResultVO.skipped("Risk level not HIGH and review not suggested");
        }

        String triggerSourceCode = followupDomainService.resolveTriggerSource(riskLevelCode, reviewSuggestedFlag);

        // 幂等保护：同一触发来源 + 同一来源ID 不重复建计划
        if (fupPlanRepository.existsActivePlan(caseId, triggerSourceCode, reportId)) {
            log.info("Followup plan already exists for caseId={}, source={}, refId={}, skipping",
                    caseId, triggerSourceCode, reportId);
            return FollowupTriggerResultVO.skipped("Followup plan already exists for this trigger");
        }

        int intervalDays = followupDomainService.resolveIntervalDays(riskLevelCode, recommendedCycleDays);
        String planTypeCode = followupDomainService.resolvePlanType(triggerSourceCode);

        // 建随访计划
        long planId = IdWorker.getId();
        String planNo = followupDomainService.buildPlanNo(planId);
        fupPlanRepository.create(new FupPlanCreateModel(
                planId,
                planNo,
                caseId,
                patientId,
                planTypeCode,
                "ACTIVE",
                followupDomainService.resolveFirstTaskDueDate(intervalDays),
                intervalDays,
                operatorUserId,
                triggerSourceCode,
                reportId,
                orgId,
                "Auto triggered from report",
                operatorUserId));

        // 派生首个随访任务
        long taskId = IdWorker.getId();
        String taskNo = followupDomainService.buildTaskNo(taskId);
        fupTaskRepository.create(new FupTaskCreateModel(
                taskId,
                taskNo,
                planId,
                caseId,
                patientId,
                "FOLLOW_CONTACT",
                "TODO",
                operatorUserId,
                followupDomainService.resolveFirstTaskDueDate(intervalDays),
                orgId,
                "First followup task",
                operatorUserId));

        // 更新病例状态为 FOLLOWUP_REQUIRED
        transitionCaseToFollowupRequired(caseId, orgId);

        // 消息留痕：任务创建提醒
        recordNotify(planId, taskId, orgId, operatorUserId,
                "随访任务已创建",
                "病例随访计划已建立，首个随访任务待处理，截止日期：" +
                        followupDomainService.resolveFirstTaskDueDate(intervalDays));

        log.info("Followup triggered: caseId={}, planId={}, taskId={}, source={}", caseId, planId, taskId, triggerSourceCode);
        return FollowupTriggerResultVO.triggered(planId, planNo, taskId, taskNo);
    }

    private void transitionCaseToFollowupRequired(Long caseId, Long orgId) {
        try {
            caseCommandAppService.transitionStatusAsSystem(caseId, orgId,
                    new CaseStatusTransitionCommand("FOLLOWUP_REQUIRED", "FOLLOWUP_TRIGGERED", "High risk followup triggered"));
        } catch (Exception e) {
            // 病例状态迁移失败不阻断随访计划创建（可能已是 FOLLOWUP_REQUIRED 或 CLOSED）
            log.warn("Failed to transition case {} to FOLLOWUP_REQUIRED: {}", caseId, e.getMessage());
        }
    }

    private void recordNotify(Long planId, Long taskId, Long orgId, Long receiverUserId,
                              String title, String contentSummary) {
        try {
            long notifyId = IdWorker.getId();
            msgNotifyRepository.create(new MsgNotifyCreateModel(
                    notifyId,
                    "FOLLOWUP",
                    taskId,
                    receiverUserId,
                    "REMINDER",
                    "IN_APP",
                    title,
                    contentSummary,
                    "PENDING",
                    orgId,
                    receiverUserId));
        } catch (Exception e) {
            log.warn("Failed to create notify record for planId={}: {}", planId, e.getMessage());
        }
    }
}
