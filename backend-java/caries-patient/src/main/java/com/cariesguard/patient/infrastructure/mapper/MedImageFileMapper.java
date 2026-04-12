package com.cariesguard.patient.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cariesguard.patient.infrastructure.dataobject.MedImageFileDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MedImageFileMapper extends BaseMapper<MedImageFileDO> {
}
