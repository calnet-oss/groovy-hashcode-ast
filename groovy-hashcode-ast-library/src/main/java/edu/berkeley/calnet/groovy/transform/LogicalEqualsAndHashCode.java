package edu.berkeley.calnet.groovy.transform;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.*;

/**
 * Usage:
 *
 * <code>
 * @LogicalEqualsAndHashCode
 * class MyClass {
 *     ...
 * }
 * </code>
 *
 * This will add logicalEquals() and a logicalHashCode() methods to a domain
 * class.  These methods will use the DomainLogicalComparator to logically
 * compare domain objects.
 *
 * A logical comparison means comparing values in the domain objects except
 * for the primary keys.
 *
 * This annotation accepts the following parameters:
 *
 * excludes=[list of strings] - Optionally pass a list of field names that
 * should be excluded from comparison.
 *
 * includes=[list of strings] - Optionally pass a list of field names that
 * should only be included in the comparison.  (The primary key field will
 * be ignored if it's included.)
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@GroovyASTTransformationClass("edu.berkeley.calnet.groovy.transform.LogicalEqualsAndHashCodeASTTransformation")
public @interface LogicalEqualsAndHashCode {
    public abstract String[] excludes() default {};

    public abstract String[] includes() default {};
}
