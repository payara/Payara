/*
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *   Copyright (c) [2017-2021] Payara Foundation and/or its affiliates.
 *   All rights reserved.
 *
 *   The contents of this file are subject to the terms of either the GNU
 *   General Public License Version 2 only ("GPL") or the Common Development
 *   and Distribution License("CDDL") (collectively, the "License").  You
 *   may not use this file except in compliance with the License.  You can
 *   obtain a copy of the License at
 *   https://github.com/payara/Payara/blob/main/LICENSE.txt
 *   See the License for the specific
 *   language governing permissions and limitations under the License.
 *
 *   When distributing the software, include this License Header Notice in each
 *   file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
import static jakarta.security.enterprise.AuthenticationStatus.NOT_DONE;
import static jakarta.security.enterprise.AuthenticationStatus.SEND_FAILURE;
import static jakarta.security.enterprise.AuthenticationStatus.SUCCESS;
import static jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters.withParams;
import static org.glassfish.soteria.cdi.AnnotationELPProcessor.evalELExpression;
import static org.glassfish.soteria.cdi.AnnotationELPProcessor.hasAnyELExpression;
import static org.glassfish.soteria.cdi.CdiUtils.getAnnotation;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.el.ELProcessor;
import jakarta.enterprise.inject.Intercepted;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import fish.payara.cdi.auth.roles.CallerAccessException;
import fish.payara.cdi.auth.roles.RolesPermitted;

/**
 * The RolesPermitted Interceptor authenticates requests to methods and classes
 * annotated with the @RolesPermitted annotation. If the security context cannot
 * find a role within the requestor which matches either all (if using the AND
 * semantic within the RolesPermitted annotation) or one of (if using the OR
 * semantic within the RolesPermitted annotation), then a CallerAccessException
 * is thrown.
 *
 * @author Michael Ranaldo <michael@ranaldo.co.uk>
 * @author Arjan Tijms
 */
@Interceptor
@RolesPermitted
@Priority(Interceptor.Priority.PLATFORM_AFTER + 1000)
public class RolesPermittedInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Bean<?> interceptedBean;

    private final NonSerializableProperties lazyProperties;

    @Context
    private transient HttpServletRequest request;

    @Context
    private transient HttpServletResponse response;

    @Inject
    public RolesPermittedInterceptor(@Intercepted Bean<?> interceptedBean) {
        this.interceptedBean = interceptedBean;
        this.lazyProperties = new NonSerializableProperties();
    }

    /**
     * Method invoked whenever a method annotated with @Roles, or a method
     * within a class annotated with @Roles is called.
     *
     * @param invocationContext Context provided by Weld.
     * @return Proceed to next interceptor in chain.
     * @throws java.lang.Exception
     * @throws fish.payara.cdi.auth.roles.CallerAccessException if access is not permitted
     */
    @AroundInvoke
    public Object method(InvocationContext invocationContext) throws Exception {
        RolesPermitted roles = getRolesPermitted(invocationContext);

        boolean isAccessPermitted = checkAccessPermitted(roles, invocationContext);
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
     * @param invocationContext
     * @return True if access is allowed, false otherwise
     */
    public boolean checkAccessPermitted(RolesPermitted roles, InvocationContext invocationContext) {

        authenticate(roles.value());

        ELProcessor eLProcessor = null;
        if (hasAnyELExpression(roles.value())) {
            eLProcessor = getElProcessor(invocationContext);
        }

        List<String> permittedRoles = asList(roles.value());

        final SecurityContext securityContext = lazyProperties.getSecurityContext();

        if (OR.equals(roles.semantics())) {
            for (String role : permittedRoles) {
                if (eLProcessor != null && hasAnyELExpression(role)) {
                    role = evalELExpression(eLProcessor, role);
                }
                if (securityContext.isCallerInRole(role)) {
                    return true;
                }
            }
        } else if (AND.equals(roles.semantics())) {
            for (String role : permittedRoles) {
                if (eLProcessor != null && hasAnyELExpression(role)) {
                    role = evalELExpression(eLProcessor, role);
                }
                if (!securityContext.isCallerInRole(role)) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    private RolesPermitted getRolesPermitted(InvocationContext invocationContext) {

        Optional<RolesPermitted> optionalRolesPermitted;

        // Try the Weld bindings first. This gives us the *exact* binding which caused this interceptor being called
        @SuppressWarnings("unchecked")
        Set<Annotation> bindings = (Set<Annotation>) invocationContext.getContextData().get("org.jboss.weld.interceptor.bindings");
        if (bindings != null) {
            optionalRolesPermitted = bindings.stream()
                    .filter(annotation -> annotation.annotationType().equals(RolesPermitted.class))
                    .findAny()
                    .map(RolesPermitted.class::cast);

            if (optionalRolesPermitted.isPresent()) {
                return optionalRolesPermitted.get();
            }
        }

        final BeanManager beanManager = lazyProperties.getBeanManager();

        // Failing the Weld binding, check the method first
        optionalRolesPermitted = getAnnotationFromMethod(beanManager, invocationContext.getMethod(), RolesPermitted.class);
        if (optionalRolesPermitted.isPresent()) {
            return optionalRolesPermitted.get();
        }

        // If nothing found on the method, check the the bean class
        optionalRolesPermitted = getAnnotation(beanManager, interceptedBean.getBeanClass(), RolesPermitted.class);
        if (optionalRolesPermitted.isPresent()) {
            return optionalRolesPermitted.get();
        }

        // If still not found; throw. Since we're being called the annotation has to be there. Failing to
        // find it signals a critical error.
        throw new IllegalStateException("@RolesPermitted not found on " + interceptedBean.getBeanClass());
    }

    public static <A extends Annotation> Optional<A> getAnnotationFromMethod(BeanManager beanManager, Method annotatedMethod, Class<A> annotationType) {

        if (annotatedMethod.isAnnotationPresent(annotationType)) {
            return Optional.of(annotatedMethod.getAnnotation(annotationType));
        }

        Queue<Annotation> annotations = new LinkedList<>(asList(annotatedMethod.getAnnotations()));

        while (!annotations.isEmpty()) {
            Annotation annotation = annotations.remove();

            if (annotation.annotationType().equals(annotationType)) {
                return Optional.of(annotationType.cast(annotation));
            }

            if (beanManager.isStereotype(annotation.annotationType())) {
                annotations.addAll(
                        beanManager.getStereotypeDefinition(
                                annotation.annotationType()
                        )
                );
            }
        }

        return Optional.empty();
    }

    private ELProcessor getElProcessor(InvocationContext invocationContext) {
        final BeanManager beanManager = lazyProperties.getBeanManager();

        ELProcessor elProcessor = new ELProcessor();
        elProcessor.getELManager().addELResolver(beanManager.getELResolver());
        elProcessor.defineBean("self", invocationContext.getTarget());

        Parameter[] parameters = invocationContext.getMethod().getParameters();
        Object[] values = invocationContext.getParameters();
        boolean paramAdded = false;
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Named named = param.getAnnotation(Named.class);
            String key = null;
            if (named != null && !(key = named.value().trim()).isEmpty()) {
                elProcessor.defineBean(key, values[i]);
                paramAdded = true;
            }
        }
        if (!paramAdded && parameters.length == 1) {
            elProcessor.defineBean("param", values[0]);
        }

        return elProcessor;
    }

    private void authenticate(String[] roles) {
        final SecurityContext securityContext = lazyProperties.getSecurityContext();

        if (request != null && response != null
                && roles.length > 0 && !isAuthenticated(securityContext)) {
            AuthenticationStatus status = securityContext.authenticate(request, response, withParams());

            // Authentication was not done at all (i.e. no credentials present) or
            // authentication failed (i.e. wrong credentials, credentials expired, etc)
            if (status == NOT_DONE || status == SEND_FAILURE) {
                throw new NotAuthorizedException(
                    "Authentication resulted in " + status,
                    Response.status(Response.Status.UNAUTHORIZED).build()
                );
            }

            // compensate for possible Soteria bug, need to investigate
            if (status == SUCCESS && !isAuthenticated(securityContext)) {
                throw new NotAuthorizedException(
                    "Authentication not done (i.e. no credential found)",
                    Response.status(Response.Status.UNAUTHORIZED).build()
                );
            }
        }
    }

    private static boolean isAuthenticated(SecurityContext securityContext) {
        return securityContext.getCallerPrincipal() != null;
    }
}
