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
package org.glassfish.security.services.impl.authorization;

import java.net.URI;
import java.security.Permission;
import java.util.Set;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.security.Policy;
import java.security.CodeSource;
import java.security.CodeSigner;
import javax.security.auth.Subject;

import org.glassfish.internal.api.ServerContext;
import org.glassfish.security.services.api.authorization.AuthorizationService;
import org.glassfish.security.services.api.authorization.AzAction;
import org.glassfish.security.services.api.authorization.AzResource;
import org.glassfish.security.services.api.authorization.AzResult;
import org.glassfish.security.services.api.authorization.AzSubject;
import org.glassfish.security.services.config.SecurityConfiguration;
import org.glassfish.security.services.impl.ServiceFactory;


//import org.jvnet.hk2.annotations.Inject;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.BaseServiceLocator;
import org.glassfish.hk2.api.PostConstruct;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.logging.LogDomains;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.security.services.api.authorization.*;
import org.glassfish.security.services.api.authorization.AzResult.Decision;
import org.glassfish.security.services.api.authorization.AzResult.Status;

import org.glassfish.security.services.config.SecurityProvider;
import org.glassfish.security.services.spi.AuthorizationProvider;


@Service
@Singleton
public class AuthorizationServiceImpl implements AuthorizationService, PostConstruct {

    protected static final Logger _logger = 
        LogDomains.getLogger(AuthorizationServiceImpl.class, LogDomains.SECURITY_LOGGER);

    @Inject
    private Domain domain;

    //TODO: do context switch
    @Inject
    ServerContext serverContext;

    @Inject
    private BaseServiceLocator serviceLocator;
    
    private org.glassfish.security.services.config.AuthorizationService atzSvCfg;
    
    private SecurityProvider atzPrvConfig;
    
    private AuthorizationProvider atzProvider;
    
    private static final CodeSource NULL_CODESOURCE = new CodeSource(null, (CodeSigner[])null);
    
    @Override
    public void initialize(SecurityConfiguration securityServiceConfiguration) {
                
        //get service level config
        atzSvCfg = (org.glassfish.security.services.config.AuthorizationService) securityServiceConfiguration;

        if (atzSvCfg == null) {
            atzProvider = createDefaultProvider();
            _logger.log(Level.WARNING, "The Authorization service is not configured in the domain configuration file; using a trivial default implementation which authorizes all access");
        } else {
        
            //get provider level config
            //consider only one provider for now
            atzPrvConfig = atzSvCfg.getSecurityProviders().get(0);

            if (atzPrvConfig == null)
                throw new RuntimeException("No provider  configured for the Authorization service in the domain configuration file");

                //get the provider
                atzProvider = serviceLocator.getComponent(AuthorizationProvider.class, atzPrvConfig.getName());
        }
        
        //init the provider  -- use the first config under the provider config???
        atzProvider.initialize(atzPrvConfig);
    }
    
    @Override
	public boolean isPermissionGranted(Subject subject, Permission permission) {
        
        Set<Principal> principalset = subject.getPrincipals();
        Principal[] principalAr = (principalset.size() == 0) ? null : principalset.toArray(new Principal[principalset.size()]);
        ProtectionDomain pd = new ProtectionDomain(NULL_CODESOURCE, null, null, principalAr); 
        Policy policy = Policy.getPolicy();
        boolean result = policy.implies(pd, permission);
        
        return result;
	}

    @Override
	public boolean isAuthorized(Subject subject, URI resource) {
		return isAuthorized(subject, resource, "*");
	}

    @Override
	public boolean isAuthorized(Subject subject, URI resource, String action) {
	    AzResult azResult = 
	        getAuthorizationDecision(makeAzSubject(subject), makeAzResource(resource), makeAzAction(action));
		
	    boolean result = false;
	    	    
	    if ( (AzResult.Decision.PERMIT.equals(azResult.getDecision())) &&
	         (AzResult.Status.OK.equals(azResult.getStatus())) ) 
	        result = true;
	    
	    return result;
	}

    @Override
	public AzResult getAuthorizationDecision(AzSubject subject,
			AzResource resource, AzAction action) {
        //TODO: setup current AzEnvironment instance. Should a null or empty instance to represent current environment?
		return atzProvider.getAuthorizationDecision(subject, resource, action, new AzEnvironmentImpl());
	}

    @Override
	public AzSubject makeAzSubject(Subject subject) {

	    if (subject == null)
	        return null;
	    
	    AzSubject azs = new AzSubjectImpl();

	    Set<Principal> principals = subject.getPrincipals();

	    String AttName = Principal.class.getSimpleName();
	    for (Principal p : principals) {
	        String pname = p.getName();
	        azs.addAttribute(AttName, pname, false);
	    }
	    
		return azs;
	}

    @Override
	public AzResource makeAzResource(URI resource) {
	    
	    if (resource == null)
	        return null;
	    
	    String attName = URI.class.getSimpleName();
	    
	    AzResource azr = new AzResourceImpl();
	    azr.addAttribute(attName, resource.toString(), false);
	    
		return azr;
	}

    @Override
	public AzAction makeAzAction(String action) {
	    if (action == null)
	        return null;
	    
	    AzAction aza = new AzActionImpl();
	    
	    aza.addAttribute("ACTION", action, false);
		return aza;
	}

    @Override
	public PolicyDeploymentContext findOrCreateDeploymentContext(
			String appContext) {
		return atzProvider.findOrCreateDeployContext(appContext);
	}

   @Override
    public void postConstruct() {
            
        org.glassfish.security.services.config.AuthorizationService atzConfiguration =
           ServiceFactory.getSecurityServiceConfiguration(
                   domain, org.glassfish.security.services.config.AuthorizationService.class);
       
        initialize(atzConfiguration);
    }
   
    private AuthorizationProvider createDefaultProvider() {
        return new AuthorizationProvider() {

            @Override
            public AzResult getAuthorizationDecision(AzSubject subject, AzResource resource, AzAction action, AzEnvironment environment) {
                return new AzResultImpl(Decision.PERMIT, Status.OK, new AzObligationsImpl());
            }

            @Override
            public PolicyDeploymentContext findOrCreateDeployContext(String appContext) {
                return null;
            }

            @Override
            public void initialize(SecurityProvider providerConfig) {
            }
        };
   }
}
