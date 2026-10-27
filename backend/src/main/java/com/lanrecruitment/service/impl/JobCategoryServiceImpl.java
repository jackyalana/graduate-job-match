package com.lanrecruitment.service.impl;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.exception.BizException;
import com.lanrecruitment.domain.entity.JobCategory;
import com.lanrecruitment.mapper.JobCategoryMapper;
import com.lanrecruitment.service.JobCategoryService;
import org.springframework.stereotype.Service;
@Service public class JobCategoryServiceImpl implements JobCategoryService {
    private final JobCategoryMapper mapper;
    public JobCategoryServiceImpl(JobCategoryMapper mapper) { this.mapper=mapper; }
    public List<JobCategory> list() { return mapper.selectList(new LambdaQueryWrapper<JobCategory>().orderByAsc(JobCategory::getId)); }
    public void create(String name) { JobCategory category=new JobCategory(); category.setName(name.trim()); mapper.insert(category); }
    public void requireExisting(Long id) { if (id!=null && mapper.selectById(id)==null) throw new BizException(ErrorCode.VALIDATION_FAILED,"岗位类别不存在"); }
}
