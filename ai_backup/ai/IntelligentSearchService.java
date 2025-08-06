package com.mengnankk.service.ai;

import com.mengnankk.dto.SearchResult;
import com.mengnankk.entity.Shop;
import com.mengnankk.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntelligentSearchService {

    private final VectorStore vectorStore;

    @Qualifier("dashScopeEmbeddingModel")
    private final EmbeddingModel embeddingModel;

    private final ChatModel chatModel;

    @Autowired
    private final ShopService shopService;

    /**
     * 智能搜索主方法
     */
    public SearchResult<Shop> searchShops(String query) {
        try {
            log.info("Starting intelligent search for query: {}", query);

            // 1. 语义搜索
            List<Document> semanticResults = performSemanticSearch(query);
            log.debug("Semantic search returned {} results", semanticResults.size());

            // 2. 传统搜索（需要根据您的实际实现调整）
            List<Shop> traditionalResults = performTraditionalSearch(query);
            log.debug("Traditional search returned {} results", traditionalResults.size());

            // 3. 融合搜索结果
            List<Shop> fusedResults = fuseSearchResults(semanticResults, traditionalResults, query);
            log.debug("Fused search returned {} results", fusedResults.size());

            // 4. AI重排序
            List<Shop> finalRanked = reRankWithAI(fusedResults, query);
            log.debug("Final ranking returned {} results", finalRanked.size());

            // 5. 构建搜索结果
            SearchResult<Shop> result = SearchResult.<Shop>builder()
                    .results(finalRanked)
                    .total(finalRanked.size())
                    .query(query)
                    .searchTime(System.currentTimeMillis())
                    .build();

            log.info("Search completed successfully with {} results", finalRanked.size());
            return result;

        } catch (Exception e) {
            log.error("Error performing intelligent search for query '{}': {}", query, e.getMessage(), e);
            // 降级到传统搜索
            return fallbackToTraditionalSearch(query);
        }
    }

    /**
     * 语义搜索
     */
    private List<Document> performSemanticSearch(String query) {
        try {
            SearchRequest request = SearchRequest.query(query)
                    .withTopK(20)
                    .withSimilarityThreshold(0.7);

            List<Document> documents = vectorStore.similaritySearch(request);
            log.debug("Semantic search found {} documents for query: {}", documents.size(), query);

            return documents;
        } catch (Exception e) {
            log.warn("Semantic search failed for query '{}': {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 传统搜索（根据您的实际ShopService实现调整）
     */
    private List<Shop> performTraditionalSearch(String query) {
        try {
            // 这里需要根据您的ShopService实际方法来调整
            // 假设您有一个按名称搜索的方法
            return shopService.searchByName(query);
        } catch (Exception e) {
            log.warn("Traditional search failed for query '{}': {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 融合搜索结果 - 语义搜索 + 传统搜索
     */
    private List<Shop> fuseSearchResults(List<Document> semanticResults,
                                         List<Shop> traditionalResults,
                                         String query) {
        Map<Long, Shop> shopMap = new HashMap<>();
        Map<Long, Double> scoreMap = new HashMap<>();

        // 处理语义搜索结果，权重0.6
        for (Document doc : semanticResults) {
            try {
                Object shopIdObj = doc.getMetadata().get("shopId");
                if (shopIdObj == null) {
                    continue;
                }

                Long shopId = Long.parseLong(shopIdObj.toString());

                // 获取相似度分数（注意：不同向量存储的距离计算方式可能不同）
                Object distanceObj = doc.getMetadata().get("distance");
                double semanticScore = distanceObj != null ?
                        (1.0 - (Double) distanceObj) : 0.5; // 转换为相似度分数

                Shop shop = shopService.getById(shopId);
                if (shop != null) {
                    shopMap.put(shopId, shop);
                    scoreMap.put(shopId, semanticScore * 0.6); // 语义搜索权重60%
                }
            } catch (Exception e) {
                log.warn("Error processing semantic result: {}", e.getMessage());
            }
        }

        // 处理传统搜索结果，权重0.4
        for (int i = 0; i < traditionalResults.size() && i < 20; i++) {
            Shop shop = traditionalResults.get(i);
            double traditionalScore = 1.0 - (i * 0.05); // 排名越前分数越高

            if (shopMap.containsKey(shop.getId())) {
                // 组合分数
                double currentScore = scoreMap.get(shop.getId());
                scoreMap.put(shop.getId(), currentScore + traditionalScore * 0.4);
            } else {
                shopMap.put(shop.getId(), shop);
                scoreMap.put(shop.getId(), traditionalScore * 0.4); // 传统搜索权重40%
            }
        }

        // 按分数排序并返回
        return shopMap.values().stream()
                .sorted((s1, s2) -> Double.compare(
                        scoreMap.getOrDefault(s2.getId(), 0.0),
                        scoreMap.getOrDefault(s1.getId(), 0.0)
                ))
                .collect(Collectors.toList());
    }

    /**
     * AI重排序
     */
    private List<Shop> reRankWithAI(List<Shop> shops, String query) {
        if (shops.size() <= 10) {
            return shops; // 结果较少时不需要重排序
        }

        try {
            // 只对前20个结果进行重排序以控制成本
            List<Shop> topShops = shops.stream().limit(20).collect(Collectors.toList());
            String reRankPrompt = buildReRankPrompt(topShops, query);

            // 调用AI模型
            Prompt prompt = new Prompt(reRankPrompt);
            ChatResponse response = chatModel.call(prompt);
            String content = response.getResult().getOutput().getContent();

            log.debug("AI reranking response: {}", content);

            // 解析AI返回的店铺ID顺序
            List<Long> orderedIds = extractShopIdsFromContent(content);

            // 重新排序
            List<Shop> reranked = reorderShops(topShops, orderedIds);

            // 添加剩余的店铺
            Set<Long> rerankedIds = reranked.stream().map(Shop::getId).collect(Collectors.toSet());
            List<Shop> remaining = shops.stream()
                    .filter(shop -> !rerankedIds.contains(shop.getId()))
                    .collect(Collectors.toList());

            reranked.addAll(remaining);
            return reranked;

        } catch (Exception e) {
            log.warn("AI reranking failed for query '{}': {}", query, e.getMessage());
            return shops; // 返回原始排序
        }
    }

    /**
     * 构建重排序提示词
     */
    private String buildReRankPrompt(List<Shop> shops, String query) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据用户查询重新排序以下店铺，综合考虑相关性、评分、人气等因素：\n\n");
        prompt.append("用户查询: ").append(query).append("\n\n");
        prompt.append("店铺列表:\n");

        for (int i = 0; i < shops.size(); i++) {
            Shop shop = shops.get(i);
            prompt.append(String.format("%d. ID:%d, 名称:%s, 评分:%.1f, 类型:%s\n",
                    i + 1,
                    shop.getId(),
                    shop.getName(),
                    shop.getScore() != null ? shop.getScore() : 0.0,
                    shop.getTypeName() != null ? shop.getTypeName() : "未知"
            ));
        }

        prompt.append("\n请只返回重新排序后的店铺ID列表，用逗号分隔，不要包含其他文字：\n");
        prompt.append("格式示例: 1,3,2,5,4");

        return prompt.toString();
    }

    /**
     * 从AI响应中提取店铺ID
     */
    private List<Long> extractShopIdsFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 清理内容，只保留数字和逗号
            String cleanedContent = content.replaceAll("[^0-9,]", "");

            return Arrays.stream(cleanedContent.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && s.matches("\\d+"))
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Error extracting shop IDs from content '{}': {}", content, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 根据ID顺序重新排序店铺
     */
    private List<Shop> reorderShops(List<Shop> shops, List<Long> orderedIds) {
        Map<Long, Shop> shopMap = shops.stream()
                .collect(Collectors.toMap(Shop::getId, shop -> shop));

        List<Shop> reordered = new ArrayList<>();

        // 按AI返回的顺序添加店铺
        for (Long id : orderedIds) {
            Shop shop = shopMap.get(id);
            if (shop != null) {
                reordered.add(shop);
                shopMap.remove(id); // 移除已添加的店铺
            }
        }

        // 添加未被AI排序的剩余店铺
        reordered.addAll(shopMap.values());

        return reordered;
    }

    /**
     * 降级到传统搜索
     */
    private SearchResult<Shop> fallbackToTraditionalSearch(String query) {
        try {
            List<Shop> traditionalResults = performTraditionalSearch(query);
            return SearchResult.<Shop>builder()
                    .results(traditionalResults)
                    .total(traditionalResults.size())
                    .query(query)
                    .searchTime(System.currentTimeMillis())
                    .fallback(true)
                    .build();
        } catch (Exception e) {
            log.error("Fallback search also failed for query '{}': {}", query, e.getMessage());
            return SearchResult.<Shop>builder()
                    .results(Collections.emptyList())
                    .total(0)
                    .query(query)
                    .searchTime(System.currentTimeMillis())
                    .error("搜索服务暂时不可用")
                    .build();
        }
    }
}
