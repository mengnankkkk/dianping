package com.mengnankk.utils;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mengnankk.dto.Result;
import com.mengnankk.dto.UserDTO;
import com.mengnankk.entity.User;
import com.mengnankk.service.BlogService;
import com.mengnankk.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.elasticsearch.client.ml.GetOverallBucketsRequest.TOP_N;

@Component
@Slf4j
@RequiredArgsConstructor
public class LayeredBlogCache {
    private final StringRedisTemplate redisTemplate;
    @Value("${cache.local.maxSize:1000}")
    private int localCacheMaxSize;

    @Value("${cache.local.expireAfterWriteMinutes:1}")
    private int localCacheExpireAfterWriteMinutes;

    @Value("${cache.redis.expireAfterWriteMinutes:5}")
    private int redisCacheExpireAfterWriteMinutes;

    @Value("${async.threadPool.size:10}")
    private int threadPoolSize;

    @Value("${async.threadPool.queueCapacity:100}")
    private int threadPoolQueueCapacity;

    @Autowired
    private final ObjectMapper objectMapper;
    @Autowired final UserService userService;

    private Cache<Long,List<UserDTO>> localCache;

    private ExecutorService asyncUpdateExecutor;


    /**
     * 初始化下，构建本地缓存和异步线程池
     */

    @PostConstruct
    public void init(){
        localCache = Caffeine.newBuilder()
                .maximumSize(localCacheMaxSize)
                .expireAfterWrite(localCacheExpireAfterWriteMinutes, TimeUnit.MINUTES)
                .removalListener((key, value, cause) ->
                        log.info("Key {} was evicted from local cache due to {}", key, cause))
                .build();

        asyncUpdateExecutor = new ThreadPoolExecutor(
                threadPoolSize,threadPoolSize,0L,TimeUnit.SECONDS,new LinkedBlockingQueue<>(threadPoolQueueCapacity),new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 缓存查询,先是本地，然后是redis。
     * @param blogId
     * @return
     */
    public List<UserDTO> getCachedLikedUsers(Long blogId) {
        List<UserDTO> likedUsers = localCache.getIfPresent(blogId);

        if (likedUsers != null) {
            log.debug("返回本地缓存，blogId:{}", blogId);
            return likedUsers;
        }

        String key = "blog:userDTO:" + blogId;
        String json = redisTemplate.opsForValue().get(key);

        if (json != null && !json.isEmpty()) {
            try {
                likedUsers = objectMapper.readValue(json, new TypeReference<List<UserDTO>>(){});//json 读取数据
                log.info("返回 Redis 缓存，blogId: {}", blogId);

                localCache.put(blogId, likedUsers);
                return likedUsers;
            }catch( Exception e){
                log.error("反序列化 Redis 缓存失败，blogId: {}，json: {}", blogId, json, e);
                //TODO 异常处理,暂定删除redis中的缓存
                redisTemplate.delete(key);
            }
        }

        return  null; //null 可以触发DB查询。
    }

    /**
     * 失效缓存
     * @param blogId
     */

    public void invalidate(Long blogId) {
        localCache.invalidate(blogId);
        String key = "blog:liked:" + blogId;
        redisTemplate.execute((RedisCallback<Object>) con->{
            con.del(key.getBytes(StandardCharsets.UTF_8));
            return null;
        });

    }

    /**
     * 向缓存写入数据,先写redis,再写本地
     * @param blogId
     * @param likedUsers
     */
    public void putCachedLikedUsers(Long blogId, List<UserDTO> likedUsers){
        asyncUpdateExecutor.execute(()->{
            try {
               String key = "blog:liked:" + blogId;
               byte[] redisvalue = objectMapper.writeValueAsBytes(likedUsers);
               redisTemplate.execute((RedisCallback<Object>) connection -> {
                   connection.set(key.getBytes(StandardCharsets.UTF_8),redisvalue);
                   redisTemplate.expire(key, Duration.ofMinutes(redisCacheExpireAfterWriteMinutes));
                   return null;
               });
                log.info("异步更新缓存  Redis和local缓存，blogId:{}", blogId);
            }catch ( JsonProcessingException e){
                log.error("异步序列化更新缓存发生异常：{}", e.getMessage(), e);
            }
        });
        localCache.put(blogId,likedUsers);
    }

}
