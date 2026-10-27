package com.lanrecruitment.service.impl;
import cn.dev33.satoken.stp.StpUtil;
import com.lanrecruitment.domain.entity.AuditLog;
import com.lanrecruitment.mapper.AuditLogMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
@Service public class AuditLogService {
 private final AuditLogMapper mapper;public AuditLogService(AuditLogMapper mapper){this.mapper=mapper;}
 public void record(String type,Long id,String action,String reason){AuditLog log=new AuditLog();log.setActorId(StpUtil.getLoginIdAsLong());log.setResourceType(type);log.setResourceId(id);log.setAction(action);log.setReason(reason);log.setCreatedAt(LocalDateTime.now().withNano(0));mapper.insert(log);}
}
