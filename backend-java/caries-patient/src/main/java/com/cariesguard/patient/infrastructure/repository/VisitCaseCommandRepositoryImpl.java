package com.cariesguard.patient.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cariesguard.patient.domain.model.CaseCreateModel;
import com.cariesguard.patient.domain.model.CaseDiagnosisCreateModel;
import com.cariesguard.patient.domain.model.CaseManagedModel;
import com.cariesguard.patient.domain.model.CaseStatusLogCreateModel;
import com.cariesguard.patient.domain.model.CaseStatusUpdateModel;
import com.cariesguard.patient.domain.model.CaseToothRecordCreateModel;
import com.cariesguard.patient.domain.model.PatientOwnedModel;
import com.cariesguard.patient.domain.model.VisitCreateModel;
import com.cariesguard.patient.domain.model.VisitOwnedModel;
import com.cariesguard.patient.domain.repository.VisitCaseCommandRepository;
import com.cariesguard.patient.infrastructure.dataobject.AnaResultSummaryDO;
import com.cariesguard.patient.infrastructure.dataobject.MedCaseDO;
import com.cariesguard.patient.infrastructure.dataobject.MedCaseDiagnosisDO;
import com.cariesguard.patient.infrastructure.dataobject.MedCaseStatusLogDO;
import com.cariesguard.patient.infrastructure.dataobject.MedCaseToothRecordDO;
import com.cariesguard.patient.infrastructure.dataobject.MedImageFileDO;
import com.cariesguard.patient.infrastructure.dataobject.MedVisitDO;
import com.cariesguard.patient.infrastructure.dataobject.PatPatientDO;
import com.cariesguard.patient.infrastructure.mapper.AnaResultSummaryMapper;
import com.cariesguard.patient.infrastructure.mapper.MedCaseMapper;
import com.cariesguard.patient.infrastructure.mapper.MedCaseDiagnosisMapper;
import com.cariesguard.patient.infrastructure.mapper.MedCaseStatusLogMapper;
import com.cariesguard.patient.infrastructure.mapper.MedCaseToothRecordMapper;
import com.cariesguard.patient.infrastructure.mapper.MedImageFileMapper;
import com.cariesguard.patient.infrastructure.mapper.MedVisitMapper;
import com.cariesguard.patient.infrastructure.mapper.PatPatientMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class VisitCaseCommandRepositoryImpl implements VisitCaseCommandRepository {

    private final PatPatientMapper patPatientMapper;
    private final MedVisitMapper medVisitMapper;
    private final MedCaseMapper medCaseMapper;
    private final MedCaseDiagnosisMapper medCaseDiagnosisMapper;
    private final MedCaseStatusLogMapper medCaseStatusLogMapper;
    private final MedCaseToothRecordMapper medCaseToothRecordMapper;
    private final MedImageFileMapper medImageFileMapper;
    private final AnaResultSummaryMapper anaResultSummaryMapper;

    public VisitCaseCommandRepositoryImpl(PatPatientMapper patPatientMapper,
                                          MedVisitMapper medVisitMapper,
                                          MedCaseMapper medCaseMapper,
                                          MedCaseDiagnosisMapper medCaseDiagnosisMapper,
                                          MedCaseStatusLogMapper medCaseStatusLogMapper,
                                          MedCaseToothRecordMapper medCaseToothRecordMapper,
                                          MedImageFileMapper medImageFileMapper,
                                          AnaResultSummaryMapper anaResultSummaryMapper) {
        this.patPatientMapper = patPatientMapper;
        this.medVisitMapper = medVisitMapper;
        this.medCaseMapper = medCaseMapper;
        this.medCaseDiagnosisMapper = medCaseDiagnosisMapper;
        this.medCaseStatusLogMapper = medCaseStatusLogMapper;
        this.medCaseToothRecordMapper = medCaseToothRecordMapper;
        this.medImageFileMapper = medImageFileMapper;
        this.anaResultSummaryMapper = anaResultSummaryMapper;
    }

    @Override
    public Optional<PatientOwnedModel> findPatient(Long patientId) {
        PatPatientDO patient = patPatientMapper.selectOne(new LambdaQueryWrapper<PatPatientDO>()
                .eq(PatPatientDO::getId, patientId)
                .eq(PatPatientDO::getDeletedFlag, 0L)
                .eq(PatPatientDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return patient == null ? Optional.empty() : Optional.of(new PatientOwnedModel(patient.getId(), patient.getOrgId()));
    }

    @Override
    public Optional<VisitOwnedModel> findVisit(Long visitId) {
        MedVisitDO visit = medVisitMapper.selectOne(new LambdaQueryWrapper<MedVisitDO>()
                .eq(MedVisitDO::getId, visitId)
                .eq(MedVisitDO::getDeletedFlag, 0L)
                .eq(MedVisitDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return visit == null
                ? Optional.empty()
                : Optional.of(new VisitOwnedModel(visit.getId(), visit.getPatientId(), visit.getDoctorUserId(), visit.getOrgId()));
    }

    @Override
    public void createVisit(VisitCreateModel model) {
        MedVisitDO entity = new MedVisitDO();
        entity.setId(model.visitId());
        entity.setVisitNo(model.visitNo());
        entity.setPatientId(model.patientId());
        entity.setDepartmentId(model.departmentId());
        entity.setDoctorUserId(model.doctorUserId());
        entity.setVisitTypeCode(model.visitTypeCode());
        entity.setVisitDate(model.visitDate());
        entity.setComplaint(model.complaint());
        entity.setTriageLevelCode(model.triageLevelCode());
        entity.setSourceChannelCode(model.sourceChannelCode());
        entity.setOrgId(model.orgId());
        entity.setStatus(model.status());
        entity.setDeletedFlag(0L);
        entity.setRemark(model.remark());
        entity.setCreatedBy(model.operatorUserId());
        entity.setUpdatedBy(model.operatorUserId());
        medVisitMapper.insert(entity);
    }

    @Override
    public void createCase(CaseCreateModel model, CaseStatusLogCreateModel statusLog) {
        MedCaseDO entity = new MedCaseDO();
        entity.setId(model.caseId());
        entity.setCaseNo(model.caseNo());
        entity.setVisitId(model.visitId());
        entity.setPatientId(model.patientId());
        entity.setCaseTitle(model.caseTitle());
        entity.setCaseTypeCode(model.caseTypeCode());
        entity.setCaseStatusCode(model.caseStatusCode());
        entity.setPriorityCode(model.priorityCode());
        entity.setChiefComplaint(model.chiefComplaint());
        entity.setClinicalNotes(model.clinicalNotes());
        entity.setOnsetDate(model.onsetDate());
        entity.setAttendingDoctorId(model.attendingDoctorId());
        entity.setScreenerUserId(model.screenerUserId());
        entity.setReportReadyFlag(model.reportReadyFlag());
        entity.setFollowupRequiredFlag(model.followupRequiredFlag());
        entity.setOrgId(model.orgId());
        entity.setVersionNo(1);
        entity.setStatus(model.status());
        entity.setDeletedFlag(0L);
        entity.setRemark(model.remark());
        entity.setCreatedBy(model.operatorUserId());
        entity.setUpdatedBy(model.operatorUserId());
        medCaseMapper.insert(entity);
        medCaseStatusLogMapper.insert(toStatusLogDO(statusLog));
    }

    @Override
    public Optional<CaseManagedModel> findManagedCase(Long caseId) {
        MedCaseDO entity = medCaseMapper.selectOne(new LambdaQueryWrapper<MedCaseDO>()
                .eq(MedCaseDO::getId, caseId)
                .eq(MedCaseDO::getDeletedFlag, 0L)
                .eq(MedCaseDO::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return entity == null
                ? Optional.empty()
                : Optional.of(new CaseManagedModel(
                        entity.getId(),
                        entity.getCaseStatusCode(),
                        entity.getReportReadyFlag(),
                        entity.getFollowupRequiredFlag(),
                        entity.getOrgId()));
    }

    @Override
    public boolean hasActiveImage(Long caseId) {
        return medImageFileMapper.selectCount(new LambdaQueryWrapper<MedImageFileDO>()
                .eq(MedImageFileDO::getCaseId, caseId)
                .eq(MedImageFileDO::getDeletedFlag, 0L)
                .eq(MedImageFileDO::getStatus, "ACTIVE")) > 0;
    }

    @Override
    public boolean hasAiSummary(Long caseId) {
        return anaResultSummaryMapper.selectCount(new LambdaQueryWrapper<AnaResultSummaryDO>()
                .eq(AnaResultSummaryDO::getCaseId, caseId)
                .eq(AnaResultSummaryDO::getDeletedFlag, 0L)
                .eq(AnaResultSummaryDO::getStatus, "ACTIVE")) > 0;
    }

    @Override
    public void updateCaseStatus(CaseStatusUpdateModel model) {
        medCaseMapper.update(null, new LambdaUpdateWrapper<MedCaseDO>()
                .eq(MedCaseDO::getId, model.caseId())
                .eq(MedCaseDO::getDeletedFlag, 0L)
                .set(MedCaseDO::getCaseStatusCode, model.caseStatusCode())
                .set(MedCaseDO::getReportReadyFlag, model.reportReadyFlag())
                .set(MedCaseDO::getFollowupRequiredFlag, model.followupRequiredFlag())
                .set(MedCaseDO::getClosedAt, model.closedAt())
                .set(MedCaseDO::getUpdatedBy, model.operatorUserId())
                .setSql("version_no = version_no + 1"));
    }

    @Override
    public void appendCaseStatusLog(CaseStatusLogCreateModel model) {
        medCaseStatusLogMapper.insert(toStatusLogDO(model));
    }

    @Override
    public void replaceDiagnoses(Long caseId,
                                 Long operatorUserId,
                                 LocalDateTime diagnosisTime,
                                 List<CaseDiagnosisCreateModel> diagnoses) {
        medCaseDiagnosisMapper.update(null, new LambdaUpdateWrapper<MedCaseDiagnosisDO>()
                .eq(MedCaseDiagnosisDO::getCaseId, caseId)
                .eq(MedCaseDiagnosisDO::getDeletedFlag, 0L)
                .setSql("deleted_flag = id")
                .set(MedCaseDiagnosisDO::getUpdatedBy, operatorUserId));
        for (CaseDiagnosisCreateModel diagnosis : diagnoses) {
            medCaseDiagnosisMapper.insert(toDiagnosisDO(diagnosis));
        }
        medCaseMapper.update(null, new LambdaUpdateWrapper<MedCaseDO>()
                .eq(MedCaseDO::getId, caseId)
                .eq(MedCaseDO::getDeletedFlag, 0L)
                .set(MedCaseDO::getUpdatedBy, operatorUserId)
                .set(MedCaseDO::getFirstDiagnosisAt, diagnosisTime)
                .setSql("version_no = version_no + 1"));
    }

    @Override
    public void replaceToothRecords(Long caseId,
                                    Long operatorUserId,
                                    List<CaseToothRecordCreateModel> toothRecords) {
        medCaseToothRecordMapper.update(null, new LambdaUpdateWrapper<MedCaseToothRecordDO>()
                .eq(MedCaseToothRecordDO::getCaseId, caseId)
                .eq(MedCaseToothRecordDO::getDeletedFlag, 0L)
                .setSql("deleted_flag = id")
                .set(MedCaseToothRecordDO::getUpdatedBy, operatorUserId));
        for (CaseToothRecordCreateModel toothRecord : toothRecords) {
            medCaseToothRecordMapper.insert(toToothRecordDO(toothRecord));
        }
    }

    private MedCaseStatusLogDO toStatusLogDO(CaseStatusLogCreateModel model) {
        MedCaseStatusLogDO entity = new MedCaseStatusLogDO();
        entity.setId(model.logId());
        entity.setCaseId(model.caseId());
        entity.setFromStatusCode(model.fromStatusCode());
        entity.setToStatusCode(model.toStatusCode());
        entity.setChangedBy(model.changedBy());
        entity.setChangeReasonCode(model.changeReasonCode());
        entity.setChangeReason(model.changeReason());
        entity.setChangedAt(model.changedAt());
        entity.setOrgId(model.orgId());
        return entity;
    }

    private MedCaseDiagnosisDO toDiagnosisDO(CaseDiagnosisCreateModel model) {
        MedCaseDiagnosisDO entity = new MedCaseDiagnosisDO();
        entity.setId(model.diagnosisId());
        entity.setCaseId(model.caseId());
        entity.setDiagnosisTypeCode(model.diagnosisTypeCode());
        entity.setDiagnosisName(model.diagnosisName());
        entity.setSeverityCode(model.severityCode());
        entity.setDiagnosisBasis(model.diagnosisBasis());
        entity.setDiagnosisDesc(model.diagnosisDesc());
        entity.setTreatmentAdvice(model.treatmentAdvice());
        entity.setReviewDoctorId(model.reviewDoctorId());
        entity.setReviewTime(model.reviewTime());
        entity.setIsFinal(model.finalFlag());
        entity.setOrgId(model.orgId());
        entity.setVersionNo(1);
        entity.setStatus(model.status());
        entity.setDeletedFlag(0L);
        entity.setRemark(model.remark());
        entity.setCreatedBy(model.operatorUserId());
        entity.setUpdatedBy(model.operatorUserId());
        return entity;
    }

    private MedCaseToothRecordDO toToothRecordDO(CaseToothRecordCreateModel model) {
        MedCaseToothRecordDO entity = new MedCaseToothRecordDO();
        entity.setId(model.recordId());
        entity.setCaseId(model.caseId());
        entity.setSourceImageId(model.sourceImageId());
        entity.setToothCode(model.toothCode());
        entity.setToothSurfaceCode(model.toothSurfaceCode());
        entity.setIssueTypeCode(model.issueTypeCode());
        entity.setSeverityCode(model.severityCode());
        entity.setFindingDesc(model.findingDesc());
        entity.setSuggestion(model.suggestion());
        entity.setSortOrder(model.sortOrder());
        entity.setReviewedBy(model.reviewedBy());
        entity.setReviewedAt(model.reviewedAt());
        entity.setOrgId(model.orgId());
        entity.setStatus(model.status());
        entity.setDeletedFlag(0L);
        entity.setRemark(model.remark());
        entity.setCreatedBy(model.operatorUserId());
        entity.setUpdatedBy(model.operatorUserId());
        return entity;
    }
}
