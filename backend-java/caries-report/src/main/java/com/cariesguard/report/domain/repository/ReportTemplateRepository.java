package com.cariesguard.report.domain.repository;

import com.cariesguard.report.domain.model.ReportTemplateModel;
import java.util.List;
import java.util.Optional;

public interface ReportTemplateRepository {

    Optional<ReportTemplateModel> findById(Long templateId);

    Optional<ReportTemplateModel> findLatestActive(Long orgId, String reportTypeCode);

    List<ReportTemplateModel> listByOrgAndType(Long orgId, String reportTypeCode);

    void create(ReportTemplateModel model);

    void update(ReportTemplateModel model);
}

