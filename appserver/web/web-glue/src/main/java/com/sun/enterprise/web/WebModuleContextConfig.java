/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.web;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.web.ContextParameter;
import org.apache.catalina.*;
import org.apache.catalina.authenticator.DigestAuthenticator;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.startup.ContextConfig;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.web.valve.GlassFishValve;

import javax.naming.NamingException;
import java.lang.String;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Startup event listener for a <b>Context</b> that configures the properties
 * of that Context, and the associated defined servlets.
 *
 * @author Jean-Francois Arcand
 */

public class WebModuleContextConfig extends ContextConfig {

    private static final String DEFAULT_DIGEST_ALGORITHM = "default-digest-algorithm";

    private static final Logger logger = com.sun.enterprise.web.WebContainer.logger;

    protected static final ResourceBundle rb = logger.getResourceBundle();

    @LogMessageInfo(
            message = "Configured an authenticator for method {0}",
            level = "FINEST")
    public static final String AUTHENTICATOR_CONFIGURED = "AS-WEB-GLUE-00254";

    @LogMessageInfo(
            message = "[{0}] failed to unbind namespace",
            level = "WARNING")
    public static final String UNBIND_NAME_SPACE_ERROR = "AS-WEB-GLUE-00255";

    @LogMessageInfo(
            message = "No Realm with name [{0}] configured to authenticate against",
            level = "WARNING")
    public static final String MISSING_REALM = "AS-WEB-GLUE-00256";

    @LogMessageInfo(
            message = "Cannot configure an authenticator for method {0}",
            level = "WARNING")
    public static final String AUTHENTICATOR_MISSING = "AS-WEB-GLUE-00257";

    @LogMessageInfo(
            message = "Cannot instantiate an authenticator of class {0}",
            level = "WARNING")
    public static final String AUTHENTICATOR_INSTANTIATE_ERROR = "AS-WEB-GLUE-00258";


    public final static int CHILDREN = 0;
    public final static int SERVLET_MAPPINGS = 1;
    public final static int LOCAL_EJBS = 2;
    public final static int EJBS = 3;
    public final static int ENVIRONMENTS = 4;
    public final static int ERROR_PAGES = 5;
    public final static int FILTER_DEFS = 6;
    public final static int FILTER_MAPS = 7;
    public final static int APPLICATION_LISTENERS = 8;
    public final static int RESOURCES = 9;
    public final static int APPLICATION_PARAMETERS = 10;
    public final static int MESSAGE_DESTINATIONS = 11;
    public final static int MESSAGE_DESTINATION_REFS = 12;
    public final static int MIME_MAPPINGS = 13;
    
    protected ServiceLocator services;
        
    
    /**
     * The DOL object representing the web.xml content.
    */
    private WebBundleDescriptorImpl webBundleDescriptor;


    /**
     * Resource references from outside the .war
     */
    private Collection<ResourceReferenceDescriptor> resRefs =
                new HashSet<ResourceReferenceDescriptor>();

    /**
     * Environment properties from outside the .war
     */
    private Collection<EnvironmentProperty> envProps =
            new HashSet<EnvironmentProperty>();


    /**
     * Customized <code>ContextConfig</code> which use the DOL for deployment.
     */
    public WebModuleContextConfig(ServiceLocator services){
        synchronized (this) {
            this.services = services;
        }
    }

    
    /**
     * Set the DOL object associated with this class.
     */
    public void setDescriptor(WebBundleDescriptorImpl wbd){
        webBundleDescriptor = wbd;
    }

    /**
     * Return the WebBundleDescriptor
     */
    public WebBundleDescriptorImpl getDescriptor() {
        return webBundleDescriptor;
    }

    
    protected synchronized void configureResource()
            throws LifecycleException {
        
        List<ApplicationParameter> appParams = 
            context.findApplicationParameters();
        ContextParameter contextParam;
        synchronized (appParams) {
            Iterator<ApplicationParameter> i = appParams.iterator(); 
            while (i.hasNext()) {
                ApplicationParameter appParam = i.next();
                contextParam = new EnvironmentProperty(
                    appParam.getName(), appParam.getValue(),
                    appParam.getDescription());
                webBundleDescriptor.addContextParameter(contextParam);
            }
        }

        ContextEnvironment[] envs = context.findEnvironments();
        EnvironmentProperty envEntry;

        for (int i=0; i<envs.length; i++) {
            envEntry = new EnvironmentProperty(
                    envs[i].getName(), envs[i].getValue(),
                    envs[i].getDescription(), envs[i].getType()); 
            if (envs[i].getValue()!=null) {
                envEntry.setValue(envs[i].getValue());
            }
            webBundleDescriptor.addEnvironmentProperty(envEntry);
            envProps.add(envEntry);
        }

        ContextResource[] resources = context.findResources();
        ResourceReferenceDescriptor resourceReference;
        Set<ResourceReferenceDescriptor> rrs =
                webBundleDescriptor.getResourceReferenceDescriptors();
        ResourcePrincipal rp;

        for (int i=0; i<resources.length; i++) {
            resourceReference = new ResourceReferenceDescriptor(
                    resources[i].getName(), resources[i].getDescription(),
                    resources[i].getType());
            resourceReference.setJndiName(resources[i].getName());
            for (ResourceReferenceDescriptor rr : rrs) {
                if (resources[i].getName().equals(rr.getName())) {
                    resourceReference.setJndiName(rr.getJndiName());
                    rp = rr.getResourcePrincipal();
                    if (rp!=null) {
                        resourceReference.setResourcePrincipal(
                                new ResourcePrincipal(rp.getName(), rp.getPassword()));
                    }
                }
            }
            resourceReference.setAuthorization(resources[i].getAuth());
            webBundleDescriptor
                    .addResourceReferenceDescriptor(resourceReference);
            resRefs.add(resourceReference);
        }    
    }


    /**
     * Process a "start" event for this Context - in background
     */
    @Override
    protected synchronized void start() throws LifecycleException {
        configureResource();

        context.setConfigured(false);

        ComponentEnvManager namingMgr = services.getService(
            com.sun.enterprise.container.common.spi.util.ComponentEnvManager.class);
        if (namingMgr != null) {
            try {
                boolean webBundleContainsEjbs =
                    (webBundleDescriptor.getExtensionsDescriptors(EjbBundleDescriptor.class).size() > 0);

                // If .war contains EJBs, .war-defined dependencies have already been bound by
                // EjbDeployer, so just add the dependencies from outside the .war
                if( webBundleContainsEjbs ) {
                    namingMgr.addToComponentNamespace(webBundleDescriptor, envProps, resRefs);
                } else {
                    namingMgr.bindToComponentNamespace(webBundleDescriptor);
                }

                String componentId = namingMgr.getComponentEnvId(webBundleDescriptor);
                ((WebModule) context).setComponentId(componentId);
            } catch (NamingException ne) {
                throw new LifecycleException(ne);
            }
        }

        try {
            TomcatDeploymentConfig.configureWebModule(
                (WebModule)context, webBundleDescriptor);
            authenticatorConfig();
            managerConfig();

            context.setConfigured(true);
        } catch(Throwable t) {
            // clean up naming in case of errors
            unbindFromComponentNamespace(namingMgr);

            if (t instanceof RuntimeException) {
                throw (RuntimeException)t;
            } else if (t instanceof LifecycleException) {
                throw (LifecycleException)t;
            } else {
                throw new LifecycleException(t);
            }
        }
    }
    
    
    /**
     * Always sets up an Authenticator regardless of any security constraints.
     */
    @Override
    protected synchronized void authenticatorConfig()
            throws LifecycleException {
        
        LoginConfig loginConfig = context.getLoginConfig();
        if (loginConfig == null) {
            loginConfig = new LoginConfig("NONE", null, null, null);
            context.setLoginConfig(loginConfig);
        }

        // Has an authenticator been configured already?
        if (context instanceof Authenticator) {
            return;
        }
        if (context instanceof ContainerBase) {
            Pipeline pipeline = ((ContainerBase) context).getPipeline();
            if (pipeline != null) {
                GlassFishValve basic = pipeline.getBasic();
                if ((basic != null) && (basic instanceof Authenticator))
                    return;
                GlassFishValve valves[] = pipeline.getValves();
                for (int i = 0; i < valves.length; i++) {
                    if (valves[i] instanceof Authenticator)
                        return;
                }
            }
        } else {
            return;     // Cannot install a Valve even if it would be needed
        }

        // Has a Realm been configured for us to authenticate against?
        /* START IASRI 4856062
        if (context.getRealm() == null) {
        */
        // BEGIN IASRI 4856062
        Realm rlm = context.getRealm();
        if (rlm == null) {
        // END IASRI 4856062
            String realmName = (context.getLoginConfig() != null) ?
                context.getLoginConfig().getRealmName() : null;
            if (realmName != null && !realmName.isEmpty()) {
                String msg = rb.getString(MISSING_REALM);
                throw new LifecycleException(
                        MessageFormat.format(msg, realmName));
            }
            return;
        }

        // BEGIN IASRI 4856062
        // If a realm is available set its name in the Realm(Adapter)
        rlm.setRealmName(loginConfig.getRealmName(),
                         loginConfig.getAuthMethod());

        // END IASRI 4856062

        /*
         * First check to see if there is a custom mapping for the login
         * method. If so, use it. Otherwise, check if there is a mapping in
         * org/apache/catalina/startup/Authenticators.properties.
         */
        GlassFishValve authenticator = null;
        if (customAuthenticators != null) {
            authenticator = (GlassFishValve)
                customAuthenticators.get(loginConfig.getAuthMethod());
        }

        if (authenticator == null) {
            // Identify the class name of the Valve we should configure
            String authenticatorName = null;

            // BEGIN RIMOD 4808402
            // If login-config is given but auth-method is null, use NONE
            // so that NonLoginAuthenticator is picked
            String authMethod = loginConfig.getAuthMethod();
            if (authMethod == null) {
                authMethod = "NONE";
            }
            authenticatorName = authenticators.getProperty(authMethod);
            // END RIMOD 4808402
            /* RIMOD 4808402
            authenticatorName =
                    authenticators.getProperty(loginConfig.getAuthMethod());
            */

            if (authenticatorName == null) {
                String msg = rb.getString(AUTHENTICATOR_MISSING);
                throw new LifecycleException(MessageFormat.format(msg,
                    loginConfig.getAuthMethod()));
            }

            // Instantiate and install an Authenticator of the requested class
            try {
                Class authenticatorClass = Class.forName(authenticatorName);
                authenticator = (GlassFishValve)
                    authenticatorClass.newInstance();
            } catch (Exception e) {
                    String msg = rb.getString(AUTHENTICATOR_INSTANTIATE_ERROR);
                throw new LifecycleException(
                    MessageFormat.format(msg, authenticatorName),
                    e);
            }
        }

        if (authenticator != null && context instanceof ContainerBase) {
            Pipeline pipeline = ((ContainerBase) context).getPipeline();
            if (pipeline != null) {
                ((ContainerBase) context).addValve(authenticator);
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST,
                        AUTHENTICATOR_CONFIGURED,
                        loginConfig.getAuthMethod());
                }
            }
        }

        if (authenticator instanceof DigestAuthenticator) {
            Config config = services.getService(Config.class, ServerEnvironment.DEFAULT_INSTANCE_NAME);
            SecurityService securityService = config.getSecurityService();
            String digestAlgorithm = null;
            if (securityService != null) {
                digestAlgorithm = securityService.getPropertyValue(DEFAULT_DIGEST_ALGORITHM);
            }
            if (digestAlgorithm != null) {
                ((DigestAuthenticator)authenticator).setAlgorithm(digestAlgorithm);
            }
        }
    }
    
    
    /**
     * Process the default configuration file, if it exists.
     * The default config must be read with the container loader - so
     * container servlets can be loaded
     */
    @Override
    protected void defaultConfig() {
        ;
    }


    /**
     * Process a "stop" event for this Context.
     */
    @Override
    protected synchronized void stop() {
        
        super.stop();
        ComponentEnvManager namingMgr = services.getService(
            com.sun.enterprise.container.common.spi.util.ComponentEnvManager.class);
        unbindFromComponentNamespace(namingMgr);

    }

    private void unbindFromComponentNamespace(ComponentEnvManager namingMgr) {
        if (namingMgr != null) {
            try {
                namingMgr.unbindFromComponentNamespace(webBundleDescriptor);
            } catch (javax.naming.NamingException ex) {
                String msg = rb.getString(UNBIND_NAME_SPACE_ERROR);
                msg = MessageFormat.format(msg, context.getName());
                logger.log(Level.WARNING, msg, ex);
            }        
        }
    }


}
