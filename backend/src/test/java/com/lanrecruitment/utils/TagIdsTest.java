package com.lanrecruitment.utils;

import com.lanrecruitment.exception.BizException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class TagIdsTest {
    @Test void emptyMeansNoFilterAndValidIdsAreParsed() {
        assertThat(TagIds.parse(null)).isEmpty();
        assertThat(TagIds.parse("  ")).isEmpty();
        assertThat(TagIds.parse(" 1, 2 ")).isEqualTo(Arrays.asList(1L,2L));
    }
    @Test void rejectEmptyElementsAndInvalidIds() {
        for (String value : Arrays.asList(",",",,","1,,2","1,",",1","abc","0","-1","9223372036854775808"))
            assertThatThrownBy(() -> TagIds.parse(value)).as(value).isInstanceOf(BizException.class);
    }
}
