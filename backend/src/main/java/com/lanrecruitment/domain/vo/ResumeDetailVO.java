package com.lanrecruitment.domain.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDetailVO {
    private ResumeVO resume;
    private List<ResumeTagVO> tags;
}
