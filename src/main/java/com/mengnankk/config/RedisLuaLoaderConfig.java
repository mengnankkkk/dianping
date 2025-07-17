package com.mengnankk.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class RedisLuaLoaderConfig {

    private final RedisTemplate<String, Object> redisTemplate;


    public RedisLuaLoaderConfig(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Long runScoreDecayScript(String key, double decayFactor, double minScore) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("score_decay.lua"));
        script.setResultType(Long.class);

        List<String> keys = Collections.singletonList(key);
        List<String> args = List.of(String.valueOf(decayFactor), String.valueOf(minScore));

        return redisTemplate.execute(script, keys, args.toArray(new String[0]));
    }
    public DefaultRedisScript<String> loadMultiRankingUpdateScript() {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("multi_ranking_update.lua"));
        script.setResultType(String.class);
        return script;
    }
}
