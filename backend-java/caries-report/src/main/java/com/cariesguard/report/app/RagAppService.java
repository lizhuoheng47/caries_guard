package com.cariesguard.report.app;

import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.report.domain.client.RagClient;
import com.cariesguard.report.domain.model.RagAnswerModel;
import com.cariesguard.report.domain.model.RagCitationModel;
import com.cariesguard.report.domain.model.RagAskRequestModel;
import com.cariesguard.report.domain.model.RagDoctorQaRequestModel;
import com.cariesguard.report.domain.model.RagPatientExplanationRequestModel;
import com.cariesguard.report.domain.model.RagRetrievedChunkModel;
import com.cariesguard.report.domain.model.ReportRenderDataModel;
import com.cariesguard.report.domain.model.ReportToothRecordModel;
import com.cariesguard.report.interfaces.command.DoctorQaCommand;
import com.cariesguard.report.interfaces.command.PatientExplanationCommand;
import com.cariesguard.report.interfaces.command.RagAskCommand;
import com.cariesguard.report.interfaces.vo.RagAnswerVO;
import com.cariesguard.report.interfaces.vo.RagCitationVO;
import com.cariesguard.report.interfaces.vo.RagRetrievedChunkVO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RagAppService {

    private static final Logger log = LoggerFactory.getLogger(RagAppService.class);
    private static final String FALLBACK_ANSWER = "RAG service is temporarily unavailable. Please rely on the structured report findings and clinical judgment.";
    private static final String PATIENT_REPORT_QUESTION = "Generate a patient-friendly explanation, care advice, and follow-up recommendation for this caries analysis report.";

    private final RagClient ragClient;

    public RagAppService(RagClient ragClient) {
        this.ragClient = ragClient;
    }

    public RagAnswerVO doctorQa(DoctorQaCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        RagDoctorQaRequestModel request = new RagDoctorQaRequestModel(
                TraceIdUtils.currentTraceId(),
                command.question(),
                command.kbCode(),
                command.topK(),
                command.relatedBizNo(),
                command.patientUuid(),
                operator.getUserId(),
                operator.getOrgId(),
                command.clinicalContext());
        return toVO(safeAnswer(() -> ragClient.doctorQa(request)));
    }

    public RagAnswerVO patientExplanation(PatientExplanationCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        RagPatientExplanationRequestModel request = new RagPatientExplanationRequestModel(
                TraceIdUtils.currentTraceId(),
                command.question(),
                command.kbCode(),
                command.topK(),
                command.relatedBizNo(),
                command.patientUuid(),
                operator.getUserId(),
                operator.getOrgId(),
                command.caseSummary(),
                command.riskLevelCode());
        return toVO(safeAnswer(() -> ragClient.patientExplanation(request)));
    }

    public RagAnswerVO ask(RagAskCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        RagAskRequestModel request = new RagAskRequestModel(
                TraceIdUtils.currentTraceId(),
                command.question(),
                command.scene(),
                command.kbCode(),
                command.topK(),
                command.relatedBizNo(),
                command.patientUuid(),
                operator.getUserId(),
                operator.getOrgId(),
                command.caseContext());
        return toVO(safeAnswer(() -> ragClient.ask(request)));
    }

    public String generatePatientReportExplanation(String reportNo, ReportRenderDataModel renderData) {
        RagAnswerVO answer = patientExplanation(new PatientExplanationCommand(
                PATIENT_REPORT_QUESTION,
                null,
                null,
                reportNo,
                null,
                caseSummary(renderData),
                renderData.riskLevelCode()));
        return StringUtils.hasText(answer.answerText()) ? answer.answerText() : FALLBACK_ANSWER;
    }

    private RagAnswerModel safeAnswer(Supplier<RagAnswerModel> supplier) {
        try {
            RagAnswerModel answer = supplier.get();
            return answer == null ? RagAnswerModel.fallback(FALLBACK_ANSWER) : answer;
        } catch (RuntimeException exception) {
            log.warn("RAG service call failed, using fallback answer: {}", exception.getMessage());
            return RagAnswerModel.fallback(FALLBACK_ANSWER);
        }
    }

    private Map<String, Object> caseSummary(ReportRenderDataModel renderData) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("caseNo", renderData.caseNo());
        summary.put("caseId", renderData.caseId());
        summary.put("patientId", renderData.patientId());
        summary.put("highestSeverity", renderData.highestSeverity());
        summary.put("uncertaintyScore", renderData.uncertaintyScore());
        summary.put("lesionCount", renderData.lesionCount());
        summary.put("abnormalToothCount", renderData.abnormalToothCount());
        summary.put("riskLevelCode", renderData.riskLevelCode());
        summary.put("recommendedCycleDays", renderData.recommendedCycleDays());
        summary.put("reviewSuggestedFlag", renderData.reviewSuggestedFlag());
        summary.put("toothFindings", toothFindings(renderData.toothRecords()));
        summary.put("visualAssetCount", renderData.visualAssetCount());
        summary.put("correctionCount", renderData.correctionCount());
        return summary;
    }

    private List<Map<String, Object>> toothFindings(List<ReportToothRecordModel> toothRecords) {
        if (toothRecords == null) {
            return List.of();
        }
        return toothRecords.stream()
                .map(this::toothFinding)
                .toList();
    }

    private Map<String, Object> toothFinding(ReportToothRecordModel toothRecord) {
        Map<String, Object> finding = new LinkedHashMap<>();
        finding.put("toothCode", toothRecord.toothCode());
        finding.put("surface", toothRecord.toothSurfaceCode());
        finding.put("issueType", toothRecord.issueTypeCode());
        finding.put("severity", toothRecord.severityCode());
        finding.put("findingDesc", toothRecord.findingDesc());
        finding.put("suggestion", toothRecord.suggestion());
        return finding;
    }

    private RagAnswerVO toVO(RagAnswerModel answer) {
        return new RagAnswerVO(
                answer.sessionNo(),
                answer.requestNo(),
                answer.answerText(),
                answer.answerText(),
                answer.citations().stream().map(this::toCitationVO).toList(),
                answer.retrievedChunks().stream().map(this::toRetrievedChunkVO).toList(),
                answer.knowledgeBaseCode(),
                answer.knowledgeVersion(),
                answer.modelName(),
                answer.safetyFlag(),
                answer.safetyFlags(),
                answer.refusalReason(),
                answer.confidence(),
                answer.caseContextSummary(),
                answer.traceId(),
                answer.latencyMs(),
                answer.fallback());
    }

    private RagCitationVO toCitationVO(RagCitationModel citation) {
        return new RagCitationVO(
                citation.rankNo(),
                citation.knowledgeBaseCode(),
                citation.documentCode(),
                citation.documentVersion(),
                citation.docId(),
                citation.docTitle(),
                citation.chunkId(),
                citation.score(),
                citation.retrievalScore(),
                citation.sourceUri(),
                citation.chunkText());
    }

    private RagRetrievedChunkVO toRetrievedChunkVO(RagRetrievedChunkModel chunk) {
        return new RagRetrievedChunkVO(
                chunk.chunkId(),
                chunk.documentCode(),
                chunk.score());
    }
}
