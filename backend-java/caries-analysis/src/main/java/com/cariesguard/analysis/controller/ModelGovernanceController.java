package com.cariesguard.analysis.controller;

import com.cariesguard.analysis.app.ModelGovernanceAppService;
import com.cariesguard.analysis.interfaces.command.ApproveModelVersionCommand;
import com.cariesguard.analysis.interfaces.command.CreateDatasetSnapshotCommand;
import com.cariesguard.analysis.interfaces.command.RecordModelEvaluationCommand;
import com.cariesguard.analysis.interfaces.command.RegisterModelVersionCommand;
import com.cariesguard.analysis.interfaces.vo.DatasetSnapshotVO;
import com.cariesguard.analysis.interfaces.vo.ModelEvaluationVO;
import com.cariesguard.analysis.interfaces.vo.ModelVersionGovernanceVO;
import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analysis/model-governance")
public class ModelGovernanceController {

    private final ModelGovernanceAppService modelGovernanceAppService;

    public ModelGovernanceController(ModelGovernanceAppService modelGovernanceAppService) {
        this.modelGovernanceAppService = modelGovernanceAppService;
    }

    @PostMapping("/model-versions")
    @RequirePermission("analysis:create")
    public ApiResponse<ModelVersionGovernanceVO> registerModelVersion(
            @Valid @RequestBody RegisterModelVersionCommand command) {
        return ApiResponse.success(modelGovernanceAppService.registerModelVersion(command), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/model-versions/{modelVersionId}/approval")
    @RequirePermission("analysis:create")
    public ApiResponse<ModelVersionGovernanceVO> approveModelVersion(
            @PathVariable Long modelVersionId,
            @Valid @RequestBody ApproveModelVersionCommand command) {
        return ApiResponse.success(
                modelGovernanceAppService.approveModelVersion(modelVersionId, command),
                TraceIdUtils.currentTraceId());
    }

    @GetMapping("/model-versions")
    @RequirePermission("analysis:view")
    public ApiResponse<List<ModelVersionGovernanceVO>> listModelVersions() {
        return ApiResponse.success(modelGovernanceAppService.listModelVersions(), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/dataset-snapshots")
    @RequirePermission("analysis:create")
    public ApiResponse<DatasetSnapshotVO> createDatasetSnapshot(
            @Valid @RequestBody CreateDatasetSnapshotCommand command) {
        return ApiResponse.success(modelGovernanceAppService.createDatasetSnapshot(command), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/evaluations")
    @RequirePermission("analysis:create")
    public ApiResponse<ModelEvaluationVO> recordModelEvaluation(
            @Valid @RequestBody RecordModelEvaluationCommand command) {
        return ApiResponse.success(modelGovernanceAppService.recordModelEvaluation(command), TraceIdUtils.currentTraceId());
    }
}
