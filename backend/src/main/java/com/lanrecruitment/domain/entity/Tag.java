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

@TableName("tag")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tag {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tag_name")
    private String tagName;

    @TableField("tag_type")
    private String tagType;

    @TableField("base_weight")
    private Integer baseWeight;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
