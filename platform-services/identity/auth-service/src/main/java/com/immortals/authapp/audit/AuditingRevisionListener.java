package com.immortals.authapp.audit;


import com.immortals.platform.domain.entity.AuditingRevisionEntity;
import com.immortals.platform.domain.enums.UserTypes;
import com.immortals.platform.common.exception.AuthenticationException;
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
            throw new AuthenticationException(e.getMessage(), e);
        }

        auditingRevisionEntity.setUsername(currentUser);
    }
}
