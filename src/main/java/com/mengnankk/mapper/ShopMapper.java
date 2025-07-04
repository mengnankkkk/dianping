package com.mengnankk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mengnankk.entity.Shop;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;

@Mapper
public interface ShopMapper extends BaseMapper<Shop> {
}
