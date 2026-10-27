package com.lanrecruitment.domain.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @TableName("behavior_event")
public class BehaviorEvent {
    @TableId(type=IdType.AUTO) private Long id;
    private Long userId; private Long jobId; private String eventType; private LocalDateTime eventTime; private String dedupeKey;
}
