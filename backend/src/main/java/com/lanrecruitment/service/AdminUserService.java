package com.lanrecruitment.service;

import com.lanrecruitment.domain.dto.AdminUserStatusUpdateDTO;
import com.lanrecruitment.domain.dto.IdDTO;
import com.lanrecruitment.domain.vo.AdminUserVO;
import com.lanrecruitment.domain.vo.PageVO;

public interface AdminUserService {
    PageVO<AdminUserVO> listUserPage(String role, Integer status, Integer auditStatus, String keyword, int page, int pageSize);

    void updateStatus(AdminUserStatusUpdateDTO dto);

    void deleteUser(IdDTO dto);
}
