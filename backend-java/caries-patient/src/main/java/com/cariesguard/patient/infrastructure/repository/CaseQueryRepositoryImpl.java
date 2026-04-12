package com.cariesguard.patient.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cariesguard.patient.domain.model.CaseDetailModel;
import com.cariesguard.patient.domain.model.CaseDiagnosisModel;
import com.cariesguard.patient.domain.model.CaseImageModel;
import com.cariesguard.patient.domain.model.CaseSummaryModel;
import com.cariesguard.patient.domain.model.PageQueryResult;
import com.cariesguard.patient.domain.repository.CaseQueryRepository;
import com.cariesguard.patient.infrastructure.dataobject.AnaResultSummaryDO;
import com.cariesguard.patient.infrastructure.dataobject.MedCaseDO;
import com.cariesguard.patient.infrastructure.dataobject.MedCaseDiagnosisDO;
import com.cariesguard.patient.infrastructure.dataobject.MedImageFileDO;
import com.cariesguard.patient.infrastructure.mapper.AnaResultSummaryMapper;
import com.cariesguard.patient.infrastructure.mapper.MedCaseDiagnosisMapper;
import com.cariesguard.patient.infrastructure.mapper.MedCaseMapper;
import com.cariesguard.patient.infrastructure.mapper.MedImageFileMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class CaseQueryRepositoryImpl implements CaseQueryRepository {

    private final MedCaseMapper medCaseMapper;
    private final MedImageFileMapper medImageFileMapper;
    private final MedCaseDiagnosisMapper medCaseDiagnosisMapper;
    private final AnaResultSummaryMapper anaResultSummaryMapper;

    public CaseQueryRepositoryImpl(MedCaseMapper medCaseMapper,
                                   MedImageFileMapper medImageFileMapper,
                                   MedCaseDiagnosisMapper medCaseDiagnosisMapper,
                                   AnaResultSummaryMapper anaResultSummaryMapper) {
        this.medCaseMapper = medCaseMapper;
        this.medImageFileMapper = medImageFileMapper;
        this.medCaseDiagnosisMapper = medCaseDiagnosisMapper;
        this.anaResultSummaryMapper = anaResultSummaryMapper;
    }

    @Override
    public Optional<CaseDetailModel> findCaseDetail(Long caseId) {
        MedCaseDO entity = medCaseMapper.selectOne(new LambdaQueryWrapper<MedCaseDO>()
                .eq(MedCaseDO::getId, caseId)
                .eq(MedCaseDO::getDeletedFlag, 0L)
                .eq(MedCaseDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        if (entity == null) {
            return Optional.empty();
        }
        AnaResultSummaryDO latestSummary = anaResultSummaryMapper.selectOne(new LambdaQueryWrapper<AnaResultSummaryDO>()
                .eq(AnaResultSummaryDO::getCaseId, caseId)
                .eq(AnaResultSummaryDO::getDeletedFlag, 0L)
                .eq(AnaResultSummaryDO::getStatus, "ACTIVE")
                .orderByDesc(AnaResultSummaryDO::getId)
                .last("LIMIT 1"));
        return Optional.of(new CaseDetailModel(
                entity.getId(),
                entity.getCaseNo(),
                entity.getPatientId(),
                entity.getVisitId(),
                entity.getCaseStatusCode(),
                entity.getReportReadyFlag(),
                entity.getFollowupRequiredFlag(),
                entity.getOrgId(),
                medImageFileMapper.selectList(new LambdaQueryWrapper<MedImageFileDO>()
                                .eq(MedImageFileDO::getCaseId, caseId)
                                .eq(MedImageFileDO::getDeletedFlag, 0L)
                                .eq(MedImageFileDO::getStatus, "ACTIVE")
                                .orderByDesc(MedImageFileDO::getIsPrimary)
                                .orderByAsc(MedImageFileDO::getId))
                        .stream()
                        .map(item -> new CaseImageModel(item.getId(), item.getImageTypeCode(), item.getQualityStatusCode(), item.getIsPrimary()))
                        .toList(),
                medCaseDiagnosisMapper.selectList(new LambdaQueryWrapper<MedCaseDiagnosisDO>()
                                .eq(MedCaseDiagnosisDO::getCaseId, caseId)
                                .eq(MedCaseDiagnosisDO::getDeletedFlag, 0L)
                                .eq(MedCaseDiagnosisDO::getStatus, "ACTIVE")
                                .orderByDesc(MedCaseDiagnosisDO::getIsFinal)
                                .orderByAsc(MedCaseDiagnosisDO::getId))
                        .stream()
                        .map(item -> new CaseDiagnosisModel(item.getDiagnosisName(), item.getSeverityCode(), item.getIsFinal()))
                        .toList(),
                latestSummary == null ? null : latestSummary.getRawResultJson()));
    }

    @Override
    public PageQueryResult<CaseSummaryModel> pageCases(Long orgId,
                                                       int pageNo,
                                                       int pageSize,
                                                       Long patientId,
                                                       String caseStatusCode,
                                                       Long attendingDoctorId) {
        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<MedCaseDO> wrapper = new LambdaQueryWrapper<MedCaseDO>()
                .eq(MedCaseDO::getDeletedFlag, 0L)
                .eq(orgId != null, MedCaseDO::getOrgId, orgId)
                .eq(patientId != null, MedCaseDO::getPatientId, patientId)
                .eq(attendingDoctorId != null, MedCaseDO::getAttendingDoctorId, attendingDoctorId)
                .eq(StringUtils.hasText(caseStatusCode), MedCaseDO::getCaseStatusCode, caseStatusCode == null ? null : caseStatusCode.trim());
        long total = medCaseMapper.selectCount(wrapper);
        var records = medCaseMapper.selectList(wrapper
                        .orderByDesc(MedCaseDO::getCreatedAt)
                        .orderByDesc(MedCaseDO::getId)
                        .last("LIMIT " + ((safePageNo - 1) * safePageSize) + "," + safePageSize))
                .stream()
                .map(item -> new CaseSummaryModel(
                        item.getId(),
                        item.getCaseNo(),
                        item.getPatientId(),
                        item.getVisitId(),
                        item.getCaseTitle(),
                        item.getCaseStatusCode(),
                        item.getPriorityCode(),
                        item.getAttendingDoctorId(),
                        item.getReportReadyFlag(),
                        item.getFollowupRequiredFlag(),
                        item.getCreatedAt()))
                .toList();
        return new PageQueryResult<>(records, total, safePageNo, safePageSize);
    }
}
