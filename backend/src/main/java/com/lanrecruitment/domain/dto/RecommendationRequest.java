package com.lanrecruitment.domain.dto;
import lombok.Data;
@Data public class RecommendationRequest {
    private Long resumeId;
    private String keyword, city, jobType, tagIds;
    private Long categoryId;
    private Integer salaryMin, salaryMax;
}
