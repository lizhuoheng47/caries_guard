package com.cariesguard.patient.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.framework.security.sensitive.ProtectedValue;
import com.cariesguard.framework.security.sensitive.SensitiveDataFacade;
import com.cariesguard.patient.domain.model.PatientCreateModel;
import com.cariesguard.patient.domain.model.PatientGuardianCreateModel;
import com.cariesguard.patient.domain.model.PatientManagedModel;
import com.cariesguard.patient.domain.model.PatientUpdateModel;
import com.cariesguard.patient.domain.repository.PatientCommandRepository;
import com.cariesguard.patient.interfaces.command.CreatePatientCommand;
import com.cariesguard.patient.interfaces.command.PatientGuardianCommand;
import com.cariesguard.patient.interfaces.command.UpdatePatientCommand;
import com.cariesguard.patient.interfaces.vo.PatientMutationVO;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PatientCommandAppService {

    private static final DateTimeFormatter PATIENT_NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PatientCommandRepository patientCommandRepository;
    private final SensitiveDataFacade sensitiveDataFacade;

    public PatientCommandAppService(PatientCommandRepository patientCommandRepository,
                                    SensitiveDataFacade sensitiveDataFacade) {
        this.patientCommandRepository = patientCommandRepository;
        this.sensitiveDataFacade = sensitiveDataFacade;
    }

    @Transactional
    public PatientMutationVO createPatient(CreatePatientCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        ProtectedValue patientName = sensitiveDataFacade.protectName(command.patientName());
        ProtectedValue birthDate = protectBirthDate(command.birthDate());
        ProtectedValue phone = protectPhone(command.phone());
        ProtectedValue idCard = protectIdCard(command.idCardNo());
        validateIdentityDuplicate(operator.getOrgId(), idCard.hash());

        long patientId = IdWorker.getId();
        PatientCreateModel model = new PatientCreateModel(
                patientId,
                buildPatientNo(patientId),
                patientName.encrypted(),
                patientName.hash(),
                patientName.masked(),
                defaultGenderCode(command.genderCode()),
                birthDate.encrypted(),
                birthDate.hash(),
                birthDate.masked(),
                calculateAge(command.birthDate()),
                phone.encrypted(),
                phone.hash(),
                phone.masked(),
                idCard.encrypted(),
                idCard.hash(),
                idCard.masked(),
                defaultSourceCode(command.sourceCode()),
                command.firstVisitDate(),
                defaultPrivacyLevel(command.privacyLevelCode()),
                operator.getOrgId(),
                defaultStatus(command.status()),
                command.remark(),
                operator.getUserId(),
                buildGuardians(patientId, operator, command.guardian()));
        patientCommandRepository.createPatient(model);
        return new PatientMutationVO(model.patientId(), model.patientNo());
    }

    @Transactional
    public PatientMutationVO updatePatient(Long patientId, UpdatePatientCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        PatientManagedModel managedPatient = patientCommandRepository.findManagedPatient(patientId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Patient does not exist"));
        ensureOrgAccess(operator, managedPatient.orgId());

        ProtectedValue patientName = sensitiveDataFacade.protectName(command.patientName());
        ProtectedValue birthDate = protectBirthDate(command.birthDate());
        ProtectedValue phone = protectPhone(command.phone());
        ProtectedValue idCard = protectIdCard(command.idCardNo());

        if (StringUtils.hasText(idCard.hash()) && !idCard.hash().equals(managedPatient.idCardHash())) {
            validateIdentityDuplicate(managedPatient.orgId(), idCard.hash());
        }

        PatientUpdateModel model = new PatientUpdateModel(
                patientId,
                patientName.encrypted(),
                patientName.hash(),
                patientName.masked(),
                defaultGenderCode(command.genderCode()),
                birthDate.encrypted(),
                birthDate.hash(),
                birthDate.masked(),
                calculateAge(command.birthDate()),
                phone.encrypted(),
                phone.hash(),
                phone.masked(),
                idCard.encrypted(),
                idCard.hash(),
                idCard.masked(),
                defaultSourceCode(command.sourceCode()),
                command.firstVisitDate(),
                defaultPrivacyLevel(command.privacyLevelCode()),
                defaultStatus(command.status()),
                command.remark(),
                operator.getUserId(),
                buildGuardians(patientId, operator, command.guardian()));
        patientCommandRepository.updatePatient(model);
        return new PatientMutationVO(patientId, managedPatient.patientNo());
    }

    private void validateIdentityDuplicate(Long orgId, String idCardHash) {
        if (StringUtils.hasText(idCardHash) && patientCommandRepository.existsPatientByIdCardHash(orgId, idCardHash)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Patient id card already exists");
        }
    }

    private ProtectedValue protectBirthDate(LocalDate birthDate) {
        return birthDate == null
                ? new ProtectedValue(null, null, null)
                : sensitiveDataFacade.protectBirthDate(birthDate.toString());
    }

    private ProtectedValue protectPhone(String phone) {
        return StringUtils.hasText(phone)
                ? sensitiveDataFacade.protectPhone(phone)
                : new ProtectedValue(null, null, null);
    }

    private ProtectedValue protectIdCard(String idCardNo) {
        return StringUtils.hasText(idCardNo)
                ? sensitiveDataFacade.protectIdCard(idCardNo)
                : new ProtectedValue(null, null, null);
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Math.max(0, Period.between(birthDate, LocalDate.now()).getYears());
    }

    private List<PatientGuardianCreateModel> buildGuardians(Long patientId,
                                                            AuthenticatedUser operator,
                                                            PatientGuardianCommand guardian) {
        if (guardian == null) {
            return List.of();
        }
        ProtectedValue guardianName = sensitiveDataFacade.protectName(guardian.guardianName());
        ProtectedValue phone = sensitiveDataFacade.protectPhone(guardian.phone());
        ProtectedValue certificateNo = sensitiveDataFacade.protectIdCard(guardian.certificateNo());
        return List.of(new PatientGuardianCreateModel(
                IdWorker.getId(),
                patientId,
                guardianName.encrypted(),
                guardianName.hash(),
                guardianName.masked(),
                defaultRelationCode(guardian.relationCode()),
                phone.encrypted(),
                phone.hash(),
                phone.masked(),
                guardian.certificateTypeCode(),
                certificateNo.encrypted(),
                certificateNo.hash(),
                certificateNo.masked(),
                defaultPrimaryFlag(guardian.primaryFlag()),
                operator.getOrgId(),
                defaultStatus(guardian.status()),
                guardian.remark(),
                operator.getUserId()));
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    private String buildPatientNo(long patientId) {
        return "PAT" + LocalDate.now().format(PATIENT_NO_DATE_FORMATTER) + String.format("%06d", patientId % 1_000_000);
    }

    private String defaultGenderCode(String genderCode) {
        return StringUtils.hasText(genderCode) ? genderCode.trim() : "UNKNOWN";
    }

    private String defaultSourceCode(String sourceCode) {
        return StringUtils.hasText(sourceCode) ? sourceCode.trim() : "OUTPATIENT";
    }

    private String defaultPrivacyLevel(String privacyLevelCode) {
        return StringUtils.hasText(privacyLevelCode) ? privacyLevelCode.trim() : "L4";
    }

    private String defaultStatus(String status) {
        return StringUtils.hasText(status) ? status.trim() : "ACTIVE";
    }

    private String defaultRelationCode(String relationCode) {
        return StringUtils.hasText(relationCode) ? relationCode.trim() : "PARENT";
    }

    private String defaultPrimaryFlag(String primaryFlag) {
        return StringUtils.hasText(primaryFlag) ? primaryFlag.trim() : "1";
    }
}
