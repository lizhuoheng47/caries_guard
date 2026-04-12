package com.cariesguard.patient.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cariesguard.patient.domain.model.PatientCreateModel;
import com.cariesguard.patient.domain.model.PatientGuardianCreateModel;
import com.cariesguard.patient.domain.repository.PatientCommandRepository;
import com.cariesguard.patient.infrastructure.dataobject.PatGuardianDO;
import com.cariesguard.patient.infrastructure.dataobject.PatPatientDO;
import com.cariesguard.patient.infrastructure.mapper.PatGuardianMapper;
import com.cariesguard.patient.infrastructure.mapper.PatPatientMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PatientCommandRepositoryImpl implements PatientCommandRepository {

    private final PatPatientMapper patPatientMapper;
    private final PatGuardianMapper patGuardianMapper;

    public PatientCommandRepositoryImpl(PatPatientMapper patPatientMapper,
                                        PatGuardianMapper patGuardianMapper) {
        this.patPatientMapper = patPatientMapper;
        this.patGuardianMapper = patGuardianMapper;
    }

    @Override
    public void createPatient(PatientCreateModel model) {
        patPatientMapper.insert(toPatientDO(model));
        for (PatientGuardianCreateModel guardian : model.guardians()) {
            patGuardianMapper.insert(toGuardianDO(guardian));
        }
    }

    @Override
    public boolean existsPatientByIdCardHash(Long orgId, String idCardHash) {
        return patPatientMapper.selectCount(new LambdaQueryWrapper<PatPatientDO>()
                .eq(PatPatientDO::getOrgId, orgId)
                .eq(PatPatientDO::getIdCardHash, idCardHash)
                .eq(PatPatientDO::getDeletedFlag, 0L)) > 0;
    }

    private PatPatientDO toPatientDO(PatientCreateModel model) {
        PatPatientDO entity = new PatPatientDO();
        entity.setId(model.patientId());
        entity.setPatientNo(model.patientNo());
        entity.setPatientNameEnc(model.patientNameEnc());
        entity.setPatientNameHash(model.patientNameHash());
        entity.setPatientNameMasked(model.patientNameMasked());
        entity.setGenderCode(model.genderCode());
        entity.setBirthDateEnc(model.birthDateEnc());
        entity.setBirthDateHash(model.birthDateHash());
        entity.setBirthDateMasked(model.birthDateMasked());
        entity.setAge(model.age());
        entity.setPhoneEnc(model.phoneEnc());
        entity.setPhoneHash(model.phoneHash());
        entity.setPhoneMasked(model.phoneMasked());
        entity.setIdCardEnc(model.idCardEnc());
        entity.setIdCardHash(model.idCardHash());
        entity.setIdCardMasked(model.idCardMasked());
        entity.setSourceCode(model.sourceCode());
        entity.setFirstVisitDate(model.firstVisitDate());
        entity.setPrivacyLevelCode(model.privacyLevelCode());
        entity.setOrgId(model.orgId());
        entity.setStatus(model.status());
        entity.setDeletedFlag(0L);
        entity.setRemark(model.remark());
        entity.setCreatedBy(model.operatorUserId());
        entity.setUpdatedBy(model.operatorUserId());
        return entity;
    }

    private PatGuardianDO toGuardianDO(PatientGuardianCreateModel guardian) {
        PatGuardianDO entity = new PatGuardianDO();
        entity.setId(guardian.guardianId());
        entity.setPatientId(guardian.patientId());
        entity.setGuardianNameEnc(guardian.guardianNameEnc());
        entity.setGuardianNameHash(guardian.guardianNameHash());
        entity.setGuardianNameMasked(guardian.guardianNameMasked());
        entity.setRelationCode(guardian.relationCode());
        entity.setPhoneEnc(guardian.phoneEnc());
        entity.setPhoneHash(guardian.phoneHash());
        entity.setPhoneMasked(guardian.phoneMasked());
        entity.setCertificateTypeCode(guardian.certificateTypeCode());
        entity.setCertificateNoEnc(guardian.certificateNoEnc());
        entity.setCertificateNoHash(guardian.certificateNoHash());
        entity.setCertificateNoMasked(guardian.certificateNoMasked());
        entity.setIsPrimary(guardian.primaryFlag());
        entity.setOrgId(guardian.orgId());
        entity.setStatus(guardian.status());
        entity.setDeletedFlag(0L);
        entity.setRemark(guardian.remark());
        entity.setCreatedBy(guardian.operatorUserId());
        entity.setUpdatedBy(guardian.operatorUserId());
        return entity;
    }
}
