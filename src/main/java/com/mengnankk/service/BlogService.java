package com.mengnankk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mengnankk.dto.Result;
import com.mengnankk.entity.Blog;

public interface BlogService extends IService<Blog> {
    public Result<?> queryBlogById(Long id);
    public Result<?> likeBlog(Long id);
    public Result<?> sign();
    public Result<?> saveBlog(Blog blog);

    Result<?> queryBlogOfFollow(Long max, Integer offset);

    Result<?> queryBlogLikes(Long id);
    
    Result<?> queryMyBlog(Integer current);
    
    Result<?> queryBlogByUserId(Long userId, Integer current);
    
    Result<?> queryHotBlog(Integer current);
}
