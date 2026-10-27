package com.lanrecruitment.controller;
import com.lanrecruitment.common.Response;
import com.lanrecruitment.domain.dto.StudentProfileDTO;
import com.lanrecruitment.domain.vo.StudentProfileVO;
import com.lanrecruitment.service.StudentProfileService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController
public class StudentProfileController {
    private final StudentProfileService service;
    public StudentProfileController(StudentProfileService service) { this.service=service; }
    @GetMapping("/api/student/profile") public Response<StudentProfileVO> get() { return Response.ok(service.get()); }
    @PutMapping("/api/student/profile") public Response<StudentProfileVO> update(@Valid @RequestBody StudentProfileDTO dto) { return Response.ok(service.update(dto)); }
}
