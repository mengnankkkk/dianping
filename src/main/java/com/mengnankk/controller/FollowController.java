package com.mengnankk.controller;

import com.mengnankk.dto.Result;
import com.mengnankk.service.FollowService;
import com.mengnankk.utils.AuthContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 关注控制器
 * 提供用户关注相关的API接口
 */
@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
@Slf4j
public class FollowController {

    private final FollowService followService;

    /**
     * 关注或取消关注用户
     * @param followUserId 被关注用户ID
     * @param isFollow 是否关注（true-关注，false-取消关注）
     * @return 操作结果
     */
    @PostMapping("/{id}/{isFollow}")
    public Result<?> follow(@PathVariable("id") Long followUserId, 
                                @PathVariable Boolean isFollow) {
        try {
            Long userId = AuthContextHolder.getUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            return followService.follow(followUserId, isFollow);
        } catch (Exception e) {
            log.error("Error following user: {}, isFollow: {}", followUserId, isFollow, e);
            return Result.fail("关注操作失败");
        }
    }

    /**
     * 查询是否关注了指定用户
     * @param followUserId 被关注用户ID
     * @return 是否关注
     */
    @GetMapping("/or/not/{id}")
    public Result<?> isFollow(@PathVariable("id") Long followUserId) {
        try {
            Long userId = AuthContextHolder.getUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            return followService.isFollow(followUserId);
        } catch (Exception e) {
            log.error("Error checking follow status: {}", followUserId, e);
            return Result.fail("查询关注状态失败");
        }
    }

    /**
     * 查询共同关注的用户
     * @param targetUserId 目标用户ID
     * @return 共同关注的用户列表
     */
    @GetMapping("/common/{id}")
    public Result<?> getCommonFollow(@PathVariable("id") Long targetUserId) {
        try {
            Long userId = AuthContextHolder.getUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            return followService.followCommons(targetUserId);
        } catch (Exception e) {
            log.error("Error getting common follows: {}", targetUserId, e);
            return Result.fail("查询共同关注失败");
        }
    }

    /**
     * 查询我关注的用户列表
     * @param current 当前页
     * @return 关注用户列表
     */
    @GetMapping("/me")
    public Result<?> getMyFollows(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        try {
            Long userId = AuthContextHolder.getUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            return followService.getMyFollows(current);
        } catch (Exception e) {
            log.error("Error getting my follows", e);
            return Result.fail("查询我的关注失败");
        }
    }

    /**
     * 查询关注我的用户列表（粉丝列表）
     * @param current 当前页
     * @return 粉丝列表
     */
    @GetMapping("/fans")
    public Result<?> getMyFans(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        try {
            Long userId = AuthContextHolder.getUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            return followService.getMyFans(current);
        } catch (Exception e) {
            log.error("Error getting my fans", e);
            return Result.fail("查询我的粉丝失败");
        }
    }
}
