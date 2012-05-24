/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.enterprise.deployment;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.enterprise.deployment.types.EjbReferenceContainer;
import com.sun.enterprise.deployment.types.MessageDestinationReferenceContainer;
import com.sun.enterprise.deployment.types.ResourceEnvReferenceContainer;
import com.sun.enterprise.deployment.types.ResourceReferenceContainer;
import com.sun.enterprise.deployment.types.ServiceReferenceContainer;
import org.glassfish.security.common.Role;

public interface EjbDescriptor extends NamedDescriptor,
        WritableJndiNameEnvironment,
        EjbReferenceContainer,
        ResourceEnvReferenceContainer,
        ResourceReferenceContainer,
        ServiceReferenceContainer,
        MessageDestinationReferenceContainer {

    /**
     * Indicates the bean will manage its own transactions.
     */
    String BEAN_TRANSACTION_TYPE = "Bean";

    /**
     * Indicates the bean expects the server to manage its transactions.
     */
    String CONTAINER_TRANSACTION_TYPE = "Container";

    EjbBundleDescriptor getEjbBundleDescriptor();

    boolean isRemoteInterfacesSupported();

    boolean isLocalInterfacesSupported();

    boolean isRemoteBusinessInterfacesSupported();

    boolean isLocalBusinessInterfacesSupported();

    boolean hasWebServiceEndpointInterface();

    boolean isLocalBean();

    String getHomeClassName();

    String getLocalHomeClassName();

    String getEjbImplClassName();

    String getWebServiceEndpointInterfaceName();

    void setWebServiceEndpointInterfaceName(String name);

    void addEjbReferencer(EjbReferenceDescriptor ref);

    Set<String> getLocalBusinessClassNames();

    Set<String> getRemoteBusinessClassNames();

    String getLocalClassName();

    Set getMethodDescriptors();

    Map getMethodPermissionsFromDD();

    String getEjbClassName();

    String getType();

    Application getApplication();

    long getUniqueId();

    void setUniqueId(long id);

    RoleReference getRoleReferenceByName(String roleReferenceName);

    Set getSecurityBusinessMethodDescriptors();

    void addPermissionedMethod(MethodPermission mp, MethodDescriptor md);

    void setUsesCallerIdentity(boolean flag);

    Boolean getUsesCallerIdentity();

    RunAsIdentityDescriptor getRunAsIdentity();

    String getRemoteClassName();

    void removeEjbReferencer(EjbReferenceDescriptor ref);

    void addRoleReference(RoleReference roleReference);

    void setRunAsIdentity(RunAsIdentityDescriptor desc);

    String getEjbTypeForDisplay();

    boolean hasInterceptorClass(String interceptorClassName);

    void addInterceptorClass(EjbInterceptor interceptor);

    void appendToInterceptorChain(List<EjbInterceptor> chain);

    void addMethodLevelChain(List<EjbInterceptor> chain, Method m, boolean aroundInvoke);

    Set getMethodPermissionsFor(MethodDescriptor methodDescriptor);

    Set<Role> getPermissionedRoles();

    String getTransactionType();

    Set<EjbIORConfigurationDescriptor> getIORConfigurationDescriptors(); // FIXME by srini - consider ejb-internal-api

    void addFrameworkInterceptor(InterceptorDescriptor interceptor); // FIXME by srini - consider ejb-internal-api

    void notifyNewModule(WebBundleDescriptor wbd); // FIXME by srini - can we eliminate the need for this

    /**
     * This method determines if all the mechanisms defined in the
     * CSIV2 CompoundSecMechList structure require protected
     * invocations.
     */
    boolean allMechanismsRequireSSL();
}
