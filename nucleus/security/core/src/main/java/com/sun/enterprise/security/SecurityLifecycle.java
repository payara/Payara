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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security;

import static com.sun.enterprise.security.SecurityLoggerInfo.secServiceStartupEnter;
import static com.sun.enterprise.security.SecurityLoggerInfo.secServiceStartupExit;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.ServerContext;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.security.audit.AuditManager;
import com.sun.enterprise.security.auth.realm.RealmsManager;
import com.sun.enterprise.security.common.Util;
import com.sun.enterprise.security.ssl.SSLUtils;

/**
 * This class extends default implementation of ServerLifecycle interface.
 * It provides security initialization and setup for the server.
 * @author  Shing Wai Chan
 */
@Service
@Singleton
public class SecurityLifecycle implements  PostConstruct, PreDestroy {
    
    @Inject
    private ServerContext sc;
    
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
    
    @Inject
    private ServiceLocator habitat;

    @Inject
    private RealmsManager realmsManager;

    @Inject @Optional
    private ContainerSecurityLifecycle eeSecLifecycle;

    private EventListener listener = null;

    private static final String SYS_PROP_LOGIN_CONF = "java.security.auth.login.config";
    private static final String SYS_PROP_JAVA_SEC_POLICY =  "java.security.policy";
 
    private static final Logger _logger = SecurityLoggerInfo.getLogger();

    public SecurityLifecycle() {
        try {

            if (Util.isEmbeddedServer()) {
                // If the user-defined login.conf/server.policy are set as system properties, then they are given priority
                if (System.getProperty(SYS_PROP_LOGIN_CONF) == null) {
                    System.setProperty(SYS_PROP_LOGIN_CONF, Util.writeConfigFileToTempDir("login.conf").getAbsolutePath());
                }
                if (System.getProperty(SYS_PROP_JAVA_SEC_POLICY) == null) {
                    System.setProperty(SYS_PROP_JAVA_SEC_POLICY, Util.writeConfigFileToTempDir("server.policy").getAbsolutePath());
                }
            }

            // Security manager is set here so that it can be accessed from
            // other lifecycles, like PEWebContainer
            java.lang.SecurityManager secMgr = System.getSecurityManager();
            if (_logger.isLoggable(INFO)) {
                if (secMgr != null) {
                    _logger.info(SecurityLoggerInfo.secMgrEnabled);
                } else {
                    _logger.info(SecurityLoggerInfo.secMgrDisabled);
                }
            }
        } catch (Exception ex) {
            _logger.log(SEVERE, "java_security.init_securitylifecycle_fail", ex);
            throw new RuntimeException(ex.toString(), ex);
        }
    }  

    // override default
    public void onInitialization() {
        try {
             if (_logger.isLoggable(INFO)) {
                 _logger.log(INFO, secServiceStartupEnter);
             }

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
            // initRoleMapperFactory();
           
           if (_logger.isLoggable(INFO)) {
                 _logger.log(INFO, secServiceStartupExit);
             }

        } catch(Exception ex) {
            throw new SecurityLifecycleException(ex);
        }
    }

    @Override
    public void postConstruct() {
        onInitialization();
        listener = new AuditServerShutdownListener();
        Events events = habitat.getService(Events.class);
        events.register(listener);

    }

    @Override
    public void preDestroy() {
    }
    
    // To audit the server shutdown event
    public class AuditServerShutdownListener implements EventListener {
        @Override
        public void event(Event event) {
            if (EventTypes.SERVER_SHUTDOWN.equals(event.type())) {
                secServUtil.getAuditManager().serverShutdown();
            }
        }
    }
}
