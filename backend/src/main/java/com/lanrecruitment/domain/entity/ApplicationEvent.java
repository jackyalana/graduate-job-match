package com.lanrecruitment.domain.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @TableName("application_event")
public class ApplicationEvent {
    @TableId(type=IdType.AUTO) private Long id;
    private Long applicationId; private Integer fromStatus; private Integer toStatus;
    private Long actorId; private String reason; private LocalDateTime createdAt;
}
