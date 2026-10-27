package com.lanrecruitment.evaluation;
import com.lanrecruitment.domain.entity.BehaviorEvent;
import com.lanrecruitment.matching.*;
import java.time.LocalDateTime;
import java.util.*;
/** Fixed synthetic experiment using production scoring functions; outputs JSON on stdout. */
public class EvaluationMain {
 static class Job {long id;long category;Job(long id,long category){this.id=id;this.category=category;}}
 static class Metrics {double p5,p10,r5,r10,n5,n10,h5,h10;int users;Set<Long> coverage=new HashSet<>();
  void add(List<Long> sorted,Set<Long> relevant){users++;int hits5=0,hits10=0;double dcg5=0,dcg10=0;
   for(int i=0;i<Math.min(10,sorted.size());i++){long id=sorted.get(i);coverage.add(id);if(relevant.contains(id)){hits10++;double gain=1/(Math.log(i+2)/Math.log(2));dcg10+=gain;if(i<5){hits5++;dcg5+=gain;}}}
   double ideal=0;for(int i=0;i<Math.min(2,relevant.size());i++)ideal+=1/(Math.log(i+2)/Math.log(2));
   p5+=hits5/5.0;p10+=hits10/10.0;r5+=(double)hits5/relevant.size();r10+=(double)hits10/relevant.size();n5+=dcg5/ideal;n10+=dcg10/ideal;h5+=hits5>0?1:0;h10+=hits10>0?1:0;
  }
  String json(int denominator){return String.format(Locale.ROOT,"{\"users\":%d,\"precision5\":%.6f,\"precision10\":%.6f,\"recall5\":%.6f,\"recall10\":%.6f,\"ndcg5\":%.6f,\"ndcg10\":%.6f,\"hit5\":%.6f,\"hit10\":%.6f,\"coverage10\":%.6f}",users,p5/users,p10/users,r5/users,r10/users,n5/users,n10/users,h5/users,h10/users,(double)coverage.size()/denominator);}
 }
 static BehaviorEvent event(long id,long user,long job,LocalDateTime time){BehaviorEvent e=new BehaviorEvent();e.setId(id);e.setUserId(user);e.setJobId(job);e.setEventTime(time);e.setEventType("APPLY");return e;}
 public static void main(String[] args){long seed=20261027L;Random random=new Random(seed);LocalDateTime start=LocalDateTime.of(2026,1,1,0,0),trainCutoff=start.plusDays(70),validCutoff=start.plusDays(85),end=start.plusDays(100);
  List<Job> jobs=new ArrayList<>();for(int j=0;j<80;j++)jobs.add(new Job(j+1,1+(j%8)));
  List<BehaviorEvent> train=new ArrayList<>(),validation=new ArrayList<>(),test=new ArrayList<>();Map<Long,Set<Long>> heldout=new TreeMap<>(),seen=new TreeMap<>();Map<Long,Long> desired=new TreeMap<>();long eventId=1;
  for(long u=1;u<=60;u++){long topic=1+random.nextInt(8);desired.put(u,u<=10?null:topic);List<Long> choices=new ArrayList<>();for(Job j:jobs)if(j.category==topic)choices.add(j.id);Collections.shuffle(choices,random);
   Set<Long> userSeen=new HashSet<>();int trainCount=u<=10?0:5;for(int i=0;i<trainCount;i++){long j=choices.get(i);userSeen.add(j);train.add(event(eventId++,u,j,start.plusDays(60+i)));}
   seen.put(u,userSeen);validation.add(event(eventId++,u,choices.get(trainCount),trainCutoff.plusDays(7)));Set<Long> positives=new HashSet<>();positives.add(choices.get(trainCount+1));positives.add(choices.get(trainCount+2));heldout.put(u,positives);test.add(event(eventId++,u,choices.get(trainCount+1),validCutoff.plusDays(1)));test.add(event(eventId++,u,choices.get(trainCount+2),validCutoff.plusDays(2)));
  }
  Map<Long,Map<Long,Integer>> vectors=CollaborativeScorer.vectors(train,Collections.emptyList(),trainCutoff);
  Map<Long,Set<Long>> applicants=new HashMap<>();for(BehaviorEvent e:train)if(!e.getEventTime().isBefore(trainCutoff.minusDays(30)))applicants.computeIfAbsent(e.getJobId(),k->new HashSet<>()).add(e.getUserId());
  Map<String,Metrics> metrics=new LinkedHashMap<>();for(String s:Arrays.asList("HOT","CONTENT","HYBRID"))metrics.put(s,new Metrics());Map<String,Metrics> coldMetrics=new LinkedHashMap<>(),warmMetrics=new LinkedHashMap<>();for(String method:metrics.keySet()){coldMetrics.put(method,new Metrics());warmMetrics.put(method,new Metrics());}int fallbacks=0,cold=0;Set<Long> eligibleUnion=new HashSet<>();
  for(long u=1;u<=60;u++){List<Long> candidates=new ArrayList<>();for(Job j:jobs)if(!seen.get(u).contains(j.id)){candidates.add(j.id);eligibleUnion.add(j.id);}if(vectors.getOrDefault(u,Collections.emptyMap()).isEmpty())cold++;
   Map<Long,Double> cf=CollaborativeScorer.scores(u,candidates,vectors);if(cf==null && desired.get(u)==null)fallbacks++;
   long max=0;for(long id:candidates)max=Math.max(max,applicants.getOrDefault(id,Collections.emptySet()).size());
   final Map<Long,Double> hot=new HashMap<>(),content=new HashMap<>(),hybrid=new HashMap<>();
   for(long id:candidates){Job j=jobs.get((int)id-1);double h=ContentScorer.hot(applicants.getOrDefault(id,Collections.emptySet()).size(),max,40);
    Double cat=desired.get(u)==null?null:(desired.get(u)==j.category?1.0:0.0);Double c=ContentScorer.content(null,cat,null,null);
    MatchExplanation exp=RecommendationCalculator.calculate(id,Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),desired.get(u),j.category,Collections.emptySet(),null,null,null,null,h,cf==null?null:cf.get(id));
    hot.put(id,h);content.put(id,c==null?0:c);hybrid.put(id,exp.getRawRecommendation());
   }
   for(String method:metrics.keySet()){Map<Long,Double> values=method.equals("HOT")?hot:method.equals("CONTENT")?content:hybrid;List<Long> ranked=new ArrayList<>(candidates);Comparator<Long> order=Comparator.<Long>comparingDouble(values::get).reversed();if(method.equals("HYBRID"))order=order.thenComparing(Comparator.<Long>comparingDouble(content::get).reversed());ranked.sort(order.thenComparingLong(x->x));metrics.get(method).add(ranked,heldout.get(u));(u<=10?coldMetrics:warmMetrics).get(method).add(ranked,heldout.get(u));}
  }
  StringBuilder raw=new StringBuilder();for(Job j:jobs)raw.append(j.id).append(':').append(j.category).append(';');
  for(Map.Entry<Long,Long> d:desired.entrySet())raw.append(d.getKey()).append(':').append(d.getValue()).append(';');
  for(List<BehaviorEvent> series:Arrays.asList(train,validation,test))for(BehaviorEvent e:series)raw.append(e.getId()).append(':').append(e.getUserId()).append(':').append(e.getJobId()).append(':').append(e.getEventTime()).append(';');
  String datasetHash;try{byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(raw.toString().getBytes("UTF-8"));StringBuilder b=new StringBuilder();for(byte x:bytes)b.append(String.format("%02x",x&255));datasetHash=b.toString();}catch(Exception e){throw new RuntimeException(e);}
  System.out.println("{\"seed\":"+seed+",\"datasetSha256\":\""+datasetHash+"\",\"data\":\"synthetic topics and independent post-cutoff positives\",\"users\":60,\"jobs\":80,\"trainEvents\":"+train.size()+",\"validationEventsNotUsed\":"+validation.size()+",\"testEvents\":"+test.size()+",\"coldUsers\":"+cold+",\"eligibleUnion\":"+eligibleUnion.size()+",\"fallbackRatio\":"+String.format(Locale.ROOT,"%.6f",fallbacks/60.0)+",\"start\":\""+start+"\",\"trainCutoff\":\""+trainCutoff+"\",\"validationCutoff\":\""+validCutoff+"\",\"testEnd\":\""+end+"\",\"metrics\":{\"HOT\":"+metrics.get("HOT").json(eligibleUnion.size())+",\"CONTENT\":"+metrics.get("CONTENT").json(eligibleUnion.size())+",\"HYBRID\":"+metrics.get("HYBRID").json(eligibleUnion.size())+"},\"coldMetrics\":{\"HOT\":"+coldMetrics.get("HOT").json(eligibleUnion.size())+",\"CONTENT\":"+coldMetrics.get("CONTENT").json(eligibleUnion.size())+",\"HYBRID\":"+coldMetrics.get("HYBRID").json(eligibleUnion.size())+"},\"warmMetrics\":{\"HOT\":"+warmMetrics.get("HOT").json(eligibleUnion.size())+",\"CONTENT\":"+warmMetrics.get("CONTENT").json(eligibleUnion.size())+",\"HYBRID\":"+warmMetrics.get("HYBRID").json(eligibleUnion.size())+"}}");
 }
}
