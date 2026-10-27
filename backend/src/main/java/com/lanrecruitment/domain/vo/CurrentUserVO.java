package com.lanrecruitment.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserVO {
    private Long id;
    private String username;
    private String role;
    private Boolean emailVerified;
}
