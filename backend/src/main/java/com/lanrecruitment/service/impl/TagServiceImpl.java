package com.lanrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lanrecruitment.domain.dto.IdDTO;
import com.lanrecruitment.domain.dto.TagSaveDTO;
import com.lanrecruitment.domain.entity.Tag;
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

    public TagServiceImpl(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    @Override
    public List<Tag> listPublic(String tagType) {
        LambdaQueryWrapper<Tag> qw = new LambdaQueryWrapper<Tag>().eq(Tag::getStatus, 1);
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

        LambdaQueryWrapper<Tag> qw = new LambdaQueryWrapper<Tag>().eq(Tag::getStatus, 1);
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
    public void save(TagSaveDTO dto) {
        if (dto.getId() == null) {
            Long cnt = tagMapper.selectCount(new LambdaQueryWrapper<Tag>()
                    .eq(Tag::getTagName, dto.getTagName())
                    .eq(Tag::getTagType, dto.getTagType()));
            if (cnt != null && cnt > 0) {
                throw new BizException(400, "标签已存在");
            }
            Tag t = new Tag();
            t.setTagName(dto.getTagName());
            t.setTagType(dto.getTagType());
            t.setBaseWeight(dto.getBaseWeight());
            t.setStatus(dto.getStatus());
            t.setCreatedAt(LocalDateTime.now());
            tagMapper.insert(t);
            return;
        }

        Tag exists = tagMapper.selectById(dto.getId());
        if (exists == null) {
            throw new BizException(404, "标签不存在");
        }
        exists.setTagName(dto.getTagName());
        exists.setTagType(dto.getTagType());
        exists.setBaseWeight(dto.getBaseWeight());
        exists.setStatus(dto.getStatus());
        tagMapper.updateById(exists);
    }

    @Override
    public void delete(IdDTO dto) {
        tagMapper.deleteById(dto.getId());
    }
}
