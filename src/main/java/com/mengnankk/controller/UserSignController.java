package com.mengnankk.controller;

import com.mengnankk.dto.Result;
import com.mengnankk.service.UserService;
import com.mengnankk.utils.AuthContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户签到控制器
 * 提供用户签到相关的API接口
 */
@RestController
@RequestMapping("/api/user/sign")
@RequiredArgsConstructor
@Slf4j
public class UserSignController {

    private final UserService userService;

    /**
     * 用户签到
     * @return 签到结果
     */
    @PostMapping
    public Result<?> sign() {
        try {
            Long userId = AuthContextHolder.getUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            return userService.sign();
        } catch (Exception e) {
            log.error("Error signing in", e);
            return Result.fail("签到失败");
        }
    }

    /**
     * 查询签到记录
     * @return 签到记录
     */
    @GetMapping
    public Result<?> getSignRecord() {
        try {
            Long userId = AuthContextHolder.getUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            return userService.signCount();
        } catch (Exception e) {
            log.error("Error getting sign record", e);
            return Result.fail("查询签到记录失败");
        }
    }

    /**
     * 查询连续签到天数
     * @return 连续签到天数
     */
    @GetMapping("/count")
    public Result<?> getSignCount() {
        try {
            Long userId = AuthContextHolder.getUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            return userService.signCount();
        } catch (Exception e) {
            log.error("Error getting sign count", e);
            return Result.fail("查询连续签到天数失败");
        }
    }
}
