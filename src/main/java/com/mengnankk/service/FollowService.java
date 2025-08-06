package com.mengnankk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mengnankk.dto.Result;
import com.mengnankk.entity.Follow;

public interface FollowService extends IService<Follow> {

    Result<?> follow(Long followUserId, Boolean isFollow);

    Result<?> isFollow(Long followUserId);

    Result<?> followCommons(Long id);
    
    Result<?> getMyFollows(Integer current);
    
    Result<?> getMyFans(Integer current);
}
