package com.immortals.otpservice.audit;


import com.immortals.authapp.model.entity.AuditingRevisionEntity;
import com.immortals.authapp.model.enums.UserTypes;
import com.immortals.authapp.service.exception.AuthException;
import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.context.SecurityContextHolder;


public class AuditingRevisionListener implements RevisionListener {
    @Override
    public void newRevision(Object revisionEntity) {
        AuditingRevisionEntity auditingRevisionEntity = (AuditingRevisionEntity) revisionEntity;

        String currentUser = UserTypes.SYSTEM.name();
        try {
            var auth = SecurityContextHolder.getContext()
                    .getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                currentUser = auth.getName();
            }
        } catch (Exception e) {
            throw new AuthException(e.getMessage(), e);
        }

        auditingRevisionEntity.setUsername(currentUser);
    }
}
