package com.mengnankk.entity.ai;

import lombok.Data;
import lombok.Builder;
import java.util.Map;

@Data
@Builder
public class AgentContext {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户位置
     */
    private String location;

    /**
     * 用户偏好
     */
    private Map<String, Object> preferences;

    /**
     * 会话历史
     */
    private String conversationHistory;

    /**
     * 当前语言
     */
    private String language;

    /**
     * 设备信息
     */
    private String deviceInfo;

    /**
     * 额外上下文
     */
    private Map<String, Object> extras;
}

