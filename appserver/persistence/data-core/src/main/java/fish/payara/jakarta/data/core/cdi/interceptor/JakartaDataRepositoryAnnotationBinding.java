package fish.payara.jakarta.data.core.cdi.interceptor;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation binding used to intercept Repository annotation
 * @author Alfonso Valdez
 */
@InterceptorBinding
@Retention(RUNTIME)
@Target(TYPE)
public @interface JakartaDataRepositoryAnnotationBinding {
}
