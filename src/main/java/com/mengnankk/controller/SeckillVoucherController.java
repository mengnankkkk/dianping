package com.mengnankk.controller;

import com.mengnankk.dto.Result;
import com.mengnankk.service.SeckillVoucherService;
import com.mengnankk.service.VoucherOrderService;
import com.mengnankk.utils.AuthContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 秒杀优惠券控制器
 * 提供优惠券秒杀相关的API接口
 */
@RestController
@RequestMapping("/api/voucher-order")
@RequiredArgsConstructor
@Slf4j
public class SeckillVoucherController {

    private final VoucherOrderService voucherOrderService;
    private final SeckillVoucherService seckillVoucherService;

    /**
     * 秒杀优惠券
     * @param voucherId 优惠券ID
     * @return 订单ID
     */
    @PostMapping("/seckill/{id}")
    public Result<?> seckillVoucher(@PathVariable("id") Long voucherId) {
        try {
            Long userId = AuthContextHolder.getUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            return voucherOrderService.seckillVoucher(voucherId);
        } catch (Exception e) {
            log.error("Error in seckill voucher: {}", voucherId, e);
            return Result.fail("秒杀失败，请重试");
        }
    }

    /**
     * 查询秒杀券信息
     * @param voucherId 优惠券ID
     * @return 秒杀券信息
     */
    @GetMapping("/seckill/{id}")
    public Result<?> getSeckillVoucher(@PathVariable("id") Long voucherId) {
        try {
            return seckillVoucherService.querySeckillVoucher(voucherId);
        } catch (Exception e) {
            log.error("Error getting seckill voucher: {}", voucherId, e);
            return Result.fail("查询秒杀券信息失败");
        }
    }

    /**
     * 查询我的优惠券订单
     * @param current 当前页
     * @return 订单列表
     */
    @GetMapping("/my")
    public Result<?> getMyVoucherOrders(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        try {
            Long userId = AuthContextHolder.getUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            return voucherOrderService.getMyVoucherOrders(current);
        } catch (Exception e) {
            log.error("Error getting my voucher orders", e);
            return Result.fail("查询我的优惠券订单失败");
        }
    }

    /**
     * 查询订单状态
     * @param orderId 订单ID
     * @return 订单状态
     */
    @GetMapping("/status/{orderId}")
    public Result<?> getOrderStatus(@PathVariable Long orderId) {
        try {
            Long userId = AuthContextHolder.getUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            return voucherOrderService.getOrderStatus(orderId);
        } catch (Exception e) {
            log.error("Error getting order status: {}", orderId, e);
            return Result.fail("查询订单状态失败");
        }
    }

    /**
     * 取消订单
     * @param orderId 订单ID
     * @return 取消结果
     */
    @PostMapping("/cancel/{orderId}")
    public Result<?> cancelOrder(@PathVariable Long orderId) {
        try {
            Long userId = AuthContextHolder.getUserId();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            return voucherOrderService.cancelOrder(orderId);
        } catch (Exception e) {
            log.error("Error canceling order: {}", orderId, e);
            return Result.fail("取消订单失败");
        }
    }
}
