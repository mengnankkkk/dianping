package com.mengnankk.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache <K,V> extends LinkedHashMap<K,V> {
    private int capacity;
    public LRUCache(int capacity){
        super(capacity,0.75f,true);// 0.75f 是默认的加载因子, true 表示按照访问顺序排序
        this.capacity = capacity;
    }
    @Override
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest){
        return size()>capacity;
    }

}
