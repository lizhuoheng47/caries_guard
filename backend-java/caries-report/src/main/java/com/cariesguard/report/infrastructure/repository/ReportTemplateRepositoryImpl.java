package com.cariesguard.report.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cariesguard.report.domain.model.ReportTemplateModel;
import com.cariesguard.report.domain.repository.ReportTemplateRepository;
import com.cariesguard.report.infrastructure.dataobject.RptTemplateDO;
import com.cariesguard.report.infrastructure.mapper.RptTemplateMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class ReportTemplateRepositoryImpl implements ReportTemplateRepository {

    private final RptTemplateMapper rptTemplateMapper;

    public ReportTemplateRepositoryImpl(RptTemplateMapper rptTemplateMapper) {
        this.rptTemplateMapper = rptTemplateMapper;
    }

    @Override
    public Optional<ReportTemplateModel> findById(Long templateId) {
        RptTemplateDO entity = rptTemplateMapper.selectOne(new LambdaQueryWrapper<RptTemplateDO>()
                .eq(RptTemplateDO::getId, templateId)
                .eq(RptTemplateDO::getDeletedFlag, 0L)
                .last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.of(toModel(entity));
    }

    @Override
    public Optional<ReportTemplateModel> findLatestActive(Long orgId, String reportTypeCode) {
        RptTemplateDO entity = rptTemplateMapper.selectOne(new LambdaQueryWrapper<RptTemplateDO>()
                .eq(RptTemplateDO::getOrgId, orgId)
                .eq(RptTemplateDO::getReportTypeCode, reportTypeCode)
                .eq(RptTemplateDO::getStatus, "ACTIVE")
                .eq(RptTemplateDO::getDeletedFlag, 0L)
                .orderByDesc(RptTemplateDO::getVersionNo)
                .orderByDesc(RptTemplateDO::getId)
                .last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.of(toModel(entity));
    }

    @Override
    public List<ReportTemplateModel> listByOrgAndType(Long orgId, String reportTypeCode) {
        return rptTemplateMapper.selectList(new LambdaQueryWrapper<RptTemplateDO>()
                        .eq(RptTemplateDO::getOrgId, orgId)
                        .eq(StringUtils.hasText(reportTypeCode), RptTemplateDO::getReportTypeCode, reportTypeCode)
                        .eq(RptTemplateDO::getDeletedFlag, 0L)
                        .orderByDesc(RptTemplateDO::getVersionNo)
                        .orderByDesc(RptTemplateDO::getId))
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public void create(ReportTemplateModel model) {
        RptTemplateDO entity = new RptTemplateDO();
        entity.setId(model.templateId());
        entity.setTemplateCode(model.templateCode());
        entity.setTemplateName(model.templateName());
        entity.setReportTypeCode(model.reportTypeCode());
        entity.setTemplateContent(model.templateContent());
        entity.setVersionNo(model.versionNo());
        entity.setOrgId(model.orgId());
        entity.setStatus(model.status());
        entity.setDeletedFlag(0L);
        entity.setRemark(model.remark());
        entity.setCreatedBy(model.operatorUserId());
        entity.setUpdatedBy(model.operatorUserId());
        rptTemplateMapper.insert(entity);
    }

    @Override
    public void update(ReportTemplateModel model) {
        rptTemplateMapper.update(null, new LambdaUpdateWrapper<RptTemplateDO>()
                .eq(RptTemplateDO::getId, model.templateId())
                .eq(RptTemplateDO::getDeletedFlag, 0L)
                .set(StringUtils.hasText(model.templateName()), RptTemplateDO::getTemplateName, model.templateName())
                .set(StringUtils.hasText(model.templateContent()), RptTemplateDO::getTemplateContent, model.templateContent())
                .set(StringUtils.hasText(model.status()), RptTemplateDO::getStatus, model.status())
                .set(RptTemplateDO::getRemark, model.remark())
                .set(RptTemplateDO::getUpdatedBy, model.operatorUserId())
                .setSql("version_no = version_no + 1"));
    }

    private ReportTemplateModel toModel(RptTemplateDO entity) {
        return new ReportTemplateModel(
                entity.getId(),
                entity.getTemplateCode(),
                entity.getTemplateName(),
                entity.getReportTypeCode(),
                entity.getTemplateContent(),
                entity.getVersionNo(),
                entity.getOrgId(),
                entity.getStatus(),
                entity.getRemark(),
                null,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}

