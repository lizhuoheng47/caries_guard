package com.cariesguard.image.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import com.cariesguard.image.app.CaseImageAppService;
import com.cariesguard.image.interfaces.command.CreateCaseImageCommand;
import com.cariesguard.image.interfaces.vo.CaseImageMutationVO;
import com.cariesguard.image.interfaces.vo.ImageVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cases/{caseId}/images")
public class CaseImageController {

    private final CaseImageAppService caseImageAppService;

    public CaseImageController(CaseImageAppService caseImageAppService) {
        this.caseImageAppService = caseImageAppService;
    }

    @PostMapping
    @RequirePermission("image:create")
    public ApiResponse<CaseImageMutationVO> create(@PathVariable Long caseId,
                                                   @Valid @RequestBody CreateCaseImageCommand command) {
        return ApiResponse.success(caseImageAppService.createCaseImage(caseId, command), TraceIdUtils.currentTraceId());
    }

    @GetMapping
    @RequirePermission("image:list")
    public ApiResponse<List<ImageVO>> list(@PathVariable Long caseId) {
        return ApiResponse.success(caseImageAppService.listCaseImages(caseId), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/detail/{imageId}")
    @RequirePermission("image:view")
    public ApiResponse<ImageVO> detail(@PathVariable Long caseId, @PathVariable Long imageId) {
        return ApiResponse.success(caseImageAppService.getImage(imageId), TraceIdUtils.currentTraceId());
    }
}
