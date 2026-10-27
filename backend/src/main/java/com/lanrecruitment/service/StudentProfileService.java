package com.lanrecruitment.service;
import com.lanrecruitment.domain.dto.StudentProfileDTO;
import com.lanrecruitment.domain.vo.StudentProfileVO;
public interface StudentProfileService {
    StudentProfileVO get();
    StudentProfileVO update(StudentProfileDTO dto);
}
