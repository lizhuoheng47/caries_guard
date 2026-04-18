package com.cariesguard.analysis.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.cariesguard.analysis.interfaces.vo.DatasetSnapshotVO;
import com.cariesguard.analysis.interfaces.vo.ModelEvaluationVO;
import com.cariesguard.analysis.interfaces.vo.ModelVersionGovernanceVO;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ModelGovernanceAppServiceTests {

    @Mock
    private AnaModelVersionRegistryMapper modelVersionMapper;

    @Mock
    private TrnDatasetSnapshotMapper datasetSnapshotMapper;

    @Mock
    private TrnDatasetSampleMapper datasetSampleMapper;

    @Mock
    private AnaModelEvalRecordMapper modelEvalRecordMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerModelVersionShouldCreateCandidate() {
        setCurrentUser();
        ModelGovernanceAppService appService = createService();
        when(modelVersionMapper.selectOne(anyModelVersionQuery())).thenReturn(null);

        ModelVersionGovernanceVO result = appService.registerModelVersion(new RegisterModelVersionCommand(
                "caries-detector",
                "caries-v2",
                "detection",
                "candidate model"));

        assertThat(result.modelVersionId()).isNotNull();
        assertThat(result.modelCode()).isEqualTo("caries-detector");
        assertThat(result.modelVersion()).isEqualTo("caries-v2");
        assertThat(result.modelTypeCode()).isEqualTo("DETECTION");
        assertThat(result.approvedFlag()).isEqualTo("0");
        assertThat(result.status()).isEqualTo("CANDIDATE");
        ArgumentCaptor<AnaModelVersionRegistryDO> captor = ArgumentCaptor.forClass(AnaModelVersionRegistryDO.class);
        verify(modelVersionMapper).insert(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(1001L);
    }

    @Test
    void approveModelVersionShouldMarkApproved() {
        setCurrentUser();
        ModelGovernanceAppService appService = createService();
        AnaModelVersionRegistryDO entity = new AnaModelVersionRegistryDO();
        entity.setId(3002L);
        entity.setModelCode("caries-detector");
        entity.setModelVersion("caries-v2");
        entity.setModelTypeCode("DETECTION");
        entity.setApprovedFlag("0");
        entity.setStatus("CANDIDATE");
        when(modelVersionMapper.selectById(3002L)).thenReturn(entity);

        ModelVersionGovernanceVO result = appService.approveModelVersion(
                3002L,
                new ApproveModelVersionCommand("approved", "passed offline eval"));

        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(result.approvedFlag()).isEqualTo("1");
        assertThat(result.approvedBy()).isEqualTo(1001L);
        assertThat(result.approvedAt()).isNotNull();
        verify(modelVersionMapper).updateById(entity);
    }

    @Test
    void createDatasetSnapshotShouldInsertSnapshotAndSamples() throws Exception {
        setCurrentUser();
        ModelGovernanceAppService appService = createService();
        when(datasetSnapshotMapper.selectOne(anyDatasetSnapshotQuery())).thenReturn(null);

        DatasetSnapshotVO result = appService.createDatasetSnapshot(new CreateDatasetSnapshotCommand(
                "eval-20260418",
                "eval",
                "approved correction samples",
                objectMapper.createObjectNode().put("source", "corrections"),
                "datasets/eval-20260418/card.md",
                "eval snapshot",
                List.of(new CreateDatasetSampleCommand(
                        "FB-9001",
                        "patient-1",
                        "image-1",
                        "correction",
                        "eval",
                        "label-v1",
                        objectMapper.createObjectNode().put("grade", "C1")))));

        assertThat(result.snapshotId()).isNotNull();
        assertThat(result.datasetVersion()).isEqualTo("eval-20260418");
        assertThat(result.snapshotTypeCode()).isEqualTo("EVAL");
        assertThat(result.sampleCount()).isEqualTo(1);
        assertThat(result.samples()).hasSize(1);
        assertThat(result.samples().get(0).sampleRefNo()).isEqualTo("FB-9001");
        ArgumentCaptor<TrnDatasetSnapshotDO> snapshotCaptor = ArgumentCaptor.forClass(TrnDatasetSnapshotDO.class);
        ArgumentCaptor<TrnDatasetSampleDO> sampleCaptor = ArgumentCaptor.forClass(TrnDatasetSampleDO.class);
        verify(datasetSnapshotMapper).insert(snapshotCaptor.capture());
        verify(datasetSampleMapper).insert(sampleCaptor.capture());
        assertThat(snapshotCaptor.getValue().getOrgId()).isEqualTo(2001L);
        assertThat(sampleCaptor.getValue().getLabelJson()).contains("\"grade\":\"C1\"");
    }

    @Test
    void recordModelEvaluationShouldInsertMetrics() throws Exception {
        setCurrentUser();
        ModelGovernanceAppService appService = createService();
        AnaModelVersionRegistryDO model = new AnaModelVersionRegistryDO();
        model.setId(3002L);
        when(modelVersionMapper.selectById(3002L)).thenReturn(model);
        TrnDatasetSnapshotDO snapshot = new TrnDatasetSnapshotDO();
        snapshot.setId(8001L);
        when(datasetSnapshotMapper.selectById(8001L)).thenReturn(snapshot);

        ModelEvaluationVO result = appService.recordModelEvaluation(new RecordModelEvaluationCommand(
                3002L,
                8001L,
                "offline",
                objectMapper.createObjectNode().put("accuracy", 0.91),
                objectMapper.createArrayNode().add("FB-9001"),
                "eval/evidence.json",
                "offline evaluation"));

        assertThat(result.evaluationId()).isNotNull();
        assertThat(result.evalTypeCode()).isEqualTo("OFFLINE");
        assertThat(result.metricJson().path("accuracy").asDouble()).isEqualTo(0.91);
        assertThat(result.evaluatorUserId()).isEqualTo(1001L);
        ArgumentCaptor<AnaModelEvalRecordDO> captor = ArgumentCaptor.forClass(AnaModelEvalRecordDO.class);
        verify(modelEvalRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getMetricJson()).contains("accuracy");
        assertThat(captor.getValue().getOrgId()).isEqualTo(2001L);
    }

    private ModelGovernanceAppService createService() {
        return new ModelGovernanceAppService(
                modelVersionMapper,
                datasetSnapshotMapper,
                datasetSampleMapper,
                modelEvalRecordMapper,
                objectMapper);
    }

    private void setCurrentUser() {
        AuthenticatedUser user = new AuthenticatedUser(
                1001L,
                2001L,
                "ops",
                "hash",
                "Ops",
                true,
                List.of("DOCTOR"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }

    private LambdaQueryWrapper<AnaModelVersionRegistryDO> anyModelVersionQuery() {
        return any();
    }

    private LambdaQueryWrapper<TrnDatasetSnapshotDO> anyDatasetSnapshotQuery() {
        return any();
    }
}
