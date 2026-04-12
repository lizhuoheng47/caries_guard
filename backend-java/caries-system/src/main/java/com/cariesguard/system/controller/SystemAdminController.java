package com.cariesguard.system.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.system.app.SystemAdminQueryAppService;
import com.cariesguard.system.app.SystemDataPermissionRuleAppService;
import com.cariesguard.system.app.SystemMenuCommandAppService;
import com.cariesguard.system.app.SystemOperationLog;
import com.cariesguard.system.app.SystemRoleCommandAppService;
import com.cariesguard.system.app.SystemUserCommandAppService;
import com.cariesguard.system.interfaces.command.CreateSystemDataPermissionRuleCommand;
import com.cariesguard.system.interfaces.command.CreateSystemMenuCommand;
import com.cariesguard.system.interfaces.command.CreateSystemRoleCommand;
import com.cariesguard.system.interfaces.command.CreateSystemUserCommand;
import com.cariesguard.system.interfaces.command.UpdateSystemDataPermissionRuleCommand;
import com.cariesguard.system.interfaces.command.UpdateSystemMenuCommand;
import com.cariesguard.system.interfaces.command.UpdateSystemRoleCommand;
import com.cariesguard.system.interfaces.command.UpdateSystemUserCommand;
import com.cariesguard.system.interfaces.vo.SystemDataPermissionRuleVO;
import com.cariesguard.system.interfaces.vo.SystemMenuMutationVO;
import com.cariesguard.system.interfaces.vo.SystemRoleMutationVO;
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
    private final SystemRoleCommandAppService systemRoleCommandAppService;
    private final SystemMenuCommandAppService systemMenuCommandAppService;
    private final SystemDataPermissionRuleAppService systemDataPermissionRuleAppService;

    public SystemAdminController(SystemAdminQueryAppService systemAdminQueryAppService,
                                 SystemUserCommandAppService systemUserCommandAppService,
                                 SystemRoleCommandAppService systemRoleCommandAppService,
                                 SystemMenuCommandAppService systemMenuCommandAppService,
                                 SystemDataPermissionRuleAppService systemDataPermissionRuleAppService) {
        this.systemAdminQueryAppService = systemAdminQueryAppService;
        this.systemUserCommandAppService = systemUserCommandAppService;
        this.systemRoleCommandAppService = systemRoleCommandAppService;
        this.systemMenuCommandAppService = systemMenuCommandAppService;
        this.systemDataPermissionRuleAppService = systemDataPermissionRuleAppService;
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

    @PostMapping("/roles")
    @SystemOperationLog(moduleCode = "SYSTEM", operationTypeCode = "CREATE", operationName = "Create system role")
    public ApiResponse<SystemRoleMutationVO> createRole(@Valid @RequestBody CreateSystemRoleCommand command) {
        return ApiResponse.success(systemRoleCommandAppService.createRole(command), TraceIdUtils.currentTraceId());
    }

    @PutMapping("/roles/{roleId}")
    @SystemOperationLog(moduleCode = "SYSTEM", operationTypeCode = "UPDATE", operationName = "Update system role")
    public ApiResponse<SystemRoleMutationVO> updateRole(@PathVariable Long roleId,
                                                        @Valid @RequestBody UpdateSystemRoleCommand command) {
        return ApiResponse.success(systemRoleCommandAppService.updateRole(roleId, command), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/menus")
    @SystemOperationLog(moduleCode = "SYSTEM", operationTypeCode = "QUERY", operationName = "List system menus")
    public ApiResponse<List<SystemMenuListItemVO>> listMenus(@RequestParam(required = false) String status) {
        return ApiResponse.success(systemAdminQueryAppService.listMenus(status), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/menus")
    @SystemOperationLog(moduleCode = "SYSTEM", operationTypeCode = "CREATE", operationName = "Create system menu")
    public ApiResponse<SystemMenuMutationVO> createMenu(@Valid @RequestBody CreateSystemMenuCommand command) {
        return ApiResponse.success(systemMenuCommandAppService.createMenu(command), TraceIdUtils.currentTraceId());
    }

    @PutMapping("/menus/{menuId}")
    @SystemOperationLog(moduleCode = "SYSTEM", operationTypeCode = "UPDATE", operationName = "Update system menu")
    public ApiResponse<SystemMenuMutationVO> updateMenu(@PathVariable Long menuId,
                                                        @Valid @RequestBody UpdateSystemMenuCommand command) {
        return ApiResponse.success(systemMenuCommandAppService.updateMenu(menuId, command), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/data-permission-rules")
    @SystemOperationLog(moduleCode = "SYSTEM", operationTypeCode = "QUERY", operationName = "List data permission rules")
    public ApiResponse<List<SystemDataPermissionRuleVO>> listDataPermissionRules(@RequestParam Long roleId,
                                                                                 @RequestParam(required = false) String moduleCode) {
        return ApiResponse.success(
                systemDataPermissionRuleAppService.listRules(roleId, moduleCode),
                TraceIdUtils.currentTraceId());
    }

    @PostMapping("/data-permission-rules")
    @SystemOperationLog(moduleCode = "SYSTEM", operationTypeCode = "CREATE", operationName = "Create data permission rule")
    public ApiResponse<SystemDataPermissionRuleVO> createDataPermissionRule(
            @Valid @RequestBody CreateSystemDataPermissionRuleCommand command) {
        return ApiResponse.success(
                systemDataPermissionRuleAppService.createRule(command),
                TraceIdUtils.currentTraceId());
    }

    @PutMapping("/data-permission-rules/{ruleId}")
    @SystemOperationLog(moduleCode = "SYSTEM", operationTypeCode = "UPDATE", operationName = "Update data permission rule")
    public ApiResponse<SystemDataPermissionRuleVO> updateDataPermissionRule(@PathVariable Long ruleId,
                                                                            @Valid @RequestBody UpdateSystemDataPermissionRuleCommand command) {
        return ApiResponse.success(
                systemDataPermissionRuleAppService.updateRule(ruleId, command),
                TraceIdUtils.currentTraceId());
    }
}
