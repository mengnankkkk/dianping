package com.mengnankk.entity.ai;

import com.mengnankk.entity.Coupon;
import com.mengnankk.entity.Shop;
import com.mengnankk.entity.UserIntent;
import com.mengnankk.service.CouponService;
import com.mengnankk.service.ShopService;
import com.mengnankk.service.ai.IntelligentSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShopAiagnt implements AiAgent{
    @Autowired
    private final ChatModel chatModel;
    @Autowired
    private final ShopService shopService;
    @Autowired
    private final CouponService couponService;
    @Autowired
    private final IntelligentSearchService searchService;

    @Override
    public String getName() {
        return "shop-recommendation";
    }

    @Override
    public String getDescription() {
        return "智能店铺推荐Agent，提供个性化店铺推荐服务";
    }

    @Override
    public AgentResponse execute(AgentRequest request) {
       try {
           log.info("Processing recommendation request: {}", request.getInput());
           UserIntent intent  = analyzeUserIntent(request.getInput(),request.getContext());
           List<Shop> recommendations = switch (intent.getType()){
              case NEARBY_SEARCH -> null;
               case CUISINE_RECOMMENDATION -> null;
               case PRICE_BASED -> null;
               case OCCASION_BASED -> null;
               default -> null;
           };
           Coupon coupons = null;
           //TODO 优惠劵获取

           String explanation = generateRecommendationExplanation(recommendations,intent, Collections.singletonList(coupons));
           return AgentResponse.builder()
                   .success(true)
                   .data(Map.of(
                           "recommendations", recommendations,
                           "coupons", coupons,
                           "explanation", explanation,
                           "intent", intent
                   ))
                   .message("为您推荐了 " + recommendations.size() + " 家店铺")
                   .build();
       }catch (Exception e){
           log.error("Error in shop recommendation agent: {}", e.getMessage(), e);
           return AgentResponse.builder()
                   .success(false)
                   .message("推荐服务暂时不可用，请稍后重试")
                   .error(e.getMessage())
                   .build();
       }
    }

    @Override
    public List<Mcptool> getAvailableTools() {
        return null;
        //把获取的东西都封装进去
    }

    /**
     * 分析用户意图
     * @param input
     * @param context
     * @return
     */
    private UserIntent analyzeUserIntent(String input,AgentContext context){
        String intentPrompt = null;

        ChatResponse response = chatModel.call(new Prompt(intentPrompt));
        String aiResponse = response.getResult().getOutput().getContent();
//        return parseIntentResponse(aiResponse, context);
        return null;
    }

    /**
     * 生成推荐说明
     * @param shops
     * @param intent
     * @param coupons
     * @return
     */
    private String generateRecommendationExplanation(List<Shop> shops, UserIntent intent, List<Coupon> coupons) {
        String explanationPrompt = String.format("""
            为用户生成店铺推荐说明：
            
            用户需求: %s
            推荐店铺: %s
            可用优惠券: %d张
            
            请生成一段自然、友好的推荐说明，突出推荐理由和亮点。
            """,
                intent.toString(),
                shops.stream().map(Shop::getName).collect(Collectors.joining(", ")),
                coupons.size()
        );

        ChatResponse response = chatModel.call(new Prompt(explanationPrompt));
        return response.getResult().getOutput().getContent();
    }

}
