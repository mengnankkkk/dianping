package com.mengnankk.controller;

import com.mengnankk.dto.Result;
import com.mengnankk.dto.SearchResult;
import com.mengnankk.entity.Shop;
import com.mengnankk.service.ai.AiChatService;
import com.mengnankk.service.ai.IntelligentSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI功能控制器
 * 提供AI聊天和智能搜索功能的REST API
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final AiChatService aiChatService;
    private final IntelligentSearchService intelligentSearchService;

    /**
     * AI聊天接口
     * @param message 用户消息
     * @return AI回复
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody String message) {
        try {
            log.info("Received chat request: {}", message);
            String response = aiChatService.chat(message);
            return Result.ok(response);
        } catch (Exception e) {
            log.error("Error in AI chat: {}", e.getMessage(), e);
            return Result.fail("AI服务暂时不可用");
        }
    }

    /**
     * 智能搜索接口
     * @param query 搜索查询
     * @return 搜索结果
     */
    @GetMapping("/search")
    public Result<SearchResult<Shop>> intelligentSearch(@RequestParam String query) {
        try {
            log.info("Received intelligent search request: {}", query);
            SearchResult<Shop> searchResult = intelligentSearchService.searchShops(query);
            return Result.ok(searchResult);
        } catch (Exception e) {
            log.error("Error in intelligent search: {}", e.getMessage(), e);
            return Result.fail("智能搜索服务暂时不可用");
        }
    }

    /**
     * 健康检查接口
     * @return 服务状态
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.ok("AI服务运行正常");
    }
}
