package com.cariesguard.system.app;

import com.cariesguard.system.domain.model.SystemMenuDetailModel;
import com.cariesguard.system.domain.model.SystemMenuSummaryModel;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CompetitionMenuProjectionService {

    private static final Map<String, CompetitionMenuPresentation> MENU_PRESENTATIONS = Map.of(
            "/patients", new CompetitionMenuPresentation("Cases & Imaging", 10),
            "/analysis/tasks", new CompetitionMenuPresentation("AI Analysis Tasks", 20),
            "/cases", new CompetitionMenuPresentation("AI Result Detail", 30),
            "/reports", new CompetitionMenuPresentation("Knowledge & Citation", 40),
            "/images", new CompetitionMenuPresentation("Review & Feedback", 50),
            "/dashboard/model-runtime", new CompetitionMenuPresentation("AI Runtime & Evaluation", 60));

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
        return new SystemMenuSummaryModel(
                item.menuId(),
                item.parentId(),
                presentation.menuName(),
                item.menuTypeCode(),
                item.routePath(),
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
        return new SystemMenuDetailModel(
                item.menuId(),
                item.parentId(),
                presentation.menuName(),
                item.menuTypeCode(),
                item.routePath(),
                item.componentPath(),
                item.permissionCode(),
                item.icon(),
                presentation.orderNum(),
                item.visible(),
                item.cache(),
                item.orgId(),
                item.status(),
                item.remark());
    }

    private record CompetitionMenuPresentation(String menuName, int orderNum) {
    }
}
