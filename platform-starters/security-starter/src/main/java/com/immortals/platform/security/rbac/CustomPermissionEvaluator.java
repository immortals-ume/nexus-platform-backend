package com.immortals.platform.security.rbac;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Custom permission evaluator for fine-grained authorization.
 * Evaluates permissions from JWT token claims.
 */
@Slf4j
@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || permission == null) {
            return false;
        }

        String permissionString = permission.toString();
        boolean hasPermission = hasPermission(authentication, permissionString);
        
        log.debug("Permission check for '{}': {}", permissionString, hasPermission);
        return hasPermission;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || permission == null) {
            return false;
        }

        String permissionString = permission.toString();
        boolean hasPermission = hasPermission(authentication, permissionString);
        
        log.debug("Permission check for '{}' on {} (ID: {}): {}", 
                permissionString, targetType, targetId, hasPermission);
        return hasPermission;
    }

    /**
     * Check if authentication has the specified permission.
     */
    private boolean hasPermission(Authentication authentication, String permission) {
        // Check if user has the permission in their authorities
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            if (authority.getAuthority().equals(permission)) {
                return true;
            }
        }

        // For JWT tokens, also check the permissions claim
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();
            
            @SuppressWarnings("unchecked")
            List<String> permissions = jwt.getClaim("permissions");
            
            if (permissions != null && permissions.contains(permission)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if authentication has any of the specified permissions.
     */
    public boolean hasAnyPermission(Authentication authentication, String... permissions) {
        for (String permission : permissions) {
            if (hasPermission(authentication, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if authentication has all of the specified permissions.
     */
    public boolean hasAllPermissions(Authentication authentication, String... permissions) {
        for (String permission : permissions) {
            if (!hasPermission(authentication, permission)) {
                return false;
            }
        }
        return true;
    }
}
