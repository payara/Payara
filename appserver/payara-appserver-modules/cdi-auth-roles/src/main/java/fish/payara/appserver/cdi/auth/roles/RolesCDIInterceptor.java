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

import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import fish.payara.cdi.auth.roles.Roles;
import fish.payara.cdi.auth.roles.LogicalOperator;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.inject.spi.CDI;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.security.enterprise.SecurityContext;
import javax.ws.rs.NotAuthorizedException;

/**
 * The Roles CDI Interceptor authenticates requests to methods and classes annotated with the @Roles annotation. If the
 * security context cannot find a role within the requestor which matches either all (if using the AND semantic within
 * the Roles annotation) or one of (if using the OR semantic within the Roles annotation), then a NotAuthorizedException
 * is thrown.
 *
 * @author Michael Ranaldo <michael@ranaldo.co.uk>
 */
@Interceptor
@Roles
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class RolesCDIInterceptor implements Serializable {

    private final static LocalStringsImpl STRINGS = new LocalStringsImpl(RolesCDIInterceptor.class);

    private final SecurityContext securityContext;

    public RolesCDIInterceptor() {
        this.securityContext = CDI.current().select(SecurityContext.class).get();
    }

    /**
     * Method invoked whenever a method annotated with @Roles, or a method within a class annotated with @Roles is
     * called.
     *
     * @param invocationContext Context provided by Weld.
     * @return Proceed to next interceptor in chain.
     */
    @AroundInvoke
    public Object method(InvocationContext invocationContext) {
        if (checkRoles(invocationContext.getMethod().getAnnotation(Roles.class))
                || (invocationContext.getMethod().getAnnotation(Roles.class) == null
                && checkRoles(invocationContext.getClass().getAnnotation(Roles.class)))) {
            try {
                Object result = invocationContext.proceed();
                return result;
            } catch (Exception ex) {
                Logger.getLogger(RolesCDIInterceptor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        throw new NotAuthorizedException(STRINGS.get("access.restricted.resource.failed"));
    }

    /**
     * Check that the roles allowed by the class or method match the roles currently granted to the caller.
     *
     * @param roles The roles declared within the @Roles annotation.
     * @return True or False
     */
    public boolean checkRoles(Roles roles) {
        List<String> permittedRoles = Arrays.asList(roles.allowed());
        if (roles.semantics().equals(LogicalOperator.OR)) {
            for (String role : permittedRoles) {
                if (securityContext.isCallerInRole(role)) {
                    return true;
                }
            }
        } else if (roles.semantics().equals(LogicalOperator.AND)) {
            for (String role : permittedRoles) {
                if (!securityContext.isCallerInRole(role)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
