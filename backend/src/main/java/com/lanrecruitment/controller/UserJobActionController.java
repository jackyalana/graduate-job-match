package com.lanrecruitment.controller;
import com.lanrecruitment.common.Response;
import com.lanrecruitment.domain.entity.UserJobAction;
import com.lanrecruitment.service.impl.UserJobActionService;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController
public class UserJobActionController {
 private final UserJobActionService service;public UserJobActionController(UserJobActionService service){this.service=service;}
 @PostMapping("/api/student/jobs/{id}/actions/{type}") public Response<Void> set(@PathVariable Long id,@PathVariable String type,@RequestParam boolean active){service.set(id,type,active);return Response.ok(null);}
 @PostMapping("/api/student/jobs/{id}/view") public Response<Void> view(@PathVariable Long id){service.view(id);return Response.ok(null);}
 @GetMapping("/api/student/job-actions") public Response<List<UserJobAction>> list(){return Response.ok(service.list());}
}
