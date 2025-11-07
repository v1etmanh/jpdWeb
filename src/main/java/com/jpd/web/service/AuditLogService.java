package com.jpd.web.service;

import com.jpd.web.model.AuditLog;
import com.jpd.web.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;



    public List<AuditLog> getLogsByCreator(Long creatorId) {
        return auditLogRepository.findByTargetCreatorIdOrderByTimestampDesc(creatorId);
    }



    @Transactional
    public void logAction(String actionType, Long targetCreatorId, String adminEmail, String reason) {
        if (actionType == null || actionType.isBlank()) {
            throw new IllegalArgumentException("Action cannot be null");
        }
        if (targetCreatorId == null) {
            throw new IllegalArgumentException("Entity ID cannot be null");
        }
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalArgumentException("Performed by cannot be empty");
        }

        AuditLog auditLog = AuditLog.builder()
                .actionType(actionType)
                .targetCreatorId(targetCreatorId)
                .adminEmail(adminEmail)
                .reason(reason)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} for creator {} by admin {}", actionType, targetCreatorId, adminEmail);
    }

    public List<AuditLog> getLogsByAdmin(String adminEmail) {
        return auditLogRepository.findByAdminEmailOrderByTimestampDesc(adminEmail);
    }
}

