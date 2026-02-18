package com.lanrecruitment.service;

import com.lanrecruitment.domain.dto.AuditDecisionDTO;
import java.util.List;
import com.lanrecruitment.domain.vo.AdminHrAuditVO;
import com.lanrecruitment.domain.vo.AdminJobAuditVO;
import com.lanrecruitment.domain.vo.AdminJobDetailVO;

public interface AdminAuditService {
    List<AdminHrAuditVO> listPendingHr();

    void auditHr(AuditDecisionDTO dto);

    List<AdminJobAuditVO> listPendingJob();

    void auditJob(AuditDecisionDTO dto);

    AdminJobDetailVO getJobDetail(Long id);
}
