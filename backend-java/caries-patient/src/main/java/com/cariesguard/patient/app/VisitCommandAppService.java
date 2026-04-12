package com.cariesguard.patient.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.patient.domain.model.PatientOwnedModel;
import com.cariesguard.patient.domain.model.VisitCreateModel;
import com.cariesguard.patient.domain.repository.VisitCaseCommandRepository;
import com.cariesguard.patient.interfaces.command.CreateVisitCommand;
import com.cariesguard.patient.interfaces.vo.VisitMutationVO;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class VisitCommandAppService {

    private static final DateTimeFormatter VISIT_NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final VisitCaseCommandRepository visitCaseCommandRepository;

    public VisitCommandAppService(VisitCaseCommandRepository visitCaseCommandRepository) {
        this.visitCaseCommandRepository = visitCaseCommandRepository;
    }

    @Transactional
    public VisitMutationVO createVisit(CreateVisitCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        PatientOwnedModel patient = visitCaseCommandRepository.findPatient(command.patientId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Patient does not exist"));
        ensureOrgAccess(operator, patient.orgId());

        long visitId = IdWorker.getId();
        VisitCreateModel model = new VisitCreateModel(
                visitId,
                buildVisitNo(visitId),
                patient.patientId(),
                command.departmentId(),
                command.doctorUserId(),
                defaultVisitType(command.visitTypeCode()),
                command.visitDate(),
                command.complaint(),
                defaultTriageLevel(command.triageLevelCode()),
                defaultSourceChannel(command.sourceChannelCode()),
                patient.orgId(),
                defaultStatus(command.status()),
                command.remark(),
                operator.getUserId());
        visitCaseCommandRepository.createVisit(model);
        return new VisitMutationVO(model.visitId(), model.visitNo());
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    private String buildVisitNo(long visitId) {
        return "VIS" + LocalDate.now().format(VISIT_NO_DATE_FORMATTER) + String.format("%06d", visitId % 1_000_000);
    }

    private String defaultVisitType(String visitTypeCode) {
        return StringUtils.hasText(visitTypeCode) ? visitTypeCode.trim() : "OUTPATIENT";
    }

    private String defaultTriageLevel(String triageLevelCode) {
        return StringUtils.hasText(triageLevelCode) ? triageLevelCode.trim() : "NORMAL";
    }

    private String defaultSourceChannel(String sourceChannelCode) {
        return StringUtils.hasText(sourceChannelCode) ? sourceChannelCode.trim() : "MANUAL";
    }

    private String defaultStatus(String status) {
        return StringUtils.hasText(status) ? status.trim() : "ACTIVE";
    }
}
