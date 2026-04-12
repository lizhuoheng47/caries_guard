package com.cariesguard.image.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import com.cariesguard.image.app.CaseImageAppService;
import com.cariesguard.image.interfaces.command.SaveImageQualityCheckCommand;
import com.cariesguard.image.interfaces.vo.ImageQualityCheckVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/images/{imageId}/quality-checks")
public class ImageQualityCheckController {

    private final CaseImageAppService caseImageAppService;

    public ImageQualityCheckController(CaseImageAppService caseImageAppService) {
        this.caseImageAppService = caseImageAppService;
    }

    @PostMapping
    @RequirePermission("image:quality-check")
    public ApiResponse<ImageQualityCheckVO> save(@PathVariable Long imageId,
                                                 @Valid @RequestBody SaveImageQualityCheckCommand command) {
        return ApiResponse.success(caseImageAppService.saveQualityCheck(imageId, command), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/current")
    @RequirePermission("image:view")
    public ApiResponse<ImageQualityCheckVO> getCurrent(@PathVariable Long imageId) {
        return ApiResponse.success(caseImageAppService.getCurrentQualityCheck(imageId), TraceIdUtils.currentTraceId());
    }
}
