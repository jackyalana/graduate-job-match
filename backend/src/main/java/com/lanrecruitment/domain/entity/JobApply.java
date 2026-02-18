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

@TableName("job_apply")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApply {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("job_id")
    private Long jobId;

    @TableField("resume_id")
    private Long resumeId;

    @TableField("user_id")
    private Long userId;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
