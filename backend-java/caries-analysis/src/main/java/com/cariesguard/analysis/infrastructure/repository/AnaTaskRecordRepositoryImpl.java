package com.cariesguard.analysis.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cariesguard.analysis.domain.model.AnalysisTaskCreateModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskStatusUpdateModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import com.cariesguard.analysis.domain.repository.AnaTaskRecordRepository;
import com.cariesguard.analysis.infrastructure.dataobject.AnaTaskRecordDO;
import com.cariesguard.analysis.infrastructure.mapper.AnalysisTaskRecordMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class AnaTaskRecordRepositoryImpl implements AnaTaskRecordRepository {

    private final AnalysisTaskRecordMapper analysisTaskRecordMapper;

    public AnaTaskRecordRepositoryImpl(AnalysisTaskRecordMapper analysisTaskRecordMapper) {
        this.analysisTaskRecordMapper = analysisTaskRecordMapper;
    }

    @Override
    public void save(AnalysisTaskCreateModel model) {
        AnaTaskRecordDO entity = new AnaTaskRecordDO();
        entity.setId(model.taskId());
        entity.setTaskNo(model.taskNo());
        entity.setCaseId(model.caseId());
        entity.setPatientId(model.patientId());
        entity.setRequestBatchNo(model.requestBatchNo());
        entity.setModelVersion(model.modelVersion());
        entity.setTaskTypeCode(model.taskTypeCode());
        entity.setTaskStatusCode(model.taskStatusCode());
        entity.setRequestPayloadJson(model.requestPayloadJson());
        entity.setOrgId(model.orgId());
        entity.setRetryFromTaskId(model.retryFromTaskId());
        entity.setStatus(model.status());
        entity.setDeletedFlag(0L);
        entity.setCreatedBy(model.operatorUserId());
        analysisTaskRecordMapper.insert(entity);
    }

    @Override
    public Optional<AnalysisTaskViewModel> findById(Long taskId) {
        return findOne(new LambdaQueryWrapper<AnaTaskRecordDO>()
                .eq(AnaTaskRecordDO::getId, taskId)
                .eq(AnaTaskRecordDO::getDeletedFlag, 0L)
                .eq(AnaTaskRecordDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
    }

    @Override
    public Optional<AnalysisTaskViewModel> findByTaskNo(String taskNo) {
        return findOne(new LambdaQueryWrapper<AnaTaskRecordDO>()
                .eq(AnaTaskRecordDO::getTaskNo, taskNo)
                .eq(AnaTaskRecordDO::getDeletedFlag, 0L)
                .eq(AnaTaskRecordDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
    }

    @Override
    public boolean existsRunningTaskByCaseId(Long caseId) {
        return analysisTaskRecordMapper.selectCount(new LambdaQueryWrapper<AnaTaskRecordDO>()
                .eq(AnaTaskRecordDO::getCaseId, caseId)
                .in(AnaTaskRecordDO::getTaskStatusCode, List.of("QUEUEING", "PROCESSING"))
                .eq(AnaTaskRecordDO::getDeletedFlag, 0L)
                .eq(AnaTaskRecordDO::getStatus, "ACTIVE")) > 0;
    }

    @Override
    public void updateStatus(AnalysisTaskStatusUpdateModel model) {
        LambdaUpdateWrapper<AnaTaskRecordDO> update = new LambdaUpdateWrapper<AnaTaskRecordDO>()
                .eq(AnaTaskRecordDO::getTaskNo, model.taskNo())
                .eq(AnaTaskRecordDO::getDeletedFlag, 0L)
                .set(AnaTaskRecordDO::getTaskStatusCode, model.taskStatusCode())
                .set(AnaTaskRecordDO::getCallbackPayloadJson, model.callbackPayloadJson())
                .set(AnaTaskRecordDO::getErrorCode, model.errorCode())
                .set(AnaTaskRecordDO::getErrorMessage, model.errorMessage())
                .set(AnaTaskRecordDO::getStartedAt, model.startedAt())
                .set(AnaTaskRecordDO::getCompletedAt, model.completedAt());
        if (model.traceId() != null) {
            update.set(AnaTaskRecordDO::getTraceId, model.traceId());
        }
        if (model.inferenceMillis() != null) {
            update.set(AnaTaskRecordDO::getInferenceMillis, Math.toIntExact(model.inferenceMillis()));
        }
        if (StringUtils.hasText(model.modelVersion())) {
            update.set(AnaTaskRecordDO::getModelVersion, model.modelVersion().trim());
        }
        analysisTaskRecordMapper.update(null, update);
    }

    @Override
    public long count(Long caseId, String taskStatusCode, Long orgId) {
        LambdaQueryWrapper<AnaTaskRecordDO> wrapper = new LambdaQueryWrapper<AnaTaskRecordDO>()
                .eq(AnaTaskRecordDO::getDeletedFlag, 0L)
                .eq(AnaTaskRecordDO::getStatus, "ACTIVE");
        if (caseId != null) {
            wrapper.eq(AnaTaskRecordDO::getCaseId, caseId);
        }
        if (taskStatusCode != null && !taskStatusCode.isBlank()) {
            wrapper.eq(AnaTaskRecordDO::getTaskStatusCode, taskStatusCode.trim());
        }
        if (orgId != null) {
            wrapper.eq(AnaTaskRecordDO::getOrgId, orgId);
        }
        return analysisTaskRecordMapper.selectCount(wrapper);
    }

    @Override
    public List<AnalysisTaskViewModel> pageQuery(Long caseId, String taskStatusCode, Long orgId, int offset, int limit) {
        LambdaQueryWrapper<AnaTaskRecordDO> wrapper = new LambdaQueryWrapper<AnaTaskRecordDO>()
                .eq(AnaTaskRecordDO::getDeletedFlag, 0L)
                .eq(AnaTaskRecordDO::getStatus, "ACTIVE");
        if (caseId != null) {
            wrapper.eq(AnaTaskRecordDO::getCaseId, caseId);
        }
        if (taskStatusCode != null && !taskStatusCode.isBlank()) {
            wrapper.eq(AnaTaskRecordDO::getTaskStatusCode, taskStatusCode.trim());
        }
        if (orgId != null) {
            wrapper.eq(AnaTaskRecordDO::getOrgId, orgId);
        }
        wrapper.orderByDesc(AnaTaskRecordDO::getCreatedAt)
                .last("LIMIT " + Math.max(offset, 0) + "," + Math.max(limit, 1));
        return analysisTaskRecordMapper.selectList(wrapper).stream().map(this::toView).toList();
    }

    @Override
    public boolean existsByRetryFromTaskId(Long originalTaskId) {
        return analysisTaskRecordMapper.selectCount(new LambdaQueryWrapper<AnaTaskRecordDO>()
                .eq(AnaTaskRecordDO::getRetryFromTaskId, originalTaskId)
                .eq(AnaTaskRecordDO::getDeletedFlag, 0L)
                .eq(AnaTaskRecordDO::getStatus, "ACTIVE")) > 0;
    }

    private Optional<AnalysisTaskViewModel> findOne(LambdaQueryWrapper<AnaTaskRecordDO> wrapper) {
        AnaTaskRecordDO entity = analysisTaskRecordMapper.selectOne(wrapper);
        return entity == null ? Optional.empty() : Optional.of(toView(entity));
    }

    private AnalysisTaskViewModel toView(AnaTaskRecordDO entity) {
        return new AnalysisTaskViewModel(
                entity.getId(),
                entity.getTaskNo(),
                entity.getCaseId(),
                entity.getPatientId(),
                entity.getRequestBatchNo(),
                entity.getModelVersion(),
                entity.getTaskTypeCode(),
                entity.getTaskStatusCode(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getOrgId(),
                entity.getRetryFromTaskId(),
                entity.getTraceId(),
                entity.getInferenceMillis() == null ? null : entity.getInferenceMillis().longValue());
    }
}
