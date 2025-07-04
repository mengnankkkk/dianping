package com.mengnankk.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.*;

@Component
@Slf4j
public class AsyncDbRedisWriter {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static StringRedisTemplate staticStringRedisTemplate;

    @PostConstruct
    public void init() {
        staticStringRedisTemplate = this.stringRedisTemplate;
    }

    private static final int CORE_POOL_SIZE = 10;
    private static final int MAX_POOL_SIZE = 20;
    private static final int QUEUE_CAPACITY = 100;
    private static final String THREAD_NAME_PREFIX = "DbRedisWriter-";

    private static final ExecutorService executorService = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            r -> new Thread(r, THREAD_NAME_PREFIX + r.hashCode()),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * 写操作
     * @param entity
     * @param dbWriteFunction
     * @param redisKey
     * @param <T>
     */

    public static <T> void write(T entity, DbWriteFunction<T> dbWriteFunction, String redisKey) {
        executorService.execute(() -> {
            try {
                writeDbAndCache(entity, dbWriteFunction, redisKey);
            } catch (Exception e) {
                log.error("Async write to DB and Redis failed", e);
            }
        });
    }

    /**
     * 更新数据库，删除缓存
     * @param entity
     * @param dbWriteFunction
     * @param redisKey
     * @param <T>
     */

    public static <T> void writeDbAndCache(T entity, DbWriteFunction<T> dbWriteFunction, String redisKey) {
        try {
            dbWriteFunction.write(entity);
            staticStringRedisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.error("Failed to write to DB and Redis, rolling back", e);
            throw new RuntimeException("Failed to write to DB and Redis", e);
        }
    }

    @FunctionalInterface
    public interface DbWriteFunction<T> {
        void write(T entity);
    }
}
