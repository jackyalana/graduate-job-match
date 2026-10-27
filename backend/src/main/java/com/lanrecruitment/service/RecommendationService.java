package com.lanrecruitment.service;
import java.util.Map;
import java.util.List;
import com.lanrecruitment.domain.dto.RecommendationRequest;
import com.lanrecruitment.domain.vo.JobCardVO;
public interface RecommendationService {
    Map<String,Object> create(RecommendationRequest request);
    Map<String,Object> page(String id, int page, int pageSize);
    Object detail(Long jobId, Long resumeId);
    List<JobCardVO> legacy(RecommendationRequest request, int offset, int pageSize);
    java.math.BigDecimal applicantMatch(Long userId, Long resumeId, Long jobId);
}
