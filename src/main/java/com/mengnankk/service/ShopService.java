package com.mengnankk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mengnankk.dto.Result;
import com.mengnankk.entity.Shop;

public interface ShopService extends IService<Shop> {
    Result queryById(Long id);
    Result queryShopList();
    Result updateShop(Shop shop);
    void saveShopToCache(Long id, Long expireSeconds);

}
