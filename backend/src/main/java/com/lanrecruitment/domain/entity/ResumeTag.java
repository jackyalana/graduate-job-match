package com.lanrecruitment.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("resume_tag")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeTag {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("resume_id")
    private Long resumeId;

    @TableField("tag_id")
    private Long tagId;

    private Integer proficiency;

    private Integer weight;
}
