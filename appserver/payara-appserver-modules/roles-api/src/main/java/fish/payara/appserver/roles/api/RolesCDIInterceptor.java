/*
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 *   Copyright (c) [2017] Payara Foundation and/or its affiliates. 
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
package fish.payara.appserver.roles.api;

import com.sun.enterprise.web.WebModule;
import org.glassfish.webservices.SecurityService;
import fish.payara.roles.api.Roles;
import java.io.Serializable;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.ejb.api.EJBInvocation;
import org.glassfish.internal.api.Globals;
import org.glassfish.webservices.WebServiceContextImpl;
import org.glassfish.webservices.WebServiceContractImpl;

/**
 *
 * @author Michael Ranaldo <michael@ranaldo.co.uk>
 */
@Interceptor
@Roles
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class RolesCDIInterceptor implements Serializable {
    
//    @Inject
//    WebServiceContextImpl wsci;

    @AroundInvoke
    public Object method(InvocationContext ctx) {
        InvocationManager ivm = Globals.getDefaultBaseServiceLocator().getService(InvocationManager.class);
        SecurityManager sm = System.getSecurityManager();
        SecurityService secServ = Globals.get(SecurityService.class);
        ComponentInvocation ejbi = WebServiceContractImpl.getInstance().getInvocationManager().getCurrentInvocation();
        ejbi.getInstanceName();
        Principal p = secServ.getUserPrincipal(true);
        // either try and find the application name for this section
        // this appears to be the proper way of doing things
        secServ.isUserInRole((WebModule) ivm.getCurrentInvocation().getContainer(), p, ivm.getCurrentInvocation().getAppName(), "user");
        WebModule wm = (WebModule) ivm.getCurrentInvocation().getContainerContext();
        Object result = null;
//        WebServiceContextImpl wsci = wsci.isUserInRole("user");
        System.out.println("within interceptor");

        // or have a look at this method for ways to get the webservicecontextimpl which will do it for me
        // along with web / ejb checks
        WebServiceContextImpl wcsi = new WebServiceContextImpl();
//        WebServiceContextImpl wcsi = InitialContext.doLookup(wm.);
        if (ctx.getMethod().getAnnotation(Roles.class) != null) {
            List<String> permittedRoles = Arrays.asList(ctx.getMethod().getAnnotation(Roles.class).allowed());
            for (String s : permittedRoles) {
                if (/*secz.isUserInRole(s)*/true) {
                    try {
                        System.out.println("pass");
                        result = ctx.proceed();
                    } catch (Exception ex) {
                        System.out.println("Exception within invoc");
                        Logger.getLogger(RolesCDIInterceptor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        System.out.println("fail");
        return result;
    }
}
