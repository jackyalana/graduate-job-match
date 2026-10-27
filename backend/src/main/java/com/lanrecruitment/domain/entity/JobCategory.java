package com.lanrecruitment.domain.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@TableName("job_category") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JobCategory { @TableId(type=IdType.AUTO) private Long id; private String name; }
