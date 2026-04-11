package com.cariesguard.system.domain.repository;

import com.cariesguard.system.domain.model.DictItemModel;
import com.cariesguard.system.domain.model.DictTypeModel;
import java.util.List;

public interface SystemDictionaryRepository {

    List<DictTypeModel> findActiveTypesByOrgId(Long orgId);

    List<DictItemModel> findActiveItemsByType(String dictType, Long orgId);
}
