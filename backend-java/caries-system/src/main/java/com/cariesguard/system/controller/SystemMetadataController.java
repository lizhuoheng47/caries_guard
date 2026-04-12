package com.cariesguard.system.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import com.cariesguard.system.app.SystemQueryAppService;
import com.cariesguard.system.app.SystemOperationLog;
import com.cariesguard.system.interfaces.vo.DictItemVO;
import com.cariesguard.system.interfaces.vo.DictTypeVO;
import com.cariesguard.system.interfaces.vo.SystemConfigVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemMetadataController {

    private final SystemQueryAppService systemQueryAppService;

    public SystemMetadataController(SystemQueryAppService systemQueryAppService) {
        this.systemQueryAppService = systemQueryAppService;
    }

    @GetMapping("/dicts")
    @RequirePermission("system:dict:list")
    @SystemOperationLog(moduleCode = "SYSTEM", operationTypeCode = "QUERY", operationName = "List dict types")
    public ApiResponse<List<DictTypeVO>> listDictTypes() {
        return ApiResponse.success(systemQueryAppService.listDictTypes(), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/dicts/{dictType}")
    @RequirePermission("system:dict:list")
    @SystemOperationLog(moduleCode = "SYSTEM", operationTypeCode = "QUERY", operationName = "List dict items")
    public ApiResponse<List<DictItemVO>> listDictItems(@PathVariable String dictType) {
        return ApiResponse.success(systemQueryAppService.listDictItems(dictType), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/configs/{configKey}")
    @RequirePermission("system:config:view")
    @SystemOperationLog(moduleCode = "SYSTEM", operationTypeCode = "QUERY", operationName = "Get system config")
    public ApiResponse<SystemConfigVO> getConfig(@PathVariable String configKey) {
        return ApiResponse.success(systemQueryAppService.getConfig(configKey), TraceIdUtils.currentTraceId());
    }
}
