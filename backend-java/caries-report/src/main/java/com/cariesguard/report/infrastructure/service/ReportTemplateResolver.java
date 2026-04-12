package com.cariesguard.report.infrastructure.service;

import com.cariesguard.report.domain.repository.ReportTemplateRepository;
import com.cariesguard.report.domain.service.ReportTemplateDomainService;
import org.springframework.stereotype.Component;

@Component
public class ReportTemplateResolver {

    private final ReportTemplateRepository reportTemplateRepository;
    private final ReportTemplateDomainService reportTemplateDomainService;

    public ReportTemplateResolver(ReportTemplateRepository reportTemplateRepository,
                                  ReportTemplateDomainService reportTemplateDomainService) {
        this.reportTemplateRepository = reportTemplateRepository;
        this.reportTemplateDomainService = reportTemplateDomainService;
    }

    public String resolveContent(Long orgId, String reportTypeCode) {
        return reportTemplateDomainService.resolveTemplateContent(
                reportTypeCode,
                reportTemplateRepository.findLatestActive(orgId, reportTypeCode));
    }
}

