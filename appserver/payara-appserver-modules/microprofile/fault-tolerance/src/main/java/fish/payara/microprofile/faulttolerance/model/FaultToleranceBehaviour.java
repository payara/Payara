package fish.payara.microprofile.faulttolerance.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

import org.eclipse.microprofile.faulttolerance.Fallback;

/**
 * Added to methods at runtime in case they are affected by one of the FT annotations.
 * This means a FT annotation is either present directly on the method or on the class declaring the method.
 * 
 * This indirection is needed for two reasons:
 * 
 * 1) Allow to process all FT annotations with a single interceptor
 * 
 * 2) Allow to process {@link Fallback} even though it cannot be annotated on type level what would be needed to bind it
 *    to an interceptor directly.
 * 
 * @author Jan Bernitt
 */
@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface FaultToleranceBehaviour {
    //marker
}
