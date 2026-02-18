package com.lanrecruitment.domain.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeVO {
    private Long id;
    private Long userId;
    private String resumeName;
    private String realName;
    private String gender;
    private String city;
    private String education;
    private String workYears;
    private String expectJobType;
    private String expectSalary;
    private String workDesc;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private Integer isDefault;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
