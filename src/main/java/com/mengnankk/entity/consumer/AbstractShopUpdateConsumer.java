package com.mengnankk.entity.consumer;

import com.mengnankk.entity.mq.ShopUpdateMessage;
import org.mybatis.logging.Logger;
import org.mybatis.logging.LoggerFactory;

public abstract class AbstractShopUpdateConsumer {
    private static final Logger logger = LoggerFactory.getLogger(AbstractShopUpdateConsumer.class);
    public abstract void consume(ShopUpdateMessage message);
}