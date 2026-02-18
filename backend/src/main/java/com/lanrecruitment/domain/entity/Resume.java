package com.lanrecruitment.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("resume")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resume {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("resume_name")
    private String resumeName;

    @TableField("real_name")
    private String realName;

    private String gender;

    private String city;

    private String education;

    @TableField("work_years")
    private String workYears;

    @TableField("expect_job_type")
    private String expectJobType;

    @TableField("expect_salary")
    private String expectSalary;

    @TableField("work_desc")
    private String workDesc;

    @TableField("file_name")
    private String fileName;

    @TableField("file_url")
    private String fileUrl;

    @TableField("file_type")
    private String fileType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("is_default")
    private Integer isDefault;

    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
