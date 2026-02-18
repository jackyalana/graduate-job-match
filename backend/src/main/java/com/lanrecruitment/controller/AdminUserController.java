package com.lanrecruitment.controller;

import com.lanrecruitment.common.Response;
import com.lanrecruitment.domain.dto.AdminUserStatusUpdateDTO;
import com.lanrecruitment.domain.dto.IdDTO;
import com.lanrecruitment.domain.vo.AdminUserVO;
import com.lanrecruitment.domain.vo.PageVO;
import com.lanrecruitment.service.AdminUserService;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/api/admin/user/page")
    public Response<PageVO<AdminUserVO>> listPage(
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "auditStatus", required = false) Integer auditStatus,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return Response.ok(adminUserService.listUserPage(role, status, auditStatus, keyword, page, pageSize));
    }

    @PostMapping("/api/admin/user/status/update")
    public Response<Void> updateStatus(@Valid @RequestBody AdminUserStatusUpdateDTO dto) {
        adminUserService.updateStatus(dto);
        return Response.ok(null);
    }

    @PostMapping("/api/admin/user/delete")
    public Response<Void> delete(@Valid @RequestBody IdDTO dto) {
        adminUserService.deleteUser(dto);
        return Response.ok(null);
    }
}
