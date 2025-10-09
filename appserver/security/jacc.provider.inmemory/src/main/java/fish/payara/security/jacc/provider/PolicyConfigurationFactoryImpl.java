/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2022] Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.security.jacc.provider;

import static com.sun.logging.LogDomains.SECURITY_LOGGER;
import fish.payara.jacc.ContextProvider;
import fish.payara.jacc.JaccConfigurationFactory;
import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyContextException;
import java.security.Permission;
import java.security.Policy;
import java.security.SecurityPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static java.util.logging.Level.FINE;
import java.util.logging.Logger;
import org.glassfish.exousia.modules.locked.SimplePolicyConfigurationFactory;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

/**
 * Implementation of jacc PolicyConfigurationFactory class
 */
@Service
@ContractsProvided({ PolicyConfigurationFactoryImpl.class, PolicyConfigurationFactory.class })
public class PolicyConfigurationFactoryImpl extends SimplePolicyConfigurationFactory implements JaccConfigurationFactory {

    
    private static Logger logger = Logger.getLogger(SECURITY_LOGGER);
    private Map<String, String> applicationToPolicyContextIdMap = new ConcurrentHashMap<String, String>();

    // Map of ContextId -> ContextProvider (per context PolicyConfigurationFactory and Policy)
    private Map<String, ContextProvider> contextToContextProviderMap = new ConcurrentHashMap<>();
    
    
    // Map of ContextId -> PolicyConfiguration
    private Map<String, PolicyConfiguration> contextToConfigurationMap = new ConcurrentHashMap<>();
    
    private Permission setPolicyPermission;
    
    private static PolicyConfigurationFactoryImpl singleton;

    public PolicyConfigurationFactoryImpl() {
        singleton = this;
    }
    
    static PolicyConfigurationFactoryImpl getInstance() {
        return singleton;
    }
    
    @Override
    public void registerContextProvider(String applicationContextId, PolicyConfigurationFactory factory, Policy policy) {
        checkSetPolicyPermission();
        
        try {
            String policyContextId = applicationToPolicyContextIdMap.get(applicationContextId);
            if (policyContextId == null) {
                throw new IllegalStateException(
                        "No policyContextId available for applicationContextId " + applicationContextId + 
                        " Is this JaccConfigurationFactory instance used by the container?");
            }
            
            if (inService(policyContextId)) {
                throw new IllegalStateException("Context :" + policyContextId + " already has an active global provider");
            }
        
            ContextProvider contextProvider = contextToContextProviderMap.get(policyContextId);
            if (contextProvider != null && contextProvider.getPolicyConfigurationFactory().inService(policyContextId)) {
                throw new IllegalStateException("Context :" + policyContextId + " already has an active context (per app) provider");
            }
            
            contextToContextProviderMap.put(policyContextId, new ContextProviderImpl(factory, policy));
            
        } catch (PolicyContextException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void addContextIdMapping(String applicationContextId, String policyContextId) {
         applicationToPolicyContextIdMap.put(applicationContextId, policyContextId);
    }

    @Override
    public boolean removeContextIdMappingByPolicyContextId(String policyContextId) {
        return applicationToPolicyContextIdMap.entrySet().removeIf(e -> e.getValue().equals(policyContextId));
    }
    
    @Override
    public ContextProvider getContextProviderByPolicyContextId(String policyContextId) {
        return contextToContextProviderMap.get(policyContextId);
    }

    @Override
    public ContextProvider removeContextProviderByPolicyContextId(String policyContextId) {
          return contextToContextProviderMap.remove(policyContextId);
    }
    
    protected List<PolicyConfiguration> getPolicyConfigurations() {
        return new ArrayList<>(contextToConfigurationMap.values());
    }

    protected PolicyConfiguration removePolicyConfiguration(String contextID) {
        return contextToConfigurationMap.remove(contextID);
    }

    
    /**
     * This method is used to obtain an instance of the provider specific class that implements the PolicyConfiguration
     * interface that corresponds to the identified policy context within the provider. The methods of the
     * PolicyConfiguration interface are used to define the policy statements of the identified policy context.
     * <P>
     * If at the time of the call, the identified policy context does not exist in the provider, then the policy context
     * will be created in the provider and the Object that implements the context's PolicyConfiguration Interface will be
     * returned. If the state of the identified context is "deleted" or "inService" it will be transitioned to the "open"
     * state as a result of the call. The states in the lifecycle of a policy context are defined by the PolicyConfiguration
     * interface.
     * <P>
     * For a given value of policy context identifier, this method must always return the same instance of
     * PolicyConfiguration and there must be at most one actual instance of a PolicyConfiguration with a given policy
     * context identifier (during a process context).
     * <P>
     * To preserve the invariant that there be at most one PolicyConfiguration object for a given policy context, it may be
     * necessary for this method to be thread safe.
     * <P>
     * 
     * @param contextID A String identifying the policy context whose PolicyConfiguration interface is to be returned. The
     * value passed to this parameter must not be null.
     * <P>
     * @param remove A boolean value that establishes whether or not the policy statements of an existing policy context are
     * to be removed before its PolicyConfiguration object is returned. If the value passed to this parameter is true, the
     * policy statements of an existing policy context will be removed. If the value is false, they will not be removed.
     *
     * @return an Object that implements the PolicyConfiguration Interface matched to the Policy provider and corresponding
     * to the identified policy context.
     *
     * @throws java.lang.SecurityException when called by an AccessControlContext that has not been granted the "setPolicy"
     * SecurityPermission.
     *
     * @throws javax.security.jacc.PolicyContextException if the implementation throws a checked exception that has not been
     * accounted for by the getPolicyConfiguration method signature. The exception thrown by the implementation class will
     * be encapsulated (during construction) in the thrown PolicyContextException.
     */
    @Override
    public PolicyConfiguration getPolicyConfiguration(String contextId, boolean remove) throws PolicyContextException {
        checkSetPolicyPermission();

        if (logger.isLoggable(FINE)) {
            logger.fine("JACC Policy Provider: Getting PolicyConfiguration object with id = " + contextId);
        }

        ContextProvider contextProvider = contextToContextProviderMap.get(contextId);

        if (contextProvider != null) {
            return contextProvider.getPolicyConfigurationFactory().getPolicyConfiguration(contextId, remove);
        }

        PolicyConfiguration policyConfiguration = super.getPolicyConfiguration(contextId, remove);
        contextToConfigurationMap.put(contextId, policyConfiguration);
        return policyConfiguration;
    }
        
    /**
     * This method determines if the identified policy context exists with state "inService" in the Policy provider
     * associated with the factory.
     * <P>
     * 
     * @param contextID A string identifying a policy context
     *
     * @return true if the identified policy context exists within the provider and its state is "inService", false
     * otherwise.
     *
     * @throws java.lang.SecurityException when called by an AccessControlContext that has not been granted the "setPolicy"
     * SecurityPermission.
     *
     * @throws javax.security.jacc.PolicyContextException if the implementation throws a checked exception that has not been
     * accounted for by the inService method signature. The exception thrown by the implementation class will be
     * encapsulated (during construction) in the thrown PolicyContextException.
     */
    @Override
    public boolean inService(String contextId) throws PolicyContextException {
        checkSetPolicyPermission();
        
        ContextProvider contextProvider = contextToContextProviderMap.get(contextId);
        
        if (contextProvider != null) {
            return contextProvider.getPolicyConfigurationFactory().inService(contextId);
        }
        
        return super.inService(contextId);
        
    }

    protected List<PolicyConfiguration> getPolicyConfigurationImpls() {
        return new ArrayList<>(contextToConfigurationMap.values());
    }
    
    protected PolicyConfiguration removePolicyConfigurationImpl(String contextID) {
        return contextToConfigurationMap.remove(contextID);
    }
    
    protected void checkSetPolicyPermission() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            if (setPolicyPermission == null) {
                setPolicyPermission = new SecurityPermission("setPolicy");
            }
            securityManager.checkPermission(setPolicyPermission);
        }
    }

}
