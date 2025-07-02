package com.mengnankk.service.Impl;

import com.google.common.hash.BloomFilter;
import com.mengnankk.service.BloomFilterService;

public class BloomFilterServiceImpl<T> implements BloomFilterService<T> {

    private final BloomFilter<T> bloomFilter;

    public BloomFilterServiceImpl(BloomFilter<T> bloomFilter) {
        this.bloomFilter = bloomFilter;
    }

    @Override
    public void put(T value) {
        bloomFilter.put(value);
    }

    @Override
    public boolean mightContain(T value) {
        return bloomFilter.mightContain(value);
    }
}
