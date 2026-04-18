package com.cariesguard.system.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cariesguard.framework.security.datascope.DataScopeContext;
import com.cariesguard.framework.security.datascope.DataScopeType;
import com.cariesguard.system.config.CompetitionModeProperties;
import com.cariesguard.system.domain.model.SystemMenuDetailModel;
import com.cariesguard.system.domain.model.SystemMenuSummaryModel;
import com.cariesguard.system.domain.model.SystemRoleDetailModel;
import com.cariesguard.system.domain.model.SystemUserDetailModel;
import com.cariesguard.system.domain.repository.SystemAdminQueryRepository;
import com.cariesguard.system.interfaces.vo.SystemMenuDetailVO;
import com.cariesguard.system.interfaces.vo.SystemMenuListItemVO;
import com.cariesguard.system.interfaces.vo.SystemRoleDetailVO;
import com.cariesguard.system.interfaces.vo.SystemUserDetailVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemAdminQueryAppServiceTests {

    @Mock
    private SystemAdminQueryRepository systemAdminQueryRepository;

    @Mock
    private SystemDataScopeService systemDataScopeService;

    private CompetitionExposureService competitionExposureService(boolean enabled) {
        CompetitionModeProperties properties = new CompetitionModeProperties();
        properties.setEnabled(enabled);
        return new CompetitionExposureService(properties);
    }

    @Test
    void getUserShouldReturnMaskedDetail() {
        SystemAdminQueryAppService service = new SystemAdminQueryAppService(
                systemAdminQueryRepository,
                systemDataScopeService,
                competitionExposureService(false));
        DataScopeContext scope = new DataScopeContext(100001L, 100001L, List.of("ORG_ADMIN"), DataScopeType.ORG, Set.of());
        when(systemDataScopeService.currentScope("SYSTEM")).thenReturn(scope);
        when(systemAdminQueryRepository.findUserDetail(scope, 2001L)).thenReturn(Optional.of(
                new SystemUserDetailModel(
                        2001L,
                        3001L,
                        "U2001",
                        "doctor01",
                        "Doctor One",
                        "D*",
                        "138****0000",
                        "d****@mail.com",
                        "/avatar.png",
                        "DOCTOR",
                        "MALE",
                        "ID_CARD",
                        "44************12",
                        100001L,
                        "ACTIVE",
                        "remark",
                        LocalDateTime.of(2026, 4, 12, 9, 0),
                        LocalDateTime.of(2026, 4, 1, 8, 0),
                        List.of(11L, 12L),
                        List.of("DOCTOR", "REVIEWER"))));

        SystemUserDetailVO result = service.getUser(2001L);

        assertThat(result.userId()).isEqualTo(2001L);
        assertThat(result.roleIds()).containsExactly(11L, 12L);
        assertThat(result.phoneMasked()).isEqualTo("138****0000");
    }

    @Test
    void getRoleShouldReturnBoundMenus() {
        SystemAdminQueryAppService service = new SystemAdminQueryAppService(
                systemAdminQueryRepository,
                systemDataScopeService,
                competitionExposureService(false));
        DataScopeContext scope = new DataScopeContext(100001L, 100001L, List.of("ORG_ADMIN"), DataScopeType.ORG, Set.of());
        when(systemDataScopeService.currentScope("SYSTEM")).thenReturn(scope);
        when(systemAdminQueryRepository.findRoleDetail(scope, 5001L)).thenReturn(Optional.of(
                new SystemRoleDetailModel(
                        5001L,
                        "CASE_REVIEWER",
                        "Case Reviewer",
                        10,
                        "ORG",
                        false,
                        100001L,
                        "ACTIVE",
                        "remark",
                        List.of(101L, 102L))));

        SystemRoleDetailVO result = service.getRole(5001L);

        assertThat(result.roleCode()).isEqualTo("CASE_REVIEWER");
        assertThat(result.menuIds()).containsExactly(101L, 102L);
    }

    @Test
    void getMenuShouldReturnIconAndFlags() {
        SystemAdminQueryAppService service = new SystemAdminQueryAppService(
                systemAdminQueryRepository,
                systemDataScopeService,
                competitionExposureService(false));
        DataScopeContext scope = new DataScopeContext(100001L, 100001L, List.of("ORG_ADMIN"), DataScopeType.ORG, Set.of());
        when(systemDataScopeService.currentScope("SYSTEM")).thenReturn(scope);
        when(systemAdminQueryRepository.findMenuDetail(scope, 6001L)).thenReturn(Optional.of(
                new SystemMenuDetailModel(
                        6001L,
                        0L,
                        "Patients",
                        "MENU",
                        "/patients",
                        "patient/index",
                        "patient:list",
                        "user-group",
                        20,
                        true,
                        false,
                        100001L,
                        "ACTIVE",
                        "remark")));

        SystemMenuDetailVO result = service.getMenu(6001L);

        assertThat(result.menuId()).isEqualTo(6001L);
        assertThat(result.icon()).isEqualTo("user-group");
        assertThat(result.visible()).isTrue();
    }

    @Test
    void competitionModeShouldHideFollowupAndGeneralDashboardMenus() {
        SystemAdminQueryAppService service = new SystemAdminQueryAppService(
                systemAdminQueryRepository,
                systemDataScopeService,
                competitionExposureService(true));
        DataScopeContext scope = new DataScopeContext(100001L, 100001L, List.of("ORG_ADMIN"), DataScopeType.ORG, Set.of());
        when(systemDataScopeService.currentScope("SYSTEM")).thenReturn(scope);
        when(systemAdminQueryRepository.listMenus(scope, null)).thenReturn(List.of(
                new SystemMenuSummaryModel(1L, 0L, "Analysis", "MENU", "/analysis/tasks", "analysis/task-index", "analysis:view", 10, true, false, 100001L, "ACTIVE"),
                new SystemMenuSummaryModel(2L, 0L, "Follow-up", "MENU", "/followups", "followup/index", "followup:view", 20, true, false, 100001L, "ACTIVE"),
                new SystemMenuSummaryModel(3L, 0L, "Dashboard", "MENU", "/dashboard", "dashboard/index", "dashboard:view", 30, true, false, 100001L, "ACTIVE"),
                new SystemMenuSummaryModel(4L, 0L, "AI Ops", "MENU", "/dashboard/model-runtime", "dashboard/model-runtime", "dashboard:ops:view", 40, true, false, 100001L, "ACTIVE")));

        List<SystemMenuListItemVO> result = service.listMenus(null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SystemMenuListItemVO::routePath)
                .containsExactly("/analysis/tasks", "/dashboard/model-runtime");
    }
}
