package com.lanrecruitment.domain.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
@TableName("student_city_preference")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentCityPreference {
    @TableId(type=IdType.AUTO) private Long id;
    private Long userId;
    private String cityCode;
}
