package com.lanrecruitment.controller;
import com.lanrecruitment.common.Response;
import com.lanrecruitment.service.impl.StatisticsService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
@RestController public class StatisticsController {
 private final StatisticsService stats;public StatisticsController(StatisticsService stats){this.stats=stats;}
 @GetMapping("/api/admin/statistics") public Response<Map<String,Object>> report(){return Response.ok(stats.report());}
}
