package com.cariesguard.system.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.system.interfaces.vo.SystemPingVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemSupportController {

    @GetMapping("/ping")
    public ApiResponse<SystemPingVO> ping() {
        return ApiResponse.success(new SystemPingVO(true, "caries-guard-backend"), TraceIdUtils.currentTraceId());
    }
}
