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

@TableName("notice")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notice {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
