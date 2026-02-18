package com.lanrecruitment.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.dao.SaTokenDaoRedisJackson;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class SaTokenDaoStoreConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestBeans.class, SaTokenDaoStoreConfig.class);

    @Test
    void shouldUseMemoryDaoByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SaTokenDao.class);
            assertThat(context.getBean(SaTokenDao.class)).isInstanceOf(SaTokenDaoDefaultImpl.class);
        });
    }

    @Test
    void shouldUseRedisDaoWhenConfigured() {
        contextRunner
                .withPropertyValues("lan.sa-token.store=redis")
                .run(context -> {
                    assertThat(context).hasSingleBean(SaTokenDao.class);
                    SaTokenDao dao = context.getBean(SaTokenDao.class);
                    assertThat(dao).isInstanceOf(SaTokenDaoRedisJackson.class);
                    assertThat(((SaTokenDaoRedisJackson) dao).isInit).isTrue();
                });
    }

    @Configuration
    static class TestBeans {

        @Bean
        public RedisConnectionFactory redisConnectionFactory() {
            return Mockito.mock(RedisConnectionFactory.class);
        }
    }
}

