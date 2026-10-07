package com.example.rediscache.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration. Disabled when the "nocache" profile is active.
 *  - keys are plain strings, e.g.  poc:products:1
 *  - values are stored as readable JSON
 *  - each cache has its own TTL
 *  - if Redis goes down, the app logs the error and falls back to the database
 */
@Configuration
@Profile("nocache")
public class CacheConfig implements CachingConfigurer {

    public static final String PRODUCTS_CACHE = "products";
    public static final String PRODUCT_LIST_CACHE = "productList";

    @Value("${app.cache.product-ttl-minutes:10}")
    private long productTtlMinutes;

    @Value("${app.cache.product-list-ttl-minutes:2}")
    private long productListTtlMinutes;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "poc:" + cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put(PRODUCTS_CACHE, defaults.entryTtl(Duration.ofMinutes(productTtlMinutes)));
        perCache.put(PRODUCT_LIST_CACHE, defaults.entryTtl(Duration.ofMinutes(productListTtlMinutes)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    /** Cache problems (e.g. Redis down) must never break the API; just log and hit the DB. */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}
