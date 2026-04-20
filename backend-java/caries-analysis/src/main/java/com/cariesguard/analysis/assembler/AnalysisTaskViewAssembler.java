package com.cariesguard.analysis.assembler;

import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.analysis.domain.model.AnalysisPatientModel;
import com.cariesguard.analysis.interfaces.vo.AnalysisDetailViewVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisSummaryVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskDetailVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskViewVO;
import com.cariesguard.image.app.AttachmentAppService;
import com.cariesguard.image.interfaces.vo.AttachmentAccessVO;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AnalysisTaskViewAssembler {

    private final AttachmentAppService attachmentAppService;

    public AnalysisTaskViewAssembler(AttachmentAppService attachmentAppService) {
        this.attachmentAppService = attachmentAppService;
    }

    public AnalysisTaskViewVO toView(AnalysisTaskDetailVO task,
                                     AnalysisCaseModel medicalCase,
                                     AnalysisPatientModel patient,
                                     AnalysisImageModel imageModel) {
        AnalysisTaskViewVO view = new AnalysisTaskViewVO();
        view.setTask(task);
        view.setPatient(toPatient(patient));
        view.setCaseInfo(toCaseInfo(task, medicalCase));
        view.setImage(toImage(imageModel));
        view.setAnalysisSummary(task.summary());
        view.setRawResultJson(resolveRawResult(task.summary()));
        view.setTimeline(List.of());
        view.setRagHint(defaultRagHint());
        return view;
    }

    private AnalysisDetailViewVO.PatientBriefVO toPatient(AnalysisPatientModel patient) {
        AnalysisDetailViewVO.PatientBriefVO vo = new AnalysisDetailViewVO.PatientBriefVO();
        if (patient == null) {
            return vo;
        }
        vo.setPatientIdMasked(maskPatientId(patient.patientId()));
        vo.setAge(patient.age());
        vo.setGender(patient.genderCode());
        return vo;
    }

    private AnalysisDetailViewVO.CaseBriefVO toCaseInfo(AnalysisTaskDetailVO task, AnalysisCaseModel medicalCase) {
        AnalysisDetailViewVO.CaseBriefVO vo = new AnalysisDetailViewVO.CaseBriefVO();
        vo.setCaseId(task.caseId());
        if (medicalCase != null && StringUtils.hasText(medicalCase.caseNo())) {
            vo.setCaseNo(medicalCase.caseNo());
        }
        return vo;
    }

    private AnalysisDetailViewVO.ImageDetailVO toImage(AnalysisImageModel imageModel) {
        AnalysisDetailViewVO.ImageDetailVO vo = new AnalysisDetailViewVO.ImageDetailVO();
        if (imageModel == null) {
            return vo;
        }
        vo.setImageId(imageModel.imageId());
        vo.setSourceDevice(imageModel.imageTypeCode());
        vo.setImageUrl(resolveImageUrl(imageModel.attachmentId()));
        return vo;
    }

    private String resolveImageUrl(Long attachmentId) {
        if (attachmentId == null) {
            return null;
        }
        try {
            AttachmentAccessVO access = attachmentAppService.createInternalAccessUrl(attachmentId);
            return access.accessUrl();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private AnalysisDetailViewVO.RagHint defaultRagHint() {
        AnalysisDetailViewVO.RagHint hint = new AnalysisDetailViewVO.RagHint();
        hint.setEnabled(Boolean.TRUE);
        return hint;
    }

    private String maskPatientId(Long patientId) {
        if (patientId == null) {
            return null;
        }
        String raw = String.valueOf(patientId);
        String suffix = raw.length() > 4 ? raw.substring(raw.length() - 4) : raw;
        return "P-***" + suffix;
    }

    private com.fasterxml.jackson.databind.JsonNode resolveRawResult(AnalysisSummaryVO summary) {
        return summary == null ? null : summary.rawResultJson();
    }
}

