package com.lanrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lanrecruitment.common.enums.CommonStatus;
import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.domain.dto.IdDTO;
import com.lanrecruitment.domain.dto.TagSaveDTO;
import com.lanrecruitment.domain.entity.Tag;
import com.lanrecruitment.domain.entity.SkillTagAlias;
import com.lanrecruitment.domain.entity.JobTag;
import com.lanrecruitment.domain.entity.ResumeTag;
import com.lanrecruitment.domain.vo.PageVO;
import com.lanrecruitment.exception.BizException;
import com.lanrecruitment.mapper.TagMapper;
import com.lanrecruitment.service.TagService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final com.lanrecruitment.mapper.SkillTagAliasMapper aliases;
    private final com.lanrecruitment.mapper.JobTagMapper jobTags;
    private final com.lanrecruitment.mapper.ResumeTagMapper resumeTags;
    private final AuditLogService auditLog;

    public TagServiceImpl(TagMapper tagMapper, com.lanrecruitment.mapper.SkillTagAliasMapper aliases,
            com.lanrecruitment.mapper.JobTagMapper jobTags, com.lanrecruitment.mapper.ResumeTagMapper resumeTags, AuditLogService auditLog) {
        this.tagMapper = tagMapper;
        this.aliases=aliases; this.jobTags=jobTags; this.resumeTags=resumeTags;
        this.auditLog=auditLog;
    }

    @Override
    public List<Tag> listPublic(String tagType) {
        LambdaQueryWrapper<Tag> qw = new LambdaQueryWrapper<Tag>().eq(Tag::getStatus, CommonStatus.ENABLED.getCode());
        if (tagType != null && !tagType.trim().isEmpty()) {
            qw.eq(Tag::getTagType, tagType);
        }
        return tagMapper.selectList(qw.orderByDesc(Tag::getId));
    }

    @Override
    public List<Tag> searchPublic(String tagType, String keyword, Integer limit) {
        int safeLimit = limit == null ? 20 : limit;
        if (safeLimit <= 0) {
            safeLimit = 20;
        }
        if (safeLimit > 50) {
            safeLimit = 50;
        }

        LambdaQueryWrapper<Tag> qw = new LambdaQueryWrapper<Tag>().eq(Tag::getStatus, CommonStatus.ENABLED.getCode());
        if (tagType != null && !tagType.trim().isEmpty()) {
            qw.eq(Tag::getTagType, tagType);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            qw.like(Tag::getTagName, keyword.trim());
        }
        return tagMapper.selectList(qw.orderByDesc(Tag::getId).last("limit " + safeLimit));
    }

    @Override
    public List<Tag> listAdmin(String tagType, Integer status) {
        LambdaQueryWrapper<Tag> qw = new LambdaQueryWrapper<>();
        if (tagType != null && !tagType.trim().isEmpty()) {
            qw.eq(Tag::getTagType, tagType);
        }
        if (status != null) {
            qw.eq(Tag::getStatus, status);
        }
        return tagMapper.selectList(qw.orderByDesc(Tag::getId));
    }

    @Override
    public PageVO<Tag> listAdminPage(String tagType, Integer status, String keyword, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 20;
        
        LambdaQueryWrapper<Tag> qw = new LambdaQueryWrapper<>();
        if (tagType != null && !tagType.trim().isEmpty()) {
            qw.eq(Tag::getTagType, tagType);
        }
        if (status != null) {
            qw.eq(Tag::getStatus, status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            qw.like(Tag::getTagName, keyword.trim());
        }
        qw.orderByDesc(Tag::getId);
        
        Page<Tag> p = new Page<>(page, pageSize);
        Page<Tag> result = tagMapper.selectPage(p, qw);
        
        return new PageVO<>(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void save(TagSaveDTO dto) {
        if (dto.getId() == null) {
            Long cnt = tagMapper.selectCount(new LambdaQueryWrapper<Tag>()
                    .eq(Tag::getTagName, dto.getTagName())
                    .eq(Tag::getTagType, dto.getTagType()));
            if (cnt != null && cnt > 0) {
                throw new BizException(ErrorCode.TAG_ALREADY_EXISTS);
            }
            Tag t = new Tag();
            t.setTagName(dto.getTagName());
            t.setTagType(dto.getTagType());
            t.setBaseWeight(dto.getBaseWeight());
            t.setStatus(dto.getStatus());
            t.setCreatedAt(LocalDateTime.now());
            tagMapper.insert(t);
            auditLog.record("TAG",t.getId(),"CREATE",null);
            return;
        }

        Tag exists = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getId,dto.getId()).last("FOR UPDATE"));
        if (exists == null) {
            throw new BizException(ErrorCode.TAG_NOT_FOUND);
        }
        if (!java.util.Objects.equals(exists.getTagType(),dto.getTagType()) || !Integer.valueOf(1).equals(dto.getStatus())) {
            assertNotReferenced(dto.getId());
        }
        exists.setTagName(dto.getTagName());
        exists.setTagType(dto.getTagType());
        exists.setBaseWeight(dto.getBaseWeight());
        exists.setStatus(dto.getStatus());
        tagMapper.updateById(exists);
        auditLog.record("TAG",exists.getId(),"UPDATE",null);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void delete(IdDTO dto) {
        Tag exists=tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getId,dto.getId()).last("FOR UPDATE"));
        if (exists==null) throw new BizException(ErrorCode.TAG_NOT_FOUND);
        assertNotReferenced(dto.getId());
        tagMapper.deleteById(dto.getId());
        auditLog.record("TAG",dto.getId(),"DELETE",null);
    }

    private void assertNotReferenced(Long id) {
        if (aliases.selectCount(new LambdaQueryWrapper<SkillTagAlias>().eq(SkillTagAlias::getAliasTagId,id).or().eq(SkillTagAlias::getCanonicalTagId,id))>0
                || jobTags.selectCount(new LambdaQueryWrapper<JobTag>().eq(JobTag::getTagId,id))>0
                || resumeTags.selectCount(new LambdaQueryWrapper<ResumeTag>().eq(ResumeTag::getTagId,id))>0)
            throw new BizException(ErrorCode.TAG_IN_USE);
    }
}
