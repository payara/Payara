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
package org.glassfish.security.services.impl.authorization;

import java.net.URI;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.security.AccessController;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.security.Policy;
import java.security.CodeSource;
import java.security.CodeSigner;
import javax.security.auth.Subject;

import org.glassfish.security.services.api.authorization.AuthorizationService;
import org.glassfish.security.services.api.authorization.AzAction;
import org.glassfish.security.services.api.authorization.AzResource;
import org.glassfish.security.services.api.authorization.AzResult;
import org.glassfish.security.services.api.authorization.AzSubject;
import org.glassfish.security.services.api.context.SecurityContextService;
import org.glassfish.security.services.common.PrivilegedLookup;
import org.glassfish.security.services.common.Secure;
import org.glassfish.security.services.config.SecurityConfiguration;
import org.glassfish.security.services.impl.ServiceFactory;
import org.glassfish.security.services.impl.ServiceLogging;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;

import com.sun.enterprise.config.serverbeans.Domain;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.logging.annotation.LogMessageInfo;

import org.glassfish.security.services.api.authorization.*;
import org.glassfish.security.services.api.common.Attributes;
import org.glassfish.security.services.config.SecurityProvider;
import org.glassfish.security.services.spi.authorization.AuthorizationProvider;

/**
 * <code>AuthorizationServiceImpl</code> implements
 * <code>{@link org.glassfish.security.services.api.authorization.AuthorizationService}</code>
 * by delegating authorization decisions to configured
 * <code>{@link org.glassfish.security.services.spi.AuthorizationProvider}</code>
 * instances.
 */
@Service
@Singleton
@Secure(accessPermissionName = "security/service/authorization")
public final class AuthorizationServiceImpl implements AuthorizationService, PostConstruct {
    private static final Level DEBUG_LEVEL = Level.FINER;
    private static final Logger logger =
        Logger.getLogger(ServiceLogging.SEC_SVCS_LOGGER,ServiceLogging.SHARED_LOGMESSAGE_RESOURCE);
    private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(AuthorizationServiceImpl.class);
    @Inject
    private volatile Domain domain;

    @Inject
    private volatile ServiceLocator serviceLocator;
    
    private volatile org.glassfish.security.services.config.AuthorizationService atzSvCfg;

    @Inject
    private volatile SecurityContextService securityContextService;

    private volatile SecurityProvider atzPrvConfig;
    
    private volatile AuthorizationProvider provider;
    
    private static final CodeSource NULL_CODESOURCE = new CodeSource(null, (CodeSigner[])null);

    enum InitializationState {
        NOT_INITIALIZED,
        SUCCESS_INIT,
        FAILED_INIT
    }
    private volatile InitializationState initialized = InitializationState.NOT_INITIALIZED;
    private volatile String reasonInitFailed =
    		localStrings.getLocalString("service.atz.never_init","Authorization Service never initialized.");

    private final List<AzAttributeResolver> attributeResolvers =
            Collections.synchronizedList(new java.util.ArrayList<AzAttributeResolver>());

	private boolean isDebug() {
		return logger.isLoggable(DEBUG_LEVEL);
	}

    /**
     * Initialize the security service instance with the specific security service configuration.
     *
     * @see org.glassfish.security.services.api.SecurityService#initialize
     */
    @Override
    public void initialize(final SecurityConfiguration securityServiceConfiguration) {

        if ( InitializationState.NOT_INITIALIZED != initialized ) {
            return;
        }

        try {
            // Get service level config
            if ( !( securityServiceConfiguration instanceof org.glassfish.security.services.config.AuthorizationService ) ) {
                throw new IllegalStateException(
                		localStrings.getLocalString("service.atz.not_config","The Authorization service is not configured in the domain configuration file."));
            }
            atzSvCfg = (org.glassfish.security.services.config.AuthorizationService) securityServiceConfiguration;

            // Get provider level config, consider only one provider for now and take the first provider found.
            List<SecurityProvider> providersConfig = atzSvCfg.getSecurityProviders();
            if ( (providersConfig == null) || ( (atzPrvConfig = providersConfig.get(0)) == null ) ) {
                throw new IllegalStateException(
                		localStrings.getLocalString("service.atz.no_prov_config","No provider configured for the Authorization service in the domain configuration file."));
            }

            // Get the provider
            final String providerName = atzPrvConfig.getName();
            if ( isDebug() ) {
                logger.log(DEBUG_LEVEL, "Attempting to get Authorization provider \"{0}\".", providerName );
            }
            provider =  AccessController.doPrivileged(
                            new PrivilegedLookup<AuthorizationProvider>(
                                    serviceLocator, AuthorizationProvider.class, providerName)); 
            if (provider == null) {
                throw new IllegalStateException(
                    localStrings.getLocalString("service.atz.not_provider","Authorization Provider {0} not found.", providerName));
            }

            // Initialize the provider
            provider.initialize(atzPrvConfig);

            initialized = InitializationState.SUCCESS_INIT;
            reasonInitFailed = null;

            logger.log(Level.INFO, ATZSVC_INITIALIZED);

        } catch ( Exception e ) {
            String eMsg = e.getMessage();
            String eClass = e.getClass().getName();
            reasonInitFailed = localStrings.getLocalString("service.atz.init_failed",
                "Authorization Service initialization failed, exception {0}, message {1}", eClass, eMsg);
            logger.log(Level.WARNING, ATZSVC_INIT_FAILED, new Object[] {eClass, eMsg});
            throw new RuntimeException( reasonInitFailed, e );
        } finally {
            if ( InitializationState.SUCCESS_INIT != initialized ) {
                initialized = InitializationState.FAILED_INIT;
            }
        }
    }

    /**
     * Determine whether the given Subject has been granted the specified Permission
     * by delegating to the configured java.security.Policy object.  This method is
     * a high-level convenience method that tests for a Subject-based permission
     * grant without reference to the AccessControlContext of the caller.
     *
     * In addition, this method isolates the query from the underlying Policy configuration
     * model.  It could, for example, multiplex queries across multiple instances of Policy
     * configured in an implementation-specific way such that different threads, or different
     * applications, query different Policy objects.  The initial implementation simply
     * delegates to the configured Policy as defined by Java SE.
     *
     * @param subject The Subject for which permission is being tested.
     * @param permission The Permission being queried.
     * @return True or false, depending on whether the specified Permission
     * is granted to the Subject by the configured Policy.
     * @throws IllegalArgumentException Given null or illegal subject or permission
     * @see AuthorizationService#isPermissionGranted(javax.security.auth.Subject, java.security.Permission)
     */
    @Override
	public boolean isPermissionGranted(
        final Subject subject, final Permission permission) {

        // Validate inputs
        if ( null == subject ) {
            throw new IllegalArgumentException(localStrings.getLocalString("service.subject_null", "The supplied Subject is null."));
        }
        if ( null == permission ) {
            throw new IllegalArgumentException(localStrings.getLocalString("service.permission_null","The supplied Permission is null."));
        }

        Set<Principal> principalset = subject.getPrincipals();
        Principal[] principalAr = (principalset.size() == 0) ? null : principalset.toArray(new Principal[principalset.size()]);
        ProtectionDomain pd = new ProtectionDomain(NULL_CODESOURCE, null, null, principalAr); 
        Policy policy = Policy.getPolicy();
        boolean result = policy.implies(pd, permission);

        return result;
	}


    /**
     * Determine whether the given Subject is authorized to access the given resource,
     * specified by a URI.
     *
     * @param subject The Subject being tested.
     * @param resource URI of the resource being tested.
     * @return True or false, depending on whether the access is authorized.
     * @throws IllegalArgumentException Given null or illegal subject or resource
     * @throws IllegalStateException Service was not initialized.
     * @see AuthorizationService#isAuthorized(javax.security.auth.Subject, java.net.URI)
     */
    @Override
	public boolean isAuthorized(Subject subject, URI resource) {
		return isAuthorized(subject, resource, null);
	}


    /**
     * Determine whether the given Subject is authorized to access the given resource,
     * specified by a URI.
     *
     * @param subject The Subject being tested.
     * @param resource URI of the resource being tested.
     * @param action The action, with respect to the resource parameter,
     * for which authorization is desired. To check authorization for all actions,
     * action is represented by null or "*".
     * @return True or false, depending on whether the access is authorized.
     * @throws IllegalArgumentException Given null or illegal subject or resource
     * @throws IllegalStateException Service was not initialized.
     * @see AuthorizationService#isAuthorized(javax.security.auth.Subject, java.net.URI, String)
     */
    @Override
    public boolean isAuthorized(final Subject subject, final URI resource, final String action) {

        checkServiceAvailability();

        // Validate inputs
        if ( null == subject ) {
            throw new IllegalArgumentException(localStrings.getLocalString("service.subject_null", "The supplied Subject is null."));
        }
        if ( null == resource ) {
            throw new IllegalArgumentException(localStrings.getLocalString("service.resource_null", "The supplied Resource is null."));
        }
        // Note: null action means all actions (i.e., no action condition)

        // Convert parameters
        AzSubject azSubject = makeAzSubject( subject );
        AzResource azResource = makeAzResource( resource );
        AzAction azAction = makeAzAction(action);

        AzResult azResult = getAuthorizationDecision(azSubject, azResource, azAction);

        boolean result =
            AzResult.Status.OK.equals(azResult.getStatus()) &&
            AzResult.Decision.PERMIT.equals(azResult.getDecision());
        return result;
	}


    /**
     * The primary authorization method.  The isAuthorized() methods call this method
     * after converting their arguments into the appropriate attribute collection type.
     * It returns a full AzResult, including authorization status, decision, and
     * obligations.
     *
     * This method performs two steps prior to invoking the configured AuthorizationProvider
     * to evaluate the request:  First, it acquires the current AzEnvironment attributes by
     * calling the Security Context service.  Second, it calls the Role Mapping service to
     * determine which roles the subject has, and adds the resulting role attributes into
     * the AzSubject.
     *
     * @param subject The attributes collection representing the Subject for which an authorization
     * decision is requested.
     * @param resource The attributes collection representing the resource for which access is
     * being requested.
     * @param action  The attributes collection representing the action, with respect to the resource,
     * for which access is being requested.  A null action is interpreted as all
     * actions, however all actions may also be represented by the AzAction instance.
     * See <code>{@link org.glassfish.security.services.api.authorization.AzAction}</code>.
     * @return The AzResult indicating the result of the access decision.
     * @throws IllegalArgumentException Given null or illegal subject or resource
     * @throws IllegalStateException Service was not initialized.
     * @see AuthorizationService#getAuthorizationDecision
     */
    @Override
    public AzResult getAuthorizationDecision(
            final AzSubject subject,
            final AzResource resource,
            final AzAction action) {

        checkServiceAvailability();

        // Validate inputs
        if ( null == subject ) {
            throw new IllegalArgumentException(localStrings.getLocalString("service.subject_null", "The supplied Subject is null."));
        }
        if ( null == resource ) {
            throw new IllegalArgumentException(localStrings.getLocalString("service.resource_null", "The supplied Resource is null."));
        }

        // TODO: setup current AzEnvironment instance. Should a null or empty instance to represent current environment?
        final AzEnvironment env = new AzEnvironmentImpl();
        final Attributes attrs = securityContextService.getEnvironmentAttributes();
        for (String attrName : attrs.getAttributeNames()) {
            env.addAttribute(attrName, attrs.getAttributeValue(attrName), true);
        }

        AzResult result =  provider.getAuthorizationDecision(
            subject, resource, action, env, attributeResolvers );

        if ( isDebug() ) {
            logger.log(DEBUG_LEVEL,
            "Authorization Service result for {0} was {1}.",
            new String[]{ subject.toString(), result.toString() } );
        }

        return result;
	}


    /**
     * Convert a Java Subject into a typed attributes collection.
     *
     * @param subject The Subject to convert.
     * @return The resulting AzSubject.
     * @throws IllegalArgumentException Given null or illegal subject
     * @see AuthorizationService#makeAzSubject(javax.security.auth.Subject)
     */
    @Override
    public AzSubject makeAzSubject(final Subject subject) {
        AzSubject azs = new AzSubjectImpl(subject);
        return azs;
    }


    /**
     * Convert a resource, expressed as a URI, into a typed attributes collection.
     * <p>
     * Query parameters in the given URI are appended to this
     * <code>AzResource</code> instance attributes collection.
     *
     * @param resource The URI to convert.
     * @return The resulting AzResource.
     * @see AuthorizationService#makeAzResource(java.net.URI)
     * @throws IllegalArgumentException Given null or illegal resource
     */
    @Override
    public AzResource makeAzResource(final URI resource) {
        AzResource azr = new AzResourceImpl( resource );
        return azr;
    }


    /**
     * Convert an action, expressed as a String, into a typed attributes collection.
     *
     * @param action The action to convert. null or "*" represents all actions.
     * @return The resulting AzAction.
     * @see AuthorizationService#makeAzAction(String)
     */
    @Override
    public AzAction makeAzAction(final String action) {
        AzAction aza = new AzActionImpl( action );
        return aza;
    }


    /**
     * Find an existing PolicyDeploymentContext, or create a new one if one does not
     * already exist for the specified appContext.  The context will be returned in
     * an "open" state, and will stay that way until commit() or delete() is called.
     *
     * @param appContext The application context for which the PolicyDeploymentContext
     * is desired.
     * @return The resulting PolicyDeploymentContext,
     * null if the configured providers do not support this feature.
     * @throws IllegalStateException Service was not initialized.
     * @see AuthorizationService#findOrCreateDeploymentContext(String)
     */
    @Override
    public PolicyDeploymentContext findOrCreateDeploymentContext(
            final String appContext) {

        checkServiceAvailability();

        // TODO: Unsupported Operation Exception undocumented, not optional

        return provider.findOrCreateDeploymentContext(appContext);
	}


    /**
     * Called when the instance has been created and the component is
     * about to be place into commission.
     * <p>
     * The component has been injected with any dependency and
     * will be placed into commission by the subsystem.
     * <p>
     * Hk2 will catch all unchecked exceptions,
     * and will consequently cause the backing inhabitant to be released.
     *
     * @see org.glassfish.hk2.api.PostConstruct#postConstruct()
     */
    @Override
    public void postConstruct() {

        org.glassfish.security.services.config.AuthorizationService atzConfiguration =
                ServiceFactory.getSecurityServiceConfiguration(
                        domain, org.glassfish.security.services.config.AuthorizationService.class);

        initialize(atzConfiguration);
    }


    /**
     * Appends the given <code>{@link org.glassfish.security.services.api.authorization.AzAttributeResolver}</code>
     * instance to the internal ordered list of <code>AzAttributeResolver</code> instances,
     * if not currently in the list based on
     * <code>{@link org.glassfish.security.services.api.authorization.AzAttributeResolver#equals}</code>.
     *
     * @param resolver The <code>AzAttributeResolver</code> instance to append.
     * @return true if the <code>AzAttributeResolver</code> was added,
     * false if the <code>AzAttributeResolver</code> was already in the list.
     * @throws IllegalArgumentException Given AzAttributeResolver was null.
     * @see AuthorizationService#appendAttributeResolver
     */
    @Override
    public boolean appendAttributeResolver(AzAttributeResolver resolver) {
        if ( null == resolver ) {
            throw new IllegalArgumentException(localStrings.getLocalString("service.resolver_null","The supplied Attribute Resolver is null."));
        }
        synchronized ( attributeResolvers ) {
            if ( !attributeResolvers.contains( resolver ) ) {
                attributeResolvers.add( resolver );
                return true;
            }
        }
        return false;
    }


    /**
     * Replaces the internal list of <code>AttributeResolver</code> instances
     * with the given list. If multiple equivalent instances exist in the given list,
     * only the first such instance will be inserted.
     *
     * @param resolverList Replacement list of <code>AzAttributeResolver</code> instances
     * @throws IllegalArgumentException Given AzAttributeResolver list was null.
     * @see AuthorizationService#setAttributeResolvers
     */
    @Override
    public void setAttributeResolvers(List<AzAttributeResolver> resolverList) {
        if ( null == resolverList ) {
            throw new IllegalArgumentException(localStrings.getLocalString("service.resolver_null","The supplied Attribute Resolver is null."));
        }

        synchronized ( attributeResolvers ) {
            attributeResolvers.clear();
            for ( AzAttributeResolver ar : resolverList ) {
                if ( (null != ar) && !attributeResolvers.contains(ar) ) {
                    attributeResolvers.add( ar );
                }
            }
        }
    }


    /**
     * Determines the current list of <code>AttributeResolver</code> instances,
     * in execution order.
     *
     * @return  The current list of AttributeResolver instances,
     * in execution order.
     * @see AuthorizationService#getAttributeResolvers
     */
    @Override
    public List<AzAttributeResolver> getAttributeResolvers() {
        return new ArrayList<AzAttributeResolver>( attributeResolvers );
    }


    /**
     * Removes all <code>AttributeResolver</code> instances from the current
     * internal list of <code>AttributeResolver</code> instances.
     *
     * @return true if any <code>AttributeResolver</code> instances were removed,
     * false if the list was empty.
     * @see AuthorizationService#removeAllAttributeResolvers
     */
    @Override
    public boolean removeAllAttributeResolvers() {
        synchronized ( attributeResolvers ) {
            if ( attributeResolvers.isEmpty() ) {
                return false;
            } else {
                attributeResolvers.clear();
                return true;
            }
        }
    }


    /**
     * Determines whether this service has been initialized.
     * @return The initialization state
     */
    final InitializationState getInitializationState() {
        return initialized;
    }


    /**
     * Determines the reason why the service failed to initialize.
     * @return The reason why the service failed to initialize,
     * null if initialization was successful or the failure reason was unknown.
     */
    final String getReasonInitializationFailed() {
        return reasonInitFailed;
    }


    /**
     * Checks whether this service is available.
     * @throws IllegalStateException This service is not available.
     */
    final void checkServiceAvailability() {
        if ( InitializationState.SUCCESS_INIT != getInitializationState() ) {
            throw new IllegalStateException(
                localStrings.getLocalString("service.atz.not_avail","The Authorization service is not available.") +
                getReasonInitializationFailed() );
        }
    }

	//
	// Log Messages
	//

	@LogMessageInfo(
			message = "Authorization Service has successfully initialized.",
			level = "INFO")
	private static final String ATZSVC_INITIALIZED = "SEC-SVCS-00100";

	@LogMessageInfo(
			message = "Authorization Service initialization failed, exception {0}, message {1}",
			level = "WARNING")
	private static final String ATZSVC_INIT_FAILED = "SEC-SVCS-00101";
}
