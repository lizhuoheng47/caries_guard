package com.cariesguard.system.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.system.app.SystemAdminQueryAppService;
import com.cariesguard.system.app.SystemOperationLog;
import com.cariesguard.system.app.SystemUserCommandAppService;
import com.cariesguard.system.interfaces.command.CreateSystemUserCommand;
import com.cariesguard.system.interfaces.command.UpdateSystemUserCommand;
import com.cariesguard.system.interfaces.vo.PageResultVO;
import com.cariesguard.system.interfaces.vo.SystemMenuListItemVO;
import com.cariesguard.system.interfaces.vo.SystemRoleListItemVO;
import com.cariesguard.system.interfaces.vo.SystemUserListItemVO;
import com.cariesguard.system.interfaces.vo.SystemUserMutationVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemAdminController {

    private final SystemAdminQueryAppService systemAdminQueryAppService;
    private final SystemUserCommandAppService systemUserCommandAppService;

    public SystemAdminController(SystemAdminQueryAppService systemAdminQueryAppService,
                                 SystemUserCommandAppService systemUserCommandAppService) {
        this.systemAdminQueryAppService = systemAdminQueryAppService;
        this.systemUserCommandAppService = systemUserCommandAppService;
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

    @PostMapping("/users")
    @SystemOperationLog(moduleCode = "SYSTEM", operationTypeCode = "CREATE", operationName = "Create system user")
    public ApiResponse<SystemUserMutationVO> createUser(@Valid @RequestBody CreateSystemUserCommand command) {
        return ApiResponse.success(systemUserCommandAppService.createUser(command), TraceIdUtils.currentTraceId());
    }

    @PutMapping("/users/{userId}")
    @SystemOperationLog(moduleCode = "SYSTEM", operationTypeCode = "UPDATE", operationName = "Update system user")
    public ApiResponse<SystemUserMutationVO> updateUser(@PathVariable Long userId,
                                                        @Valid @RequestBody UpdateSystemUserCommand command) {
        return ApiResponse.success(systemUserCommandAppService.updateUser(userId, command), TraceIdUtils.currentTraceId());
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
