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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 *
 * Portions Copyright [2017-2019] Payara Foundation and/or affiliates
 */

package com.sun.enterprise.deployment;

import static java.util.Collections.emptySet;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.glassfish.security.common.Role;

import com.sun.enterprise.deployment.types.EjbReferenceContainer;
import com.sun.enterprise.deployment.types.MessageDestinationReferenceContainer;
import com.sun.enterprise.deployment.types.ResourceEnvReferenceContainer;
import com.sun.enterprise.deployment.types.ResourceReferenceContainer;
import com.sun.enterprise.deployment.types.ServiceReferenceContainer;

/**
 * Interface for information about an EJB
 */
public interface EjbDescriptor extends NamedDescriptor, WritableJndiNameEnvironment, EjbReferenceContainer, ResourceEnvReferenceContainer,
        ResourceReferenceContainer, ServiceReferenceContainer, MessageDestinationReferenceContainer {

    /**
     * Indicates the bean will manage its own transactions.
     */
    String BEAN_TRANSACTION_TYPE = "Bean";

    /**
     * Indicates the bean expects the server to manage its transactions.
     */
    String CONTAINER_TRANSACTION_TYPE = "Container";

    EjbBundleDescriptor getEjbBundleDescriptor();
    
    /**
     * Gets the application which the EJB is in
     * 
     * @return
     */
    Application getApplication();

    long getUniqueId();
    void setUniqueId(long id);
    
    boolean isLocalBean();
    
    // ### Interfaces supported

    boolean isRemoteInterfacesSupported();
    boolean isLocalInterfacesSupported();

    /**
     * Returns true if the EJB can be accessed remotely
     * 
     * @return
     */
    boolean isRemoteBusinessInterfacesSupported();
    boolean isLocalBusinessInterfacesSupported();
    boolean hasWebServiceEndpointInterface();
    
    String getWebServiceEndpointInterfaceName();
    void setWebServiceEndpointInterfaceName(String name);
    
    
    // ### Class names

    String getHomeClassName();
    String getLocalHomeClassName();
    String getEjbImplClassName();
    Set<String> getLocalBusinessClassNames();
    Set<String> getRemoteBusinessClassNames();
    String getLocalClassName();
    String getEjbClassName();
    String getRemoteClassName();

    String getType();
    String getEjbTypeForDisplay();
    
    Set<MethodDescriptor> getMethodDescriptors();

    void addEjbReferencer(EjbReferenceDescriptor ref);
    void removeEjbReferencer(EjbReferenceDescriptor ref);
    
    
    // ### Interceptors

    boolean hasInterceptorClass(String interceptorClassName);
    void addInterceptorClass(EjbInterceptor interceptor);
    void appendToInterceptorChain(List<EjbInterceptor> chain);
    void addMethodLevelChain(List<EjbInterceptor> chain, Method method, boolean aroundInvoke);

    
    // ### Security related methods
    
    Map<MethodPermission, List<MethodDescriptor>> getMethodPermissionsFromDD();
    
    Set<MethodPermission> getMethodPermissionsFor(MethodDescriptor methodDescriptor);

    Set<Role> getPermissionedRoles();
    default Set<RoleReference> getRoleReferences() {
        return emptySet();
    }
    RoleReference getRoleReferenceByName(String roleReferenceName);
    void addRoleReference(RoleReference roleReference);

    Set getSecurityBusinessMethodDescriptors();

    void addPermissionedMethod(MethodPermission mp, MethodDescriptor md);

    Boolean getUsesCallerIdentity();
    void setUsesCallerIdentity(boolean flag);
    
    RunAsIdentityDescriptor getRunAsIdentity();
    void setRunAsIdentity(RunAsIdentityDescriptor desc);
    
    /**
     * This method determines if all the mechanisms defined in the CSIV2 CompoundSecMechList structure require protected
     * invocations.
     */
    boolean allMechanismsRequireSSL();

    String getTransactionType();

    Set<EjbIORConfigurationDescriptor> getIORConfigurationDescriptors(); // FIXME by srini - consider ejb-internal-api

    void addFrameworkInterceptor(InterceptorDescriptor interceptor); // FIXME by srini - consider ejb-internal-api

    void notifyNewModule(WebBundleDescriptor wbd); // FIXME by srini - can we eliminate the need for this
    
}
