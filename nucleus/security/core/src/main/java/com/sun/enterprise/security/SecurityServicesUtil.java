/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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
 */
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.security.auth.callback.CallbackHandler;

import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.admin.ProcessEnvironment.ProcessType;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.security.audit.AuditManager;

@Service
@Singleton
public class SecurityServicesUtil {

    private static ServiceLocator habitat = Globals.getDefaultHabitat();

    @Inject
    private ProcessEnvironment processEnv;

    @Inject
    private AuditManager auditManager;

    // the appclient CBH
    private CallbackHandler callbackHandler;

    public ServiceLocator getHabitat() {
        return habitat;
    }

    public AuditManager getAuditManager() {
        return auditManager;
    }

    public static SecurityServicesUtil getInstance() {
        if (habitat == null) {
            return null;
        }
        
        return habitat.getService(SecurityServicesUtil.class);
    }

    public ProcessEnvironment getProcessEnv() {
        return processEnv;
    }

    public boolean isACC() {
        return processEnv.getProcessType().equals(ProcessType.ACC);
    }

    public boolean isServer() {
        return processEnv.getProcessType().isServer();
    }

    public boolean isNotServerOrACC() {
        return processEnv.getProcessType().equals(ProcessType.Other);
    }

    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }

    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

}
