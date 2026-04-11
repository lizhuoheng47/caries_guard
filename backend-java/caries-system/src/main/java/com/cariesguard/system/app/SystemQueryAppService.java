package com.cariesguard.system.app;

import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.framework.security.sensitive.MaskingService;
import com.cariesguard.system.domain.model.DictItemModel;
import com.cariesguard.system.domain.model.DictTypeModel;
import com.cariesguard.system.domain.model.SystemConfigModel;
import com.cariesguard.system.domain.repository.SystemConfigRepository;
import com.cariesguard.system.domain.repository.SystemDictionaryRepository;
import com.cariesguard.system.interfaces.vo.DictItemVO;
import com.cariesguard.system.interfaces.vo.DictTypeVO;
import com.cariesguard.system.interfaces.vo.SystemConfigVO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SystemQueryAppService {

    private final SystemDictionaryRepository systemDictionaryRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final MaskingService maskingService;

    public SystemQueryAppService(SystemDictionaryRepository systemDictionaryRepository,
                                 SystemConfigRepository systemConfigRepository,
                                 MaskingService maskingService) {
        this.systemDictionaryRepository = systemDictionaryRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.maskingService = maskingService;
    }

    public List<DictTypeVO> listDictTypes() {
        Long orgId = SecurityContextUtils.currentDataScope().orgId();
        return systemDictionaryRepository.findActiveTypesByOrgId(orgId).stream()
                .map(this::toDictTypeVO)
                .toList();
    }

    public List<DictItemVO> listDictItems(String dictType) {
        Long orgId = SecurityContextUtils.currentDataScope().orgId();
        return systemDictionaryRepository.findActiveItemsByType(dictType, orgId).stream()
                .map(this::toDictItemVO)
                .toList();
    }

    public SystemConfigVO getConfig(String configKey) {
        AuthenticatedUser currentUser = SecurityContextUtils.currentUser();
        SystemConfigModel config = systemConfigRepository.findActiveByKey(configKey, currentUser.getOrgId())
                .orElse(null);
        if (config == null) {
            return null;
        }
        String configValue = config.configValue();
        if (config.sensitive() && !currentUser.hasAnyRole("SYS_ADMIN", "ADMIN")) {
            configValue = maskingService.maskGeneric(configValue);
        }
        return new SystemConfigVO(
                config.configKey(),
                config.configName(),
                configValue,
                config.valueTypeCode(),
                config.sensitive());
    }

    private DictTypeVO toDictTypeVO(DictTypeModel model) {
        return new DictTypeVO(model.dictType(), model.dictName(), model.systemFlag(), model.sortOrder());
    }

    private DictItemVO toDictItemVO(DictItemModel model) {
        return new DictItemVO(model.label(), model.value(), model.code(), model.sortOrder(), model.defaultFlag());
    }
}
