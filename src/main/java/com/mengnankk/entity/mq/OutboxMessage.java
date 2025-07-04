package com.mengnankk.entity.mq;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("outbox_message")
public class OutboxMessage {

    @TableId
    private String id;

    private String eventType;

    private String payload;

    private Status status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
    private long timestamp;   // 消息生成时间戳
    int retryCount; // 重试次数，用于MQ内部管理或消费者侧判断
    Map<String, String> headers; // 扩展头部信息

    // ✅ 你需要的构造函数
    public OutboxMessage(String id, String eventType, String payload, Status status) {
        this.id = id;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
    }

    public enum Status {
        PENDING(0),
        SENT(1),
        FAILED(-1);

        private final int code;

        Status(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
