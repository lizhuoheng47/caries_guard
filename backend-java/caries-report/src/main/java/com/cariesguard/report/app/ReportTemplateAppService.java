package com.cariesguard.report.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.report.domain.model.ReportTemplateModel;
import com.cariesguard.report.domain.repository.ReportTemplateRepository;
import com.cariesguard.report.domain.service.ReportDomainService;
import com.cariesguard.report.domain.service.ReportTemplateDomainService;
import com.cariesguard.report.interfaces.command.CreateReportTemplateCommand;
import com.cariesguard.report.interfaces.command.UpdateReportTemplateCommand;
import com.cariesguard.report.interfaces.query.ReportTemplateListQuery;
import com.cariesguard.report.interfaces.vo.ReportTemplateMutationVO;
import com.cariesguard.report.interfaces.vo.ReportTemplateVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReportTemplateAppService {

    private final ReportTemplateRepository reportTemplateRepository;
    private final ReportDomainService reportDomainService;
    private final ReportTemplateDomainService reportTemplateDomainService;

    public ReportTemplateAppService(ReportTemplateRepository reportTemplateRepository,
                                    ReportDomainService reportDomainService,
                                    ReportTemplateDomainService reportTemplateDomainService) {
        this.reportTemplateRepository = reportTemplateRepository;
        this.reportDomainService = reportDomainService;
        this.reportTemplateDomainService = reportTemplateDomainService;
    }

    public ReportTemplateMutationVO createTemplate(CreateReportTemplateCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        String reportTypeCode = reportDomainService.normalizeReportType(command.reportTypeCode());
        reportTemplateDomainService.validateTemplateContent(command.templateContent());
        long templateId = IdWorker.getId();
        reportTemplateRepository.create(new ReportTemplateModel(
                templateId,
                command.templateCode().trim(),
                command.templateName().trim(),
                reportTypeCode,
                command.templateContent(),
                1,
                operator.getOrgId(),
                defaultStatus(command.status()),
                trimToNull(command.remark()),
                operator.getUserId(),
                null,
                null));
        return new ReportTemplateMutationVO(templateId, command.templateCode().trim(), 1, defaultStatus(command.status()));
    }

    public ReportTemplateMutationVO updateTemplate(Long templateId, UpdateReportTemplateCommand command) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        ReportTemplateModel template = loadTemplate(templateId);
        ensureOrgAccess(operator, template.orgId());
        reportTemplateRepository.update(new ReportTemplateModel(
                template.templateId(),
                template.templateCode(),
                trimToNull(command.templateName()),
                template.reportTypeCode(),
                command.templateContent(),
                template.versionNo(),
                template.orgId(),
                trimToNull(command.status()),
                trimToNull(command.remark()),
                operator.getUserId(),
                template.createdAt(),
                template.updatedAt()));
        ReportTemplateModel updated = loadTemplate(templateId);
        return new ReportTemplateMutationVO(
                updated.templateId(),
                updated.templateCode(),
                updated.versionNo(),
                updated.status());
    }

    public List<ReportTemplateVO> listTemplates(ReportTemplateListQuery query) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        String reportTypeCode = StringUtils.hasText(query.reportTypeCode())
                ? reportDomainService.normalizeReportType(query.reportTypeCode())
                : null;
        return reportTemplateRepository.listByOrgAndType(operator.getOrgId(), reportTypeCode).stream()
                .map(this::toTemplateVO)
                .toList();
    }

    public ReportTemplateVO getTemplate(Long templateId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        ReportTemplateModel template = loadTemplate(templateId);
        ensureOrgAccess(operator, template.orgId());
        return toTemplateVO(template);
    }

    private ReportTemplateVO toTemplateVO(ReportTemplateModel model) {
        return new ReportTemplateVO(
                model.templateId(),
                model.templateCode(),
                model.templateName(),
                model.reportTypeCode(),
                model.templateContent(),
                model.versionNo(),
                model.status(),
                model.remark(),
                model.createdAt(),
                model.updatedAt());
    }

    private ReportTemplateModel loadTemplate(Long templateId) {
        return reportTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Template does not exist"));
    }

    private String defaultStatus(String status) {
        return StringUtils.hasText(status) ? status.trim() : "ACTIVE";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}

