package com.cariesguard.image.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cariesguard.image.infrastructure.dataobject.MedImageFileDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImageFileMapper extends BaseMapper<MedImageFileDO> {
}
