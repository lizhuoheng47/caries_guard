package com.cariesguard.analysis.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cariesguard.analysis.domain.model.AnalysisVisualAssetCreateModel;
import com.cariesguard.analysis.domain.model.AnalysisVisualAssetModel;
import com.cariesguard.analysis.domain.repository.AnaVisualAssetRepository;
import com.cariesguard.analysis.infrastructure.dataobject.AnaVisualAssetDO;
import com.cariesguard.analysis.infrastructure.mapper.AnaVisualAssetMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AnaVisualAssetRepositoryImpl implements AnaVisualAssetRepository {

    private final AnaVisualAssetMapper anaVisualAssetMapper;

    public AnaVisualAssetRepositoryImpl(AnaVisualAssetMapper anaVisualAssetMapper) {
        this.anaVisualAssetMapper = anaVisualAssetMapper;
    }

    @Override
    public void replaceByTaskId(Long taskId, List<AnalysisVisualAssetCreateModel> models) {
        anaVisualAssetMapper.update(null, new LambdaUpdateWrapper<AnaVisualAssetDO>()
                .eq(AnaVisualAssetDO::getTaskId, taskId)
                .eq(AnaVisualAssetDO::getDeletedFlag, 0L)
                .set(AnaVisualAssetDO::getDeletedFlag, 1L));
        for (AnalysisVisualAssetCreateModel model : models) {
            AnaVisualAssetDO entity = new AnaVisualAssetDO();
            entity.setId(model.assetId());
            entity.setTaskId(model.taskId());
            entity.setCaseId(model.caseId());
            entity.setModelVersion(model.modelVersion());
            entity.setAssetTypeCode(model.assetTypeCode());
            entity.setAttachmentId(model.attachmentId());
            entity.setRelatedImageId(model.relatedImageId());
            entity.setToothCode(model.toothCode());
            entity.setOrgId(model.orgId());
            entity.setStatus("ACTIVE");
            entity.setDeletedFlag(0L);
            entity.setCreatedBy(model.operatorUserId());
            entity.setUpdatedBy(model.operatorUserId());
            anaVisualAssetMapper.insert(entity);
        }
    }

    @Override
    public List<AnalysisVisualAssetModel> listByTaskId(Long taskId) {
        return anaVisualAssetMapper.selectList(new LambdaQueryWrapper<AnaVisualAssetDO>()
                        .eq(AnaVisualAssetDO::getTaskId, taskId)
                        .eq(AnaVisualAssetDO::getDeletedFlag, 0L)
                        .eq(AnaVisualAssetDO::getStatus, "ACTIVE")
                        .orderByAsc(AnaVisualAssetDO::getAssetTypeCode)
                        .orderByAsc(AnaVisualAssetDO::getId))
                .stream()
                .map(item -> new AnalysisVisualAssetModel(
                        item.getAssetTypeCode(),
                        item.getAttachmentId(),
                        item.getRelatedImageId(),
                        item.getToothCode()))
                .toList();
    }
}
