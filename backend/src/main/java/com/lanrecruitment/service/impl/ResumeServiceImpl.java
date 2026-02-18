package com.lanrecruitment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.common.enums.YesNo;
import com.lanrecruitment.common.enums.TagType;
import com.lanrecruitment.domain.dto.IdDTO;
import com.lanrecruitment.domain.dto.ResumeSaveDTO;
import com.lanrecruitment.domain.dto.ResumeTagItemDTO;
import com.lanrecruitment.domain.dto.ResumeTagSaveDTO;
import com.lanrecruitment.domain.entity.Resume;
import com.lanrecruitment.domain.entity.ResumeTag;
import com.lanrecruitment.domain.entity.Tag;
import com.lanrecruitment.exception.BizException;
import com.lanrecruitment.mapper.ResumeMapper;
import com.lanrecruitment.mapper.ResumeTagMapper;
import com.lanrecruitment.mapper.TagMapper;
import com.lanrecruitment.service.FileStorageService;
import com.lanrecruitment.service.ResumeService;
import com.lanrecruitment.domain.vo.ResumeDetailVO;
import com.lanrecruitment.domain.vo.ResumeTagVO;
import com.lanrecruitment.domain.vo.ResumeVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeServiceImpl implements ResumeService {

    private final ResumeMapper resumeMapper;
    private final ResumeTagMapper resumeTagMapper;
    private final TagMapper tagMapper;
    private final FileStorageService fileStorageService;

    public ResumeServiceImpl(
            ResumeMapper resumeMapper,
            ResumeTagMapper resumeTagMapper,
            TagMapper tagMapper,
            FileStorageService fileStorageService
    ) {
        this.resumeMapper = resumeMapper;
        this.resumeTagMapper = resumeTagMapper;
        this.tagMapper = tagMapper;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public List<ResumeVO> listMy() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<Resume> list = resumeMapper.selectList(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .orderByDesc(Resume::getUpdateTime));
        List<ResumeVO> res = new ArrayList<>();
        for (Resume r : list) {
            res.add(toVO(r));
        }
        return res;
    }

    @Override
    public ResumeDetailVO detail(IdDTO dto) {
        Resume r = getMyResume(dto.getId());
        List<ResumeTagVO> tags = buildTagVOList(r.getId());
        ResumeDetailVO vo = new ResumeDetailVO();
        vo.setResume(toVO(r));
        vo.setTags(tags);
        return vo;
    }

    @Override
    public void save(ResumeSaveDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();

        if (dto.getId() == null) {
            Resume r = new Resume();
            r.setUserId(userId);
            r.setResumeName(dto.getResumeName());
            r.setRealName(dto.getRealName());
            r.setGender(dto.getGender());
            r.setCity(dto.getCity());
            r.setEducation(dto.getEducation());
            r.setWorkYears(dto.getWorkYears());
            r.setExpectJobType(dto.getExpectJobType());
            r.setExpectSalary(dto.getExpectSalary());
            r.setWorkDesc(dto.getWorkDesc());
            r.setStatus(dto.getStatus());
            r.setIsDefault(YesNo.NO.getCode());
            r.setCreateTime(now);
            r.setUpdateTime(now);
            resumeMapper.insert(r);

            Long cnt = resumeMapper.selectCount(new LambdaQueryWrapper<Resume>().eq(Resume::getUserId, userId));
            if (cnt != null && cnt == 1) {
                setDefaultById(r.getId());
            }
            return;
        }

        Resume exists = getMyResume(dto.getId());
        exists.setResumeName(dto.getResumeName());
        exists.setRealName(dto.getRealName());
        exists.setGender(dto.getGender());
        exists.setCity(dto.getCity());
        exists.setEducation(dto.getEducation());
        exists.setWorkYears(dto.getWorkYears());
        exists.setExpectJobType(dto.getExpectJobType());
        exists.setExpectSalary(dto.getExpectSalary());
        exists.setWorkDesc(dto.getWorkDesc());
        exists.setStatus(dto.getStatus());
        exists.setUpdateTime(now);
        resumeMapper.updateById(exists);
    }

    @Override
    public void delete(IdDTO dto) {
        Resume r = getMyResume(dto.getId());
        resumeTagMapper.delete(new LambdaQueryWrapper<ResumeTag>().eq(ResumeTag::getResumeId, r.getId()));
        resumeMapper.deleteById(r.getId());
    }

    @Override
    public void setDefault(IdDTO dto) {
        setDefaultById(dto.getId());
    }

    @Override
    public void saveTags(ResumeTagSaveDTO dto) {
        Resume r = getMyResume(dto.getResumeId());
        resumeTagMapper.delete(new LambdaQueryWrapper<ResumeTag>().eq(ResumeTag::getResumeId, r.getId()));

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            return;
        }

        Set<Long> tagIds = new HashSet<>();
        for (ResumeTagItemDTO item : dto.getItems()) {
            tagIds.add(item.getTagId());
        }
        List<Tag> tags = tagMapper.selectBatchIds(tagIds);
        Map<Long, Tag> tagMap = new HashMap<>();
        for (Tag t : tags) {
            tagMap.put(t.getId(), t);
        }

        for (ResumeTagItemDTO item : dto.getItems()) {
            Tag t = tagMap.get(item.getTagId());
            if (t == null) {
                continue;
            }
            int base = t.getBaseWeight() == null ? 1 : t.getBaseWeight();
            int proficiency = item.getProficiency() == null ? 1 : item.getProficiency();
            int weight = base;
            if (TagType.isSkill(t.getTagType())) {
                weight = base * proficiency;
            }

            ResumeTag rt = new ResumeTag();
            rt.setResumeId(r.getId());
            rt.setTagId(item.getTagId());
            rt.setProficiency(TagType.isSkill(t.getTagType()) ? proficiency : null);
            rt.setWeight(weight);
            resumeTagMapper.insert(rt);
        }
    }

    @Override
    public ResumeVO uploadAttachment(Long resumeId, MultipartFile file) {
        Resume r = getMyResume(resumeId);
        String url = fileStorageService.storeResumeAttachment(file);
        r.setFileUrl(url);
        r.setFileName(file.getOriginalFilename());
        r.setFileSize(file.getSize());
        r.setFileType(ext(file.getOriginalFilename()));
        r.setUpdateTime(LocalDateTime.now());
        resumeMapper.updateById(r);
        return toVO(r);
    }

    private List<ResumeTagVO> buildTagVOList(Long resumeId) {
        List<ResumeTag> rts = resumeTagMapper.selectList(new LambdaQueryWrapper<ResumeTag>()
                .eq(ResumeTag::getResumeId, resumeId));
        Set<Long> tagIds = new HashSet<>();
        for (ResumeTag rt : rts) {
            tagIds.add(rt.getTagId());
        }
        List<Tag> tags = tagIds.isEmpty() ? new ArrayList<>() : tagMapper.selectBatchIds(tagIds);
        Map<Long, Tag> tagMap = new HashMap<>();
        for (Tag t : tags) {
            tagMap.put(t.getId(), t);
        }

        List<ResumeTagVO> res = new ArrayList<>();
        for (ResumeTag rt : rts) {
            Tag t = tagMap.get(rt.getTagId());
            ResumeTagVO vo = new ResumeTagVO();
            vo.setTagId(rt.getTagId());
            vo.setProficiency(rt.getProficiency());
            vo.setWeight(rt.getWeight());
            if (t != null) {
                vo.setTagName(t.getTagName());
                vo.setTagType(t.getTagType());
            }
            res.add(vo);
        }
        return res;
    }

    private Resume getMyResume(Long resumeId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Resume r = resumeMapper.selectById(resumeId);
        if (r == null || r.getUserId() == null || !r.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.RESUME_NOT_FOUND);
        }
        return r;
    }

    private ResumeVO toVO(Resume r) {
        ResumeVO vo = new ResumeVO();
        vo.setId(r.getId());
        vo.setUserId(r.getUserId());
        vo.setResumeName(r.getResumeName());
        vo.setRealName(r.getRealName());
        vo.setGender(r.getGender());
        vo.setCity(r.getCity());
        vo.setEducation(r.getEducation());
        vo.setWorkYears(r.getWorkYears());
        vo.setExpectJobType(r.getExpectJobType());
        vo.setExpectSalary(r.getExpectSalary());
        vo.setWorkDesc(r.getWorkDesc());
        vo.setFileName(r.getFileName());
        vo.setFileUrl(r.getFileUrl());
        vo.setFileType(r.getFileType());
        vo.setFileSize(r.getFileSize());
        vo.setIsDefault(r.getIsDefault());
        vo.setStatus(r.getStatus());
        vo.setCreateTime(r.getCreateTime());
        vo.setUpdateTime(r.getUpdateTime());
        return vo;
    }

    private void setDefaultById(Long resumeId) {
        Resume r = getMyResume(resumeId);
        Long userId = StpUtil.getLoginIdAsLong();
        resumeMapper.update(null, new LambdaUpdateWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .set(Resume::getIsDefault, YesNo.NO.getCode()));
        r.setIsDefault(YesNo.YES.getCode());
        r.setUpdateTime(LocalDateTime.now());
        resumeMapper.updateById(r);
    }

    private String ext(String filename) {
        if (filename == null) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }
}
