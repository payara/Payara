/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.security.Security;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextHandler;

import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.component.PreDestroy;

import com.sun.enterprise.security.audit.AuditManager;
import com.sun.enterprise.security.auth.realm.RealmsManager;
import com.sun.enterprise.security.authorize.PolicyContextHandlerImpl;
import com.sun.enterprise.security.common.Util;
import com.sun.enterprise.security.jmac.config.GFAuthConfigFactory;
import com.sun.enterprise.security.ssl.SSLUtils;
import org.glassfish.internal.api.ServerContext;
import com.sun.logging.LogDomains;
import org.glassfish.api.Startup.Lifecycle;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.external.probe.provider.StatsProviderManager;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.Singleton;
import org.jvnet.hk2.config.ConfigListener;

/**
 * This class extends default implementation of ServerLifecycle interface.
 * It provides security initialization and setup for the server.
 * @author  Shing Wai Chan
 */
@Service
@Scoped(Singleton.class)
public class SecurityLifecycle implements  PostConstruct, PreDestroy {
    
    @Inject
    private ServerContext sc;
    
    //@Inject 
    //private RealmConfig realmConfig;
    
    @Inject 
    private PolicyLoader policyLoader;
    
    @Inject
    private SecurityServicesUtil secServUtil;
    
    @Inject 
    private Util util;
    
    @Inject
    private SSLUtils sslUtils;
    
    @Inject
    private SecurityConfigListener configListener;
    
    @Inject(name="MessageSecurityConfigListener", optional=true)
    private ConfigListener msgSecurityConfigListener;

    @Inject
    private Habitat habitat;

    @Inject
    private RealmsManager realmsManager;
    
    private static WebSecurityDeployerStatsProvider webStatsProvider = null;

    private EventListener listener = null;

    private static final String SYS_PROP_LOGIN_CONF = "java.security.auth.login.config";
    private static final String SYS_PROP_JAVA_SEC_POLICY =  "java.security.policy";
 
    private static final Logger _logger = LogDomains.getLogger(SecurityLifecycle.class, LogDomains.SECURITY_LOGGER);

    public SecurityLifecycle() {
	try {

            if (Util.isEmbeddedServer()) {
                //If the user-defined login.conf/server.policy are set as system properties, then they are given priority
                if (System.getProperty(SYS_PROP_LOGIN_CONF) == null) {
                    System.setProperty(SYS_PROP_LOGIN_CONF, Util.writeConfigFileToTempDir("login.conf").getAbsolutePath());
                }
                if (System.getProperty(SYS_PROP_JAVA_SEC_POLICY) == null) {
                    System.setProperty(SYS_PROP_JAVA_SEC_POLICY, Util.writeConfigFileToTempDir("server.policy").getAbsolutePath());
                }
            }
            
            // security manager is set here so that it can be accessed from
            // other lifecycles, like PEWebContainer
            java.lang.SecurityManager secMgr = System.getSecurityManager();
            if (secMgr != null &&
                    !(J2EESecurityManager.class.equals(secMgr.getClass()))) {
                J2EESecurityManager mgr = new J2EESecurityManager();
                try {
                    System.setSecurityManager(mgr);
                } catch (SecurityException ex) {
                    _logger.log(Level.WARNING, "security.secmgr.could.not.override");
                }
            }
            
            if (_logger.isLoggable(Level.INFO)) {
                if (secMgr != null) {
                    _logger.info("security.secmgron");
                } else {
                    _logger.info("security.secmgroff");
                }
            }
	} catch(Exception ex) {
            _logger.log(Level.SEVERE, "java_security.init_securitylifecycle_fail", ex);
            throw new RuntimeException(ex.toString(), ex);
	}
    }   

    // override default
    public void onInitialization() {

        try {
             if (_logger.isLoggable(Level.INFO)) {
                 _logger.log(Level.INFO, "sec.service.startup.enter");
             }
             
             webStatsProvider = new WebSecurityDeployerStatsProvider();
             StatsProviderManager.register("security", PluginPoint.SERVER, "security/web", webStatsProvider);
             

            //TODO:V3 LoginContextDriver has a static variable dependency on AuditManager
            //And since LoginContextDriver has too many static methods that use AuditManager
            //we have to make this workaround here.
             //Commenting this since this is being handles in LoginContextDriver
        //    LoginContextDriver.AUDIT_MANAGER = secServUtil.getAuditManager();

            //replaced with SharedSecureRandom API
            //secServUtil.initSecureSeed();

            //jmac
            initializeJMAC();

            // jacc
            //registerPolicyHandlers();
            // assert(policyLoader != null);
            policyLoader.loadPolicy();

            realmsManager.createRealms();
            // start the audit mechanism
            AuditManager auditManager = secServUtil.getAuditManager();
            auditManager.loadAuditModules();

            //Audit the server started event
            auditManager.serverStarted();
            
            // initRoleMapperFactory is in J2EEServer.java and not moved to here
            // this is because a DummyRoleMapperFactory is register due
            // to invocation of ConnectorRuntime.createActiveResourceAdapter
            // initRoleMapperFactory is called after it
            //initRoleMapperFactory();
           
           if (_logger.isLoggable(Level.INFO)) {
                 _logger.log(Level.INFO, "sec.service.startup.exit");
             }

        } catch(Exception ex) {
            throw new SecurityLifecycleException(ex);
        }
    }

    private void initializeJMAC() throws IOException {

	// define default factory if it is not already defined
	// factory will be constructed on first getFactory call.

	String defaultFactory = Security.getProperty
	    (AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY);
	if (defaultFactory == null) {
	    Security.setProperty
		(AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY,
		 GFAuthConfigFactory.class.getName());
 	}
    }

    private void registerPolicyHandlers()
            throws javax.security.jacc.PolicyContextException {
        PolicyContextHandler pch = PolicyContextHandlerImpl.getInstance();
        PolicyContext.registerHandler(PolicyContextHandlerImpl.ENTERPRISE_BEAN,
            pch, true);
        PolicyContext.registerHandler(PolicyContextHandlerImpl.SUBJECT, pch, true);
        PolicyContext.registerHandler(PolicyContextHandlerImpl.EJB_ARGUMENTS,
            pch, true);
        /*V3 Commented: PolicyContext.registerHandler(PolicyContextHandlerImpl.SOAP_MESSAGE,
            pch, true);
         */
        PolicyContext.registerHandler(PolicyContextHandlerImpl.HTTP_SERVLET_REQUEST,
            pch, true);
        PolicyContext.registerHandler(PolicyContextHandlerImpl.REUSE, pch, true);
    }

    public Lifecycle getLifecycle() {
        return Lifecycle.START;
    }

    @Override
    public void postConstruct() {
        onInitialization();
        listener = new AuditServerShutdownListener();
        Events events = habitat.getByContract(Events.class);
        events.register(listener);

    }

    @Override
    public void preDestroy() {
        //DO Nothing ?
        //TODO:V3 need to see if something needs cleanup
       
    }
    
    //To audit the server shutdown event
    public class AuditServerShutdownListener implements EventListener {
        @Override
        public void event(Event event) {
            if (EventTypes.SERVER_SHUTDOWN.equals(event.type())) {
                secServUtil.getAuditManager().serverShutdown();
            }
        }
    }
}
