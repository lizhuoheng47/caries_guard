package com.cariesguard.analysis.interfaces.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Data;

@Data
public class AnalysisTaskViewVO {
    private AnalysisTaskDetailVO task;
    private AnalysisDetailViewVO.PatientBriefVO patient;
    private AnalysisDetailViewVO.CaseBriefVO caseInfo;
    private AnalysisDetailViewVO.ImageDetailVO image;
    private AnalysisSummaryVO analysisSummary;
    private JsonNode rawResultJson;
    private List<AnalysisDetailViewVO.TimelineNodeVO> timeline;
    private AnalysisDetailViewVO.RagHint ragHint;
}

