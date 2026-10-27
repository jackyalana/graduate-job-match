package com.lanrecruitment.domain.dto;
import javax.validation.constraints.*;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JobCategoryDTO { @NotBlank @Size(max=100) private String name; }
