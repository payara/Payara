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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
 */

// Portions Copyright [2016-2021] [Payara Foundation]

package com.sun.enterprise.security.ee.audit;

import static java.util.logging.Level.INFO;

import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;

import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.Service;

import com.sun.appserv.security.AuditModule;
import com.sun.enterprise.security.BaseAuditModule;
import com.sun.enterprise.security.audit.BaseAuditManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.LogDomains;

/**
 * An EE-specific implementation of the audit manager.
 * <p>
 * This class delegates the nucleus-based work of handling server start-up and shutdown and user authentication to its
 * superclass, adding only the work specific to EE auditing here.
 *
 * @author Harpreet Singh
 * @author Shing Wai Chan
 * @author tjquinn
 */
@Service
@Singleton
@Rank(20) // so the app server prefers this impl to the non-EE one in nucleus
public final class AppServerAuditManager extends BaseAuditManager<AuditModule> {

    private static final Logger _logger = LogDomains.getLogger(AppServerAuditManager.class, LogDomains.SECURITY_LOGGER, false);
    private static final LocalStringManagerImpl _localStrings = new LocalStringManagerImpl(AppServerAuditManager.class);
    
    private static final String AUDIT_MGR_WS_INVOCATION_KEY = "auditmgr.webServiceInvocation";
    private static final String AUDIT_MGR_EJB_AS_WS_INVOCATION_KEY = "auditmgr.ejbAsWebServiceInvocation";
   
    private List<AuditModule> myAuditModules;

    @Override
    public BaseAuditModule addAuditModule(String name, String classname, Properties props) throws Exception {
        BaseAuditModule auditModule = super.addAuditModule(name, classname, props);
        if (AuditModule.class.isAssignableFrom(auditModule.getClass())) {
            myAuditModules().add((AuditModule) auditModule);
        }
        
        return auditModule;
    }

    @Override
    public BaseAuditModule removeAuditModule(String name) {
        BaseAuditModule auditModule = super.removeAuditModule(name);
        if (AuditModule.class.isAssignableFrom(auditModule.getClass())) {
            myAuditModules().remove((AuditModule) auditModule);
        }
        
        return auditModule;
    }

    /**
     * logs the web authorization call for all loaded modules
     * 
     * @see com.sun.appserv.security.AuditModule.webInvocation
     */
    public void webInvocation(String user, HttpServletRequest request, String type, boolean success) {
        if (auditOn) {
            for (AuditModule auditModule : myAuditModules()) {
                try {
                    auditModule.webInvocation(user, request, type, success);
                } catch (Exception ex) {
                    _logger.log(INFO, 
                        _localStrings.getLocalString(
                            "auditmgr.webinvocation",
                            " Audit Module {0} threw the following exception during web invocation :", 
                            moduleName(auditModule)), ex);
                }
            }
        }
    }

    /**
     * logs the ejb authorization call for all ejb modules
     * 
     * @see com.sun.appserv.security.AuditModule.ejbInvocation
     */
    public void ejbInvocation(String user, String ejb, String method, boolean success) {
        if (auditOn) {
            for (AuditModule auditModule : myAuditModules()) {
                try {
                    auditModule.ejbInvocation(user, ejb, method, success);
                } catch (Exception ex) {
                    _logger.log(INFO, _localStrings.getLocalString("auditmgr.ejbinvocation",
                            " Audit Module {0} threw the following exception during ejb invocation :", moduleName(auditModule)), ex);
                }
            }
        }
    }

    /**
     * This method is called for the web service calls with MLS set and the endpoints deployed as servlets
     * 
     * @see com.sun.appserv.security.AuditModule.webServiceInvocation
     */
    public void webServiceInvocation(String uri, String endpoint, boolean validRequest) {
        if (auditOn) {
            for (AuditModule auditModule : myAuditModules()) {
                try {
                    auditModule.webServiceInvocation(uri, endpoint, validRequest);
                } catch (Exception ex) {
                    final String name = moduleName(auditModule);
                    final String msg = _localStrings.getLocalString(AUDIT_MGR_WS_INVOCATION_KEY,
                            " Audit Module {0} threw the following exception during web service invocation :", name);
                    _logger.log(INFO, msg, ex);
                }
            }
        }
    }

    /**
     * This method is called for the web service calls with MLS set and the endpoints deployed as servlets
     * 
     * @see com.sun.appserv.security.AuditModule.webServiceInvocation
     */
    public void ejbAsWebServiceInvocation(String endpoint, boolean validRequest) {
        if (auditOn) {
            for (AuditModule auditModule : myAuditModules()) {
                try {
                    auditModule.ejbAsWebServiceInvocation(endpoint, validRequest);
                } catch (Exception ex) {
                    final String name = moduleName(auditModule);
                    final String msg = _localStrings.getLocalString(AUDIT_MGR_EJB_AS_WS_INVOCATION_KEY,
                            " Audit Module {0} threw the following exception during ejb as web service invocation :", name);
                    _logger.log(INFO, msg, ex);
                }
            }
        }
    }
    
    private synchronized List<AuditModule> myAuditModules() {
        if (myAuditModules == null) {
            myAuditModules = instances(AuditModule.class);
        }
        
        return myAuditModules;
    }
}
