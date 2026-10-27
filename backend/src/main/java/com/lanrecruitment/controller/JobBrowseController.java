package com.lanrecruitment.controller;

import com.lanrecruitment.common.Response;
import com.lanrecruitment.service.JobBrowseService;
import com.lanrecruitment.domain.vo.JobCardVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobBrowseController {

    private final JobBrowseService jobBrowseService;
    private final com.lanrecruitment.service.StudentProfileService profiles;

    public JobBrowseController(JobBrowseService jobBrowseService, com.lanrecruitment.service.StudentProfileService profiles) {
        this.jobBrowseService = jobBrowseService; this.profiles=profiles;
    }

    @GetMapping("/api/public/job/list")
    public Response<List<JobCardVO>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "jobType", required = false) String jobType,
            @RequestParam(value = "tagIds", required = false) String tagIds,
            @RequestParam(value = "cursorId", required = false) Long cursorId,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "salaryMin", required = false) Integer salaryMin,
            @RequestParam(value = "salaryMax", required = false) Integer salaryMax
    ) {
        return Response.ok(jobBrowseService.listPublic(keyword, city, jobType, tagIds, cursorId, pageSize, categoryId, salaryMin, salaryMax));
    }

    @GetMapping("/api/jobs")
    public Response<com.lanrecruitment.domain.vo.JobPageVO> searchPublic(
            @RequestParam(value="keyword",required=false) String keyword,
            @RequestParam(value="city",required=false) String city,
            @RequestParam(value="jobType",required=false) String jobType,
            @RequestParam(value="tagIds",required=false) String tagIds,
            @RequestParam(value="categoryId",required=false) Long categoryId,
            @RequestParam(value="salaryMin",required=false) Integer salaryMin,
            @RequestParam(value="salaryMax",required=false) Integer salaryMax,
            @RequestParam(value="page",defaultValue="1") int page,
            @RequestParam(value="pageSize",defaultValue="10") int pageSize) {
        return Response.ok(jobBrowseService.searchPublic(keyword,city,jobType,tagIds,categoryId,salaryMin,salaryMax,page,pageSize));
    }
    @GetMapping("/api/jobs/{id}")
    public Response<JobCardVO> detailV1(@org.springframework.web.bind.annotation.PathVariable("id") Long id) {
        return Response.ok(jobBrowseService.detail(id));
    }

    @GetMapping("/api/public/job/detail")
    public Response<JobCardVO> detail(@RequestParam("id") Long id) {
        return Response.ok(jobBrowseService.detail(id));
    }

    @GetMapping("/api/user/job/recommend")
    public Response<List<JobCardVO>> recommend(
            @RequestParam(value = "resumeId", required = false) Long resumeId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "jobType", required = false) String jobType,
            @RequestParam(value = "tagIds", required = false) String tagIds,
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "salaryMin", required = false) Integer salaryMin,
            @RequestParam(value = "salaryMax", required = false) Integer salaryMax
    ) {
        com.lanrecruitment.domain.vo.StudentProfileVO profile=profiles.get();
        java.util.List<String> missing=new java.util.ArrayList<>();
        if (profile.getEducationRank()==null) missing.add("educationRank");
        if (profile.getGraduationYear()==null) missing.add("graduationYear");
        return Response.ok(jobBrowseService.recommend(resumeId, keyword, city, jobType, tagIds, offset, pageSize, categoryId, salaryMin, salaryMax))
            .withDetails(java.util.Collections.singletonMap("missingProfileFields",missing));
    }
}
