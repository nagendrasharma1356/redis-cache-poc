package com.example.rediscache.controller;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Small helper endpoints to inspect Redis while demoing. (POC only - KEYS is not for production.) */
@RestController
@RequestMapping("/api/cache")
public class CacheController {

    private final StringRedisTemplate redis;
    private final CacheManager cacheManager;

    public CacheController(StringRedisTemplate redis, CacheManager cacheManager) {
        this.redis = redis;
        this.cacheManager = cacheManager;
    }

    /** Lists the keys currently stored in Redis with their remaining TTL (seconds). */
    @GetMapping("/keys")
    public ResponseEntity<Map<String, Object>> keys() {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            Set<String> keys = redis.keys("poc:*");
            Map<String, Long> withTtl = new LinkedHashMap<>();
            if (keys != null) {
                for (String key : new TreeSet<>(keys)) {
                    withTtl.put(key, redis.getExpire(key));
                }
            }
            body.put("count", withTtl.size());
            body.put("keysWithTtlSeconds", withTtl);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("error", "Redis not reachable: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }

    /** Raw JSON value stored in Redis for a key, e.g. /api/cache/value?key=poc:products:1 */
    @GetMapping("/value")
    public ResponseEntity<Map<String, Object>> value(@RequestParam String key) {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            body.put("key", key);
            body.put("value", redis.opsForValue().get(key));
            body.put("ttlSeconds", redis.getExpire(key));
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("error", "Redis not reachable: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }

    /** Clears every Spring cache (products + productList). */
    @DeleteMapping("/clear")
    public Map<String, Object> clearAll() {
        Map<String, Object> body = new LinkedHashMap<>();
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
        body.put("cleared", cacheManager.getCacheNames());
        return body;
    }
}
