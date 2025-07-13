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
import com.mengnankk.mapper.BlogMapper;
import com.mengnankk.service.BlogService;
import com.mengnankk.service.FollowService;
import com.mengnankk.service.UserService;
import com.mengnankk.utils.AuthContextHolder;
import com.mengnankk.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements BlogService {
    @Autowired
    private final UserService userService;
    @Autowired
    private final StringRedisTemplate redisTemplate;
    @Autowired
    private final ObjectMapper objectMapper;
    @Autowired
    private final FollowService followService;


    private String getBlogKey(long blogid){
        return  "blog:id:"+blogid;
    }
    private void isBlogLiked(Blog blog){
        long userId = AuthContextHolder.getUserId();
        if (userId==0) return;
    }

    public BlogServiceImpl(UserService userService, StringRedisTemplate redisTemplate, ObjectMapper objectMapper, FollowService followService) {
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.followService = followService;
    }


    /**
     * 根据id查询博客，使用缓存
     */
    @Override
    public Result queryBlogById(Long id) {
        // 1. 从缓存查询
        String blogKey = getBlogKey(id);
        String blogJson = redisTemplate.opsForValue().get(blogKey);

        Blog blog = null;
        if (StrUtil.isNotBlank(blogJson)) {
            // 2. 缓存存在，反序列化
            try {
                blog = objectMapper.readValue(blogJson, Blog.class);
            } catch (JsonProcessingException e) {
                log.error("Error deserializing blog from cache: {}");
                // Consider: Move to database if deserialization fails repeatedly
            }
            if(blog != null) {
                queryUserByBlog(blog);
                isBlogLiked(blog);
                return Result.ok(blog);
            }
        }

        // 3. 缓存未命中，查询数据库
        blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在");
        }

        // 4. 查询相关用户信息及点赞状态
        queryUserByBlog(blog);
        isBlogLiked(blog);

        // 5. 存入缓存
        try {
            redisTemplate.opsForValue().set(blogKey, objectMapper.writeValueAsString(blog));
        } catch (JsonProcessingException e) {
            log.error("Error serializing blog to cache: {}");
            //TODO 存入缓存失败应该怎么办？可以使用补偿机制
        }
        return Result.ok(blog);
    }

    /**
     * 点赞功能
     * @param id
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result likeBlog(Long id) {
        Long userid = AuthContextHolder.getUserId();
        String key = getBlogKey(id);


        List<Object> results =redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            public <K, V> List<Object> execute(RedisOperations<K, V> operations) throws DataAccessException {
               Double scores= operations.opsForZSet().score((K) key,(V) userid.toString());
               operations.multi();

               boolean increment = (scores==null);
               String sql = "liked = liked " + (increment ? "+ 1" : "- 1");
                boolean dbResult = update()
                        .setSql("liked = liked " + (increment ? "+1" : "-1"))
                        .eq("id", id)
                        .update();

               if (dbResult){
                   if (increment){
                       operations.opsForZSet().add((K) key,(V) userid.toString(),System.currentTimeMillis());
                   }else {
                       operations.opsForZSet().remove((K) key, (V) userid.toString());
                   }
               }else {
                   operations.discard();
                   throw new RuntimeException("Database update failed for blog like.");
               }
               return operations.exec();
            }
        });
        return Result.ok();
    }

    /**
     * 签到方法
     * @return
     */
    @Override
    public Result sign() {
        Long userid = AuthContextHolder.getUserId();
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = userid+keySuffix;
        int dayOfMounth = now.getDayOfMonth();
        redisTemplate.opsForValue().setBit(key,dayOfMounth-1,true);
        return Result.ok("签到成功");
    }

    /**
     * 保存探店笔记，异步推送到粉丝
     * @param blog
     * @return
     */

    @Override
    public Result saveBlog(Blog blog) {
        Long userid = AuthContextHolder.getUserId();
        blog.setUserId(userid);

        boolean isSucess = save(blog);
        if (!isSucess){
            return Result.fail("保存失败");
        }


        CompletableFuture.runAsync(()->{
            List<Follow> follows = followService.list(new LambdaQueryWrapper<Follow>().eq(Follow::getFollowUserId,userid));

            if (!CollectionUtils.isEmpty(follows)){
                for (Follow follow:follows){
                    Long followId = follow.getUserId();
                    String key = RedisConstants.FEED_KEY+followId;
                    redisTemplate.opsForZSet().add(key, blog.getId().toString(),System.currentTimeMillis());
                }
                log.info("Pushed blog {} to {} followers of user {}", blog.getId(), follows.size(), userid);
            }else {
                log.info("User {} has no followers, no feed to push", userid);
            }
        });
        return Result.ok(blog.getId());
    }

    private void queryUserByBlog(Blog blog){
        Long userid = blog.getUserId();
        User user =userService.getById(userid);
        blog.setName(user.getUsername());
    }

    /**
     * 分页查询关注着
     * @param max
     * @param offset
     * @return
     */
    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        Long userId = AuthContextHolder.getUserId();
        String redisKey = "follows:blog:" + userId;

        // 从 Redis 获取 ZSet 中的博客 ID，按时间戳倒序分页
        Set<ZSetOperations.TypedTuple<String>> typedTuples =
                redisTemplate.opsForZSet()
                        .reverseRangeWithScores(redisKey, offset, offset + 9);

        // 空结果直接返回
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        // 提取 blogId 和 minTime（作为下一次 max 的参考）
        List<Long> ids = new ArrayList<>();
        long minTime = 0L;
        int newOffset = 1; // 重复时间的 offset，默认为 1

        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            String idStr = tuple.getValue();
            if (idStr != null) {
                ids.add(Long.valueOf(idStr));
            }
            Double score = tuple.getScore();
            if (score != null) {
                long time = score.longValue();
                if (time == minTime) {
                    newOffset++;
                } else {
                    minTime = time;
                    newOffset = 1;
                }
            }
        }

        // 按照 ids 顺序查询博客（MySQL 中 in 查询会乱序，需要手动排序）
        List<Blog> blogs = list(
                new LambdaQueryWrapper<Blog>()
                        .in(Blog::getId, ids)
                        .orderByDesc(Blog::getUpdateTime)
        );

        // 补充作者信息
        blogs.forEach(this::queryUserByBlog);

        // 返回分页结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", blogs);
        result.put("offset", newOffset);
        result.put("minTime", minTime);

        return Result.ok(result);
    }

    /**
     * 查询所有点赞的博客的用户
     * @param id
     * @return
     */
    @Override
    public Result queryBlogLikes(Long id){
        String key = "blog:likes:" + id;
        String cachelikes = redisTemplate.opsForValue().get(key);

        List<UserDTO> userDTOList;

        if (StrUtil.isNotBlank(cachelikes)){
            try{
                userDTOList =Arrays.asList(objectMapper.readValue(cachelikes,UserDTO.class));
                log.info("Fetched blog likes from cache for blog ID: {}", id);
                return Result.ok(userDTOList);
            }catch (JsonProcessingException e){
                log.error("Failed to deserialize blog likes for blog ID: {}. Fetching from DB and rewriting cache.", id, e);
            }
        }
        String likedKey = "blog:like"+id;
        Set<String> top5 = redisTemplate.opsForZSet().range(likedKey,0,4);
        if (CollectionUtils.isEmpty(top5)){
            log.info("No likes found for blog ID: {}. Returning empty list.", id);
            return Result.ok(Collections.emptyList());
        }

        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",",ids);
        Map<Long, User> userMap = userService.listByIds(ids)
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        List<UserDTO> dblist = userService.list(new LambdaQueryWrapper<User>()
                .in(User::getId,ids)
                .last("order by fiel(id,"+idStr+")"))
                .stream().map(user -> BeanUtil.copyProperties(user,UserDTO.class))
                .collect(Collectors.toList());

        try {
            redisTemplate.opsForValue().set(key,objectMapper.writeValueAsString(dblist));
        }catch (JsonProcessingException e){
            log.error("Failed to serialize and cache blog likes for blog ID: {}", id, e);
        }
        log.info("Fetched blog likes from DB for blog ID: {}", id);
        return Result.ok(dblist);
    }



}
