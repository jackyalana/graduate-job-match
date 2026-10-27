package com.lanrecruitment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.domain.entity.*;
import com.lanrecruitment.domain.dto.RecommendationRequest;
import com.lanrecruitment.domain.vo.JobCardVO;
import com.lanrecruitment.exception.BizException;
import com.lanrecruitment.mapper.*;
import com.lanrecruitment.matching.*;
import com.lanrecruitment.service.JobEligibilityService;
import com.lanrecruitment.service.RecommendationService;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationServiceImpl implements RecommendationService {
    private final JobMapper jobs; private final ResumeMapper resumes; private final ResumeTagMapper resumeTags;
    private final JobTagMapper jobTags; private final TagMapper tags; private final StudentProfileMapper profiles;
    private final StudentCityPreferenceMapper cities; private final JobApplyMapper applies;
    private final RecommendationBatchMapper batches; private final JobEligibilityService eligibility; private final ObjectMapper json;
    private final SkillTagAliasMapper aliases;
    private final BehaviorEventMapper behaviors;
    private final UserJobActionMapper actions;
    public RecommendationServiceImpl(JobMapper jobs, ResumeMapper resumes, ResumeTagMapper resumeTags, JobTagMapper jobTags,
            TagMapper tags, StudentProfileMapper profiles, StudentCityPreferenceMapper cities, JobApplyMapper applies,
            RecommendationBatchMapper batches, JobEligibilityService eligibility, ObjectMapper json, SkillTagAliasMapper aliases,
            BehaviorEventMapper behaviors, UserJobActionMapper actions) {
        this.jobs=jobs;this.resumes=resumes;this.resumeTags=resumeTags;this.jobTags=jobTags;this.tags=tags;
        this.profiles=profiles;this.cities=cities;this.applies=applies;this.batches=batches;this.eligibility=eligibility;this.json=json;this.aliases=aliases;this.behaviors=behaviors;this.actions=actions;
    }
    private static class Snapshot {
        StudentProfile profile; Resume resume; List<StudentCityPreference> cities; List<ResumeTag> resumeTags;
        List<Job> jobs; List<JobTag> jobTags; List<Tag> tags; List<SkillTagAlias> aliases; List<JobApply> applies;
        List<UserJobAction> favorites; Map<Long,Map<Long,Integer>> vectors; Long userId; String hash;
    }
    private Resume resolve(Long user,Long id) {
        if(id!=null) {
            Resume r=resumes.selectById(id);
            if(r==null || !user.equals(r.getUserId())) throw new BizException(ErrorCode.RESUME_NOT_FOUND);
            if(!Integer.valueOf(1).equals(r.getStatus())) throw new BizException(ErrorCode.RESUME_NOT_AVAILABLE);
            return r;
        }
        return resumes.selectOne(new LambdaQueryWrapper<Resume>().eq(Resume::getUserId,user).eq(Resume::getIsDefault,1).eq(Resume::getStatus,1).last("limit 1"));
    }
    private Snapshot snapshot(Long user,RecommendationRequest request,LocalDateTime now) {
        return snapshot(user,request,now,false);
    }
    private Snapshot snapshot(Long user,RecommendationRequest request,LocalDateTime now,boolean includeApplied) {
        Snapshot s=new Snapshot(); s.userId=user; s.resume=resolve(user,request.getResumeId());
        s.profile=profiles.selectOne(new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId,user));
        s.cities=cities.selectList(new LambdaQueryWrapper<StudentCityPreference>().eq(StudentCityPreference::getUserId,user).orderByAsc(StudentCityPreference::getId));
        s.resumeTags=s.resume==null ? Collections.emptyList() : resumeTags.selectList(new LambdaQueryWrapper<ResumeTag>().eq(ResumeTag::getResumeId,s.resume.getId()).orderByAsc(ResumeTag::getId));
        LambdaQueryWrapper<Job> q=new LambdaQueryWrapper<>(); if(includeApplied)eligibility.personalQueryIncludingApplied(q,user);else eligibility.personalQuery(q,user);
        Integer min=request.getSalaryMin(),max=request.getSalaryMax(); Long category=request.getCategoryId();
        if((min!=null && min<0)||(max!=null && max<0)||(min!=null && max!=null && min>max)||(category!=null && category<=0)) throw new BizException(ErrorCode.VALIDATION_FAILED);
        if(category!=null)q.eq(Job::getCategoryId,category);if(min!=null)q.ge(Job::getSalaryMax,min);if(max!=null)q.le(Job::getSalaryMin,max);
        if(request.getKeyword()!=null && !request.getKeyword().trim().isEmpty())q.and(w->w.like(Job::getJobName,request.getKeyword()).or().like(Job::getDescription,request.getKeyword()));
        if(request.getCity()!=null && !request.getCity().trim().isEmpty())q.and(w->w.eq(Job::getCityCode,request.getCity()).or().eq(Job::getCity,request.getCity()));
        if(request.getJobType()!=null && !request.getJobType().trim().isEmpty())q.eq(Job::getJobType,request.getJobType());
        List<Long> selected=com.lanrecruitment.utils.TagIds.parse(request.getTagIds());
        if(!selected.isEmpty()) {
            List<Long> matched=jobTags.selectList(new LambdaQueryWrapper<JobTag>().in(JobTag::getTagId,selected)).stream().map(JobTag::getJobId).distinct().collect(Collectors.toList());
            if(matched.isEmpty())q.eq(Job::getId,-1L);else q.in(Job::getId,matched);
        }
        s.jobs=jobs.selectList(q.orderByAsc(Job::getId));
        List<Long> ids=s.jobs.stream().map(Job::getId).collect(Collectors.toList());
        s.jobTags=ids.isEmpty()?Collections.emptyList():jobTags.selectList(new LambdaQueryWrapper<JobTag>().in(JobTag::getJobId,ids).orderByAsc(JobTag::getId));
        s.tags=tags.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getId));
        s.aliases=aliases.selectList(new LambdaQueryWrapper<SkillTagAlias>().orderByAsc(SkillTagAlias::getAliasTagId));
        s.applies=applies.selectList(new LambdaQueryWrapper<JobApply>().select(JobApply::getId,JobApply::getJobId,JobApply::getUserId,JobApply::getCreatedAt).ge(JobApply::getCreatedAt,now.minusDays(90)).le(JobApply::getCreatedAt,now).orderByAsc(JobApply::getId));
        List<BehaviorEvent> activity=behaviors.selectList(new LambdaQueryWrapper<BehaviorEvent>().ge(BehaviorEvent::getEventTime,now.minusDays(90)).le(BehaviorEvent::getEventTime,now).orderByAsc(BehaviorEvent::getId));
        s.vectors=CollaborativeScorer.vectors(activity,s.applies,now);
        s.favorites=actions.selectList(new LambdaQueryWrapper<UserJobAction>().eq(UserJobAction::getActionType,"FAVORITE").eq(UserJobAction::getActive,true).ge(UserJobAction::getUpdatedAt,now.minusDays(30)).le(UserJobAction::getUpdatedAt,now).orderByAsc(UserJobAction::getId));
        try {
            List<Object> signature=new ArrayList<>(); signature.add(ContentScorer.VERSION); signature.add(s.profile); signature.add(s.cities);
            signature.add(s.resume==null?null:Arrays.asList(s.resume.getId(),s.resume.getVersion(),s.resume.getStatus(),s.resume.getIsDefault()));
            signature.add(s.resumeTags);signature.add(s.jobs);signature.add(s.jobTags);signature.add(s.tags);signature.add(s.aliases);
            Set<String> uniquePairs=new TreeSet<>();
            for(JobApply application:s.applies) if(application.getCreatedAt()!=null && !application.getCreatedAt().isBefore(now.minusDays(30)) && application.getJobId()!=null && application.getUserId()!=null) uniquePairs.add(application.getJobId()+":"+application.getUserId());
            Set<String> favoritePairs=new TreeSet<>();for(UserJobAction a:s.favorites)favoritePairs.add(a.getJobId()+":"+a.getUserId());
            signature.add(uniquePairs);signature.add(favoritePairs);signature.add(s.vectors);
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(signature));
            StringBuilder b=new StringBuilder();for(byte x:hash)b.append(String.format("%02x",x & 255));s.hash=b.toString();
        } catch (Exception e) { throw new IllegalStateException("Cannot calculate input fingerprint",e); }
        return s;
    }
    private List<Map<String,Object>> rank(Snapshot s,LocalDateTime now) {
        Map<Long,Tag> tagMap=s.tags.stream().collect(Collectors.toMap(Tag::getId,t->t));
        Map<Long,Long> canonical=new HashMap<>();
        for(SkillTagAlias alias:s.aliases) canonical.put(alias.getAliasTagId(),alias.getCanonicalTagId());
        for(Long alias:canonical.keySet()) {
            Tag source=tagMap.get(alias);
            if(source==null || !"SKILL".equals(source.getTagType()) || !Integer.valueOf(1).equals(source.getStatus()))throw new BizException(ErrorCode.TAG_DATA_INVALID);
            canonicalSkill(alias,canonical,tagMap);
        }
        Map<Long,Integer> student=new LinkedHashMap<>();
        for(ResumeTag rt:s.resumeTags) { Tag t=tagMap.get(rt.getTagId());
            if(t!=null && "SKILL".equals(t.getTagType()) && Integer.valueOf(1).equals(t.getStatus()) && valid(rt.getProficiency())) student.merge(canonicalSkill(rt.getTagId(),canonical,tagMap),rt.getProficiency(),Math::max); }
        Map<Long,List<JobTag>> grouped=s.jobTags.stream().collect(Collectors.groupingBy(JobTag::getJobId));
        Map<Long,Set<Long>> applicants=new HashMap<>();
        for(JobApply a:s.applies) if(a.getCreatedAt()!=null && !a.getCreatedAt().isBefore(now.minusDays(30)) && a.getJobId()!=null && a.getUserId()!=null) applicants.computeIfAbsent(a.getJobId(),k->new HashSet<>()).add(a.getUserId());
        Map<Long,Set<Long>> favoriteUsers=new HashMap<>();for(UserJobAction a:s.favorites)favoriteUsers.computeIfAbsent(a.getJobId(),k->new HashSet<>()).add(a.getUserId());
        long max=s.jobs.stream().mapToLong(j->5L*applicants.getOrDefault(j.getId(),Collections.emptySet()).size()+3L*favoriteUsers.getOrDefault(j.getId(),Collections.emptySet()).size()).max().orElse(0);
        Map<Long,Double> cf=CollaborativeScorer.scores(s.userId,s.jobs.stream().map(Job::getId).collect(Collectors.toList()),s.vectors);
        Set<String> cityCodes=s.cities.stream().map(StudentCityPreference::getCityCode).collect(Collectors.toSet());
        List<Map<String,Object>> result=new ArrayList<>();
        for(Job j:s.jobs) {
            Map<Long,ContentScorer.Requirement> required=new LinkedHashMap<>(); Map<Long,String> names=new HashMap<>();
            for(JobTag jt:grouped.getOrDefault(j.getId(),Collections.emptyList())) {
                Tag t=tagMap.get(jt.getTagId());
                if(t==null) throw new BizException(ErrorCode.TAG_DATA_INVALID);
                if(!"SKILL".equals(t.getTagType())) {
                    if(jt.getProficiency()!=null) throw new BizException(ErrorCode.TAG_DATA_INVALID);
                    continue;
                }
                if(!Integer.valueOf(1).equals(t.getStatus()) || !valid(jt.getProficiency())) throw new BizException(ErrorCode.TAG_DATA_INVALID);
                int importance=t.getBaseWeight()==null ? 1 : Math.max(1,Math.min(3,t.getBaseWeight()));
                Long key=canonicalSkill(t.getId(),canonical,tagMap); ContentScorer.Requirement prior=required.get(key);
                required.put(key,new ContentScorer.Requirement(prior==null?jt.getProficiency():Math.max(prior.level,jt.getProficiency()),prior==null?importance:Math.max(prior.importance,importance))); names.put(key,tagMap.get(key).getTagName());
            }
            double days=j.getCreatedAt()==null?0:Math.max(0,Duration.between(j.getCreatedAt(),now).toMillis()/86400000.0);
            long h=5L*applicants.getOrDefault(j.getId(),Collections.emptySet()).size()+3L*favoriteUsers.getOrDefault(j.getId(),Collections.emptySet()).size();
            double pop=max==0?0:Math.log1p(h)/Math.log1p(max);double hot=0.8*pop+0.2*Math.exp(-days/30.0);
            MatchExplanation e=RecommendationCalculator.calculate(j.getId(),student,required,names,s.profile==null?null:s.profile.getExpectedCategoryId(),j.getCategoryId(),cityCodes,j.getCityCode(),s.profile==null?null:s.profile.getSalaryMin(),j.getSalaryMax(),j.getSalaryNegotiable(),hot,cf==null?null:cf.get(j.getId()));
            Map<String,Object> item=new LinkedHashMap<>(); item.put("id",j.getId());item.put("jobName",j.getJobName());item.put("companyName",j.getCompanyName());item.put("city",j.getCity());item.put("jobType",j.getJobType());item.put("salaryRange",j.getSalaryRange());item.put("description",j.getDescription());item.put("createdAt",j.getCreatedAt());
            item.put("match",e);item.put("_sortR",e.getRawRecommendation());item.put("_sortC",e.getRawContent());result.add(item);
        }
        result.sort((a,b)->{
            int c=Double.compare((Double)b.get("_sortR"),(Double)a.get("_sortR")); if(c!=0)return c;
            Double x=(Double)a.get("_sortC"),y=(Double)b.get("_sortC");
            if(x==null && y!=null)return 1;if(x!=null && y==null)return -1;if(x!=null && (c=Double.compare(y,x))!=0)return c;
            LocalDateTime ta=(LocalDateTime)a.get("createdAt"),tb=(LocalDateTime)b.get("createdAt");
            if(ta==null && tb!=null)return 1;if(ta!=null && tb==null)return -1;if(ta!=null && (c=tb.compareTo(ta))!=0)return c;
            return Long.compare((Long)a.get("id"),(Long)b.get("id"));
        });
        for(Map<String,Object> item:result){item.remove("_sortR");item.remove("_sortC");}
        return result;
    }
    private static boolean valid(Integer value){return value!=null && value>=1 && value<=3;}
    private Long canonicalSkill(Long id,Map<Long,Long> aliases,Map<Long,Tag> tags) {
        Set<Long> seen=new HashSet<>(); Long current=id;
        while(aliases.containsKey(current)) { if(!seen.add(current))throw new BizException(ErrorCode.TAG_DATA_INVALID); current=aliases.get(current); }
        Tag result=tags.get(current);
        if(result==null || !"SKILL".equals(result.getTagType()) || !Integer.valueOf(1).equals(result.getStatus()))throw new BizException(ErrorCode.TAG_DATA_INVALID);
        return current;
    }
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 3600000L, initialDelay = 3600000L)
    @Transactional
    public void removeOldBatches() {
        LocalDateTime now=LocalDateTime.now();
        batches.delete(new LambdaQueryWrapper<RecommendationBatch>().lt(RecommendationBatch::getCreatedAt,now.minusDays(7)).lt(RecommendationBatch::getExpiresAt,now));
    }
    private Map<String,Object> pageData(RecommendationBatch batch,List<Map<String,Object>> all,int page,int pageSize) {
        if(page<1 || pageSize<1 || pageSize>50)throw new BizException(ErrorCode.VALIDATION_FAILED);
        int offset=(int)Math.min((long)(page-1)*pageSize,all.size()); int end=Math.min(offset+pageSize,all.size());
        Map<String,Object> result=new LinkedHashMap<>();result.put("batchId",batch.getId());result.put("algorithmVersion",batch.getAlgorithmVersion());
        result.put("expiresAt",batch.getExpiresAt());result.put("totalEligible",batch.getTotalEligible());result.put("totalRanked",all.size());
        result.put("items",all.subList(offset,end));result.put("page",page);result.put("pageSize",pageSize);return result;
    }
    private void addMissing(Map<String,Object> result,Snapshot s) {
        List<String> missing=new ArrayList<>();
        if(s.profile==null || s.profile.getEducationRank()==null)missing.add("educationRank");
        if(s.profile==null || s.profile.getGraduationYear()==null)missing.add("graduationYear");
        if(s.profile==null || s.profile.getExpectedCategoryId()==null)missing.add("expectedCategoryId");
        if(s.profile==null || s.profile.getSalaryMin()==null || s.profile.getSalaryMin()<=0)missing.add("salaryMin");
        if(s.cities.isEmpty())missing.add("cityCodes");
        Set<Long> skills=s.tags.stream().filter(t->"SKILL".equals(t.getTagType()) && Integer.valueOf(1).equals(t.getStatus())).map(Tag::getId).collect(Collectors.toSet());
        if(s.resumeTags.stream().noneMatch(rt->skills.contains(rt.getTagId()) && valid(rt.getProficiency())))missing.add("skills");
        result.put("missingProfileFields",missing);
    }
    @Override @Transactional(isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> create(RecommendationRequest request) {
        Long user=StpUtil.getLoginIdAsLong(); LocalDateTime now=LocalDateTime.now();Snapshot s=snapshot(user,request,now);
        List<Map<String,Object>> result=rank(s,now); int total=s.jobs.size();if(result.size()>200)result=new ArrayList<>(result.subList(0,200));
        try {
            RecommendationBatch batch=RecommendationBatch.builder().id(UUID.randomUUID().toString()).userId(user).resumeId(s.resume==null?null:s.resume.getId())
                .createdAt(now).expiresAt(now.plusMinutes(10)).algorithmVersion(ContentScorer.VERSION).inputHash(s.hash).totalEligible(total).filtersJson(json.writeValueAsString(request)).resultJson(json.writeValueAsString(result)).build();
            batches.insert(batch);Map<String,Object> page=pageData(batch,result,1,10);addMissing(page,s);return page;
        } catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> page(String id,int page,int pageSize) {
        Long user=StpUtil.getLoginIdAsLong(); RecommendationBatch batch=batches.selectById(id);
        if(batch==null || !user.equals(batch.getUserId())) throw new BizException(ErrorCode.BATCH_NOT_FOUND);
        LocalDateTime now=LocalDateTime.now(); if(!batch.getExpiresAt().isAfter(now) || !ContentScorer.VERSION.equals(batch.getAlgorithmVersion()))throw new BizException(ErrorCode.BATCH_STALE);
        Snapshot s; try { RecommendationRequest request=json.readValue(batch.getFiltersJson(),RecommendationRequest.class); request.setResumeId(batch.getResumeId()); s=snapshot(user,request,now); }
        catch(BizException e){throw new BizException(ErrorCode.BATCH_STALE);} catch(java.io.IOException e){throw new IllegalStateException(e);}
        if(!s.hash.equals(batch.getInputHash()))throw new BizException(ErrorCode.BATCH_STALE);
        try{Map<String,Object> result=pageData(batch,json.readValue(batch.getResultJson(),new TypeReference<List<Map<String,Object>>>(){}),page,pageSize);addMissing(result,s);return result;}
        catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Object detail(Long jobId,Long resumeId) {
        Long user=StpUtil.getLoginIdAsLong(); Job job=jobs.selectById(jobId);if(!eligibility.publiclyVisible(job))throw new BizException(ErrorCode.JOB_NOT_FOUND);
        LocalDateTime now=LocalDateTime.now();RecommendationRequest request=new RecommendationRequest();request.setResumeId(resumeId);Snapshot s=snapshot(user,request,now);List<Map<String,Object>> items=rank(s,now);
        return items.stream().filter(x->jobId.equals(x.get("id"))).findFirst().map(x->x.get("match"))
            .orElseGet(()->{
                List<String> reasons=new ArrayList<>(),missing=new ArrayList<>();
                if(!Boolean.TRUE.equals(job.getAcceptFreshGraduate()))reasons.add("FRESH_GRADUATES_NOT_ACCEPTED");
                Integer education=s.profile==null?null:s.profile.getEducationRank(),year=s.profile==null?null:s.profile.getGraduationYear();
                if(job.getEducationMinRank()!=null) { if(education==null)missing.add("educationRank");else if(education<job.getEducationMinRank())reasons.add("EDUCATION_REQUIREMENT_NOT_MET"); }
                if(job.getGraduateYearMin()!=null || job.getGraduateYearMax()!=null) {
                    if(year==null)missing.add("graduationYear");else if((job.getGraduateYearMin()!=null && year<job.getGraduateYearMin()) || (job.getGraduateYearMax()!=null && year>job.getGraduateYearMax()))reasons.add("GRADUATION_YEAR_REQUIREMENT_NOT_MET");
                }
                if(applies.selectCount(new LambdaQueryWrapper<JobApply>().eq(JobApply::getUserId,user).eq(JobApply::getJobId,jobId))>0)reasons.add("ALREADY_APPLIED");
                if(actions.selectCount(new LambdaQueryWrapper<UserJobAction>().eq(UserJobAction::getUserId,user).eq(UserJobAction::getJobId,jobId).eq(UserJobAction::getActionType,"HIDE").eq(UserJobAction::getActive,true))>0)reasons.add("HIDDEN_BY_STUDENT");
                Map<String,Object> unavailable=new LinkedHashMap<>();unavailable.put("jobId",jobId);unavailable.put("eligible",false);unavailable.put("reasons",reasons);unavailable.put("missingProfileFields",missing);return unavailable;
            });
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public List<JobCardVO> legacy(RecommendationRequest request,int offset,int pageSize) {
        if(offset<0 || pageSize<1 || pageSize>50)throw new BizException(ErrorCode.VALIDATION_FAILED);
        LocalDateTime now=LocalDateTime.now();List<Map<String,Object>> ranked=rank(snapshot(StpUtil.getLoginIdAsLong(),request,now),now);
        int end=Math.min(200,ranked.size()), start=Math.min(offset,end);List<JobCardVO> result=new ArrayList<>();
        for(Map<String,Object> item:ranked.subList(start,Math.min(start+pageSize,end))) {
            JobCardVO card=new JobCardVO();card.setId((Long)item.get("id"));card.setJobName((String)item.get("jobName"));card.setCompanyName((String)item.get("companyName"));
            card.setCity((String)item.get("city"));card.setJobType((String)item.get("jobType"));card.setSalaryRange((String)item.get("salaryRange"));card.setDescription((String)item.get("description"));card.setCreatedAt((LocalDateTime)item.get("createdAt"));
            MatchExplanation explanation=(MatchExplanation)item.get("match"); if(explanation.getMatchScore()!=null)card.setMatchScore(java.math.BigDecimal.valueOf(explanation.getMatchScore())); result.add(card);
        }return result;
    }
    @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public java.math.BigDecimal applicantMatch(Long userId,Long resumeId,Long jobId) {
        RecommendationRequest request=new RecommendationRequest();request.setResumeId(resumeId);LocalDateTime now=LocalDateTime.now();
        return rank(snapshot(userId,request,now,true),now).stream().filter(x->jobId.equals(x.get("id")))
            .map(x->((MatchExplanation)x.get("match")).getMatchScore()).filter(Objects::nonNull).findFirst()
            .map(java.math.BigDecimal::valueOf).orElse(null);
    }
}
