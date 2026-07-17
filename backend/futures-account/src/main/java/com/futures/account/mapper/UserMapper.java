package com.futures.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.futures.account.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {}
