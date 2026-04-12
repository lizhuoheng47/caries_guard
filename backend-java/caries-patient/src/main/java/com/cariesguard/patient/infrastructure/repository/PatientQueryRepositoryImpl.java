package com.cariesguard.patient.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cariesguard.patient.domain.model.PageQueryResult;
import com.cariesguard.patient.domain.model.PatientDetailModel;
import com.cariesguard.patient.domain.model.PatientGuardianModel;
import com.cariesguard.patient.domain.model.PatientProfileSnapshotModel;
import com.cariesguard.patient.domain.model.PatientSummaryModel;
import com.cariesguard.patient.domain.repository.PatientQueryRepository;
import com.cariesguard.patient.infrastructure.dataobject.PatGuardianDO;
import com.cariesguard.patient.infrastructure.dataobject.PatPatientDO;
import com.cariesguard.patient.infrastructure.dataobject.PatProfileDO;
import com.cariesguard.patient.infrastructure.mapper.PatGuardianMapper;
import com.cariesguard.patient.infrastructure.mapper.PatPatientMapper;
import com.cariesguard.patient.infrastructure.mapper.PatProfileMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

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

    @Override
    public PageQueryResult<PatientSummaryModel> pagePatients(Long orgId,
                                                             int pageNo,
                                                             int pageSize,
                                                             String keyword,
                                                             String sourceCode,
                                                             String status) {
        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<PatPatientDO> wrapper = new LambdaQueryWrapper<PatPatientDO>()
                .eq(PatPatientDO::getDeletedFlag, 0L)
                .eq(orgId != null, PatPatientDO::getOrgId, orgId)
                .eq(StringUtils.hasText(sourceCode), PatPatientDO::getSourceCode, trim(sourceCode))
                .eq(StringUtils.hasText(status), PatPatientDO::getStatus, trim(status))
                .and(StringUtils.hasText(keyword), item -> item
                        .like(PatPatientDO::getPatientNo, trim(keyword))
                        .or()
                        .like(PatPatientDO::getPatientNameMasked, trim(keyword))
                        .or()
                        .like(PatPatientDO::getPhoneMasked, trim(keyword))
                        .or()
                        .like(PatPatientDO::getIdCardMasked, trim(keyword)));
        long total = patPatientMapper.selectCount(wrapper);
        var records = patPatientMapper.selectList(wrapper
                        .orderByDesc(PatPatientDO::getCreatedAt)
                        .orderByDesc(PatPatientDO::getId)
                        .last("LIMIT " + ((safePageNo - 1) * safePageSize) + "," + safePageSize))
                .stream()
                .map(item -> new PatientSummaryModel(
                        item.getId(),
                        item.getPatientNo(),
                        item.getPatientNameMasked(),
                        item.getGenderCode(),
                        item.getAge(),
                        item.getPhoneMasked(),
                        item.getSourceCode(),
                        item.getFirstVisitDate(),
                        item.getStatus()))
                .toList();
        return new PageQueryResult<>(records, total, safePageNo, safePageSize);
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

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
