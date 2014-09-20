/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.ee;

import com.sun.enterprise.security.PolicyLoader;
import com.sun.enterprise.security.WebSecurityDeployerProbeProvider;
import org.glassfish.deployment.common.SecurityRoleMapperFactory;
import com.sun.enterprise.deployment.interfaces.SecurityRoleMapperFactoryMgr;
import com.sun.enterprise.security.web.integration.WebSecurityManagerFactory;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.api.container.Container;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Security container service
 *
 */
@Service(name="com.sun.enterprise.security.ee.SecurityContainer")
public class SecurityContainer implements Container, PostConstruct{

    @Inject 
    private PolicyLoader policyLoader;
    
    @Inject
    private ServerContext serverContext;

    @Inject
    private ServiceLocator habitat;

    @Inject
    private Provider<ClassLoaderHierarchy> classLoaderHierarchyProvider;

    @Inject
    private Provider<WebSecurityManagerFactory> webSecurityManagerFactoryProvider;

    static {
        initRoleMapperFactory();
    }
    
    /**
     * The system-assigned default web module's name/identifier.
     *
     * This has to be the same value as is in j2ee/WebModule.cpp.
     */
    public static final String DEFAULT_WEB_MODULE_NAME = "__default-web-module";

    public String getName() {
        return "Security";
    }

    public Class<? extends org.glassfish.api.deployment.Deployer> 
        getDeployer() {
        return SecurityDeployer.class;
    }

    public void postConstruct() {
        /* This is handled by SecurityDeployer
        //Generate Policy for the Dummy Module
        WebBundleDescriptor wbd = new WebBundleDescriptor();
        Application application = Application.createApplication();
        application.setVirtual(true);
        application.setName(DEFAULT_WEB_MODULE_NAME);
        application.setRegistrationName(DEFAULT_WEB_MODULE_NAME);
        wbd.setApplication(application);
        generatePolicy(wbd);     */
    }
    /*
    private void generatePolicy(WebBundleDescriptor wbd) {
        String name = null;
        ClassLoader oldTcc = Thread.currentThread().getContextClassLoader();
        try {
            //TODO: workaround here. Once fixed in V3 we should be able to use
            //Context ClassLoader instead.
            ClassLoaderHierarchy hierarchy = classLoaderHierarchyProvider.get();
            ClassLoader tcc = hierarchy.getCommonClassLoader();
            Thread.currentThread().setContextClassLoader(tcc);
            
            policyLoader.loadPolicy();
            
            WebSecurityManagerFactory wsmf = webSecurityManagerFactoryProvider.get();
            // this should create all permissions
            wsmf.createManager(wbd,true,serverContext);
            // for an application the securityRoleMapper should already be
            // created. I am just creating the web permissions and handing
            // it to the security component.
            name = WebSecurityManager.getContextID(wbd);
            SecurityUtil.generatePolicyFile(name);
            websecurityProbeProvider.policyCreationEvent(name);

        } catch (IASSecurityException se) {
            String msg = "Error in generating security policy for " + name;
            throw new RuntimeException(msg, se);
        } finally {
            Thread.currentThread().setContextClassLoader(oldTcc);
        }
    }*/
    
    private static void initRoleMapperFactory() //throws Exception
    {
        Object o = null;
        Class c = null;
        // this should never fail.
        try {
            c = Class.forName("com.sun.enterprise.security.acl.RoleMapperFactory");
            if (c != null) {
                o = c.newInstance();
                if (o != null && o instanceof SecurityRoleMapperFactory) {
                    SecurityRoleMapperFactoryMgr.registerFactory((SecurityRoleMapperFactory) o);
                }
            }
            if (o == null) {
            //               _logger.log(Level.SEVERE,_localStrings.getLocalString("j2ee.norolemapper", "Cannot instantiate the SecurityRoleMapperFactory"));
            }
        } catch (Exception cnfe) {
//            _logger.log(Level.SEVERE,
//			_localStrings.getLocalString("j2ee.norolemapper", "Cannot instantiate the SecurityRoleMapperFactory"), 
//			cnfe);
//		cnfe.printStackTrace();
//		throw new RuntimeException(cnfe);
        //   throw  cnfe;
        }
    }
}
