/*
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *   Copyright (c) [2017-2018] Payara Foundation and/or its affiliates.
 *   All rights reserved.
 *
 *   The contents of this file are subject to the terms of either the GNU
 *   General Public License Version 2 only ("GPL") or the Common Development
 *   and Distribution License("CDDL") (collectively, the "License").  You
 *   may not use this file except in compliance with the License.  You can
 *   obtain a copy of the License at
 *   https://github.com/payara/Payara/blob/master/LICENSE.txt
 *   See the License for the specific
 *   language governing permissions and limitations under the License.
 *
 *   When distributing the software, include this License Header Notice in each
 *   file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *   GPL Classpath Exception:
 *   The Payara Foundation designates this particular file as subject to the
 *   "Classpath" exception as provided by the Payara Foundation in the GPL
 *   Version 2 section of the License file that accompanied this code.
 *
 *   Modifications:
 *   If applicable, add the following below the License Header, with the fields
 *   enclosed by brackets [] replaced by your own identifying information:
 *   "Portions Copyright [year] [name of copyright owner]"
 *
 *   Contributor(s):
 *   If you wish your version of this file to be governed by only the CDDL or
 *   only the GPL Version 2, indicate your decision by adding "[Contributor]
 *   elects to include this software in this distribution under the [CDDL or GPL
 *   Version 2] license."  If you don't indicate a single choice of license, a
 *   recipient has the option to distribute your version of this file under
 *   either the CDDL, the GPL Version 2 or to extend the choice of license to
 *   its licensees as provided above.  However, if you add GPL Version 2 code
 *   and therefore, elected the GPL Version 2 license, then the option applies
 *   only if the new code is made subject to such option by the copyright
 *   holder.
 */
package fish.payara.appserver.cdi.auth.roles;

import static fish.payara.cdi.auth.roles.LogicalOperator.AND;
import static fish.payara.cdi.auth.roles.LogicalOperator.OR;
import static java.util.Arrays.asList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Priority;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.security.enterprise.SecurityContext;

import fish.payara.cdi.auth.roles.CallerAccessException;
import fish.payara.cdi.auth.roles.RolesPermitted;

/**
 * The Roles CDI Interceptor authenticates requests to methods and classes
 * annotated with the @Roles annotation. If the security context cannot find a
 * role within the requestor which matches either all (if using the AND semantic
 * within the Roles annotation) or one of (if using the OR semantic within the
 * Roles annotation), then a NotAuthorizedException is thrown.
 *
 * @author Michael Ranaldo <michael@ranaldo.co.uk>
 * @author Arjan Tijms
 */
@Interceptor
@RolesPermitted
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class RolesCDIInterceptor {

    private final SecurityContext securityContext;

    private BeanManager beanManager;

    private Bean<?> interceptedBean;

    @Inject
    public RolesCDIInterceptor(@Intercepted Bean<?> interceptedBean, BeanManager beanManager) {
        this.securityContext = CDI.current().select(SecurityContext.class).get();
        this.interceptedBean = interceptedBean;
        this.beanManager = beanManager;
    }

    /**
     * Method invoked whenever a method annotated with @Roles, or a method
     * within a class annotated with @Roles is called.
     *
     * @param invocationContext Context provided by Weld.
     * @return Proceed to next interceptor in chain.
     */
    @AroundInvoke
    public Object method(InvocationContext invocationContext) throws Exception {
        RolesPermitted roles = getRolesPermitted(invocationContext);

        boolean isAccessPermitted = checkAccessPermitted(roles);

        if (!isAccessPermitted) {
            throw new CallerAccessException("Caller was not permitted access to a protected resource");
        }

        return invocationContext.proceed();
    }

    /**
     * Check that the roles allowed by the class or method match the roles
     * currently granted to the caller.
     *
     * @param roles The roles declared within the @Roles annotation.
     * @return True if access is allowed, false otherwise
     */
    public boolean checkAccessPermitted(RolesPermitted roles) {
        List<String> permittedRoles = asList(roles.value());

        if (roles.semantics().equals(OR)) {
            for (String role : permittedRoles) {
                if (securityContext.isCallerInRole(role)) {
                    return true;
                }
            }
        } else if (roles.semantics().equals(AND)) {
            for (String role : permittedRoles) {
                if (!securityContext.isCallerInRole(role)) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    private RolesPermitted getRolesPermitted(InvocationContext invocationContext) {

        RolesPermitted rolesPermitted = null;
        @SuppressWarnings("unchecked")
        Set<Annotation> bindings = (Set<Annotation>) invocationContext.getContextData().get("org.jboss.weld.interceptor.bindings");
        if (bindings != null) {
            for (Annotation annotation : bindings) {
                if (annotation.annotationType().equals(RolesPermitted.class)) {
                    rolesPermitted = RolesPermitted.class.cast(annotation);
                }
            }

            if (rolesPermitted != null) {
                return rolesPermitted;
            }
        }

        // Failing the Weld binding, check the method first
        rolesPermitted = getAnnotationFromMethod(beanManager, invocationContext.getMethod(), RolesPermitted.class);
        if (rolesPermitted != null) {
            return rolesPermitted;
        }

        // If nothing found on the method, check the the bean class
        rolesPermitted = getAnnotationFromClass(beanManager, interceptedBean.getBeanClass(), RolesPermitted.class);
        if (rolesPermitted != null) {
            return rolesPermitted;
        }

        // If still not found; throw. Since we're being called the annotation has to be there. Failing to
        // find it signals a critical error.
        throw new IllegalStateException("@RolesPermitted not found on " + interceptedBean.getBeanClass());
    }

    public static <A extends Annotation> A getAnnotationFromMethod(BeanManager beanManager, Method annotatedMethod, Class<A> annotationType) {

        if (annotatedMethod.isAnnotationPresent(annotationType)) {
            if (annotatedMethod.getAnnotation(annotationType) != null) {
                return annotatedMethod.getAnnotation(annotationType);
            }
        }

        Queue<Annotation> annotations = new LinkedList<>(asList(annotatedMethod.getAnnotations()));

        while (!annotations.isEmpty()) {
            Annotation annotation = annotations.remove();

            if (annotation.annotationType().equals(annotationType)) {
                if (annotationType.cast(annotation) != null) {
                    return annotationType.cast(annotation);
                }
            }

            if (beanManager.isStereotype(annotation.annotationType())) {
                annotations.addAll(
                        beanManager.getStereotypeDefinition(
                                annotation.annotationType()
                        )
                );
            }
        }

        return null;
    }

    public static <A extends Annotation> A getAnnotationFromClass(BeanManager beanManager, Class<?> annotatedClass, Class<A> annotationType) {

        if (annotatedClass.isAnnotationPresent(annotationType)) {
            if (annotatedClass.getAnnotation(annotationType) != null) {
                return annotatedClass.getAnnotation(annotationType);
            }
        }

        Queue<Annotation> annotations = new LinkedList<>(asList(annotatedClass.getAnnotations()));

        while (!annotations.isEmpty()) {
            Annotation annotation = annotations.remove();

            if (annotation.annotationType().equals(annotationType)) {
                if (annotationType.cast(annotation) != null) {
                    return annotationType.cast(annotation);
                }
            }

            if (beanManager.isStereotype(annotation.annotationType())) {
                annotations.addAll(
                        beanManager.getStereotypeDefinition(
                                annotation.annotationType()
                        )
                );
            }
        }

        return null;
    }
}
