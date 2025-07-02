package com.mengnankk.service;


public interface BloomFilterService<T> {
    void put(T value);
    boolean mightContain(T value);
}
