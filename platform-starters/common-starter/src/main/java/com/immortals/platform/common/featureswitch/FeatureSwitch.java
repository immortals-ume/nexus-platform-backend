package com.immortals.platform.common.featureswitch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods/classes that are controlled by feature switches
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FeatureSwitch {
    
    /**
     * The name of the feature switch
     */
    String value();
    
    /**
     * Default behavior when switch is disabled
     */
    boolean defaultEnabled() default false;
}