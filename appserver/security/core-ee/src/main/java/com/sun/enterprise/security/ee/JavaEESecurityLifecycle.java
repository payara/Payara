/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018-2024] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.ee;

import com.sun.enterprise.security.ContainerSecurityLifecycle;
import com.sun.enterprise.security.ee.authorization.PolicyLoader;
import com.sun.enterprise.security.ee.authentication.jakarta.AuthMessagePolicy;
import com.sun.enterprise.security.ee.authentication.jakarta.ConfigDomainParser;
import com.sun.enterprise.security.ee.authentication.jakarta.WebServicesDelegate;
import com.sun.logging.LogDomains;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.MessagePolicy;
import java.security.Provider;
import java.security.Security;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;
import org.glassfish.common.util.Constants;
import org.glassfish.epicyro.config.factory.file.AuthConfigFileFactory;
import org.glassfish.epicyro.config.module.configprovider.GFServerConfigProvider;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.Rank;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.InitRunLevel;
import org.jvnet.hk2.annotations.Service;

import static jakarta.security.auth.message.config.AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY;
import static org.glassfish.epicyro.config.factory.file.AuthConfigFileFactory.DEFAULT_FACTORY_DEFAULT_PROVIDERS;


/**
 * @author vbkumarjayanti
 * @author David Matejcek
 */
@InitRunLevel
@Rank(Constants.IMPORTANT_RUN_LEVEL_SERVICE)
@Service
@Singleton
public class JavaEESecurityLifecycle implements ContainerSecurityLifecycle, PostConstruct {

    private static final Logger LOG = LogDomains.getLogger(JavaEESecurityLifecycle.class, LogDomains.SECURITY_LOGGER, false);

    @Inject
    PolicyLoader policyLoader;

    @Override
    public void postConstruct() {
        onInitialization();
    }

    @Override
    public void onInitialization() {
        initializeJakartaAuthentication();
        initializeJakartaAuthorization();
    }

    private void initializeJakartaAuthentication() {
        // Define default factory if it is not already defined.
        // The factory will be constructed on first getFactory call.

        String defaultFactory = Security.getProperty(DEFAULT_FACTORY_SECURITY_PROPERTY);
        if (defaultFactory == null) {
            Security.setProperty(DEFAULT_FACTORY_SECURITY_PROPERTY, AuthConfigFileFactory.class.getName());
        }

        String defaultProvidersString = null;
        WebServicesDelegate delegate = Globals.get(WebServicesDelegate.class);
        if (delegate == null) {
            defaultProvidersString = GFServerConfigProvider.class.getName();
        } else {
            // NOTE: Order matters here. Providers for the same auth layer (HttpServlet or SOAP) will be overwritten
            //       by ones that appear later in this string without warning.
            defaultProvidersString = delegate.getDefaultWebServicesProvider() + " " + GFServerConfigProvider.class.getName();
        }

        Security.setProperty(DEFAULT_FACTORY_DEFAULT_PROVIDERS, defaultProvidersString);

        Function<MessageInfo, String> authContextIdGenerator =
                e -> Globals.get(WebServicesDelegate.class).getAuthContextID(e);

        BiFunction<String, Map<String, Object>, MessagePolicy[]> soapPolicyGenerator =
                (authContextId, properties) -> AuthMessagePolicy.getSOAPPolicies(
                        AuthMessagePolicy.getMessageSecurityBinding("SOAP", properties),
                        authContextId, true);

        Provider provider = new Provider("EleosProvider", "1.0", "") {
            private static final long serialVersionUID = 1L;
        };
        provider.put("authContextIdGenerator", authContextIdGenerator);
        provider.put("soapPolicyGenerator", soapPolicyGenerator);

        Security.addProvider(provider);

        System.setProperty("config.parser", ConfigDomainParser.class.getName());
    }

    private void initializeJakartaAuthorization() {
        policyLoader.loadPolicy();
    }
}
