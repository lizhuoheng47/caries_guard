package com.cariesguard.dashboard.app;

import com.cariesguard.dashboard.infrastructure.repository.DashboardStatsRepository;
import com.cariesguard.dashboard.interfaces.vo.ModelRuntimeVO;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DashboardOpsMetricsAppService {

    private final DashboardStatsRepository dashboardStatsRepository;
    private final JdbcTemplate jdbcTemplate;
    private final String aiDatabase;

    @Value("${caries.system.runtime-mode:COMPETITION}")
    private String runtimeMode;

    public DashboardOpsMetricsAppService(
            DashboardStatsRepository dashboardStatsRepository,
            JdbcTemplate jdbcTemplate,
            @Value("${caries.ai.database:${CARIES_MYSQL_DATABASE_AI:caries_ai}}") String aiDatabase) {
        this.dashboardStatsRepository = dashboardStatsRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.aiDatabase = normalizeSchemaName(aiDatabase);
    }

    public ModelRuntimeVO getModelRuntime() {
        ModelRuntimeVO vo = dashboardStatsRepository.queryModelRuntime(SecurityContextUtils.currentUser().getOrgId());
        
        String llmProviderCode = "UNKNOWN";
        String llmModelName = "UNKNOWN";
        String knowledgeVersion = "UNKNOWN";

        try {
            List<Map<String, Object>> llmModels = jdbcTemplate.queryForList(
                    "SELECT model_code, model_name FROM " + aiTable("mdl_model_version")
                            + " WHERE model_type_code = 'LLM' AND active_flag = '1' ORDER BY id DESC LIMIT 1");
            if (!llmModels.isEmpty()) {
                llmProviderCode = String.valueOf(llmModels.get(0).get("model_code"));
                llmModelName = String.valueOf(llmModels.get(0).get("model_name"));
            }

            List<Map<String, Object>> kbs = jdbcTemplate.queryForList(
                    "SELECT version_no FROM " + aiTable("mdl_model_version")
                            + " WHERE model_type_code = 'KNOWLEDGE_BASE' AND active_flag = '1' ORDER BY id DESC LIMIT 1");
            if (!kbs.isEmpty()) {
                knowledgeVersion = String.valueOf(kbs.get(0).get("version_no"));
            }
        } catch (Exception e) {
            // Fallback if caries_ai is not reachable
        }

        return new ModelRuntimeVO(
                vo.currentModelVersion(),
                vo.recentTaskCount(),
                vo.successTaskCount(),
                vo.failedTaskCount(),
                vo.successRate(),
                vo.averageInferenceMillis(),
                vo.highUncertaintyRate(),
                vo.reviewSuggestedRate(),
                vo.correctionFeedbackCount(),
                
                vo.callbackTotalCount(),
                vo.callbackSuccessCount(),
                vo.callbackSuccessRate(),
                
                vo.visualAssetExpectedCount(),
                vo.visualAssetGeneratedCount(),
                vo.visualAssetSuccessRate(),
                
                vo.reviewSuggestedCount(),
                vo.reviewCompletedCount(),
                vo.reviewCompletionRate(),
                
                vo.riskAssessmentTriggeredCount(),
                vo.riskAssessmentCoveredCount(),
                vo.riskOutputCoverage(),
                
                vo.ragRequestCount(),
                vo.citationPresentCount(),
                vo.citationCompleteness(),
                
                vo.doctorReviewTotalCount(),
                vo.doctorReviewAgreeCount(),
                vo.doctorReviewAgreementRate(),
                
                knowledgeVersion,
                runtimeMode,
                llmProviderCode,
                llmModelName,
                vo.modelVersions()
        );
    }

    private static String normalizeSchemaName(String schemaName) {
        if (schemaName != null && schemaName.matches("[A-Za-z0-9_]+")) {
            return schemaName;
        }
        return "caries_ai";
    }

    private String aiTable(String table) {
        return "`" + aiDatabase + "`." + table;
    }
}
