package com.immortals.platform.security.rbac;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

/**
 * Configuration for role hierarchy.
 * Defines role inheritance (e.g., ADMIN inherits USER permissions).
 */
@Slf4j
@Configuration
public class RoleHierarchyConfig {

    @Bean
    public RoleHierarchy roleHierarchy() {
        log.info("Configuring role hierarchy");
        
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        
        // Define role hierarchy
        // Format: "ROLE_HIGHER > ROLE_LOWER"
        String hierarchy = """
                ROLE_SUPER_ADMIN > ROLE_ADMIN
                ROLE_ADMIN > ROLE_MANAGER
                ROLE_MANAGER > ROLE_USER
                ROLE_USER > ROLE_GUEST
                """;
        
        roleHierarchy.setHierarchy(hierarchy);
        
        log.info("Role hierarchy configured: SUPER_ADMIN > ADMIN > MANAGER > USER > GUEST");
        return roleHierarchy;
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            RoleHierarchy roleHierarchy,
            CustomPermissionEvaluator permissionEvaluator) {
        
        log.info("Configuring method security expression handler");
        
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        expressionHandler.setPermissionEvaluator(permissionEvaluator);
        
        return expressionHandler;
    }
}
