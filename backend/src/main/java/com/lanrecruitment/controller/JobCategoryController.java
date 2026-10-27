package com.lanrecruitment.controller;
import java.util.List;
import javax.validation.Valid;
import com.lanrecruitment.common.Response;
import com.lanrecruitment.domain.dto.JobCategoryDTO;
import com.lanrecruitment.domain.entity.JobCategory;
import com.lanrecruitment.service.JobCategoryService;
import org.springframework.web.bind.annotation.*;
@RestController public class JobCategoryController {
    private final JobCategoryService service;
    public JobCategoryController(JobCategoryService service) { this.service=service; }
    @GetMapping("/api/public/job/categories") public Response<List<JobCategory>> list() { return Response.ok(service.list()); }
    @PostMapping("/api/admin/job/categories") public Response<Void> create(@Valid @RequestBody JobCategoryDTO dto) { service.create(dto.getName()); return Response.ok(null); }
}
