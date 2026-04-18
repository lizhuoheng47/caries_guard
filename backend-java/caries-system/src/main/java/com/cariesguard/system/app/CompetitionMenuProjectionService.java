package com.cariesguard.system.app;

import com.cariesguard.system.domain.model.SystemMenuDetailModel;
import com.cariesguard.system.domain.model.SystemMenuSummaryModel;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CompetitionMenuProjectionService {

    public enum CompetitionReadyState {
        READY,
        PROJECTED_ONLY
    }

    private static final Map<String, CompetitionMenuPresentation> MENU_PRESENTATIONS = Map.of(
            "/patients", new CompetitionMenuPresentation("Cases & Imaging", 10, CompetitionReadyState.PROJECTED_ONLY, null),
            "/analysis/tasks", new CompetitionMenuPresentation("AI Analysis Tasks", 20, CompetitionReadyState.READY, null),
            "/cases", new CompetitionMenuPresentation("AI Result Detail", 30, CompetitionReadyState.PROJECTED_ONLY, null),
            "/reports", new CompetitionMenuPresentation("Knowledge & Citation", 40, CompetitionReadyState.PROJECTED_ONLY, null),
            "/images", new CompetitionMenuPresentation("Review & Feedback", 50, CompetitionReadyState.PROJECTED_ONLY, null),
            "/dashboard/model-runtime", new CompetitionMenuPresentation("AI Runtime & Evaluation", 60, CompetitionReadyState.READY, null));

    private final CompetitionExposureService competitionExposureService;

    public CompetitionMenuProjectionService(CompetitionExposureService competitionExposureService) {
        this.competitionExposureService = competitionExposureService;
    }

    public SystemMenuSummaryModel project(SystemMenuSummaryModel item) {
        if (!competitionExposureService.isEnabled()) {
            return item;
        }
        CompetitionMenuPresentation presentation = MENU_PRESENTATIONS.get(item.routePath());
        if (presentation == null) {
            return item;
        }
        String finalRoutePath = presentation.overrideRoutePath() != null ? presentation.overrideRoutePath() : item.routePath();
        return new SystemMenuSummaryModel(
                item.menuId(),
                item.parentId(),
                presentation.menuName(),
                item.menuTypeCode(),
                finalRoutePath,
                item.componentPath(),
                item.permissionCode(),
                presentation.orderNum(),
                item.visible(),
                item.cache(),
                item.orgId(),
                item.status());
    }

    public SystemMenuDetailModel project(SystemMenuDetailModel item) {
        if (!competitionExposureService.isEnabled()) {
            return item;
        }
        CompetitionMenuPresentation presentation = MENU_PRESENTATIONS.get(item.routePath());
        if (presentation == null) {
            return item;
        }
        String finalRoutePath = presentation.overrideRoutePath() != null ? presentation.overrideRoutePath() : item.routePath();
        String finalRemark = "STATE:" + presentation.state();
        return new SystemMenuDetailModel(
                item.menuId(),
                item.parentId(),
                presentation.menuName(),
                item.menuTypeCode(),
                finalRoutePath,
                item.componentPath(),
                item.permissionCode(),
                item.icon(),
                presentation.orderNum(),
                item.visible(),
                item.cache(),
                item.orgId(),
                item.status(),
                finalRemark);
    }

    private record CompetitionMenuPresentation(String menuName, int orderNum, CompetitionReadyState state, String overrideRoutePath) {
    }
}
