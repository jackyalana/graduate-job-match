package com.lanrecruitment.domain.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @TableName("audit_log")
public class AuditLog {
 @TableId(type=IdType.AUTO) private Long id;
 private Long actorId;private String resourceType;private Long resourceId;private String action;private String reason;private LocalDateTime createdAt;
}
