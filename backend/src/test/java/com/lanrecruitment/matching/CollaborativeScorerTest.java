package com.lanrecruitment.matching;
import com.lanrecruitment.domain.entity.BehaviorEvent;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class CollaborativeScorerTest {
 private BehaviorEvent event(long id,long user,long job,String type,LocalDateTime time){BehaviorEvent e=new BehaviorEvent();e.setId(id);e.setUserId(user);e.setJobId(job);e.setEventType(type);e.setEventTime(time);return e;}
 @Test void cutoffNeighborsAndCancellationAreRespected(){
  LocalDateTime t=LocalDateTime.of(2026,10,27,0,0);List<BehaviorEvent> events=Arrays.asList(
   event(1,1,1,"VIEW",t.minusDays(3)),event(2,1,2,"FAVORITE",t.minusDays(2)),event(3,1,3,"APPLY",t.minusDays(1)),
   event(4,2,1,"VIEW",t.minusDays(3)),event(5,2,2,"FAVORITE",t.minusDays(2)),event(6,2,3,"APPLY",t.minusDays(1)),event(7,2,4,"APPLY",t.minusHours(1)),
   event(8,2,5,"FAVORITE",t.minusHours(2)),event(9,2,5,"UNFAVORITE",t.minusHours(1)),event(10,2,6,"APPLY",t.plusSeconds(1)));
  Map<Long,Map<Long,Integer>> v=CollaborativeScorer.vectors(events,Collections.emptyList(),t);
  assertThat(v.get(2L)).doesNotContainKeys(5L,6L);
  Map<Long,Double> scores=CollaborativeScorer.scores(1L,Arrays.asList(4L,5L,6L),v);
  assertThat(scores.get(4L)).isEqualTo(1.0);assertThat(scores.get(5L)).isEqualTo(0.0);assertThat(scores.get(6L)).isEqualTo(0.0);
  assertThat(CollaborativeScorer.scores(3L,Collections.singletonList(4L),v)).isNull();
 }
}
