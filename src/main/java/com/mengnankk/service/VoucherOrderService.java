package com.mengnankk.service;

import com.mengnankk.dto.Result;

/**
 * 优惠券订单服务接口
 */
public interface VoucherOrderService {
    
    /**
     * 秒杀下单
     * @param voucherId 优惠券ID
     * @return 订单结果
     */
    Result<?> seckillVoucher(Long voucherId);
    
    /**
     * 查询用户的优惠券订单
     * @param userId 用户ID
     * @return 订单列表
     */
    Result<?> queryUserOrders(Long userId);
    
    /**
     * 取消订单
     * @param orderId 订单ID
     * @return 结果
     */
    Result<?> cancelOrder(Long orderId);
    
    /**
     * 查询我的优惠券订单
     * @param current 当前页
     * @return 订单列表
     */
    Result<?> getMyVoucherOrders(Integer current);
    
    /**
     * 查询订单状态
     * @param orderId 订单ID
     * @return 订单状态
     */
    Result<?> getOrderStatus(Long orderId);
}
