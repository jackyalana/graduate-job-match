package com.lanrecruitment.matching;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ContentScorerTest {
    @Test void goldenExampleUsesUnroundedValues() {
        double content=ContentScorer.content(0.8,1.0,1.0,0.5);
        assertThat(content).isCloseTo(0.825,within(0.00000001));
        assertThat(100*content).isCloseTo(82.5,within(0.00000001));
        assertThat(100*ContentScorer.recommendation(content,0.4)).isCloseTo(74.0,within(0.00000001));
    }
    @Test void missingDiffersFromMismatch() {
        assertThat(ContentScorer.content(null,null,null,null)).isNull();
        assertThat(ContentScorer.content(null,1.0,null,0.0)).isCloseTo(0.2/0.35,within(0.00000001));
        Map<Long,Integer> student=Collections.singletonMap(1L,2);
        Map<Long,ContentScorer.Requirement> job=Collections.singletonMap(2L,new ContentScorer.Requirement(3,2));
        assertThat(ContentScorer.skill(Collections.emptyMap(),job)).isNull();
        assertThat(ContentScorer.skill(student,Collections.emptyMap())).isNull();
        assertThat(ContentScorer.skill(student,job)).isEqualTo(0.0);
        assertThat(ContentScorer.salary(0,10000,false)).isNull();
        assertThat(ContentScorer.salary(10000,5000,false)).isEqualTo(0.5);
        assertThat(ContentScorer.salary(10000,5000,true)).isNull();
        assertThat(ContentScorer.recommendation(null,0.4)).isEqualTo(0.4);
    }
    @Test void coverageAndCosineIncludeExtraSkillsAndImportance() {
        Map<Long,Integer> student=new HashMap<>(); student.put(1L,2); student.put(3L,1);
        Map<Long,ContentScorer.Requirement> job=new HashMap<>(); job.put(1L,new ContentScorer.Requirement(2,3)); job.put(2L,new ContentScorer.Requirement(1,1));
        assertThat(ContentScorer.skill(student,job)).isCloseTo(0.5*(4.0/5.0)+0.5*(3.0/4.0),within(0.00000001));
        assertThatThrownBy(()->new ContentScorer.Requirement(2,6)).isInstanceOf(IllegalArgumentException.class);
    }
}
