package com.lanrecruitment.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.dao.SaTokenDaoRedisJackson;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
public class SaTokenDaoStoreConfig {

    @Bean
    @ConditionalOnProperty(prefix = "lan.sa-token", name = "store", havingValue = "redis")
    @ConditionalOnClass({RedisConnectionFactory.class, SaTokenDaoRedisJackson.class})
    public SaTokenDao saTokenDaoRedis(RedisConnectionFactory redisConnectionFactory) {
        SaTokenDaoRedisJackson saTokenDao = new SaTokenDaoRedisJackson();
        saTokenDao.init(redisConnectionFactory);
        return saTokenDao;
    }

    @Bean
    @ConditionalOnProperty(prefix = "lan.sa-token", name = "store", havingValue = "memory", matchIfMissing = true)
    @ConditionalOnClass(SaTokenDaoDefaultImpl.class)
    public SaTokenDao saTokenDaoMemory() {
        return new SaTokenDaoDefaultImpl();
    }
}

