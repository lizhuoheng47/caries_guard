package com.cariesguard.system.infrastructure.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cariesguard.system.domain.model.DictItemModel;
import com.cariesguard.system.domain.model.DictTypeModel;
import com.cariesguard.system.domain.repository.SystemDictionaryRepository;
import com.cariesguard.system.infrastructure.dataobject.SysDictItemDO;
import com.cariesguard.system.infrastructure.dataobject.SysDictTypeDO;
import com.cariesguard.system.infrastructure.mapper.SysDictItemMapper;
import com.cariesguard.system.infrastructure.mapper.SysDictTypeMapper;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class SystemDictionaryRepositoryImpl implements SystemDictionaryRepository {

    private static final long GLOBAL_ORG_ID = 0L;

    private final SysDictTypeMapper sysDictTypeMapper;
    private final SysDictItemMapper sysDictItemMapper;

    public SystemDictionaryRepositoryImpl(SysDictTypeMapper sysDictTypeMapper, SysDictItemMapper sysDictItemMapper) {
        this.sysDictTypeMapper = sysDictTypeMapper;
        this.sysDictItemMapper = sysDictItemMapper;
    }

    @Override
    public List<DictTypeModel> findVisibleActiveTypes(Long orgId) {
        return sysDictTypeMapper.selectList(Wrappers.<SysDictTypeDO>lambdaQuery()
                        .in(SysDictTypeDO::getOrgId, visibleOrgIds(orgId))
                        .eq(SysDictTypeDO::getStatus, "ACTIVE")
                        .eq(SysDictTypeDO::getDeletedFlag, 0L)
                        .orderByAsc(SysDictTypeDO::getSortOrder)
                        .orderByAsc(SysDictTypeDO::getId))
                .stream()
                .map(this::toDictTypeModel)
                .toList();
    }

    @Override
    public List<DictItemModel> findVisibleActiveItems(String dictType, Long orgId) {
        SysDictTypeDO type = sysDictTypeMapper.selectOne(Wrappers.<SysDictTypeDO>lambdaQuery()
                .eq(SysDictTypeDO::getDictType, dictType)
                .in(SysDictTypeDO::getOrgId, visibleOrgIds(orgId))
                .eq(SysDictTypeDO::getStatus, "ACTIVE")
                .eq(SysDictTypeDO::getDeletedFlag, 0L)
                .orderByDesc(SysDictTypeDO::getOrgId)
                .last("LIMIT 1"));
        if (type == null) {
            return Collections.emptyList();
        }
        return sysDictItemMapper.selectList(Wrappers.<SysDictItemDO>lambdaQuery()
                        .eq(SysDictItemDO::getDictTypeId, type.getId())
                        .in(SysDictItemDO::getOrgId, visibleOrgIds(orgId))
                        .eq(SysDictItemDO::getStatus, "ACTIVE")
                        .eq(SysDictItemDO::getDeletedFlag, 0L)
                        .orderByAsc(SysDictItemDO::getItemSort)
                        .orderByAsc(SysDictItemDO::getId))
                .stream()
                .map(this::toDictItemModel)
                .toList();
    }

    private List<Long> visibleOrgIds(Long orgId) {
        if (orgId == null || orgId == GLOBAL_ORG_ID) {
            return List.of(GLOBAL_ORG_ID);
        }
        return List.of(GLOBAL_ORG_ID, orgId);
    }

    private DictTypeModel toDictTypeModel(SysDictTypeDO item) {
        return new DictTypeModel(
                item.getDictType(),
                item.getDictName(),
                "1".equals(item.getSystemFlag()),
                item.getSortOrder() == null ? 0 : item.getSortOrder());
    }

    private DictItemModel toDictItemModel(SysDictItemDO item) {
        return new DictItemModel(
                item.getItemLabel(),
                item.getItemValue(),
                item.getItemCode(),
                item.getItemSort() == null ? 0 : item.getItemSort(),
                "1".equals(item.getIsDefault()));
    }
}
