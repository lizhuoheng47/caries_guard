package com.cariesguard.analysis.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.analysis.infrastructure.dataobject.AnaModelEvalRecordDO;
import com.cariesguard.analysis.infrastructure.dataobject.AnaModelVersionRegistryDO;
import com.cariesguard.analysis.infrastructure.dataobject.TrnDatasetSampleDO;
import com.cariesguard.analysis.infrastructure.dataobject.TrnDatasetSnapshotDO;
import com.cariesguard.analysis.infrastructure.mapper.AnaModelEvalRecordMapper;
import com.cariesguard.analysis.infrastructure.mapper.AnaModelVersionRegistryMapper;
import com.cariesguard.analysis.infrastructure.mapper.TrnDatasetSampleMapper;
import com.cariesguard.analysis.infrastructure.mapper.TrnDatasetSnapshotMapper;
import com.cariesguard.analysis.interfaces.command.ApproveModelVersionCommand;
import com.cariesguard.analysis.interfaces.command.CreateDatasetSampleCommand;
import com.cariesguard.analysis.interfaces.command.CreateDatasetSnapshotCommand;
import com.cariesguard.analysis.interfaces.command.RecordModelEvaluationCommand;
import com.cariesguard.analysis.interfaces.command.RegisterModelVersionCommand;
import com.cariesguard.analysis.interfaces.vo.DatasetSampleVO;
import com.cariesguard.analysis.interfaces.vo.DatasetSnapshotVO;
import com.cariesguard.analysis.interfaces.vo.ModelEvaluationVO;
import com.cariesguard.analysis.interfaces.vo.ModelVersionGovernanceVO;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ModelGovernanceAppService {

    private static final Set<String> MODEL_STATUS_CODES = Set.of("CANDIDATE", "EVALUATING", "APPROVED", "RETIRED");

    private final AnaModelVersionRegistryMapper modelVersionMapper;
    private final TrnDatasetSnapshotMapper datasetSnapshotMapper;
    private final TrnDatasetSampleMapper datasetSampleMapper;
    private final AnaModelEvalRecordMapper modelEvalRecordMapper;
    private final ObjectMapper objectMapper;

    public ModelGovernanceAppService(AnaModelVersionRegistryMapper modelVersionMapper,
                                     TrnDatasetSnapshotMapper datasetSnapshotMapper,
                                     TrnDatasetSampleMapper datasetSampleMapper,
                                     AnaModelEvalRecordMapper modelEvalRecordMapper,
                                     ObjectMapper objectMapper) {
        this.modelVersionMapper = modelVersionMapper;
        this.datasetSnapshotMapper = datasetSnapshotMapper;
        this.datasetSampleMapper = datasetSampleMapper;
        this.modelEvalRecordMapper = modelEvalRecordMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ModelVersionGovernanceVO registerModelVersion(RegisterModelVersionCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        String modelCode = requiredText(command.modelCode(), "Model code must not be empty");
        String modelVersion = requiredText(command.modelVersion(), "Model version must not be empty");
        AnaModelVersionRegistryDO existing = modelVersionMapper.selectOne(new LambdaQueryWrapper<AnaModelVersionRegistryDO>()
                .eq(AnaModelVersionRegistryDO::getModelCode, modelCode)
                .eq(AnaModelVersionRegistryDO::getModelVersion, modelVersion));
        if (existing != null) {
            return toModelVersionVO(existing);
        }
        AnaModelVersionRegistryDO entity = new AnaModelVersionRegistryDO();
        entity.setId(IdWorker.getId());
        entity.setModelCode(modelCode);
        entity.setModelVersion(modelVersion);
        entity.setModelTypeCode(defaultCode(command.modelTypeCode(), "DETECTION"));
        entity.setApprovedFlag("0");
        entity.setStatus("CANDIDATE");
        entity.setRemark(trimToNull(command.remark()));
        entity.setCreatedBy(operator.getUserId());
        entity.setUpdatedBy(operator.getUserId());
        modelVersionMapper.insert(entity);
        return toModelVersionVO(entity);
    }

    @Transactional
    public ModelVersionGovernanceVO approveModelVersion(Long modelVersionId, ApproveModelVersionCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        AnaModelVersionRegistryDO entity = modelVersionMapper.selectById(modelVersionId);
        if (entity == null) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Model version does not exist");
        }
        String decisionCode = defaultCode(command.decisionCode(), "APPROVED");
        if (!MODEL_STATUS_CODES.contains(decisionCode)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Unsupported model decision");
        }
        entity.setStatus(decisionCode);
        entity.setApprovedFlag("APPROVED".equals(decisionCode) ? "1" : "0");
        entity.setApprovedBy("APPROVED".equals(decisionCode) ? operator.getUserId() : null);
        entity.setApprovedAt("APPROVED".equals(decisionCode) ? LocalDateTime.now() : null);
        entity.setRemark(trimToNull(command.remark()));
        entity.setUpdatedBy(operator.getUserId());
        modelVersionMapper.updateById(entity);
        return toModelVersionVO(entity);
    }

    public List<ModelVersionGovernanceVO> listModelVersions() {
        return modelVersionMapper.selectList(new LambdaQueryWrapper<AnaModelVersionRegistryDO>()
                        .orderByDesc(AnaModelVersionRegistryDO::getCreatedAt)
                        .orderByDesc(AnaModelVersionRegistryDO::getId))
                .stream()
                .map(this::toModelVersionVO)
                .toList();
    }

    @Transactional
    public DatasetSnapshotVO createDatasetSnapshot(CreateDatasetSnapshotCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        String datasetVersion = requiredText(command.datasetVersion(), "Dataset version must not be empty");
        TrnDatasetSnapshotDO existing = datasetSnapshotMapper.selectOne(new LambdaQueryWrapper<TrnDatasetSnapshotDO>()
                .eq(TrnDatasetSnapshotDO::getDatasetVersion, datasetVersion));
        if (existing != null) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Dataset snapshot already exists");
        }
        List<CreateDatasetSampleCommand> sampleCommands = command.samples() == null ? List.of() : command.samples();
        TrnDatasetSnapshotDO snapshot = new TrnDatasetSnapshotDO();
        snapshot.setId(IdWorker.getId());
        snapshot.setDatasetVersion(datasetVersion);
        snapshot.setSnapshotTypeCode(defaultCode(command.snapshotTypeCode(), "EVAL"));
        snapshot.setSourceSummary(trimToNull(command.sourceSummary()));
        snapshot.setSampleCount(sampleCommands.size());
        snapshot.setMetadataJson(toJson(command.metadataJson()));
        snapshot.setDatasetCardPath(trimToNull(command.datasetCardPath()));
        snapshot.setReleasedAt(LocalDateTime.now());
        snapshot.setOrgId(operator.getOrgId());
        snapshot.setStatus("ACTIVE");
        snapshot.setDeletedFlag(0L);
        snapshot.setRemark(trimToNull(command.remark()));
        snapshot.setCreatedBy(operator.getUserId());
        snapshot.setUpdatedBy(operator.getUserId());
        datasetSnapshotMapper.insert(snapshot);

        List<DatasetSampleVO> samples = sampleCommands.stream()
                .map(sample -> createDatasetSample(snapshot.getId(), sample, operator))
                .toList();
        return toDatasetSnapshotVO(snapshot, samples);
    }

    @Transactional
    public ModelEvaluationVO recordModelEvaluation(RecordModelEvaluationCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        AnaModelVersionRegistryDO modelVersion = modelVersionMapper.selectById(command.modelVersionId());
        if (modelVersion == null) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Model version does not exist");
        }
        if (command.datasetSnapshotId() != null && datasetSnapshotMapper.selectById(command.datasetSnapshotId()) == null) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Dataset snapshot does not exist");
        }
        AnaModelEvalRecordDO entity = new AnaModelEvalRecordDO();
        entity.setId(IdWorker.getId());
        entity.setModelVersionId(command.modelVersionId());
        entity.setDatasetSnapshotId(command.datasetSnapshotId());
        entity.setEvalTypeCode(defaultCode(command.evalTypeCode(), "OFFLINE"));
        entity.setMetricJson(toJson(command.metricJson()));
        entity.setErrorCaseJson(toJson(command.errorCaseJson()));
        entity.setEvidenceAttachmentKey(trimToNull(command.evidenceAttachmentKey()));
        entity.setEvaluatedAt(LocalDateTime.now());
        entity.setEvaluatorUserId(operator.getUserId());
        entity.setOrgId(operator.getOrgId());
        entity.setStatus("ACTIVE");
        entity.setDeletedFlag(0L);
        entity.setRemark(trimToNull(command.remark()));
        entity.setCreatedBy(operator.getUserId());
        entity.setUpdatedBy(operator.getUserId());
        modelEvalRecordMapper.insert(entity);
        return toModelEvaluationVO(entity);
    }

    private DatasetSampleVO createDatasetSample(Long snapshotId,
                                                CreateDatasetSampleCommand command,
                                                AuthenticatedUser operator) {
        TrnDatasetSampleDO entity = new TrnDatasetSampleDO();
        entity.setId(IdWorker.getId());
        entity.setSnapshotId(snapshotId);
        entity.setSampleRefNo(requiredText(command.sampleRefNo(), "Sample ref no must not be empty"));
        entity.setPatientUuid(trimToNull(command.patientUuid()));
        entity.setImageRefNo(trimToNull(command.imageRefNo()));
        entity.setSourceTypeCode(defaultCode(command.sourceTypeCode(), "CORRECTION"));
        entity.setSplitTypeCode(defaultCode(command.splitTypeCode(), "EVAL"));
        entity.setLabelVersion(trimToNull(command.labelVersion()));
        entity.setLabelJson(toJson(command.labelJson()));
        entity.setOrgId(operator.getOrgId());
        entity.setCreatedAt(LocalDateTime.now());
        datasetSampleMapper.insert(entity);
        return toDatasetSampleVO(entity);
    }

    private ModelVersionGovernanceVO toModelVersionVO(AnaModelVersionRegistryDO entity) {
        return new ModelVersionGovernanceVO(
                entity.getId(),
                entity.getModelCode(),
                entity.getModelVersion(),
                entity.getModelTypeCode(),
                entity.getApprovedFlag(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getStatus(),
                entity.getRemark());
    }

    private DatasetSnapshotVO toDatasetSnapshotVO(TrnDatasetSnapshotDO entity, List<DatasetSampleVO> samples) {
        return new DatasetSnapshotVO(
                entity.getId(),
                entity.getDatasetVersion(),
                entity.getSnapshotTypeCode(),
                entity.getSourceSummary(),
                entity.getSampleCount(),
                readJson(entity.getMetadataJson()),
                entity.getDatasetCardPath(),
                samples);
    }

    private DatasetSampleVO toDatasetSampleVO(TrnDatasetSampleDO entity) {
        return new DatasetSampleVO(
                entity.getId(),
                entity.getSnapshotId(),
                entity.getSampleRefNo(),
                entity.getPatientUuid(),
                entity.getImageRefNo(),
                entity.getSourceTypeCode(),
                entity.getSplitTypeCode(),
                entity.getLabelVersion(),
                readJson(entity.getLabelJson()));
    }

    private ModelEvaluationVO toModelEvaluationVO(AnaModelEvalRecordDO entity) {
        return new ModelEvaluationVO(
                entity.getId(),
                entity.getModelVersionId(),
                entity.getDatasetSnapshotId(),
                entity.getEvalTypeCode(),
                readJson(entity.getMetricJson()),
                readJson(entity.getErrorCaseJson()),
                entity.getEvidenceAttachmentKey(),
                entity.getEvaluatedAt(),
                entity.getEvaluatorUserId(),
                entity.getStatus());
    }

    private String requiredText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), message);
        }
        return trimmed;
    }

    private String defaultCode(String value, String fallback) {
        String trimmed = trimToNull(value);
        return (trimmed == null ? fallback : trimmed).toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String toJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR);
        }
    }

    private JsonNode readJson(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }
}
