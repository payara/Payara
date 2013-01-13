/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.container.common.impl;


import org.glassfish.api.invocation.ApplicationEnvironment;
import org.glassfish.api.naming.NamespacePrefixes;
import org.glassfish.api.naming.NamedNamingObjectProxy;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;

import com.sun.enterprise.deployment.*;


import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;

import javax.naming.NamingException;

import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.container.common.spi.ManagedBeanManager;
import com.sun.logging.LogDomains;

import javax.naming.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.admin.ProcessEnvironment.ProcessType;


@Service
@NamespacePrefixes({JavaModuleNamingProxy.JAVA_APP_CONTEXT,
        JavaModuleNamingProxy.JAVA_APP_NAME,
        JavaModuleNamingProxy.JAVA_MODULE_CONTEXT,
        JavaModuleNamingProxy.JAVA_MODULE_NAME,
        JavaModuleNamingProxy.JAVA_APP_SERVICE_LOCATOR})
public class JavaModuleNamingProxy
        implements NamedNamingObjectProxy, PostConstruct {

    @Inject
    ServiceLocator habitat;

    @Inject
    private ProcessEnvironment processEnv;
    
    @Inject
    private ApplicationRegistry applicationRegistry;

    private ProcessEnvironment.ProcessType processType;


    private static Logger _logger = LogDomains.getLogger(JavaModuleNamingProxy.class,
            LogDomains.NAMING_LOGGER);

    private InitialContext ic;

    public void postConstruct() {
        try {
            ic = new InitialContext();
        } catch(NamingException ne) {
            throw new RuntimeException("JavaModuleNamingProxy InitialContext creation failure", ne);
        }

        processType = processEnv.getProcessType();
    }

    static final String JAVA_MODULE_CONTEXT
            = "java:module/";

    static final String JAVA_APP_CONTEXT
            = "java:app/";

    static final String JAVA_APP_NAME
            = "java:app/AppName";

    static final String JAVA_MODULE_NAME
            = "java:module/ModuleName";
    
    static final String JAVA_APP_SERVICE_LOCATOR
            = "java:app/hk2/ServiceLocator";

    public Object handle(String name) throws NamingException {

        // Return null if this proxy is not responsible for processing the name.
        Object returnValue = null;

        if( name.equals(JAVA_APP_NAME) ) {

            returnValue = getAppName();

        } else if( name.equals(JAVA_MODULE_NAME) ) {

            returnValue = getModuleName();
            
        } else if( name.equals(JAVA_APP_SERVICE_LOCATOR) ) {
            
            returnValue = getAppServiceLocator();

        } else if (name.startsWith(JAVA_MODULE_CONTEXT) || name.startsWith(JAVA_APP_CONTEXT)) {

            // Check for any automatically defined portable EJB names under
            // java:module/ or java:app/.

            // If name is not found, return null instead
            // of throwing an exception.
            // The application can explicitly define environment dependencies within this
            // same namespace, so this will allow other name checking to take place.
            returnValue = getJavaModuleOrAppEJB(name);
        }

        return returnValue;
    }

    private String getAppName() throws NamingException {

        ComponentEnvManager namingMgr =
                habitat.getService(ComponentEnvManager.class);

        String appName = null;

        if( namingMgr != null ) {
            JndiNameEnvironment env = namingMgr.getCurrentJndiNameEnvironment();

            BundleDescriptor bd = null;

            if( env instanceof EjbDescriptor ) {
                bd = ((EjbDescriptor)env).getEjbBundleDescriptor();
            } else if( env instanceof BundleDescriptor ) {
                bd = (BundleDescriptor) env;
            }

            if( bd != null ) {

                Application app = bd.getApplication();

                appName = app.getAppName();               
            }
            else {
                ApplicationEnvironment applicationEnvironment = namingMgr.getCurrentApplicationEnvironment();
                
                if (applicationEnvironment != null) {
                    appName = applicationEnvironment.getName();
                }
            }
        }

        if( appName == null ) {
            throw new NamingException("Could not resolve " + JAVA_APP_NAME);
        }

        return appName;

    }

    private String getModuleName() throws NamingException {

        ComponentEnvManager namingMgr =
                habitat.getService(ComponentEnvManager.class);

        String moduleName = null;

        if( namingMgr != null ) {
            JndiNameEnvironment env = namingMgr.getCurrentJndiNameEnvironment();

            BundleDescriptor bd = null;

            if( env instanceof EjbDescriptor ) {
                bd = ((EjbDescriptor)env).getEjbBundleDescriptor();
            } else if( env instanceof BundleDescriptor ) {
                bd = (BundleDescriptor) env;
            }

            if( bd != null ) {
                moduleName = bd.getModuleDescriptor().getModuleName();
            }
        }

        if( moduleName == null ) {
            throw new NamingException("Could not resolve " + JAVA_MODULE_NAME);
        }

        return moduleName;

    }
    
    private ServiceLocator getAppServiceLocator() throws NamingException {
        
        String appName = getAppName();

        ApplicationInfo info = applicationRegistry.get(appName);
        
        if (info == null) {
            throw new NamingException("Could not resolve " + JAVA_APP_SERVICE_LOCATOR);
        }
        
        return info.getAppServiceLocator();

    }



    private Object getJavaModuleOrAppEJB(String name) throws NamingException {

        String newName = null;
        Object returnValue = null;

        if( habitat != null ) {
            ComponentEnvManager namingMgr =
                habitat.getService(ComponentEnvManager.class);

            if( namingMgr != null ) {
                JndiNameEnvironment env = namingMgr.getCurrentJndiNameEnvironment();

                BundleDescriptor bd = null;

                if( env instanceof EjbDescriptor ) {
                    bd = ((EjbDescriptor)env).getEjbBundleDescriptor();
                } else if( env instanceof BundleDescriptor ) {
                    bd = (BundleDescriptor) env;
                }

                if( bd != null ) {
                    Application app = bd.getApplication();

                    String appName = null;

                    if ( !app.isVirtual() )  {
                        appName = app.getAppName();
                    }

                    String moduleName = bd.getModuleDescriptor().getModuleName();

                    StringBuffer javaGlobalName = new StringBuffer("java:global/");


                    if( name.startsWith(JAVA_APP_CONTEXT) ) {

                        // For portable EJB names relative to java:app, module
                        // name is already contained in the lookup string.  We just
                        // replace the logical java:app with the application name
                        // in the case of an .ear.  Otherwise, in the stand-alone
                        // module case the existing module-name already matches the global
                        // syntax.

                        if (appName != null) {
                            javaGlobalName.append(appName);
                            javaGlobalName.append("/");
                        } 

                        // Replace java:app/ with the fully-qualified global portion
                        int javaAppLength = JAVA_APP_CONTEXT.length();
                        javaGlobalName.append(name.substring(javaAppLength));

                    } else {

                        // For portable EJB names relative to java:module, only add
                        // the application name if it's an .ear, but always add
                        // the module name.
   
                        if (appName != null) {
                            javaGlobalName.append(appName);
                            javaGlobalName.append("/");
                        }

                        javaGlobalName.append(moduleName);
                        javaGlobalName.append("/");

                        // Replace java:module/ with the fully-qualified global portion
                        int javaModuleLength = JAVA_MODULE_CONTEXT.length();
                        javaGlobalName.append(name.substring(javaModuleLength));
                    }

                    newName = javaGlobalName.toString();

                }
            }

        }

        if( newName != null ) {

            try {

                if( processType == ProcessType.ACC) {

                    ManagedBeanManager mbMgr = habitat.getService(ManagedBeanManager.class);

                    try {
                        returnValue = mbMgr.getManagedBean(newName);
                    } catch(Exception e) {
                        NamingException ne = new NamingException("Error creating ACC managed bean " + newName);
                        ne.initCause(e);
                        throw ne;                     
                    }

                }

                if( returnValue == null ) {
                    returnValue = ic.lookup(newName);
                }
            } catch(NamingException ne) {

                _logger.log(Level.FINE, newName + " Unable to map " + name + " to derived name " +
                        newName, ne);
            }
        }

        return returnValue;
    }

}



