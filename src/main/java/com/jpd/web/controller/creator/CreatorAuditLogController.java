package com.jpd.web.controller.creator;

import com.jpd.web.model.AuditLog;
import com.jpd.web.service.AuditLogService;
import com.jpd.web.service.utils.RequestAttributeExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/creator/auditlog")
public class CreatorAuditLogController {
    @Autowired
    private AuditLogService auditLogService;
    @GetMapping()
    public ResponseEntity<?> getAuditLog(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request){
        long creatorId= RequestAttributeExtractor.extractCreatorId(request);
        List <AuditLog> as=this.auditLogService.getLogsByCreator(creatorId);
        return ResponseEntity.ok(as);
    }
}
