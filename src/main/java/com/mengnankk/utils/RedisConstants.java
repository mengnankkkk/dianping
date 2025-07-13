package com.mengnankk.utils;

public class RedisConstants {
    public static final String CACHE_SHOP_KEY = "cache:shop:";
    public static final Long CACHE_SHOP_TTL = 30L; // 30分钟
    public static final String CACHE_SHOP_TYPE_KEY = "cache:shopType:";
    public static final Long CACHE_SHOP_TYPE_TTL = 60L; // 60分钟
    public static final Long CACHE_NULL_TTL = 2L; // 2分钟，用于缓存空对象
    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L; // 10秒
    public static final Long CACHE_SHOP_LOGICAL_TTL = 60L * 60 * 24L;
    public static final String BLOG_LIKED_KEY = "blog:liked:"; // blog:liked:{id}
    public static final String USER_SIGN_KEY = "sign:"; // sign:userId:yyyyMM
    public static final String SHOP_GEO_KEY = "shop:geo:"; // shop:geo:{typeId}
    public static final String FOLLOW_KEY = "follows:";// follows:{userId}
    public static final String FEED_KEY = "feed:"; // feed:{userId}
}
