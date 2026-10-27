package com.lanrecruitment.controller;

import com.lanrecruitment.common.Response;
import com.lanrecruitment.domain.dto.IdDTO;
import com.lanrecruitment.domain.dto.ResumeSaveDTO;
import com.lanrecruitment.domain.dto.ResumeTagSaveDTO;
import com.lanrecruitment.service.ResumeService;
import com.lanrecruitment.domain.vo.ResumeDetailVO;
import com.lanrecruitment.domain.vo.ResumeVO;
import java.util.List;
import javax.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
public class UserResumeController {

    private final ResumeService resumeService;

    public UserResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @GetMapping("/api/user/resume/list")
    public Response<List<ResumeVO>> listMy() {
        return Response.ok(resumeService.listMy());
    }

    @GetMapping("/api/user/resume/detail")
    public Response<ResumeDetailVO> detail(@Valid IdDTO dto) {
        return Response.ok(resumeService.detail(dto));
    }

    @PostMapping("/api/user/resume/save")
    public Response<Void> save(@Valid @RequestBody ResumeSaveDTO dto) {
        resumeService.save(dto);
        return Response.ok(null);
    }

    @PostMapping("/api/user/resume/delete")
    public Response<Void> delete(@Valid @RequestBody IdDTO dto) {
        resumeService.delete(dto);
        return Response.ok(null);
    }

    @PostMapping("/api/user/resume/set-default")
    public Response<Void> setDefault(@Valid @RequestBody IdDTO dto) {
        resumeService.setDefault(dto);
        return Response.ok(null);
    }

    @PostMapping("/api/user/resume/tags/save")
    public Response<Void> saveTags(@Valid @RequestBody ResumeTagSaveDTO dto) {
        resumeService.saveTags(dto);
        return Response.ok(null);
    }

    @PostMapping("/api/user/resume/file/upload")
    public Response<ResumeVO> uploadAttachment(
            @RequestParam("resumeId") Long resumeId,
            @RequestParam("file") MultipartFile file
    ) {
        return Response.ok(resumeService.uploadAttachment(resumeId, file));
    }

    @GetMapping("/api/user/resume/file/download")
    public ResponseEntity<Resource> downloadAttachment(@RequestParam("resumeId") Long resumeId) {
        return resumeService.downloadMyAttachment(resumeId);
    }

    @PostMapping("/api/student/resumes/{id}/attachment")
    public Response<ResumeVO> uploadAttachmentV1(@org.springframework.web.bind.annotation.PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file) { return Response.ok(resumeService.uploadAttachment(id,file)); }

    @GetMapping("/api/student/resumes/{id}/attachment")
    public ResponseEntity<Resource> downloadAttachmentV1(@org.springframework.web.bind.annotation.PathVariable("id") Long id) {
        return resumeService.downloadMyAttachment(id);
    }

    @PostMapping("/api/user/resume/file/delete")
    public Response<ResumeVO> deleteAttachment(@RequestParam("resumeId") Long resumeId) {
        return Response.ok(resumeService.deleteMyAttachment(resumeId));
    }
}
