package com.mengnankk.entity.ai;


import java.util.List;

/**
 * AI Agent 基础接口
 * 定义所有AI Agent的通用行为
 */
public interface AiAgent {

    /**
     * 获取Agent名称
     */
    String getName();

    /**
     * 获取Agent描述
     */
    String getDescription();

    /**
     * 执行Agent任务
     * @param request Agent请求
     * @return Agent响应
     */
    AgentResponse execute(AgentRequest request);

    /**
     * 获取可用工具列表
     */
    List<Mcptool> getAvailableTools();

    /**
     * 获取Agent版本（可选）
     */
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * 检查Agent是否可用（可选）
     */
    default boolean isAvailable() {
        return true;
    }
}
