package com.lanrecruitment.service;
import java.util.List;
import com.lanrecruitment.domain.entity.JobCategory;
public interface JobCategoryService { List<JobCategory> list(); void create(String name); void requireExisting(Long id); }
