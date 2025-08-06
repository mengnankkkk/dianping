package com.mengnankk.entity.ai;



import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class AgentResponse {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private Map<String, Object> data;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 响应时间
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 处理耗时（毫秒）
     */
    private Long duration;
}

