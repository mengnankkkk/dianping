package com.mengnankk.entity.ai;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ChatContext {
    private String userId;        // 用户ID
    private String userName;      // 用户名（可选）
    private boolean streamMode;   // 是否开启流式响应（可选）
    private String sessionId;

    // 你可以根据需求增加更多字段，比如请求时间、会话状态等
    private String location;             // 位置，例如 "上海市"
    private Map<String, String> preferences;     // 用户偏好，例如 {品类: "美妆"}
    private Map<String, Object> businessContext; // 页面/业务上下文信息，例如 {page: "详情页"}
}
