package com.lanrecruitment.service;

import com.lanrecruitment.domain.dto.IdDTO;
import com.lanrecruitment.domain.dto.ResumeSaveDTO;
import com.lanrecruitment.domain.dto.ResumeTagSaveDTO;
import com.lanrecruitment.domain.vo.ResumeDetailVO;
import com.lanrecruitment.domain.vo.ResumeVO;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeService {
    List<ResumeVO> listMy();

    ResumeDetailVO detail(IdDTO dto);

    void save(ResumeSaveDTO dto);

    void delete(IdDTO dto);

    void setDefault(IdDTO dto);

    void saveTags(ResumeTagSaveDTO dto);

    ResumeVO uploadAttachment(Long resumeId, MultipartFile file);

    ResponseEntity<Resource> downloadMyAttachment(Long resumeId);

    ResumeVO deleteMyAttachment(Long resumeId);
}
