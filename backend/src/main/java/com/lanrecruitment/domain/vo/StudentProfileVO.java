package com.lanrecruitment.domain.vo;
import java.util.List;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentProfileVO {
    private String school;
    private String majorCode;
    private Integer educationRank;
    private Integer graduationYear;
    private Long expectedCategoryId;
    private Integer salaryMin;
    private Long profileVersion;
    private List<String> cityCodes;
}
