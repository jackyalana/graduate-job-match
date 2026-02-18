package com.lanrecruitment.service;

import com.lanrecruitment.domain.dto.ApplyStatusUpdateDTO;
import com.lanrecruitment.domain.dto.IdDTO;
import com.lanrecruitment.domain.dto.JobSaveDTO;
import com.lanrecruitment.domain.dto.JobTagSaveDTO;
import com.lanrecruitment.domain.vo.HrJobApplicationVO;
import com.lanrecruitment.domain.vo.HrJobVO;
import com.lanrecruitment.domain.vo.JobTagVO;
import java.util.List;

public interface HrJobService {
    List<HrJobVO> listMy();

    void save(JobSaveDTO dto);

    void delete(IdDTO dto);

    void remove(IdDTO dto);

    List<JobTagVO> listJobTags(Long jobId);

    List<HrJobApplicationVO> listApplications(Long jobId);

    void updateApplyStatus(ApplyStatusUpdateDTO dto);
}
