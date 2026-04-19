package com.cariesguard.analysis.app;

import com.cariesguard.analysis.interfaces.vo.AnalysisDetailViewVO.*;
import com.cariesguard.analysis.interfaces.vo.ReviewWorkbenchVO;
import com.cariesguard.analysis.interfaces.vo.ReviewWorkbenchVO.*;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskDetailVO;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ReviewBffAppService {

    private final AnalysisQueryAppService analysisQueryAppService;

    public ReviewBffAppService(AnalysisQueryAppService analysisQueryAppService) {
        this.analysisQueryAppService = analysisQueryAppService;
    }

    public List<AnalysisTaskDetailVO> getReviewQueue() {
        return new ArrayList<>();
    }

    public ReviewWorkbenchVO getReviewTaskView(Long taskId) {
        AnalysisTaskDetailVO taskDetail = analysisQueryAppService.getTaskDetail(taskId);
        ReviewWorkbenchVO viewVO = new ReviewWorkbenchVO();

        viewVO.setTask(taskDetail);

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

        AiResult aiResult = new AiResult();
        aiResult.setGradingLabel("G3");
        aiResult.setUncertaintyScore(0.72);

        List<DetectionBoxVO> detections = new ArrayList<>();
        DetectionBoxVO d1 = new DetectionBoxVO();
        d1.setId("box-1");
        d1.setX(0.45);
        d1.setY(0.4);
        d1.setWidth(0.1);
        d1.setHeight(0.2);
        d1.setLabel("G3");
        d1.setConfidence(0.85);
        detections.add(d1);
        aiResult.setDetections(detections);
        aiResult.setVisualAssets(taskDetail.visualAssets());
        viewVO.setAiResult(aiResult);

        DoctorDraft draft = new DoctorDraft();
        draft.setDraftId(1L);
        draft.setRevisedGrade("G2");
        draft.setReasonTags(Arrays.asList("深度判断偏差", "病灶范围过估"));
        draft.setNote("病变尚未波及牙本质内层，属于 G2 级，AI 可能将局部重叠阴影误判为深层扩展。");

        List<DetectionBoxVO> docDetections = new ArrayList<>();
        DetectionBoxVO d2 = new DetectionBoxVO();
        d2.setId("doc-box-1");
        d2.setX(0.45);
        d2.setY(0.42);
        d2.setWidth(0.1);
        d2.setHeight(0.15);
        d2.setLabel("G2");
        d2.setConfidence(1.0);
        docDetections.add(d2);
        draft.setRevisedDetections(docDetections);
        viewVO.setDoctorDraft(draft);

        ReviewOptions options = new ReviewOptions();
        options.setGradeOptions(Arrays.asList("G0", "G1", "G2", "G3", "G4"));
        options.setReasonTags(Arrays.asList("病灶范围过估", "病灶范围低估", "边界误判", "深度判断偏差", "影像伪影", "其它"));
        viewVO.setReviewOptions(options);

        return viewVO;
    }
}
