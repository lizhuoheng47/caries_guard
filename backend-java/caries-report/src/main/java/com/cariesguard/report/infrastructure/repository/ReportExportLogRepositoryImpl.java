package com.cariesguard.report.infrastructure.repository;

import com.cariesguard.report.domain.model.ReportExportLogModel;
import com.cariesguard.report.domain.repository.ReportExportLogRepository;
import com.cariesguard.report.infrastructure.dataobject.RptExportLogDO;
import com.cariesguard.report.infrastructure.mapper.RptExportLogMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ReportExportLogRepositoryImpl implements ReportExportLogRepository {

    private final RptExportLogMapper rptExportLogMapper;

    public ReportExportLogRepositoryImpl(RptExportLogMapper rptExportLogMapper) {
        this.rptExportLogMapper = rptExportLogMapper;
    }

    @Override
    public void create(ReportExportLogModel model) {
        RptExportLogDO entity = new RptExportLogDO();
        entity.setId(model.exportLogId());
        entity.setReportId(model.reportId());
        entity.setAttachmentId(model.attachmentId());
        entity.setExportTypeCode(model.exportTypeCode());
        entity.setExportChannelCode(model.exportChannelCode());
        entity.setExportedBy(model.exportedBy());
        entity.setExportedAt(model.exportedAt());
        entity.setOrgId(model.orgId());
        rptExportLogMapper.insert(entity);
    }
}

