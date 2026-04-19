package com.cariesguard.analysis.app;

import com.cariesguard.analysis.interfaces.vo.AnalysisDetailViewVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisDetailViewVO.*;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskDetailVO;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;

@Service
public class AnalysisBffAppService {

    private final AnalysisQueryAppService analysisQueryAppService;

    public AnalysisBffAppService(AnalysisQueryAppService analysisQueryAppService) {
        this.analysisQueryAppService = analysisQueryAppService;
    }

    public AnalysisDetailViewVO getTaskDetailView(Long taskId) {
        AnalysisTaskDetailVO taskDetail = analysisQueryAppService.getTaskDetail(taskId);
        AnalysisDetailViewVO viewVO = new AnalysisDetailViewVO();
        viewVO.setTask(taskDetail);

        PatientBriefVO patient = new PatientBriefVO();
        patient.setPatientIdMasked("P-***" + (taskId % 1000));
        patient.setPatientNameMasked("J** D**");
        patient.setAge(45);
        patient.setGender("M");
        viewVO.setPatient(patient);

        CaseBriefVO caseInfo = new CaseBriefVO();
        caseInfo.setCaseId(taskDetail.caseId());
        caseInfo.setCaseNo("C-" + taskDetail.caseId());
        caseInfo.setVisitTime("2026-04-19 10:00");
        viewVO.setCaseInfo(caseInfo);

        ImageDetailVO image = new ImageDetailVO();
        image.setImageId(1L);
        image.setImageUrl("/demo-xray.png");
        image.setSourceDevice("Planmeca ProMax 3D");
        viewVO.setImage(image);

        AnalysisSummary summary = new AnalysisSummary();
        summary.setGradingLabel("G3");
        summary.setConfidenceScore(0.92);
        summary.setUncertaintyScore(0.25);
        summary.setNeedsReview(true);
        summary.setRiskLevel("HIGH");
        summary.setRiskFactors(Arrays.asList("Deep caries detected", "Proximity to pulp"));
        summary.setVisualAssets(taskDetail.visualAssets());
        viewVO.setAnalysisSummary(summary);

        ArrayList<TimelineNodeVO> timeline = new ArrayList<>();
        TimelineNodeVO t1 = new TimelineNodeVO();
        t1.setTime("10:01:00"); t1.setTitle("Task Created"); t1.setContent("Image uploaded"); t1.setStatus("SUCCESS"); t1.setDuration("-");
        TimelineNodeVO t2 = new TimelineNodeVO();
        t2.setTime("10:01:02"); t2.setTitle("Analyzing"); t2.setContent("Model inference in progress"); t2.setStatus("SUCCESS"); t2.setDuration("1.2s");
        timeline.add(t1);
        timeline.add(t2);
        viewVO.setTimeline(timeline);

        RagHint hint = new RagHint();
        hint.setEnabled(true);
        hint.setLatestAnswer("Deep caries indicates enamel and dentin destruction.");
        viewVO.setRagHint(hint);

        return viewVO;
    }
}
