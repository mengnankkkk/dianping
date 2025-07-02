package com.mengnankk.config;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.mengnankk.service.BloomFilterService;
import com.mengnankk.service.Impl.BloomFilterServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BloomFilterConfig {

    @Value("${bloomfilter.expected-insertions}")
    private int expectedInsertions;

    @Value("${bloomfilter.false-positive-probability}")
    private double falsePositiveProbability;

    @Bean
    public BloomFilterService<Long> bloomFilterService() {
        BloomFilter<Long> bloomFilter = BloomFilter.create(
                Funnels.longFunnel(),
                expectedInsertions,
                falsePositiveProbability
        );
        return new BloomFilterServiceImpl<>(bloomFilter);
    }
}