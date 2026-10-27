package com.lanrecruitment.service.impl;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.domain.entity.*;
import com.lanrecruitment.exception.BizException;
import com.lanrecruitment.mapper.*;
import com.lanrecruitment.service.JobEligibilityService;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class UserJobActionService {
 private final UserJobActionMapper actions;private final SysUserMapper users;private final JobMapper jobs;private final BehaviorEventMapper events;private final JobEligibilityService eligibility;private final ApplicationWorkflowService workflow;
 public UserJobActionService(UserJobActionMapper actions,SysUserMapper users,JobMapper jobs,BehaviorEventMapper events,JobEligibilityService eligibility,ApplicationWorkflowService workflow){this.actions=actions;this.users=users;this.jobs=jobs;this.events=events;this.eligibility=eligibility;this.workflow=workflow;}
 @Transactional public void set(Long jobId,String type,boolean active){
  if(!"FAVORITE".equals(type) && !"HIDE".equals(type))throw new BizException(ErrorCode.VALIDATION_FAILED);
  Long user=StpUtil.getLoginIdAsLong(); users.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getId,user).last("FOR UPDATE"));
  if(active && !eligibility.publiclyVisible(jobs.selectById(jobId)))throw new BizException(ErrorCode.JOB_NOT_FOUND);
  UserJobAction a=actions.selectOne(new LambdaQueryWrapper<UserJobAction>().eq(UserJobAction::getUserId,user).eq(UserJobAction::getJobId,jobId).eq(UserJobAction::getActionType,type));
  if(a!=null && Boolean.valueOf(active).equals(a.getActive()))return;
  if(a==null){a=new UserJobAction();a.setUserId(user);a.setJobId(jobId);a.setActionType(type);a.setActive(active);a.setUpdatedAt(LocalDateTime.now().withNano(0));actions.insert(a);}
  else {a.setActive(active);a.setUpdatedAt(LocalDateTime.now().withNano(0));actions.updateById(a);}
  workflow.recordBehavior(user,jobId,active?type:"UN"+type,UUID.randomUUID().toString());
 }
 @Transactional public void view(Long jobId){Long user=StpUtil.getLoginIdAsLong();if(!eligibility.publiclyVisible(jobs.selectById(jobId)))throw new BizException(ErrorCode.JOB_NOT_FOUND);
  String key="view:"+user+":"+jobId+":"+LocalDate.now();
  if(events.selectCount(new LambdaQueryWrapper<BehaviorEvent>().eq(BehaviorEvent::getDedupeKey,key))==0)try{workflow.recordBehavior(user,jobId,"VIEW",key);}catch(org.springframework.dao.DuplicateKeyException ignored){}
 }
 public List<UserJobAction> list(){return actions.selectList(new LambdaQueryWrapper<UserJobAction>().eq(UserJobAction::getUserId,StpUtil.getLoginIdAsLong()));}
}
