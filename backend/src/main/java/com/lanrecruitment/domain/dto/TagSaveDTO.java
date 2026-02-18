package com.lanrecruitment.domain.dto;

import com.lanrecruitment.common.enums.TagType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagSaveDTO {
    private Long id;

    @NotBlank(message = "标签名称不能为空")
    private String tagName;

    @NotBlank(message = "标签类型不能为空")
    @Pattern(regexp = TagType.REGEX, message = "标签类型不合法")
    private String tagType;

    @NotNull(message = "基础权重不能为空")
    @Min(value = 1, message = "基础权重不能小于1")
    private Integer baseWeight;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
