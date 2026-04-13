package com.cariesguard.followup.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cariesguard.followup.domain.model.FupRecordCreateModel;
import com.cariesguard.followup.domain.model.FupRecordModel;
import com.cariesguard.followup.domain.repository.FupRecordRepository;
import com.cariesguard.followup.infrastructure.dataobject.FupRecordDO;
import com.cariesguard.followup.infrastructure.mapper.FupRecordMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class FupRecordRepositoryImpl implements FupRecordRepository {

    private final FupRecordMapper fupRecordMapper;

    public FupRecordRepositoryImpl(FupRecordMapper fupRecordMapper) {
        this.fupRecordMapper = fupRecordMapper;
    }

    @Override
    public void create(FupRecordCreateModel model) {
        FupRecordDO entity = new FupRecordDO();
        entity.setId(model.recordId());
        entity.setRecordNo(model.recordNo());
        entity.setTaskId(model.taskId());
        entity.setPlanId(model.planId());
        entity.setCaseId(model.caseId());
        entity.setPatientId(model.patientId());
        entity.setFollowupMethodCode(model.followupMethodCode());
        entity.setContactResultCode(model.contactResultCode());
        entity.setFollowNextFlag(model.followNextFlag());
        entity.setNextIntervalDays(model.nextIntervalDays());
        entity.setOutcomeSummary(model.outcomeSummary());
        entity.setDoctorNotes(model.doctorNotes());
        entity.setRecordedAt(model.recordedAt());
        entity.setOrgId(model.orgId());
        entity.setStatus("ACTIVE");
        entity.setDeletedFlag(0L);
        entity.setRemark(model.remark());
        entity.setCreatedBy(model.operatorUserId());
        entity.setUpdatedBy(model.operatorUserId());
        fupRecordMapper.insert(entity);
    }

    @Override
    public Optional<FupRecordModel> findById(Long recordId) {
        FupRecordDO entity = fupRecordMapper.selectOne(new LambdaQueryWrapper<FupRecordDO>()
                .eq(FupRecordDO::getId, recordId)
                .eq(FupRecordDO::getDeletedFlag, 0L)
                .last("LIMIT 1"));
        return Optional.ofNullable(entity).map(this::toModel);
    }

    @Override
    public List<FupRecordModel> listByTaskId(Long taskId) {
        return fupRecordMapper.selectList(new LambdaQueryWrapper<FupRecordDO>()
                        .eq(FupRecordDO::getTaskId, taskId)
                        .eq(FupRecordDO::getDeletedFlag, 0L)
                        .orderByDesc(FupRecordDO::getRecordedAt))
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public List<FupRecordModel> listByCaseId(Long caseId) {
        return fupRecordMapper.selectList(new LambdaQueryWrapper<FupRecordDO>()
                        .eq(FupRecordDO::getCaseId, caseId)
                        .eq(FupRecordDO::getDeletedFlag, 0L)
                        .orderByDesc(FupRecordDO::getRecordedAt))
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public Optional<FupRecordModel> findLatestByCaseId(Long caseId) {
        FupRecordDO entity = fupRecordMapper.selectOne(new LambdaQueryWrapper<FupRecordDO>()
                .eq(FupRecordDO::getCaseId, caseId)
                .eq(FupRecordDO::getDeletedFlag, 0L)
                .orderByDesc(FupRecordDO::getRecordedAt)
                .last("LIMIT 1"));
        return Optional.ofNullable(entity).map(this::toModel);
    }

    private FupRecordModel toModel(FupRecordDO entity) {
        return new FupRecordModel(
                entity.getId(),
                entity.getRecordNo(),
                entity.getTaskId(),
                entity.getPlanId(),
                entity.getCaseId(),
                entity.getPatientId(),
                entity.getFollowupMethodCode(),
                entity.getContactResultCode(),
                entity.getFollowNextFlag(),
                entity.getNextIntervalDays(),
                entity.getOutcomeSummary(),
                entity.getDoctorNotes(),
                entity.getRecordedAt(),
                entity.getOrgId(),
                entity.getCreatedAt());
    }
}
