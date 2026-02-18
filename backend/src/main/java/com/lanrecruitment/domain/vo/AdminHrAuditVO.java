package com.lanrecruitment.domain.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminHrAuditVO {
    private Long id;
    private String username;
    private String email;
    private Integer status;
    private Integer auditStatus;
    private LocalDateTime createdAt;
}
