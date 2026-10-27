package com.lanrecruitment.domain.vo;
import java.util.List;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JobPageVO { private List<JobCardVO> items; private long total; private int page; private int pageSize; }
