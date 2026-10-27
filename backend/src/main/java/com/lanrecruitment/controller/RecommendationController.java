package com.lanrecruitment.controller;
import com.lanrecruitment.common.Response;
import com.lanrecruitment.service.RecommendationService;
import java.util.Map;
import com.lanrecruitment.domain.dto.RecommendationRequest;
import org.springframework.web.bind.annotation.*;
@RestController
public class RecommendationController {
    private final RecommendationService recommendations;
    public RecommendationController(RecommendationService recommendations){this.recommendations=recommendations;}
    @PostMapping("/api/student/recommendation-batches")
    public Response<Map<String,Object>> create(@RequestBody(required=false) RecommendationRequest request){return Response.ok(recommendations.create(request==null?new RecommendationRequest():request));}
    @GetMapping("/api/student/recommendation-batches/{id}")
    public Response<Map<String,Object>> page(@PathVariable String id,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int pageSize){return Response.ok(recommendations.page(id,page,pageSize));}
    @GetMapping("/api/student/matches/{jobId}")
    public Response<Object> detail(@PathVariable Long jobId,@RequestParam(required=false) Long resumeId){return Response.ok(recommendations.detail(jobId,resumeId));}
}
