package com.golagrocery.audit;

import com.golagrocery.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Writes security- and business-relevant actions to the audit_logs table. Never log secrets or OTPs. */
@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, Object entityId, String details) {
        AuditLog l = new AuditLog();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser u) {
            l.setActorId(u.id());
            l.setActorRole(String.join(",", u.roles()));
        }
        l.setAction(action);
        l.setEntityType(entityType);
        l.setEntityId(entityId == null ? null : String.valueOf(entityId));
        l.setDetails(details);
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a) {
            HttpServletRequest r = a.getRequest();
            l.setIpAddress(r.getRemoteAddr());
        }
        repo.save(l);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAs(Long actorId, String role, String action, String entityType, Object entityId, String details) {
        AuditLog l = new AuditLog();
        l.setActorId(actorId);
        l.setActorRole(role);
        l.setAction(action);
        l.setEntityType(entityType);
        l.setEntityId(entityId == null ? null : String.valueOf(entityId));
        l.setDetails(details);
        repo.save(l);
    }
}
