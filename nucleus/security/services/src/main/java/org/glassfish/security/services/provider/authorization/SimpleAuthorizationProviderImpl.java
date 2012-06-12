/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.security.services.provider.authorization;

import org.glassfish.security.services.api.authorization.AzAction;
import org.glassfish.security.services.api.authorization.AzEnvironment;
import org.glassfish.security.services.api.authorization.AzObligations;
import org.glassfish.security.services.api.authorization.AzResource;
import org.glassfish.security.services.api.authorization.AzResult;
import org.glassfish.security.services.api.authorization.AzResult.Decision;
import org.glassfish.security.services.api.authorization.AzResult.Status;
import org.glassfish.security.services.api.authorization.AzSubject;
import org.glassfish.security.services.api.authorization.AuthorizationService.PolicyDeploymentContext;

import org.glassfish.security.services.config.SecurityProvider;
import org.glassfish.security.services.spi.AuthorizationProvider;
import org.glassfish.security.services.impl.authorization.AzResultImpl;
import org.glassfish.security.services.impl.authorization.AzObligationsImpl;

import org.glassfish.hk2.api.PerLookup;

import org.jvnet.hk2.annotations.Service;



@Service (name="Simple Authorization Provider")
@PerLookup
public class SimpleAuthorizationProviderImpl implements AuthorizationProvider {

    
    private AuthorizationProviderConfig cfg; 
    private boolean deployable;
    private String version;
    
    @Override
    public void initialize(SecurityProvider providerConfig) {

        cfg = (AuthorizationProviderConfig)providerConfig;
        deployable = cfg.getSupportPolicyDeploy();
        version = cfg.getVersion();
    }

    @Override
    public AzResult getAuthorizationDecision(AzSubject subject,
            AzResource resource, AzAction action, AzEnvironment environment) {

        //TODO: get user roles from Rolemapper, and do the policy  evaluation
        //return ok for now
        AzResult rtn = new AzResultImpl(Decision.PERMIT, Status.OK, new AzObligationsImpl());
        
        return rtn;
    }

    @Override
    public PolicyDeploymentContext findOrCreateDeployContext(String appContext) {

        return null;
    }
    
}
