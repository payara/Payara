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

import fish.payara.jacc.ContextProvider;
import fish.payara.jacc.JaccConfigurationFactory;
import jakarta.security.jacc.PolicyContext;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import org.glassfish.exousia.modules.locked.SimplePolicyProvider;


/**
 * Implementation of jacc PolicyProvider class
 */
public class PolicyProviderImpl extends SimplePolicyProvider {

    private Policy basePolicy;

    /**
     * Create a new instance of PolicyProviderImpl
     * Delegates to existing policy provider
     */
    public PolicyProviderImpl() {
        basePolicy = Policy.getPolicy();
        if (basePolicy == null) {
            try {
                basePolicy = Policy.getInstance("JavaPolicy", null);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    private static final ThreadLocal<Object> contextProviderReentry = new ThreadLocal<Object>() {
        @Override
        protected Object initialValue() {
            return new byte[]{0};
        }
    };

    @Override
    public boolean implies(ProtectionDomain domain, Permission permission) {

        byte[] alreadyCalled = (byte[]) contextProviderReentry.get();
        if (alreadyCalled[0] == 1) {
            return true;
        }
        alreadyCalled[0] = 1;
        try {
            if (!permission.getClass().getName().startsWith("jakarta.")) {
                return basePolicy.implies(domain, permission);
            }
            String contextId = PolicyContext.getContextID();
            if (contextId != null) {
                ContextProvider contextProvider = getContextProvider(contextId, getPolicyFactory());
                if (contextProvider != null) {
                    return contextProvider.getPolicy().implies(domain, permission);
                }
            }
            return super.implies(domain, permission);
        } finally {
            alreadyCalled[0] = 0;
        }
    }

    // Obtains PolicyConfigurationFactory
    private PolicyConfigurationFactoryImpl getPolicyFactory() {
        return PolicyConfigurationFactoryImpl.getInstance();
    }

    private ContextProvider getContextProvider(String contextId, JaccConfigurationFactory configurationFactory) {
        if (configurationFactory != null && contextId != null) {
            return configurationFactory.getContextProviderByPolicyContextId(contextId);
        }
        return null;
    }
    
}
