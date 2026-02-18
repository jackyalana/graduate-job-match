package com.lanrecruitment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanrecruitment.common.enums.CommonStatus;
import com.lanrecruitment.common.enums.ErrorCode;
import com.lanrecruitment.domain.dto.IdDTO;
import com.lanrecruitment.domain.dto.NoticeSaveDTO;
import com.lanrecruitment.domain.entity.Notice;
import com.lanrecruitment.exception.BizException;
import com.lanrecruitment.mapper.NoticeMapper;
import com.lanrecruitment.service.NoticeService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NoticeServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;

    public NoticeServiceImpl(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    @Override
    public List<Notice> listPublic() {
        return noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, CommonStatus.ENABLED.getCode())
                .orderByDesc(Notice::getId));
    }

    @Override
    public List<Notice> listAdmin(Integer status) {
        LambdaQueryWrapper<Notice> qw = new LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(Notice::getStatus, status);
        }
        return noticeMapper.selectList(qw.orderByDesc(Notice::getId));
    }

    @Override
    public void save(NoticeSaveDTO dto) {
        if (dto.getId() == null) {
            Notice n = new Notice();
            n.setTitle(dto.getTitle());
            n.setContent(dto.getContent());
            n.setStatus(dto.getStatus());
            n.setCreatedAt(LocalDateTime.now());
            noticeMapper.insert(n);
            return;
        }

        Notice exists = noticeMapper.selectById(dto.getId());
        if (exists == null) {
            throw new BizException(ErrorCode.NOTICE_NOT_FOUND);
        }
        exists.setTitle(dto.getTitle());
        exists.setContent(dto.getContent());
        exists.setStatus(dto.getStatus());
        noticeMapper.updateById(exists);
    }

    @Override
    public void delete(IdDTO dto) {
        noticeMapper.deleteById(dto.getId());
    }
}
