package com.lanrecruitment.domain.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder @TableName("skill_tag_alias")
public class SkillTagAlias {
    @TableId(type=IdType.INPUT) private Long aliasTagId;
    private Long canonicalTagId;
}
