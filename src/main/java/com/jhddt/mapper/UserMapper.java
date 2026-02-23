package com.jhddt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jhddt.domain.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
