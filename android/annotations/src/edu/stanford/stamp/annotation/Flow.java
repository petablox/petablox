package edu.stanford.stamp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation describing a single information flow fact about a method.
 *
 * @see edu.stanford.stamp.annotation.FlowEndpoint
 * @author Manolis Papadakis
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface Flow
{
    /**
     * The origin of this flow.
     *
     * Declared as a string, possible values include:
     * <ul>
     * <li>A formal parameter of the method, denoted by its name, e.g.
     *     {@code "param1"}.</li>
     * <li>A high-level source, denoted by its name preceded by a {@code $},
     *     e.g. {@code "$CONTACTS"}.</li>
     * </ul>
     */
    String from();

    /**
     * The filter that is applied to this flow, if any.
     */
    String through() default "";

    /**
     * The target of this flow.
     *
     * Declared as a string, possible values include:
     * <ul>
     * <li>A formal parameter of the method, denoted by its name, e.g.
     *     {@code "param1"}.</li>
     * <li>A high-level sink, denoted by its name preceded by a {@code !}, e.g.
	 *     {@code "!INTERNET"}.</li>
     * <li>The return value of the method, denoted by {@code "@return"}.</li>
     * </ul>
     */
    String to();
}
