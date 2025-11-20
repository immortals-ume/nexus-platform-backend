package com.immortals.platform.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to require any of the specified roles for method access.
 * Usage: @RequiresAnyRole({"ADMIN", "MANAGER"})
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAnyRole(#roles)")
public @interface RequiresAnyRole {
    String[] value();
}
