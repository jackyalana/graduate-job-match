package com.lanrecruitment.cache.redis;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisClient {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void setEx(String key, String value, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    public void del(String key) {
        stringRedisTemplate.delete(key);
    }

    public void del(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        stringRedisTemplate.delete(keys);
    }

    public void sadd(String key, String member) {
        stringRedisTemplate.opsForSet().add(key, member);
    }

    public Set<String> smembers(String key) {
        Set<String> members = stringRedisTemplate.opsForSet().members(key);
        return members == null ? Collections.emptySet() : members;
    }

    public void srem(String key, String member) {
        stringRedisTemplate.opsForSet().remove(key, member);
    }

    public void expire(String key, long ttlSeconds) {
        stringRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }
}
