package com.mengnankk.service.Impl;

import com.mengnankk.dto.Result;
import com.mengnankk.entity.RankingItem;
import com.mengnankk.entity.RankingSegment;
import com.mengnankk.entity.User;
import com.mengnankk.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdvancedRankingService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserService userService;

    /**
     * 更新多维排行榜分数（
     */
    @Scheduled(cron = "0 0 * * * ?")
    public Result updateMultiDimensionRanking(String category, Long memberId, double baseScore, Map<String, Double> weights) {
        try {
            String member = memberId.toString();

            List<String> args = new ArrayList<>();
            args.add(member);
            args.add(String.valueOf(baseScore));

            for (Map.Entry<String, Double> entry : weights.entrySet()) {
                String rankingKey = generateRankingKey(category, entry.getKey()); // 根据时间维度生成排行榜 key
                args.add(rankingKey);
                args.add(entry.getValue().toString());
            }

            DefaultRedisScript<String> script = new DefaultRedisScript<>();
            script.setLocation(new org.springframework.core.io.ClassPathResource("scripts/multi_ranking_update.lua"));
            script.setResultType(String.class);

            redisTemplate.execute(script, Collections.emptyList(), args.toArray());
            return Result.ok("更新成功");

        } catch (Exception e) {
            log.error("更新排行榜失败，类别: {}, 错误: {}", category, e.getMessage(), e);
            return Result.fail("更新失败");
        }
    }

    /**
     * 分页获取排行榜成员
     */
    public Result getRanking(String category, String timeType, int page, int size, boolean withScores) {
        try {
            String rankingKey = generateRankingKey(category, timeType);

            Long total = redisTemplate.opsForZSet().zCard(rankingKey);
            if (total == null || total == 0) {
                return Result.fail("排行榜为空");
            }

            long start = (long) page * size;
            long end = start + size - 1;
            if (start >= total) {
                return Result.fail("超出排行榜范围");
            }

            Set<ZSetOperations.TypedTuple<Object>> rawRanking;
            if (withScores) {
                rawRanking = redisTemplate.opsForZSet().reverseRangeWithScores(rankingKey, start, end);
            } else {
                Set<Object> members = redisTemplate.opsForZSet().reverseRange(rankingKey, start, end);
                rawRanking = members.stream()
                        .map(m -> ZSetOperations.TypedTuple.of(m, 0.0))
                        .collect(Collectors.toSet());
            }

            List<RankingItem> items = new ArrayList<>();
            long currentRank = start + 1;

            for (ZSetOperations.TypedTuple<Object> tuple : rawRanking) {
                Long memberId = Long.parseLong(tuple.getValue().toString());
                Double score = tuple.getScore();

                User user = userService.getById(memberId);

                RankingItem item = RankingItem.builder()
                        .rank((int) currentRank++)
                        .member(memberId)

                        .score(score != null ? score : 0.0)
                        .build();

                items.add(item);
            }

            return Result.ok(items);

        } catch (Exception e) {
            log.error("分页查询排行榜失败，类别: {}, 错误: {}", category, e.getMessage(), e);
            return Result.fail("查询失败");
        }
    }

    /**
     * 生成排行榜 key（可自定义更多规则）
     */
    private String generateRankingKey(String category, String timeType) {
        return String.format("ranking:%s:%s", category, timeType); // 例如：ranking:video:week
    }

    /**
     * 排行榜分段统计
     * @param category
     * @param timeType
     * @param scoreBreakpoints
     * @return
     */

    public Result getRankingSegments(String category, String timeType, List<Double> scoreBreakpoints){
        try {
            String rankingkey = generateRankingKey(category,timeType);
            List<RankingSegment> segments = new ArrayList<>();

            for (int i=0;i<scoreBreakpoints.size()-1;i++){
                double minscore = scoreBreakpoints.get(i);
                double maxscore = scoreBreakpoints.get(i+1);

                Long count = redisTemplate.opsForZSet().count(rankingkey,minscore,maxscore);

                Set<Object> sampleMembers =redisTemplate.opsForZSet().rangeByScore(rankingkey,minscore,maxscore,0,5);

                RankingSegment segment = RankingSegment.builder()
                        .minRank(minscore)
                        .maxRank(maxscore)
                        .build();
                segments.add(segment);
            }
            return Result.ok(segments);
        }catch (Exception e){
            log.error("排行榜分段统计失败，类别: {}, 错误: {}", category, e.getMessage(), e);
            return Result.ok(Collections.emptyList());
        }
    }

}
