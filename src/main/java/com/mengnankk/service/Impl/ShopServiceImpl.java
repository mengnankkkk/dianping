package com.mengnankk.service.Impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mengnankk.dto.Result;
import com.mengnankk.entity.Shop;
import com.mengnankk.entity.produer.ShopUpdateProducerService;
import com.mengnankk.mapper.ShopMapper;
import com.mengnankk.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.mengnankk.utils.RedisConstants.*;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements ShopService {
    private final StringRedisTemplate stringRedisTemplate;
    private static final Shop EMPTY_SHOP =new Shop();
    private static final long CACHE_NULL_TTL = 60L;
    private final Random random =new Random();
    private static final long LOCK_SHOP_TTL = 10L;
    private static final String LOCK_SHOP_KEY = "lock:shop:";

    @Autowired
    private ShopUpdateProducerService shopUpdateProducerService;

    public ShopServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 根据id查，保持数据的一致性
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        String key =CACHE_SHOP_KEY+id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //缓存部分
        if (StrUtil.isNotBlank(shopJson)){
            if (shopJson.equals(JSONUtil.toJsonStr(EMPTY_SHOP))){
                return Result.fail("店铺不存在");
            }
            Shop shop = JSONUtil.toBean(shopJson,Shop.class);
            return Result.ok(shop);
        }
        String lockkey = LOCK_SHOP_KEY+id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockkey);
            if (!isLock){
                Thread.sleep(50);
                return queryById(id);
            }
            shopJson = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(shopJson)) {
                if (shopJson.equals(JSONUtil.toJsonStr(EMPTY_SHOP))) {
                    return Result.fail("店铺不存在");
                }
                shop = JSONUtil.toBean(shopJson, Shop.class);
                return Result.ok(shop);
            }
            shop = this.getById(id);
            if (Objects.isNull(shop)){
                stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(EMPTY_SHOP),CACHE_NULL_TTL,TimeUnit.SECONDS);
                return Result.fail("店铺不存在");
            }

            long randwomTtl = CACHE_SHOP_TTL+random.nextInt(20*60)-10*60;
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shopJson),randwomTtl, TimeUnit.SECONDS);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程中断",e);
        }finally {
            unlock(lockkey);
        }
        return Result.ok(shop);
    }

    @Override
    public Result queryShopList() {
        String key =CACHE_SHOP_KEY+"list";
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        List<Shop> shoplist = null;
        //缓存部分
        if (StrUtil.isNotBlank(shopJson)){
            if (shopJson.equals(JSONUtil.toJsonStr(EMPTY_SHOP))){
                return Result.fail("店铺不存在");
            }
            Shop shop = JSONUtil.toBean(shopJson,Shop.class);
            return Result.ok(shop);
        }
        String lockkey = LOCK_SHOP_KEY+"list";
        try {
            boolean isLock = tryLock(lockkey);
            if (!isLock){
                Thread.sleep(50);
                return queryShopList();
            }
            shopJson = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(shopJson)) {
                if (shopJson.equals(JSONUtil.toJsonStr(EMPTY_SHOP))) {
                    return Result.fail("店铺不存在");
                }
                shoplist = JSONUtil.toList(shopJson, Shop.class);
                return Result.ok(shoplist);
            }
            shoplist =this.list(new LambdaQueryWrapper<Shop>().orderByAsc(Shop::getId));
            if (Objects.isNull(shoplist)){
                stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(EMPTY_SHOP),CACHE_NULL_TTL,TimeUnit.SECONDS);
                return Result.fail("店铺不存在");
            }

            long randwomTtl = CACHE_SHOP_TTL+random.nextInt(20*60)-10*60;
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shopJson),randwomTtl, TimeUnit.SECONDS);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程中断",e);
        }finally {
            unlock(lockkey);
        }
        return Result.ok(shoplist);
    }

    /**
     * 更新数据
     * @param shop
     * @return
     */
    @Transactional
    @Override
    public Result updateShop(Shop shop) {
        Long id =shop.getId();
        if (id==null) return Result.fail("店铺id为空");

        Result mqresult = shopUpdateProducerService.requestShopUpdate(shop);
        if (!mqresult.isSuccess()){
            log.error("请求店铺 {} 更新消息失败: {}");
            return Result.fail("系统繁忙，请稍后再试");
        }
        return Result.ok();
    }

    /**
     * 保存热点数据
     * @param id
     * @param expireSeconds
     */

    @Override
    public void saveShopToCache(Long id, Long expireSeconds) {
        Shop shop = this.getById(id);
        if (shop==null){
            return;
        }
        String key = CACHE_SHOP_KEY+id;
        stringRedisTemplate.opsForValue().set(
                key,JSONUtil.toJsonStr(shop),
                expireSeconds,TimeUnit.SECONDS
        );
    }

    @Override
    public List<Shop> searchByName(String query) {
        return null;
    }

    /**
     * 尝试获取锁和释放锁
     * @param key
     * @return
     */
    private boolean tryLock(String key){
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",LOCK_SHOP_TTL,TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
