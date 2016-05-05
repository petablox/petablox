package edu.stanford.stamp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods (and constructors) that describes their behavior at a
 * granularity suitable for our analyses.
 *
 * @author Manolis Papadakis
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface STAMP
{
    /**
     * All the flows that will be caused if this method is called.
     */
    Flow[] flows() default {};
    
    String origin() default "manual";
}
