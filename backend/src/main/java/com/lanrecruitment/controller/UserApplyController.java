package com.lanrecruitment.controller;

import com.lanrecruitment.common.Response;
import com.lanrecruitment.domain.dto.ApplyJobDTO;
import com.lanrecruitment.domain.dto.ApplyResumeUpdateDTO;
import com.lanrecruitment.domain.entity.JobApply;
import com.lanrecruitment.domain.vo.UserApplyVO;
import com.lanrecruitment.service.ApplyService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class UserApplyController {

    private final ApplyService applyService;
    private final com.lanrecruitment.service.impl.ApplicationWorkflowService workflow;

    public UserApplyController(ApplyService applyService, com.lanrecruitment.service.impl.ApplicationWorkflowService workflow) {
        this.applyService = applyService;
        this.workflow=workflow;
    }

    @PostMapping("/api/user/job/apply")
    public Response<Void> apply(@Valid @RequestBody ApplyJobDTO dto) {
        applyService.apply(dto);
        return Response.ok(null);
    }

    @GetMapping("/api/user/apply/list")
    public Response<List<JobApply>> listMy() {
        return Response.ok(applyService.listMy());
    }

    @GetMapping("/api/user/apply/list/detail")
    public Response<List<UserApplyVO>> listMyDetail() {
        return Response.ok(applyService.listMyDetail());
    }

    @PostMapping("/api/user/apply/resume/update")
    public Response<Void> updateApplyResume(@Valid @RequestBody ApplyResumeUpdateDTO dto) {
        applyService.updateApplyResume(dto);
        return Response.ok(null);
    }
    @PostMapping("/api/user/apply/status")
    public Response<Void> changeStatus(@Valid @RequestBody com.lanrecruitment.domain.dto.ApplyStatusUpdateDTO dto){workflow.transition(dto,false);return Response.ok(null);}
    @GetMapping("/api/user/apply/events")
    public Response<List<com.lanrecruitment.domain.entity.ApplicationEvent>> timeline(@org.springframework.web.bind.annotation.RequestParam("applyId") Long id){return Response.ok(workflow.timeline(id,false));}
    @GetMapping("/api/user/apply/attachment")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> attachment(@org.springframework.web.bind.annotation.RequestParam("applyId") Long id){return workflow.downloadOwnAttachment(id);}
}
