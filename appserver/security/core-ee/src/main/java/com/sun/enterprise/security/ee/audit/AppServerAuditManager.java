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

/*
 * AppServerAuditManager.java
 *
 * Created on July 28, 2003, 1:56 PM
 */

package com.sun.enterprise.security.ee.audit;
import java.util.logging.Logger;


import com.sun.appserv.security.AuditModule;
import com.sun.enterprise.security.audit.AuditManager;
import com.sun.enterprise.security.audit.BaseAuditManager;
import com.sun.enterprise.security.BaseAuditModule;
import com.sun.logging.LogDomains;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import org.glassfish.hk2.api.Rank;

/**
 * An EE-specific implementation of the audit manager.
 * <p>
 * This class delegates the nucleus-based work of handling server start-up and
 * shutdown and user authentication to its superclass, adding only the work
 * specific to EE auditing here.
 * 
 * @author  Harpreet Singh
 * @author  Shing Wai Chan
 * @author  tjquinn
 */
@Service
@Singleton
@Rank(20) // so the app server prefers this impl to the non-EE one in nucleus
public final class AppServerAuditManager extends BaseAuditManager<AuditModule> 
        {
    
    private static final String AUDIT_MGR_WS_INVOCATION_KEY = 
        "auditmgr.webServiceInvocation";
    private static final String AUDIT_MGR_EJB_AS_WS_INVOCATION_KEY = 
        "auditmgr.ejbAsWebServiceInvocation";
    
    private static final Logger _logger = 
             LogDomains.getLogger(AppServerAuditManager.class, LogDomains.SECURITY_LOGGER);

    private static final LocalStringManagerImpl _localStrings =
	new LocalStringManagerImpl(AppServerAuditManager.class);

    private List<AuditModule> myAuditModules;
    
    private synchronized List<AuditModule> myAuditModules() {
        if (myAuditModules == null) {
            myAuditModules = instances(AuditModule.class);
        }
        return myAuditModules;
    }
    
    @Override
    public BaseAuditModule addAuditModule(String name, String classname, Properties props) throws Exception {
        final BaseAuditModule am = super.addAuditModule(name, classname, props);
        if (AuditModule.class.isAssignableFrom(am.getClass())) {
            myAuditModules().add((AuditModule) am);
        }
        return am;
    }

    @Override
    public BaseAuditModule removeAuditModule(String name) {
        final BaseAuditModule am = super.removeAuditModule(name);
        if (AuditModule.class.isAssignableFrom(am.getClass())) {
            myAuditModules().remove((AuditModule) am);
        }
        return am;
    }
    
    /**
     * logs the web authorization call for all loaded modules
     * @see com.sun.appserv.security.AuditModule.webInvocation
     */
    public void webInvocation(final String user, final HttpServletRequest req,
        final String type, final boolean success){
        if (auditOn) {
            for (AuditModule am : myAuditModules()) {
                try {
                    am.webInvocation(user, req, type, success);
                } catch (Exception ex) {
                    final String name = moduleName(am);
                    final String msg = 
                        _localStrings.getLocalString(
                            "auditmgr.webinvocation",
                            " Audit Module {0} threw the following exception during web invocation :",
                            name);
                    _logger.log(Level.INFO, msg, ex);
                }
            }
        }
    }

    /**
     * logs the ejb authorization call for all ejb modules
     * @see com.sun.appserv.security.AuditModule.ejbInvocation
     */
    public void ejbInvocation(final String user, final String ejb, final String method, 
            final boolean success){
        if (auditOn) {
            for (AuditModule am : myAuditModules()) {
                try {
                    am.ejbInvocation(user, ejb, method, success);
                } catch (Exception ex) {
                    final String name = moduleName(am);
                    final String msg = 
                        _localStrings.getLocalString(
                            "auditmgr.ejbinvocation",
                            " Audit Module {0} threw the following exception during ejb invocation :",
                            name);
                    _logger.log(Level.INFO, msg, ex);
                }
            }
        }
    }
    
    /**
     * This method is called for the web service calls with MLS set 
     * and the endpoints deployed as servlets  
     * @see com.sun.appserv.security.AuditModule.webServiceInvocation
     */
    public void webServiceInvocation(final String uri, final String endpoint, 
                                     final boolean validRequest){
        if (auditOn) {
            for (AuditModule am : myAuditModules()) {
                try {
                    am.webServiceInvocation(uri, endpoint, validRequest);
                } catch (Exception ex) {
                    final String name = moduleName(am);
                    final String msg = 
                        _localStrings.getLocalString(
                            AUDIT_MGR_WS_INVOCATION_KEY,
                            " Audit Module {0} threw the following exception during web service invocation :",
                            name);
                    _logger.log(Level.INFO, msg, ex);
                }
            }
        }
    }

    /**
     * This method is called for the web service calls with MLS set 
     * and the endpoints deployed as servlets  
     * @see com.sun.appserv.security.AuditModule.webServiceInvocation
     */
    public void ejbAsWebServiceInvocation(final String endpoint, final boolean validRequest){
        if (auditOn) {
            for (AuditModule am : myAuditModules()) {
                try {
                    am.ejbAsWebServiceInvocation(endpoint, validRequest);
                } catch (Exception ex) {
                    final String name = moduleName(am);
                    final String msg = 
                        _localStrings.getLocalString(
                            AUDIT_MGR_EJB_AS_WS_INVOCATION_KEY,
                            " Audit Module {0} threw the following exception during ejb as web service invocation :",
                            name);
                    _logger.log(Level.INFO, msg, ex);
                }
            }
        }
    }
}
