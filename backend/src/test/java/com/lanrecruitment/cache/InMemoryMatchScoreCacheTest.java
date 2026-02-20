package com.lanrecruitment.cache;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryMatchScoreCacheTest {

    @Test
    void shouldReturnCachedScore() {
        InMemoryMatchScoreCache cache = new InMemoryMatchScoreCache();
        cache.put(1L, 2L, new BigDecimal("0.1234"));
        assertThat(cache.get(1L, 2L)).isEqualByComparingTo("0.1234");
    }

    @Test
    void shouldInvalidateByResume() {
        InMemoryMatchScoreCache cache = new InMemoryMatchScoreCache();
        cache.put(1L, 2L, new BigDecimal("0.1111"));
        cache.put(1L, 3L, new BigDecimal("0.2222"));
        cache.put(9L, 2L, new BigDecimal("0.9999"));

        cache.invalidateByResume(1L);

        assertThat(cache.get(1L, 2L)).isNull();
        assertThat(cache.get(1L, 3L)).isNull();
        assertThat(cache.get(9L, 2L)).isEqualByComparingTo("0.9999");
    }

    @Test
    void shouldInvalidateByJob() {
        InMemoryMatchScoreCache cache = new InMemoryMatchScoreCache();
        cache.put(1L, 2L, new BigDecimal("0.1111"));
        cache.put(9L, 2L, new BigDecimal("0.9999"));
        cache.put(9L, 3L, new BigDecimal("0.2222"));

        cache.invalidateByJob(2L);

        assertThat(cache.get(1L, 2L)).isNull();
        assertThat(cache.get(9L, 2L)).isNull();
        assertThat(cache.get(9L, 3L)).isEqualByComparingTo("0.2222");
    }
}
