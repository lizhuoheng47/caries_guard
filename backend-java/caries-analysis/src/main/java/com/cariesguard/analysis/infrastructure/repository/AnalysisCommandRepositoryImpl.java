package com.cariesguard.analysis.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.analysis.domain.model.AnalysisPatientModel;
import com.cariesguard.analysis.domain.repository.AnalysisCommandRepository;
import com.cariesguard.analysis.infrastructure.dataobject.AnalysisAttachmentDO;
import com.cariesguard.analysis.infrastructure.dataobject.AnalysisCaseDO;
import com.cariesguard.analysis.infrastructure.dataobject.AnalysisImageFileDO;
import com.cariesguard.analysis.infrastructure.dataobject.AnalysisPatientDO;
import com.cariesguard.analysis.infrastructure.mapper.AnalysisAttachmentMapper;
import com.cariesguard.analysis.infrastructure.mapper.AnalysisCaseMapper;
import com.cariesguard.analysis.infrastructure.mapper.AnalysisImageFileMapper;
import com.cariesguard.analysis.infrastructure.mapper.AnalysisPatientMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AnalysisCommandRepositoryImpl implements AnalysisCommandRepository {

    private final AnalysisCaseMapper analysisCaseMapper;
    private final AnalysisPatientMapper analysisPatientMapper;
    private final AnalysisImageFileMapper analysisImageFileMapper;
    private final AnalysisAttachmentMapper analysisAttachmentMapper;

    public AnalysisCommandRepositoryImpl(AnalysisCaseMapper analysisCaseMapper,
                                         AnalysisPatientMapper analysisPatientMapper,
                                         AnalysisImageFileMapper analysisImageFileMapper,
                                         AnalysisAttachmentMapper analysisAttachmentMapper) {
        this.analysisCaseMapper = analysisCaseMapper;
        this.analysisPatientMapper = analysisPatientMapper;
        this.analysisImageFileMapper = analysisImageFileMapper;
        this.analysisAttachmentMapper = analysisAttachmentMapper;
    }

    @Override
    public Optional<AnalysisCaseModel> findCase(Long caseId) {
        AnalysisCaseDO entity = analysisCaseMapper.selectOne(new LambdaQueryWrapper<AnalysisCaseDO>()
                .eq(AnalysisCaseDO::getId, caseId)
                .eq(AnalysisCaseDO::getDeletedFlag, 0L)
                .eq(AnalysisCaseDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.of(new AnalysisCaseModel(
                entity.getId(),
                entity.getCaseNo(),
                entity.getPatientId(),
                entity.getOrgId(),
                entity.getCaseStatusCode()));
    }

    @Override
    public Optional<AnalysisPatientModel> findPatient(Long patientId) {
        AnalysisPatientDO entity = analysisPatientMapper.selectOne(new LambdaQueryWrapper<AnalysisPatientDO>()
                .eq(AnalysisPatientDO::getId, patientId)
                .eq(AnalysisPatientDO::getDeletedFlag, 0L)
                .eq(AnalysisPatientDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.of(new AnalysisPatientModel(
                entity.getId(),
                entity.getAge(),
                entity.getGenderCode()));
    }

    @Override
    public List<AnalysisImageModel> listCaseImages(Long caseId) {
        return analysisImageFileMapper.selectList(new LambdaQueryWrapper<AnalysisImageFileDO>()
                        .eq(AnalysisImageFileDO::getCaseId, caseId)
                        .eq(AnalysisImageFileDO::getDeletedFlag, 0L)
                        .eq(AnalysisImageFileDO::getStatus, "ACTIVE")
                        .orderByDesc(AnalysisImageFileDO::getIsPrimary)
                        .orderByAsc(AnalysisImageFileDO::getImageIndexNo)
                        .orderByAsc(AnalysisImageFileDO::getId))
                .stream()
                .map(this::toImageModel)
                .toList();
    }

    @Override
    public Optional<AnalysisImageModel> findImage(Long imageId) {
        AnalysisImageFileDO image = analysisImageFileMapper.selectOne(new LambdaQueryWrapper<AnalysisImageFileDO>()
                .eq(AnalysisImageFileDO::getId, imageId)
                .eq(AnalysisImageFileDO::getDeletedFlag, 0L)
                .eq(AnalysisImageFileDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return image == null ? Optional.empty() : Optional.of(toImageModel(image));
    }

    private AnalysisImageModel toImageModel(AnalysisImageFileDO image) {
        AnalysisAttachmentDO attachment = analysisAttachmentMapper.selectOne(new LambdaQueryWrapper<AnalysisAttachmentDO>()
                .eq(AnalysisAttachmentDO::getId, image.getAttachmentId())
                .eq(AnalysisAttachmentDO::getDeletedFlag, 0L)
                .eq(AnalysisAttachmentDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return new AnalysisImageModel(
                image.getId(),
                image.getCaseId(),
                image.getAttachmentId(),
                image.getImageTypeCode(),
                image.getQualityStatusCode(),
                attachment == null ? null : attachment.getBucketName(),
                attachment == null ? null : attachment.getObjectKey());
    }
}
