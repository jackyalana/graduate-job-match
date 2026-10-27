package com.lanrecruitment.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanrecruitment.domain.entity.Job;
public interface JobEligibilityService {
    void visibleQuery(LambdaQueryWrapper<Job> query);
    void personalQuery(LambdaQueryWrapper<Job> query, Long userId);
    boolean publiclyVisible(Job job);
}
