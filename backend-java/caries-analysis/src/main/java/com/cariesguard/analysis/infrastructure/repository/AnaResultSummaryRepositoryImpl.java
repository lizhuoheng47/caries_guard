package com.cariesguard.analysis.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cariesguard.analysis.domain.model.AnalysisResultSummaryModel;
import com.cariesguard.analysis.domain.repository.AnaResultSummaryRepository;
import com.cariesguard.analysis.infrastructure.dataobject.AnaResultSummaryDO;
import com.cariesguard.analysis.infrastructure.mapper.AnaResultSummaryMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AnaResultSummaryRepositoryImpl implements AnaResultSummaryRepository {

    private final AnaResultSummaryMapper anaResultSummaryMapper;

    public AnaResultSummaryRepositoryImpl(AnaResultSummaryMapper anaResultSummaryMapper) {
        this.anaResultSummaryMapper = anaResultSummaryMapper;
    }

    @Override
    public void save(AnalysisResultSummaryModel model) {
        AnaResultSummaryDO entity = new AnaResultSummaryDO();
        entity.setId(model.summaryId());
        entity.setTaskId(model.taskId());
        entity.setCaseId(model.caseId());
        entity.setRawResultJson(model.rawResultJson());
        entity.setOverallHighestSeverity(model.overallHighestSeverity());
        entity.setUncertaintyScore(model.uncertaintyScore());
        entity.setReviewSuggestedFlag(model.reviewSuggestedFlag());
        entity.setOrgId(model.orgId());
        entity.setStatus("ACTIVE");
        entity.setDeletedFlag(0L);
        entity.setCreatedBy(model.operatorUserId());
        anaResultSummaryMapper.insert(entity);
    }

    @Override
    public Optional<AnalysisResultSummaryModel> findByTaskId(Long taskId) {
        return findOne(new LambdaQueryWrapper<AnaResultSummaryDO>()
                .eq(AnaResultSummaryDO::getTaskId, taskId)
                .eq(AnaResultSummaryDO::getDeletedFlag, 0L)
                .eq(AnaResultSummaryDO::getStatus, "ACTIVE")
                .orderByDesc(AnaResultSummaryDO::getId)
                .last("LIMIT 1"));
    }

    @Override
    public Optional<AnalysisResultSummaryModel> findLatestByCaseId(Long caseId) {
        return findOne(new LambdaQueryWrapper<AnaResultSummaryDO>()
                .eq(AnaResultSummaryDO::getCaseId, caseId)
                .eq(AnaResultSummaryDO::getDeletedFlag, 0L)
                .eq(AnaResultSummaryDO::getStatus, "ACTIVE")
                .orderByDesc(AnaResultSummaryDO::getId)
                .last("LIMIT 1"));
    }

    private Optional<AnalysisResultSummaryModel> findOne(LambdaQueryWrapper<AnaResultSummaryDO> wrapper) {
        AnaResultSummaryDO entity = anaResultSummaryMapper.selectOne(wrapper);
        return entity == null ? Optional.empty() : Optional.of(new AnalysisResultSummaryModel(
                entity.getId(),
                entity.getTaskId(),
                entity.getCaseId(),
                entity.getRawResultJson(),
                entity.getOverallHighestSeverity(),
                entity.getUncertaintyScore(),
                entity.getReviewSuggestedFlag(),
                entity.getOrgId(),
                entity.getCreatedBy()));
    }
}
