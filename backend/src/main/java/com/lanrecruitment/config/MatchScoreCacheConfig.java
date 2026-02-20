package com.lanrecruitment.config;

import com.lanrecruitment.cache.InMemoryMatchScoreCache;
import com.lanrecruitment.cache.MatchScoreCache;
import com.lanrecruitment.cache.RedisMatchScoreCache;
import com.lanrecruitment.cache.redis.RedisClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class MatchScoreCacheConfig {

    @Bean
    @ConditionalOnProperty(prefix = "lan.match.cache", name = "store", havingValue = "redis")
    @ConditionalOnClass(StringRedisTemplate.class)
    public RedisClient redisClient(StringRedisTemplate stringRedisTemplate) {
        return new RedisClient(stringRedisTemplate);
    }

    @Bean
    @ConditionalOnProperty(prefix = "lan.match.cache", name = "store", havingValue = "redis")
    @ConditionalOnClass(StringRedisTemplate.class)
    public MatchScoreCache matchScoreCacheRedis(RedisClient redisClient) {
        return new RedisMatchScoreCache(redisClient);
    }

    @Bean
    @ConditionalOnProperty(prefix = "lan.match.cache", name = "store", havingValue = "memory", matchIfMissing = true)
    public MatchScoreCache matchScoreCacheMemory() {
        return new InMemoryMatchScoreCache();
    }
}
