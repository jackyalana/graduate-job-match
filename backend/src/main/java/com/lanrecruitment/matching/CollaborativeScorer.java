package com.lanrecruitment.matching;
import com.lanrecruitment.domain.entity.BehaviorEvent;
import com.lanrecruitment.domain.entity.JobApply;
import java.time.LocalDateTime;
import java.util.*;
/** Implicit user-neighbour collaborative filtering; deterministic and cutoff bounded. */
public final class CollaborativeScorer {
 private CollaborativeScorer(){}
 private static class State {boolean view,favorite,apply;}
 public static Map<Long,Map<Long,Integer>> vectors(List<BehaviorEvent> events,List<JobApply> applications,LocalDateTime cutoff){
  Map<Long,Map<Long,State>> states=new TreeMap<>();LocalDateTime since=cutoff.minusDays(90);
  List<BehaviorEvent> ordered=new ArrayList<>(events);ordered.sort(Comparator.comparing(BehaviorEvent::getEventTime).thenComparing(BehaviorEvent::getId));
  for(BehaviorEvent e:ordered){if(e.getEventTime()==null || e.getEventTime().isAfter(cutoff) || e.getEventTime().isBefore(since))continue;
   State s=states.computeIfAbsent(e.getUserId(),k->new TreeMap<>()).computeIfAbsent(e.getJobId(),k->new State());
   switch(e.getEventType()){case "VIEW":s.view=true;break;case "FAVORITE":s.favorite=true;break;case "UNFAVORITE":s.favorite=false;break;case "APPLY":s.apply=true;break;default:break;}
  }
  for(JobApply a:applications){if(a.getCreatedAt()!=null && !a.getCreatedAt().isAfter(cutoff) && !a.getCreatedAt().isBefore(since))states.computeIfAbsent(a.getUserId(),k->new TreeMap<>()).computeIfAbsent(a.getJobId(),k->new State()).apply=true;}
  Map<Long,Map<Long,Integer>> vectors=new TreeMap<>();
  for(Map.Entry<Long,Map<Long,State>> user:states.entrySet())for(Map.Entry<Long,State> item:user.getValue().entrySet()){
   State s=item.getValue();int weight=s.apply?5:s.favorite?3:s.view?1:0;if(weight>0)vectors.computeIfAbsent(user.getKey(),k->new TreeMap<>()).put(item.getKey(),weight);
  }return vectors;
 }
 private static class Neighbor {long id;double similarity;Map<Long,Integer> vector;Neighbor(long id,double similarity,Map<Long,Integer> vector){this.id=id;this.similarity=similarity;this.vector=vector;}}
 public static Map<Long,Double> scores(Long user,Collection<Long> candidates,Map<Long,Map<Long,Integer>> all) {
  Map<Long,Integer> target=all.getOrDefault(user,Collections.emptyMap());if(target.size()<3)return null;
  double targetNorm=0;for(int x:target.values())targetNorm+=x*x;List<Neighbor> neighbors=new ArrayList<>();
  for(Map.Entry<Long,Map<Long,Integer>> e:all.entrySet()) {if(user.equals(e.getKey()))continue;int common=0;double dot=0,norm=0;
   for(Map.Entry<Long,Integer> item:e.getValue().entrySet()){int value=item.getValue();norm+=value*value;Integer mine=target.get(item.getKey());if(mine!=null){common++;dot+=mine*value;}}
   if(common>=2 && dot>0 && norm>0)neighbors.add(new Neighbor(e.getKey(),dot/(Math.sqrt(targetNorm)*Math.sqrt(norm)),e.getValue()));
  }
  neighbors.sort(Comparator.<Neighbor>comparingDouble(n->n.similarity).reversed().thenComparingLong(n->n.id));
  if(neighbors.isEmpty())return null;if(neighbors.size()>20)neighbors=neighbors.subList(0,20);
  double denominator=0;for(Neighbor n:neighbors)denominator+=n.similarity;
  Map<Long,Double> result=new HashMap<>();for(Long job:candidates){double sum=0;for(Neighbor n:neighbors)sum+=n.similarity*n.vector.getOrDefault(job,0)/5.0;result.put(job,sum/denominator);}return result;
 }
}
