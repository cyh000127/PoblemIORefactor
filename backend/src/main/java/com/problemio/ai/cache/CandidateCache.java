package com.problemio.ai.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class CandidateCache {

    private final Cache<String, byte[]> cache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(200)
            .build();

    public void put(String candidateId, byte[] bytes) {
        cache.put(candidateId, bytes);
    }

    public byte[] get(String candidateId) {
        return cache.getIfPresent(candidateId);
    }

    public void evict(String candidateId) {
        cache.invalidate(candidateId);
    }
}
