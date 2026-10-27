package com.lanrecruitment.domain.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder @TableName("recommendation_batch")
public class RecommendationBatch {
    @TableId(type=IdType.INPUT) private String id;
    private Long userId;
    private Long resumeId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String algorithmVersion;
    private String inputHash;
    private Integer totalEligible;
    private String filtersJson;
    private String resultJson;
}
