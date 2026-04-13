package com.cariesguard.followup.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cariesguard.followup.domain.model.FupPlanCreateModel;
import com.cariesguard.followup.domain.model.FupPlanModel;
import com.cariesguard.followup.domain.repository.FupPlanRepository;
import com.cariesguard.followup.infrastructure.dataobject.FupPlanDO;
import com.cariesguard.followup.infrastructure.mapper.FupPlanMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class FupPlanRepositoryImpl implements FupPlanRepository {

    private static final Set<String> ACTIVE_PLAN_STATUSES = Set.of("PLANNED", "ACTIVE");

    private final FupPlanMapper fupPlanMapper;

    public FupPlanRepositoryImpl(FupPlanMapper fupPlanMapper) {
        this.fupPlanMapper = fupPlanMapper;
    }

    @Override
    public void create(FupPlanCreateModel model) {
        FupPlanDO entity = new FupPlanDO();
        entity.setId(model.planId());
        entity.setPlanNo(model.planNo());
        entity.setCaseId(model.caseId());
        entity.setPatientId(model.patientId());
        entity.setPlanTypeCode(model.planTypeCode());
        entity.setPlanStatusCode(model.planStatusCode());
        entity.setNextFollowupDate(model.nextFollowupDate());
        entity.setIntervalDays(model.intervalDays());
        entity.setOwnerUserId(model.ownerUserId());
        entity.setTriggerSourceCode(model.triggerSourceCode());
        entity.setTriggerRefId(model.triggerRefId());
        entity.setOrgId(model.orgId());
        entity.setStatus("ACTIVE");
        entity.setDeletedFlag(0L);
        entity.setRemark(model.remark());
        entity.setCreatedBy(model.operatorUserId());
        entity.setUpdatedBy(model.operatorUserId());
        fupPlanMapper.insert(entity);
    }

    @Override
    public Optional<FupPlanModel> findById(Long planId) {
        FupPlanDO entity = fupPlanMapper.selectOne(new LambdaQueryWrapper<FupPlanDO>()
                .eq(FupPlanDO::getId, planId)
                .eq(FupPlanDO::getDeletedFlag, 0L)
                .last("LIMIT 1"));
        return Optional.ofNullable(entity).map(this::toModel);
    }

    @Override
    public List<FupPlanModel> listByCaseId(Long caseId) {
        return fupPlanMapper.selectList(new LambdaQueryWrapper<FupPlanDO>()
                        .eq(FupPlanDO::getCaseId, caseId)
                        .eq(FupPlanDO::getDeletedFlag, 0L)
                        .orderByDesc(FupPlanDO::getCreatedAt))
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public boolean existsActivePlan(Long caseId, String triggerSourceCode, Long triggerRefId) {
        Long count = fupPlanMapper.selectCount(new LambdaQueryWrapper<FupPlanDO>()
                .eq(FupPlanDO::getCaseId, caseId)
                .eq(FupPlanDO::getTriggerSourceCode, triggerSourceCode)
                .eq(FupPlanDO::getTriggerRefId, triggerRefId)
                .in(FupPlanDO::getPlanStatusCode, ACTIVE_PLAN_STATUSES)
                .eq(FupPlanDO::getDeletedFlag, 0L));
        return count != null && count > 0;
    }

    @Override
    public void updateStatus(Long planId, String planStatusCode, Long operatorUserId) {
        fupPlanMapper.update(null, new LambdaUpdateWrapper<FupPlanDO>()
                .eq(FupPlanDO::getId, planId)
                .eq(FupPlanDO::getDeletedFlag, 0L)
                .set(FupPlanDO::getPlanStatusCode, planStatusCode)
                .set(FupPlanDO::getUpdatedBy, operatorUserId));
    }

    private FupPlanModel toModel(FupPlanDO entity) {
        return new FupPlanModel(
                entity.getId(),
                entity.getPlanNo(),
                entity.getCaseId(),
                entity.getPatientId(),
                entity.getPlanTypeCode(),
                entity.getPlanStatusCode(),
                entity.getNextFollowupDate(),
                entity.getIntervalDays(),
                entity.getOwnerUserId(),
                entity.getTriggerSourceCode(),
                entity.getTriggerRefId(),
                entity.getOrgId(),
                entity.getRemark(),
                entity.getCreatedAt());
    }
}
