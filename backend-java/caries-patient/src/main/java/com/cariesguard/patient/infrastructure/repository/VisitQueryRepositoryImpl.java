package com.cariesguard.patient.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cariesguard.patient.domain.model.PageQueryResult;
import com.cariesguard.patient.domain.model.VisitDetailModel;
import com.cariesguard.patient.domain.model.VisitSummaryModel;
import com.cariesguard.patient.domain.repository.VisitQueryRepository;
import com.cariesguard.patient.infrastructure.dataobject.MedVisitDO;
import com.cariesguard.patient.infrastructure.mapper.MedVisitMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class VisitQueryRepositoryImpl implements VisitQueryRepository {

    private final MedVisitMapper medVisitMapper;

    public VisitQueryRepositoryImpl(MedVisitMapper medVisitMapper) {
        this.medVisitMapper = medVisitMapper;
    }

    @Override
    public Optional<VisitDetailModel> findVisitDetail(Long visitId) {
        MedVisitDO visit = medVisitMapper.selectOne(new LambdaQueryWrapper<MedVisitDO>()
                .eq(MedVisitDO::getId, visitId)
                .eq(MedVisitDO::getDeletedFlag, 0L)
                .last("LIMIT 1"));
        if (visit == null) {
            return Optional.empty();
        }
        return Optional.of(new VisitDetailModel(
                visit.getId(),
                visit.getVisitNo(),
                visit.getPatientId(),
                visit.getDepartmentId(),
                visit.getDoctorUserId(),
                visit.getVisitTypeCode(),
                visit.getVisitDate(),
                visit.getComplaint(),
                visit.getTriageLevelCode(),
                visit.getSourceChannelCode(),
                visit.getOrgId(),
                visit.getStatus(),
                visit.getRemark()));
    }

    @Override
    public PageQueryResult<VisitSummaryModel> pageVisits(Long orgId,
                                                         int pageNo,
                                                         int pageSize,
                                                         Long patientId,
                                                         Long doctorUserId,
                                                         String visitTypeCode) {
        int safePageNo = Math.max(pageNo, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<MedVisitDO> wrapper = new LambdaQueryWrapper<MedVisitDO>()
                .eq(MedVisitDO::getDeletedFlag, 0L)
                .eq(orgId != null, MedVisitDO::getOrgId, orgId)
                .eq(patientId != null, MedVisitDO::getPatientId, patientId)
                .eq(doctorUserId != null, MedVisitDO::getDoctorUserId, doctorUserId)
                .eq(StringUtils.hasText(visitTypeCode), MedVisitDO::getVisitTypeCode, visitTypeCode == null ? null : visitTypeCode.trim());
        long total = medVisitMapper.selectCount(wrapper);
        var records = medVisitMapper.selectList(wrapper
                .orderByDesc(MedVisitDO::getVisitDate)
                .orderByDesc(MedVisitDO::getId)
                .last("LIMIT " + ((safePageNo - 1) * safePageSize) + "," + safePageSize))
                .stream()
                .map(item -> new VisitSummaryModel(
                        item.getId(),
                        item.getVisitNo(),
                        item.getPatientId(),
                        item.getDoctorUserId(),
                        item.getVisitTypeCode(),
                        item.getVisitDate(),
                        item.getTriageLevelCode(),
                        item.getStatus()))
                .toList();
        return new PageQueryResult<>(records, total, safePageNo, safePageSize);
    }
}
