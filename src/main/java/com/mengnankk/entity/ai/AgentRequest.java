package com.mengnankk.entity.ai;

import lombok.Data;
import lombok.Builder;
import java.util.Map;

@Data
@Builder
public class AgentRequest {

    /**
     * 用户输入内容
     */
    private String input;

    /**
     * 请求上下文
     */
    private AgentContext context;

    /**
     * 额外参数
     */
    private Map<String, Object> parameters;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 会话ID
     */
    private String sessionId;
}
