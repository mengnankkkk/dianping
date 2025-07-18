package com.mengnankk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mengnankk.dto.Result;
import com.mengnankk.entity.Shop;

import java.util.List;

public interface ShopService extends IService<Shop> {
    Result queryById(Long id);
    Result queryShopList();
    Result updateShop(Shop shop);
    void saveShopToCache(Long id, Long expireSeconds);

    List<Shop> searchByName(String query);
}
