package com.cariesguard.analysis.app;

import com.cariesguard.analysis.assembler.ReviewQueueAssembler;
import com.cariesguard.analysis.assembler.ReviewTaskAssembler;
import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import com.cariesguard.analysis.domain.repository.AnaTaskRecordRepository;
import com.cariesguard.analysis.domain.repository.AnalysisCommandRepository;
import com.cariesguard.analysis.interfaces.query.ReviewQueueQuery;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskDetailVO;
import com.cariesguard.analysis.interfaces.vo.ReviewQueueItemVO;
import com.cariesguard.analysis.interfaces.vo.ReviewQueuePageVO;
import com.cariesguard.analysis.interfaces.vo.ReviewTaskDetailVO;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReviewBffAppService {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final AnalysisQueryAppService analysisQueryAppService;
    private final AnaTaskRecordRepository anaTaskRecordRepository;
    private final AnalysisCommandRepository analysisCommandRepository;
    private final ReviewQueueAssembler reviewQueueAssembler;
    private final ReviewTaskAssembler reviewTaskAssembler;

    public ReviewBffAppService(AnalysisQueryAppService analysisQueryAppService,
                               AnaTaskRecordRepository anaTaskRecordRepository,
                               AnalysisCommandRepository analysisCommandRepository,
                               ReviewQueueAssembler reviewQueueAssembler,
                               ReviewTaskAssembler reviewTaskAssembler) {
        this.analysisQueryAppService = analysisQueryAppService;
        this.anaTaskRecordRepository = anaTaskRecordRepository;
        this.analysisCommandRepository = analysisCommandRepository;
        this.reviewQueueAssembler = reviewQueueAssembler;
        this.reviewTaskAssembler = reviewTaskAssembler;
    }

    public ReviewQueuePageVO getReviewQueue(ReviewQueueQuery query) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        Long orgId = operator.hasAnyRole("ADMIN", "SYS_ADMIN") ? null : operator.getOrgId();
        int pageNo = normalizePageNo(query == null ? null : query.pageNo());
        int pageSize = normalizePageSize(query == null ? null : query.pageSize());
        String taskStatusCode = normalizeTaskStatusCode(query == null ? null : query.taskStatusCode());

        long total = anaTaskRecordRepository.count(null, taskStatusCode, orgId);
        int offset = (pageNo - 1) * pageSize;
        List<ReviewQueueItemVO> records = anaTaskRecordRepository.pageQuery(null, taskStatusCode, orgId, offset, pageSize).stream()
                .map(this::toQueueItem)
                .toList();
        return new ReviewQueuePageVO(pageNo, pageSize, total, records);
    }

    public ReviewTaskDetailVO getReviewTaskDetail(String taskIdentifier) {
        AnalysisTaskDetailVO taskDetail = resolveTaskDetail(taskIdentifier);
        AnalysisCaseModel medicalCase = analysisCommandRepository.findCase(taskDetail.caseId()).orElse(null);
        AnalysisImageModel imageModel = resolvePrimaryImage(taskDetail.caseId());
        return reviewTaskAssembler.toDetail(taskDetail, medicalCase, imageModel);
    }

    private ReviewQueueItemVO toQueueItem(AnalysisTaskViewModel task) {
        AnalysisTaskDetailVO taskDetail = analysisQueryAppService.getTaskDetail(task.taskId());
        AnalysisCaseModel medicalCase = analysisCommandRepository.findCase(task.caseId()).orElse(null);
        return reviewQueueAssembler.toItem(taskDetail, medicalCase);
    }

    private AnalysisImageModel resolvePrimaryImage(Long caseId) {
        if (caseId == null) {
            return null;
        }
        return analysisCommandRepository.listCaseImages(caseId).stream().findFirst().orElse(null);
    }

    private int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo < 1) {
            return DEFAULT_PAGE_NO;
        }
        return pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String normalizeTaskStatusCode(String taskStatusCode) {
        if (!StringUtils.hasText(taskStatusCode)) {
            return null;
        }
        return taskStatusCode.trim();
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
}
