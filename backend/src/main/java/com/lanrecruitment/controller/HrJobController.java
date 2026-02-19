package com.lanrecruitment.controller;

import com.lanrecruitment.common.Response;
import com.lanrecruitment.domain.dto.ApplyStatusUpdateDTO;
import com.lanrecruitment.domain.dto.IdDTO;
import com.lanrecruitment.domain.dto.JobSaveDTO;
import com.lanrecruitment.domain.dto.JobTagSaveDTO;
import com.lanrecruitment.service.HrJobService;
import com.lanrecruitment.domain.vo.HrJobApplicationVO;
import com.lanrecruitment.domain.vo.HrJobVO;
import com.lanrecruitment.domain.vo.JobTagVO;
import java.util.List;
import javax.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class HrJobController {

    private final HrJobService hrJobService;

    public HrJobController(HrJobService hrJobService) {
        this.hrJobService = hrJobService;
    }

    @GetMapping("/api/hr/job/list")
    public Response<List<HrJobVO>> listMy() {
        return Response.ok(hrJobService.listMy());
    }

    @PostMapping("/api/hr/job/save")
    public Response<Void> save(@Valid @RequestBody JobSaveDTO dto) {
        hrJobService.save(dto);
        return Response.ok(null);
    }

    @PostMapping("/api/hr/job/delete")
    public Response<Void> delete(@Valid @RequestBody IdDTO dto) {
        hrJobService.delete(dto);
        return Response.ok(null);
    }

    @PostMapping("/api/hr/job/remove")
    public Response<Void> remove(@Valid @RequestBody IdDTO dto) {
        hrJobService.remove(dto);
        return Response.ok(null);
    }

    @GetMapping("/api/hr/job/tags")
    public Response<List<JobTagVO>> listJobTags(@RequestParam("jobId") Long jobId) {
        return Response.ok(hrJobService.listJobTags(jobId));
    }

    @GetMapping("/api/hr/job/applications")
    public Response<List<HrJobApplicationVO>> listApplications(@RequestParam("jobId") Long jobId) {
        return Response.ok(hrJobService.listApplications(jobId));
    }

    @PostMapping("/api/hr/job/apply/status")
    public Response<Void> updateApplyStatus(@Valid @RequestBody ApplyStatusUpdateDTO dto) {
        hrJobService.updateApplyStatus(dto);
        return Response.ok(null);
    }

    @GetMapping("/api/hr/resume/file/download")
    public ResponseEntity<Resource> downloadResumeAttachment(@RequestParam("applyId") Long applyId) {
        return hrJobService.downloadResumeAttachment(applyId);
    }
}
