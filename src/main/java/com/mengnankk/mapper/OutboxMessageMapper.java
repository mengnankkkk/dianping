package com.mengnankk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mengnankk.entity.mq.OutboxMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OutboxMessageMapper extends BaseMapper<OutboxMessage> {
    List<OutboxMessage> selectByStatus(OutboxMessage.Status status);
}
