package com.lanrecruitment.domain.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@TableName("student_profile")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentProfile {
    @TableId(type=IdType.AUTO) private Long id;
    private Long userId;
    private String majorCode;
    private Integer educationRank;
    private Integer graduationYear;
    private Long expectedCategoryId;
    private Integer salaryMin;
    private Long profileVersion;
}
