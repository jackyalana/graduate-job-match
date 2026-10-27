package com.lanrecruitment.service.impl;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lanrecruitment.domain.dto.StudentProfileDTO;
import com.lanrecruitment.domain.entity.*;
import com.lanrecruitment.domain.vo.StudentProfileVO;
import com.lanrecruitment.mapper.*;
import com.lanrecruitment.service.StudentProfileService;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentProfileServiceImpl implements StudentProfileService {
    private final StudentProfileMapper profiles;
    private final StudentCityPreferenceMapper cities;
    private final SysUserMapper users;
    private final com.lanrecruitment.service.JobCategoryService categories;
    public StudentProfileServiceImpl(StudentProfileMapper profiles, StudentCityPreferenceMapper cities, SysUserMapper users, com.lanrecruitment.service.JobCategoryService categories) {
        this.profiles=profiles; this.cities=cities; this.users=users; this.categories=categories;
    }
    @Override public StudentProfileVO get() {
        Long userId=StpUtil.getLoginIdAsLong();
        SysUser user=users.selectById(userId);
        StudentProfile p=profiles.selectOne(new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId,userId));
        List<String> codes=cities.selectList(new LambdaQueryWrapper<StudentCityPreference>().eq(StudentCityPreference::getUserId,userId).orderByAsc(StudentCityPreference::getCityCode))
            .stream().map(StudentCityPreference::getCityCode).collect(Collectors.toList());
        return StudentProfileVO.builder().school(user.getSchool()).majorCode(p==null?null:p.getMajorCode())
            .educationRank(p==null?null:p.getEducationRank()).graduationYear(p==null?null:p.getGraduationYear())
            .expectedCategoryId(p==null?null:p.getExpectedCategoryId()).salaryMin(p==null?null:p.getSalaryMin())
            .profileVersion(p==null?0L:p.getProfileVersion()).cityCodes(codes).build();
    }
    @Transactional @Override public StudentProfileVO update(StudentProfileDTO dto) {
        Long userId=StpUtil.getLoginIdAsLong();
        categories.requireExisting(dto.getExpectedCategoryId());
        users.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getId,userId).last("FOR UPDATE"));
        users.update(null,new LambdaUpdateWrapper<SysUser>().eq(SysUser::getId,userId).set(SysUser::getSchool,dto.getSchool()));
        StudentProfile p=profiles.selectOne(new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId,userId));
        if (p==null) {
            p=new StudentProfile(); p.setUserId(userId); p.setProfileVersion(1L);
            p.setMajorCode(dto.getMajorCode()); p.setEducationRank(dto.getEducationRank()); p.setGraduationYear(dto.getGraduationYear());
            p.setExpectedCategoryId(dto.getExpectedCategoryId()); p.setSalaryMin(dto.getSalaryMin()); profiles.insert(p);
        } else {
            profiles.update(null,new LambdaUpdateWrapper<StudentProfile>().eq(StudentProfile::getUserId,userId)
                .set(StudentProfile::getMajorCode,dto.getMajorCode()).set(StudentProfile::getEducationRank,dto.getEducationRank())
                .set(StudentProfile::getGraduationYear,dto.getGraduationYear()).set(StudentProfile::getExpectedCategoryId,dto.getExpectedCategoryId())
                .set(StudentProfile::getSalaryMin,dto.getSalaryMin()).set(StudentProfile::getProfileVersion,p.getProfileVersion()+1));
        }
        cities.delete(new LambdaQueryWrapper<StudentCityPreference>().eq(StudentCityPreference::getUserId,userId));
        if (dto.getCityCodes()!=null) for (String code : dto.getCityCodes().stream().map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new))) {
            StudentCityPreference c=new StudentCityPreference(); c.setUserId(userId); c.setCityCode(code); cities.insert(c);
        }
        return get();
    }
}
