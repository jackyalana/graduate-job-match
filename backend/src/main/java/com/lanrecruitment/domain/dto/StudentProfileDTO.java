package com.lanrecruitment.domain.dto;
import java.util.List;
import javax.validation.constraints.*;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentProfileDTO {
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unsupported field: " + field); }
    @Size(max=100) private String school;
    @Size(max=64) private String majorCode;
    @Min(1) @Max(4) private Integer educationRank;
    @Min(1900) @Max(2100) private Integer graduationYear;
    @Positive private Long expectedCategoryId;
    @Min(0) private Integer salaryMin;
    @Size(max=50) private List<@NotBlank @Size(max=64) String> cityCodes;
}
