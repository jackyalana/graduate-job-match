package com.lanrecruitment.config;

import com.lanrecruitment.cache.InMemoryMatchScoreCache;
import com.lanrecruitment.cache.MatchScoreCache;
import com.lanrecruitment.cache.RedisMatchScoreCache;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class MatchScoreCacheConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestBeans.class, MatchScoreCacheConfig.class);

    @Test
    void shouldUseMemoryCacheByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MatchScoreCache.class);
            assertThat(context.getBean(MatchScoreCache.class)).isInstanceOf(InMemoryMatchScoreCache.class);
        });
    }

    @Test
    void shouldUseRedisCacheWhenConfigured() {
        contextRunner.withPropertyValues("lan.match.cache.store=redis").run(context -> {
            assertThat(context).hasSingleBean(MatchScoreCache.class);
            assertThat(context.getBean(MatchScoreCache.class)).isInstanceOf(RedisMatchScoreCache.class);
        });
    }

    @Configuration
    static class TestBeans {

        @Bean
        public RedisConnectionFactory redisConnectionFactory() {
            return Mockito.mock(RedisConnectionFactory.class);
        }

        @Bean
        public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
            return new StringRedisTemplate(redisConnectionFactory);
        }
    }
}

