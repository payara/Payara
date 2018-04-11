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

package org.glassfish.webservices.monitoring;

import java.security.Principal;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import org.glassfish.webservices.LogUtils;

/**
 * Log all authentication successes and failures.
 *
 * @author Jerome Dochez
 */
public class LogAuthenticationListener implements AuthenticationListener {
    
    private static final Logger logger = LogUtils.getLogger();
    
    
    /** Creates a new instance of LogAuthenticationListener */
    public LogAuthenticationListener() {
    }
    
    /**
     * notification that a user properly authenticated while making 
     * a web service invocation.
     */
    @Override
    public void authSucess(BundleDescriptor bundleDesc, Endpoint endpoint, Principal principal) {
        if (DOLUtils.ejbType().equals(bundleDesc.getModuleType())) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, LogUtils.AUTHENTICATION_SUCCESS,
                        new Object[] {endpoint.getEndpointSelector(),
                            bundleDesc.getModuleID(), "ejb module"});
            }
        } else {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, LogUtils.AUTHENTICATION_SUCCESS,
                        new Object[] {endpoint.getEndpointSelector(),
                            bundleDesc.getModuleID(), "web app"});
            }
        }
    }
    
    /**
     * notification that a user authentication attempt has failed.
     * @param endpoint the endpoint selector 
     * @param principal Optional principal that failed
     */
    @Override
    public void authFailure(BundleDescriptor bundleDesc, Endpoint endpoint, Principal principal) {
        if (DOLUtils.ejbType().equals(bundleDesc.getModuleType())) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, LogUtils.AUTHENTICATION_FAILURE,
                        new Object[] {endpoint.getEndpointSelector(),
                            bundleDesc.getModuleID(), "ejb module"});
            }
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, LogUtils.AUTHENTICATION_FAILURE,
                        new Object[] {endpoint.getEndpointSelector(),
                            bundleDesc.getModuleID(), "web app"});
            }
        }
    }
}
