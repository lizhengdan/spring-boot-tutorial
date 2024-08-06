package com.mhkj.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mhkj.entity.Resource;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourceRepository extends BaseMapper<Resource> {
}
