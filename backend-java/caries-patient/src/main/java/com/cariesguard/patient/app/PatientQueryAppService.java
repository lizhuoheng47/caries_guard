package com.cariesguard.patient.app;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.patient.domain.model.PatientDetailModel;
import com.cariesguard.patient.domain.model.PatientGuardianModel;
import com.cariesguard.patient.domain.model.PatientProfileSnapshotModel;
import com.cariesguard.patient.domain.repository.PatientQueryRepository;
import com.cariesguard.patient.interfaces.vo.PatientDetailVO;
import com.cariesguard.patient.interfaces.vo.PatientGuardianVO;
import com.cariesguard.patient.interfaces.vo.PatientProfileVO;
import org.springframework.stereotype.Service;

@Service
public class PatientQueryAppService {

    private final PatientQueryRepository patientQueryRepository;

    public PatientQueryAppService(PatientQueryRepository patientQueryRepository) {
        this.patientQueryRepository = patientQueryRepository;
    }

    public PatientDetailVO getPatient(Long patientId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        PatientDetailModel patient = patientQueryRepository.findPatientDetail(patientId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Patient does not exist"));
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !patient.orgId().equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        return new PatientDetailVO(
                patient.patientId(),
                patient.patientNo(),
                patient.patientNameMasked(),
                patient.genderCode(),
                patient.age(),
                patient.sourceCode(),
                patient.guardianList().stream().map(this::toGuardianVO).toList(),
                toProfileVO(patient.currentProfile()));
    }

    private PatientGuardianVO toGuardianVO(PatientGuardianModel guardian) {
        return new PatientGuardianVO(
                guardian.guardianNameMasked(),
                guardian.relationCode(),
                guardian.phoneMasked(),
                guardian.primaryFlag());
    }

    private PatientProfileVO toProfileVO(PatientProfileSnapshotModel profile) {
        if (profile == null) {
            return null;
        }
        return new PatientProfileVO(
                profile.brushingFreqPerDay(),
                profile.sugarDietLevelCode(),
                profile.fluorideUseFlag(),
                profile.familyCariesHistoryFlag(),
                profile.orthodonticHistoryFlag(),
                profile.previousCariesCount(),
                profile.lastDentalCheckMonths(),
                profile.oralHygieneLevelCode(),
                profile.effectiveDate());
    }
}
