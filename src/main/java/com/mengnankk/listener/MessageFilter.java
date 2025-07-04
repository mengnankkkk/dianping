package com.mengnankk.listener;

import com.mengnankk.entity.mq.ShopUpdateMessage;

public interface MessageFilter {
    boolean filter(ShopUpdateMessage message);
}
