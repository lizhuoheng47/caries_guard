package com.cariesguard.system.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.system.app.SystemAdminQueryAppService;
import com.cariesguard.system.app.SystemOperationLog;
import com.cariesguard.system.interfaces.vo.PageResultVO;
import com.cariesguard.system.interfaces.vo.SystemMenuListItemVO;
import com.cariesguard.system.interfaces.vo.SystemRoleListItemVO;
import com.cariesguard.system.interfaces.vo.SystemUserListItemVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemAdminController {

    private final SystemAdminQueryAppService systemAdminQueryAppService;

    public SystemAdminController(SystemAdminQueryAppService systemAdminQueryAppService) {
        this.systemAdminQueryAppService = systemAdminQueryAppService;
    }

    @GetMapping("/users")
    @SystemOperationLog(moduleCode = "SYSTEM", operationTypeCode = "QUERY", operationName = "Page system users")
    public ApiResponse<PageResultVO<SystemUserListItemVO>> pageUsers(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String userTypeCode,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(
                systemAdminQueryAppService.pageUsers(pageNo, pageSize, keyword, deptId, userTypeCode, status),
                TraceIdUtils.currentTraceId());
    }

    @GetMapping("/roles")
    @SystemOperationLog(moduleCode = "SYSTEM", operationTypeCode = "QUERY", operationName = "List system roles")
    public ApiResponse<List<SystemRoleListItemVO>> listRoles(@RequestParam(required = false) String status) {
        return ApiResponse.success(systemAdminQueryAppService.listRoles(status), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/menus")
    @SystemOperationLog(moduleCode = "SYSTEM", operationTypeCode = "QUERY", operationName = "List system menus")
    public ApiResponse<List<SystemMenuListItemVO>> listMenus(@RequestParam(required = false) String status) {
        return ApiResponse.success(systemAdminQueryAppService.listMenus(status), TraceIdUtils.currentTraceId());
    }
}
