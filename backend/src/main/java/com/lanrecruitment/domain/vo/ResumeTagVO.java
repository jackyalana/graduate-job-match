package com.lanrecruitment.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeTagVO {
    private Long tagId;
    private String tagName;
    private String tagType;
    private Integer proficiency;
    private Integer weight;
}
