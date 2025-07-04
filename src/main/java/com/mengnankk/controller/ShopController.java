package com.mengnankk.controller;

import com.mengnankk.dto.Result;
import com.mengnankk.entity.Shop;
import com.mengnankk.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop")
public class ShopController {
    @Autowired
    private ShopService shopService;
    @GetMapping("/{id}") // 例如：/shop/123
    public Result queryShopById(@PathVariable("id") Long id) {
        return shopService.queryById(id);
    }
    @GetMapping("/list")
    public Result queryShopList() {
        return shopService.queryShopList();
    }
    @PutMapping // 例如：/shop
    public Result updateShop(@RequestBody Shop shop) {
        return shopService.updateShop(shop); // 使用 MQ 异步更新
    }

}
