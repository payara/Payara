/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.config.serverbeans.SecureAdminPrincipal;
import java.net.URI;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.logging.annotation.LogMessageInfo;

import org.glassfish.security.services.api.authorization.*;
import org.glassfish.security.services.api.authorization.AzResult.Decision;
import org.glassfish.security.services.api.authorization.AzResult.Status;
import org.glassfish.security.services.api.authorization.AuthorizationService.PolicyDeploymentContext;
import org.glassfish.security.services.api.authorization.AuthorizationAdminConstants;
import org.glassfish.security.services.common.Secure;
import org.glassfish.security.services.config.SecurityProvider;
import org.glassfish.security.services.spi.authorization.AuthorizationProvider;
import org.glassfish.security.services.impl.ServiceLogging;
import org.glassfish.security.services.impl.authorization.AzResultImpl;
import org.glassfish.security.services.impl.authorization.AzObligationsImpl;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

@Service (name="simpleAuthorization")
@PerLookup
@Secure(accessPermissionName = "security/service/authorization/provider/simple")
public class SimpleAuthorizationProviderImpl implements AuthorizationProvider{

    private final static Level DEBUG_LEVEL = Level.FINER;
    private static final Logger _logger =
        Logger.getLogger(ServiceLogging.SEC_PROV_LOGGER,ServiceLogging.SHARED_LOGMESSAGE_RESOURCE);
    
    private AuthorizationProviderConfig cfg; 
    private boolean deployable;
    private String version;
    
    @Inject
    private ServerEnvironment serverEnv;
    
    @Inject
    private ServiceLocator serviceLocator;
    
    private Domain domain = null;
    
    private SecureAdmin secureAdmin = null;
    
    private Decider decider;
    
    @Override
    public void initialize(SecurityProvider providerConfig) {
                
        cfg = (AuthorizationProviderConfig)providerConfig.getSecurityProviderConfig().get(0);        
        deployable = cfg.getSupportPolicyDeploy();
        version = cfg.getVersion();
        domain = serviceLocator.getService(Domain.class);
        secureAdmin = domain.getSecureAdmin();
        if (isDebug()) {
            _logger.log(DEBUG_LEVEL, "provide to do policy deploy: " + deployable);
            _logger.log(DEBUG_LEVEL, "provide version to use: " + version);
        }
    }
    
    protected Decider createDecider() {
        return new Decider();
    }
    
    private synchronized Decider getDecider() {
        if (decider == null) {
            decider = createDecider();
            if (isDebug()) {
                _logger.log(DEBUG_LEVEL, "Created SimpleAuthorizationProviderImpl Decider of type {0}", decider.getClass().getName());
            }
        }
        return decider;
    }

    private boolean isDebug() {
        return _logger.isLoggable(DEBUG_LEVEL);
    }
    
    @Override
    public AzResult getAuthorizationDecision(
        AzSubject subject,
        AzResource resource,
        AzAction action,
        AzEnvironment environment,
        List<AzAttributeResolver> attributeResolvers ) {

        //TODO: get user roles from Rolemapper, and do the policy  evaluation
        if ( ! isAdminResource(resource)) {
            /*
             * Log a loud warning if the resource name lacks the correct
             * scheme, but go ahead anyway and make the authorization decision.
             */
            final String resourceName = resource.getUri() == null ? "null" : resource.getUri().toASCIIString();
            _logger.log(Level.WARNING, ATZPROV_BAD_RESOURCE, resourceName);
            _logger.log(Level.WARNING, "IllegalArgumentException", new IllegalArgumentException(resourceName));
        }
        return getAdminDecision(subject, resource, action, environment);
    }
    
    private boolean isAdminResource(final AzResource resource) {
        final URI resourceURI = resource.getUri();
        return "admin".equals(resourceURI.getScheme());
    }
    
    private AzResult getAdminDecision(
            final AzSubject subject,
            final AzResource resource,
            final AzAction action,
            final AzEnvironment environment) {
        if (isDebug()) {
            _logger.log(DEBUG_LEVEL, "");
        }
        AzResult rtn = new AzResultImpl(getDecider().decide(subject, resource, action, environment), 
                Status.OK, new AzObligationsImpl());
        
        return rtn;
    }

    @Override
    public PolicyDeploymentContext findOrCreateDeploymentContext(String appContext) {

        return null;
    }
    
    /**
     * Chooses what authorization decision to render.
     * 
     * We always require that the user be an administrator, established 
     * (for open-source) by having a Principal with name asadmin.
     * 
     * Beyond that, there are historical requirements for authenticated admin access:
     *  
     * - "External" users (CLI, browser, JMX)
     *   - can perform all actions locally on the DAS
     *   - can perform all actions remotely on the DAS if secure admin has been enabled [1]
     *   - JMX users can perform read-only actions on a non-DAS instance,
     *     remotely if secure admin has been enabled and always locally
     * 
     * - Selected local commands can act locally on the local DAS or local instance
     *   using the local password mechanism (stop-local-instance, for example)
     * 
     * - A server in the same domain can perform all actions in a local or remote server
     * 
     * - A client (typically run in a shell created by the DAS) can perform all actions 
     *   on a local or remote DAS if it uses the admin token mechanism to authenticate
     * 
     * [1] Note that any attempted remote access that is not permitted has 
     * already been rejected during authentication.
     * 
     * For enforcing read-only access we assume that any action other than the literal "read"
     * makes some change in the system.
     */
    protected class Decider {
        
        protected Decision decide(final AzSubject subject, final AzResource resource,
                final AzAction action, final AzEnvironment env) {
            /*
             * Basically, if the subject has one of the "special" principals
             * (token, local password, etc.) then we accept it for any action
             * on the DAS and on instances.  Otherwise, it's a person and
             * we allow full access on the DAS but read-only on instances.
             */
            Decision result = 
                    isSubjectTrustedForDASAndInstances(subject)
                   
                    || // Looks external.  Allow full access on DAS, read-only on instance.
                   
                    (isSubjectAnAdministrator(subject)
                    && ( serverEnv.isDas()
                        || isActionRead(action)
                       )
                   ) ? Decision.PERMIT : Decision.DENY;
            
            return result;
        }
        
        protected String getAdminGroupName() {
            return AuthorizationAdminConstants.ADMIN_GROUP;
        }
        private boolean isSubjectTrustedForDASAndInstances(final AzSubject subject) {
            final Set<String> principalNames = new HashSet<String>();
            for (Principal p : subject.getSubject().getPrincipals()) {
                principalNames.add(p.getName());
            }
            principalNames.retainAll(AuthorizationAdminConstants.TRUSTED_FOR_DAS_OR_INSTANCE);
            return ! principalNames.isEmpty();
        }
        
        private boolean isActionRead(final AzAction action) {
            return "read".equals(action.getAction());
        }

        private boolean isSubjectAnAdministrator(final AzSubject subject) {
            return isPrincipalType(subject, getAdminGroupName()) ||
                    hasSecureAdminPrincipal(subject);
        }

        private boolean isPrincipalType(final AzSubject subject, final String type) {
            for (Principal p : subject.getSubject().getPrincipals()) {
                if (type.equals(p.getName())) {
                    return true;
                }
            }
            return false;
        }
        
        private boolean hasSecureAdminPrincipal(final AzSubject subject) {
            /*
             * If the subject has a principal with a name that matches a
             * name in the secure admin principals then it's most likely the
             * DAS sending a request to an instance, in which case it's
             * an administrator.
             */
            if (secureAdmin == null) {
                return false;
            }
            for (Principal p : subject.getSubject().getPrincipals()) {
                for (SecureAdminPrincipal sap : secureAdmin.getSecureAdminPrincipal()) {
                    if (sap.getDn().equals(p.getName())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

	@LogMessageInfo(
			message = "Authorization Provider supplied an invalid resource: {0}",
			level = "WARNING")
	private static final String ATZPROV_BAD_RESOURCE = "SEC-PROV-00100";
}
