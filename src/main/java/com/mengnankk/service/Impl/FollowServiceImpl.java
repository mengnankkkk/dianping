package com.mengnankk.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mengnankk.dto.Result;
import com.mengnankk.dto.UserDTO;
import com.mengnankk.entity.Blog;
import com.mengnankk.entity.Follow;
import com.mengnankk.entity.User;
import com.mengnankk.mapper.FollowMapper;
import com.mengnankk.service.FollowService;
import com.mengnankk.service.UserService;
import com.mengnankk.utils.AuthContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.mengnankk.utils.RedisConstants.FOLLOW_KEY;

@Service
@Slf4j
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

@Autowired
private final StringRedisTemplate redisTemplate;
@Autowired
private final UserService userService;
@Autowired
private final ObjectMapper objectMapper;

    public FollowServiceImpl(StringRedisTemplate redisTemplate, UserService userService, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    /**
     * 关注用户
     * @param followUserId
     * @param isFollow
     * @return
     */
    @Override
    public Result follow(Long followUserId, Boolean isFollow){
        Long userId = AuthContextHolder.getUserId();
        String key = "follow:blogId"+userId;

        if (isFollow){
            boolean   added = redisTemplate.opsForSet().add(key,followUserId.toString())>0;

            if (added){
                //TODO 可改为消息队列来完成
                CompletableFuture.runAsync(()->{
                    try {
                        Follow follow = new Follow();
                        follow.setUserId(userId);
                        follow.setFollowUserId(followUserId);
                        getBaseMapper().insert(follow);
                        log.info("用户 {} 关注了 User: {}", userId, followUserId);
                    }catch (Exception e){
                        log.error("Failed to save follow data to DB for User: {} following {}: {}",
                                userId, followUserId, e.getMessage());
                        //TODO 加入失败的补偿机制
                    }
                });
            }
        }else {
            boolean removed = redisTemplate.opsForSet().remove(key,followUserId.toString())>0;
            if (removed){
                CompletableFuture.runAsync(()->{
                    try {
                        getBaseMapper().delete(new LambdaQueryWrapper<Follow>()
                                .eq(Follow::getUserId,userId).eq(Follow::getFollowUserId,followUserId));
                        log.info("User {} unfollowed User {}", userId, followUserId);
                    }catch (Exception e ){
                        log.error("Failed to delete follow data from DB for User {} unfollowing {}: {}",
                                userId, followUserId, e.getMessage());
                        //TODO 加入失败的补偿机制
                    }
                });
            }
        }
        return Result.ok();
    }

    /**
     * 查询是否关注
     * @param followUserId
     * @return
     */
    @Override
    public Result isFollow(Long followUserId){
        Long userId = AuthContextHolder.getUserId();
        String key = "follow:blogId"+userId;

        Boolean isMember = redisTemplate.opsForSet().isMember(key,followUserId.toString());
        return Result.ok(isMember!=null&&isMember);
    }



    private void queryUserByBlog(Blog blog){
        Long userid = blog.getUserId();
        User user =userService.getById(userid);
        blog.setName(user.getUsername());
    }

    /**
     * 查询共同好友
     * @param id
     * @return
     */

    @Override
    public Result followCommons(Long id) {
        Long userId = AuthContextHolder.getUserId();

        String cacheKey = "user:commons:" + userId + ":" + id;

        String cachedCommons = redisTemplate.opsForValue().get(cacheKey);
        List<UserDTO> commonUsers = new ArrayList<>();

        if (StrUtil.isNotBlank(cachedCommons)) {
            try {
                commonUsers = Arrays.asList(objectMapper.readValue(cachedCommons, UserDTO[].class));
                log.info("Retrieved common followed users from cache for user {} and target user {}", userId, id);
                return Result.ok(commonUsers);
            } catch (JsonProcessingException e) {
                log.error("Error deserializing common followed users from cache: {}", e.getMessage());
            }
        }


        String key1 = FOLLOW_KEY + userId;
        String key2 = FOLLOW_KEY + id;

        Set<String> intersect = redisTemplate.opsForSet().intersect(key1, key2);

        if (CollectionUtils.isEmpty(intersect)) {
            log.info("No common followed users found for user {} and target user {}", userId, id);
            return Result.ok(Collections.emptyList());
        }

        List<Long> ids = intersect.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        if(CollectionUtils.isEmpty(ids))
        {
            return Result.ok(Collections.emptyList());
        }
        List<UserDTO> dbList = userService.listByIds(ids).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        // Cache the computed result
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(dbList));
            log.info("Cached common followed users for user {} and target user {}", userId, id);
        } catch (JsonProcessingException e) {
            log.error("Error serializing common followed users to cache: {}", e.getMessage());
        }

        return Result.ok(dbList);
    }

}
