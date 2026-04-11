package com.cariesguard.system.domain.repository;

import com.cariesguard.system.domain.model.DictItemModel;
import com.cariesguard.system.domain.model.DictTypeModel;
import java.util.List;

public interface SystemDictionaryRepository {

    List<DictTypeModel> findVisibleActiveTypes(Long orgId);

    List<DictItemModel> findVisibleActiveItems(String dictType, Long orgId);
}
