package com.lanrecruitment.controller;

import com.lanrecruitment.common.Response;
import com.lanrecruitment.domain.dto.AuditDecisionDTO;
import com.lanrecruitment.service.AdminAuditService;
import com.lanrecruitment.domain.vo.AdminHrAuditVO;
import com.lanrecruitment.domain.vo.AdminJobAuditVO;
import com.lanrecruitment.domain.vo.AdminJobDetailVO;
import java.util.List;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @GetMapping("/api/admin/audit/hr/list")
    public Response<List<AdminHrAuditVO>> listPendingHr() {
        return Response.ok(adminAuditService.listPendingHr());
    }

    @PostMapping("/api/admin/audit/hr/decision")
    public Response<Void> auditHr(@Valid @RequestBody AuditDecisionDTO dto) {
        adminAuditService.auditHr(dto);
        return Response.ok(null);
    }

    @GetMapping("/api/admin/audit/job/list")
    public Response<List<AdminJobAuditVO>> listPendingJob() {
        return Response.ok(adminAuditService.listPendingJob());
    }

    @PostMapping("/api/admin/audit/job/decision")
    public Response<Void> auditJob(@Valid @RequestBody AuditDecisionDTO dto) {
        adminAuditService.auditJob(dto);
        return Response.ok(null);
    }

    @GetMapping("/api/admin/audit/job/detail")
    public Response<AdminJobDetailVO> jobDetail(@RequestParam("id") Long id) {
        return Response.ok(adminAuditService.getJobDetail(id));
    }
}
