package com.cariesguard.patient.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cariesguard.patient.domain.model.PatientDetailModel;
import com.cariesguard.patient.domain.model.PatientGuardianModel;
import com.cariesguard.patient.domain.model.PatientProfileSnapshotModel;
import com.cariesguard.patient.domain.repository.PatientQueryRepository;
import com.cariesguard.patient.infrastructure.dataobject.PatGuardianDO;
import com.cariesguard.patient.infrastructure.dataobject.PatPatientDO;
import com.cariesguard.patient.infrastructure.dataobject.PatProfileDO;
import com.cariesguard.patient.infrastructure.mapper.PatGuardianMapper;
import com.cariesguard.patient.infrastructure.mapper.PatPatientMapper;
import com.cariesguard.patient.infrastructure.mapper.PatProfileMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PatientQueryRepositoryImpl implements PatientQueryRepository {

    private final PatPatientMapper patPatientMapper;
    private final PatGuardianMapper patGuardianMapper;
    private final PatProfileMapper patProfileMapper;

    public PatientQueryRepositoryImpl(PatPatientMapper patPatientMapper,
                                      PatGuardianMapper patGuardianMapper,
                                      PatProfileMapper patProfileMapper) {
        this.patPatientMapper = patPatientMapper;
        this.patGuardianMapper = patGuardianMapper;
        this.patProfileMapper = patProfileMapper;
    }

    @Override
    public Optional<PatientDetailModel> findPatientDetail(Long patientId) {
        PatPatientDO patient = patPatientMapper.selectOne(new LambdaQueryWrapper<PatPatientDO>()
                .eq(PatPatientDO::getId, patientId)
                .eq(PatPatientDO::getDeletedFlag, 0L)
                .last("LIMIT 1"));
        if (patient == null) {
            return Optional.empty();
        }
        return Optional.of(new PatientDetailModel(
                patient.getId(),
                patient.getPatientNo(),
                patient.getPatientNameMasked(),
                patient.getGenderCode(),
                patient.getAge(),
                patient.getSourceCode(),
                patient.getOrgId(),
                patGuardianMapper.selectList(new LambdaQueryWrapper<PatGuardianDO>()
                                .eq(PatGuardianDO::getPatientId, patientId)
                                .eq(PatGuardianDO::getDeletedFlag, 0L)
                                .orderByDesc(PatGuardianDO::getIsPrimary)
                                .orderByAsc(PatGuardianDO::getId))
                        .stream()
                        .map(item -> new PatientGuardianModel(
                                item.getGuardianNameMasked(),
                                item.getRelationCode(),
                                item.getPhoneMasked(),
                                item.getIsPrimary()))
                        .toList(),
                patProfileMapper.selectList(new LambdaQueryWrapper<PatProfileDO>()
                                .eq(PatProfileDO::getPatientId, patientId)
                                .eq(PatProfileDO::getDeletedFlag, 0L)
                                .orderByDesc(PatProfileDO::getEffectiveDate)
                                .orderByDesc(PatProfileDO::getId))
                        .stream()
                        .findFirst()
                        .map(this::toProfileModel)
                        .orElse(null)));
    }

    private PatientProfileSnapshotModel toProfileModel(PatProfileDO profile) {
        return new PatientProfileSnapshotModel(
                profile.getBrushingFreqPerDay(),
                profile.getSugarDietLevelCode(),
                profile.getFluorideUseFlag(),
                profile.getFamilyCariesHistoryFlag(),
                profile.getOrthodonticHistoryFlag(),
                profile.getPreviousCariesCount(),
                profile.getLastDentalCheckMonths(),
                profile.getOralHygieneLevelCode(),
                profile.getEffectiveDate());
    }
}
