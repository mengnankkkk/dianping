package com.mengnankk.entity.mq;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("retry_message")
public class RetryMessage {
    private String exchange;
    private String routingKey;
    private String data;
}
