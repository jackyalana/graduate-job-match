package com.lanrecruitment.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanrecruitment.domain.entity.*;
import com.lanrecruitment.mapper.*;
import com.lanrecruitment.service.JobEligibilityService;
import java.util.*;
import org.springframework.stereotype.Service;
@Service
public class StatisticsService {
 private final JobMapper jobs;private final JobApplyMapper applies;private final JobEligibilityService eligibility;
 public StatisticsService(JobMapper jobs,JobApplyMapper applies,JobEligibilityService eligibility){this.jobs=jobs;this.applies=applies;this.eligibility=eligibility;}
 public Map<String,Object> report(){LambdaQueryWrapper<Job> q=new LambdaQueryWrapper<>();eligibility.visibleQuery(q);List<Job> live=jobs.selectList(q);
  List<JobApply> applications=applies.selectList(new LambdaQueryWrapper<JobApply>().select(JobApply::getId,JobApply::getStatus));Map<Integer,Long> statuses=new TreeMap<>();for(JobApply a:applications)statuses.merge(a.getStatus(),1L,Long::sum);
  Map<Long,Long> categories=new TreeMap<>();for(Job j:live)if(j.getCategoryId()!=null)categories.merge(j.getCategoryId(),1L,Long::sum);
  Map<String,Object> result=new LinkedHashMap<>();result.put("openJobs",live.size());result.put("applications",applications.size());result.put("applicationStatusCounts",statuses);result.put("openJobsByCategory",categories);result.put("scope","all-time applications; currently eligible public jobs");return result;}
}
