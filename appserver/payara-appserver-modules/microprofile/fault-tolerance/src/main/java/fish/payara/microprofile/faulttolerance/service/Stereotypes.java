package fish.payara.microprofile.faulttolerance.service;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Decouples sterotype-annotation lookup from application server context.
 *
 * @author Jan Bernitt
 */
public interface Stereotypes {

    boolean isStereotype(Class<? extends Annotation> annotationType);

    Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> stereotype);
}