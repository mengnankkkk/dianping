package com.mengnankk.controller;

import com.mengnankk.dto.Result;
import com.mengnankk.entity.Blog;
import com.mengnankk.service.BlogService;
import com.mengnankk.utils.AuthContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 博客控制器
 * 提供博客相关的API接口
 */
@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
@Slf4j
public class BlogController {

    private final BlogService blogService;

    /**
     * 根据ID查询博客
     * @param id 博客ID
     * @return 博客信息
     */
    @GetMapping("/{id}")
    public Result<?> getBlogById(@PathVariable Long id) {
        try {
            return blogService.queryBlogById(id);
        } catch (Exception e) {
            log.error("Error getting blog by id: {}", id, e);
            return Result.fail("查询博客失败");
        }
    }

    /**
     * 保存探店笔记
     * @param blog 博客信息
     * @return 保存结果
     */
    @PostMapping
    public Result<?> saveBlog(@RequestBody Blog blog) {
        try {
            Long userId = AuthContextHolder.getUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            blog.setUserId(userId);
            return blogService.saveBlog(blog);
        } catch (Exception e) {
            log.error("Error saving blog", e);
            return Result.fail("保存博客失败");
        }
    }

    /**
     * 点赞博客
     * @param id 博客ID
     * @return 点赞结果
     */
    @PostMapping("/like/{id}")
    public Result<?> likeBlog(@PathVariable Long id) {
        try {
            Long userId = AuthContextHolder.getUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            return blogService.likeBlog(id);
        } catch (Exception e) {
            log.error("Error liking blog: {}", id, e);
            return Result.fail("点赞失败");
        }
    }

    /**
     * 查询博客的点赞用户列表
     * @param id 博客ID
     * @return 点赞用户列表
     */
    @GetMapping("/likes/{id}")
    public Result<?> getBlogLikes(@PathVariable Long id) {
        try {
            return blogService.queryBlogLikes(id);
        } catch (Exception e) {
            log.error("Error getting blog likes: {}", id, e);
            return Result.fail("查询点赞用户失败");
        }
    }

    /**
     * 查询我的博客
     * @param current 当前页
     * @return 博客列表
     */
    @GetMapping("/of/me")
    public Result<?> getBlogsOfCurrentUser(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        try {
            Long userId = AuthContextHolder.getUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            return blogService.queryMyBlog(current);
        } catch (Exception e) {
            log.error("Error getting my blogs", e);
            return Result.fail("查询我的博客失败");
        }
    }

    /**
     * 查询用户的博客
     * @param current 当前页
     * @param id 用户ID
     * @return 博客列表
     */
    @GetMapping("/of/user")
    public Result<?> getBlogsOfUser(@RequestParam(value = "current", defaultValue = "1") Integer current,
                                   @RequestParam("id") Long id) {
        try {
            return blogService.queryBlogByUserId(id, current);
        } catch (Exception e) {
            log.error("Error getting user blogs: {}", id, e);
            return Result.fail("查询用户博客失败");
        }
    }

    /**
     * 查询关注的人的博客（分页滚动查询）
     * @param max 最大时间戳
     * @param offset 偏移量
     * @return 博客列表
     */
    @GetMapping("/of/follow")
    public Result<?> getBlogsOfFollow(@RequestParam(value = "lastId", required = false) Long max,
                                               @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        try {
            Long userId = AuthContextHolder.getUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            return blogService.queryBlogOfFollow(max, offset);
        } catch (Exception e) {
            log.error("Error getting follow blogs", e);
            return Result.fail("查询关注博客失败");
        }
    }

    /**
     * 查询热门博客
     * @param current 当前页
     * @return 博客列表
     */
    @GetMapping("/hot")
    public Result<?> getHotBlogs(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        try {
            return blogService.queryHotBlog(current);
        } catch (Exception e) {
            log.error("Error getting hot blogs", e);
            return Result.fail("查询热门博客失败");
        }
    }
}
