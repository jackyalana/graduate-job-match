package com.lanrecruitment.domain.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @TableName("user_job_action")
public class UserJobAction {
    @TableId(type=IdType.AUTO) private Long id;
    private Long userId; private Long jobId; private String actionType; private Boolean active; private LocalDateTime updatedAt;
}
