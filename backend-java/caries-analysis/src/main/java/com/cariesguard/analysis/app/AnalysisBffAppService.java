package com.cariesguard.analysis.app;

import com.cariesguard.analysis.assembler.AnalysisTaskViewAssembler;
import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.analysis.domain.model.AnalysisPatientModel;
import com.cariesguard.analysis.domain.repository.AnalysisCommandRepository;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskDetailVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskViewVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AnalysisBffAppService {

    private final AnalysisQueryAppService analysisQueryAppService;
    private final AnalysisCommandRepository analysisCommandRepository;
    private final AnalysisTaskViewAssembler analysisTaskViewAssembler;

    public AnalysisBffAppService(AnalysisQueryAppService analysisQueryAppService,
                                 AnalysisCommandRepository analysisCommandRepository,
                                 AnalysisTaskViewAssembler analysisTaskViewAssembler) {
        this.analysisQueryAppService = analysisQueryAppService;
        this.analysisCommandRepository = analysisCommandRepository;
        this.analysisTaskViewAssembler = analysisTaskViewAssembler;
    }

    public AnalysisTaskViewVO getTaskDetailView(String taskIdentifier) {
        AnalysisTaskDetailVO taskDetail = resolveTaskDetail(taskIdentifier);
        AnalysisCaseModel medicalCase = analysisCommandRepository.findCase(taskDetail.caseId()).orElse(null);
        AnalysisPatientModel patient = resolvePatient(medicalCase);
        AnalysisImageModel imageModel = resolvePrimaryImage(taskDetail.caseId());
        return analysisTaskViewAssembler.toView(taskDetail, medicalCase, patient, imageModel);
    }

    private AnalysisTaskDetailVO resolveTaskDetail(String taskIdentifier) {
        String normalized = taskIdentifier == null ? null : taskIdentifier.trim();
        if (!StringUtils.hasText(normalized)) {
            return analysisQueryAppService.getTaskDetailByTaskNo(taskIdentifier);
        }
        if (normalized.chars().allMatch(Character::isDigit)) {
            return analysisQueryAppService.getTaskDetail(Long.parseLong(normalized));
        }
        return analysisQueryAppService.getTaskDetailByTaskNo(normalized);
    }

    private AnalysisPatientModel resolvePatient(AnalysisCaseModel medicalCase) {
        if (medicalCase == null || medicalCase.patientId() == null) {
            return null;
        }
        return analysisCommandRepository.findPatient(medicalCase.patientId()).orElse(null);
    }

    private AnalysisImageModel resolvePrimaryImage(Long caseId) {
        if (caseId == null) {
            return null;
        }
        return analysisCommandRepository.listCaseImages(caseId).stream().findFirst().orElse(null);
    }
}
