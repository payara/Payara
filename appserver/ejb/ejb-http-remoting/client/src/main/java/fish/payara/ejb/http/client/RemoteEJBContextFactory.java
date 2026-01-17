/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.ejb.http.client;

import java.util.Hashtable;

import javax.naming.Context;
import static javax.naming.Context.SECURITY_CREDENTIALS;
import static javax.naming.Context.SECURITY_PRINCIPAL;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * This is the context factory that creates the context used for looking up and invoking
 * remote EJBs.
 * 
 * <p>
 * Clients wanting to use remote EJB in this way should set the property in the initial context
 * environment as follows:
 * 
 * <pre>
 * <code>
 *   Hashtable&lt;String, String&gt; environment = new Hashtable&lt;&gt;();
 *   environment.put(INITIAL_CONTEXT_FACTORY, "fish.payara.ejb.rest.client.RemoteEJBContextFactory");
 *    ...
 *   new InitialContext(environment);
 * <code>
 * </pre>
 * 
 * @author Arjan Tijms
 * @since Payara 5.191
 */
public class RemoteEJBContextFactory implements InitialContextFactory {

    public static final String FACTORY_CLASS = RemoteEJBContextFactory.class.getName();
    public static final String CLIENT_ADAPTER = "fish.payara.clientAdapter";

    public static final String PROVIDER_AUTH_TYPE = "fish.payara.provider.authType";
    public static final String PROVIDER_PRINCIPAL = "fish.payara.provider.principal";
    public static final String PROVIDER_CREDENTIALS = "fish.payara.provider.credentials";

    public static final String JAXRS_CLIENT_REQUEST_FILTER = "fish.payara.requestFilter";
    public static final String JAXRS_CLIENT_RESPONSE_FILTER = "fish.payara.responseFilter";
    public static final String JAXRS_CLIENT_CONFIG = "fish.payara.withConfig";
    public static final String JAXRS_CLIENT_TRUST_STORE = "fish.payara.trustStore";
    public static final String JAXRS_CLIENT_SSL_CONTEXT = "fish.payara.sslContext";
    public static final String JAXRS_CLIENT_SCHEDULED_EXECUTOR_SERVICE = "fish.payara.scheduledExecutorService";
    public static final String JAXRS_CLIENT_READ_TIMEOUT = "fish.payara.readTimeout";
    public static final String JAXRS_CLIENT_KEY_STORE = "fish.payara.keyStore";
    public static final String JAXRS_CLIENT_KEY_STORE_PASSOWRD = "fish.payara.keyStorePassword";
    public static final String JAXRS_CLIENT_HOSTNAME_VERIFIER = "fish.payara.hostnameVerifier";
    public static final String JAXRS_CLIENT_EXECUTOR_SERVICE = "fish.payara.executorService";
    public static final String JAXRS_CLIENT_CONNECT_TIMEOUT = "fish.payara.connectTimeout";
    public static final String JAXRS_CLIENT_SERIALIZATION = "fish.payara.ejb.http.serialization";
    public static final String JAXRS_CLIENT_PROTOCOL_VERSION = "fish.payara.ejb.http.version";

    @Deprecated
    public static final String FISH_PAYARA_WITH_CONFIG = JAXRS_CLIENT_CONFIG;

    @Deprecated
    public static final String FISH_PAYARA_TRUST_STORE = JAXRS_CLIENT_TRUST_STORE;

    @Deprecated
    public static final String FISH_PAYARA_SSL_CONTEXT = JAXRS_CLIENT_SSL_CONTEXT;

    @Deprecated
    public static final String FISH_PAYARA_SCHEDULED_EXECUTOR_SERVICE = JAXRS_CLIENT_SCHEDULED_EXECUTOR_SERVICE;

    @Deprecated
    public static final String FISH_PAYARA_READ_TIMEOUT = JAXRS_CLIENT_READ_TIMEOUT;

    @Deprecated
    public static final String FISH_PAYARA_KEY_STORE = JAXRS_CLIENT_KEY_STORE;

    @Deprecated
    public static final String FISH_PAYARA_HOSTNAME_VERIFIER = JAXRS_CLIENT_HOSTNAME_VERIFIER;

    @Deprecated
    public static final String FISH_PAYARA_EXECUTOR_SERVICE = JAXRS_CLIENT_EXECUTOR_SERVICE;

    @Deprecated
    public static final String FISH_PAYARA_CONNECT_TIMEOUT = JAXRS_CLIENT_CONNECT_TIMEOUT;

    @Deprecated
    public static final String FISH_PAYARA_CLIENT_ADAPTER = CLIENT_ADAPTER;

    /**
     * The keys checked when creating a {@link Context} with {@link #getInitialContext(Hashtable)}. If these are not set
     * in the environment they are initialised to a {@link System#getProperty(String)} if present. 
     * 
     * The name of the property is the same as the environment variable name.
     */
    private String[] SYSTEM_PROPERTY_KEYS = {
            Context.INITIAL_CONTEXT_FACTORY,
            Context.OBJECT_FACTORIES,
            Context.STATE_FACTORIES,
            Context.URL_PKG_PREFIXES,
            Context.PROVIDER_URL,
            Context.DNS_URL,
            Context.AUTHORITATIVE,
            Context.BATCHSIZE,
            Context.REFERRAL,
            Context.SECURITY_PROTOCOL,
            Context.SECURITY_AUTHENTICATION,
            Context.SECURITY_PRINCIPAL,
            Context.SECURITY_CREDENTIALS,
            Context.LANGUAGE,
            JAXRS_CLIENT_CONFIG,
            JAXRS_CLIENT_CONNECT_TIMEOUT,
            JAXRS_CLIENT_EXECUTOR_SERVICE,
            JAXRS_CLIENT_HOSTNAME_VERIFIER,
            JAXRS_CLIENT_KEY_STORE,
            JAXRS_CLIENT_READ_TIMEOUT,
            JAXRS_CLIENT_SCHEDULED_EXECUTOR_SERVICE,
            JAXRS_CLIENT_SSL_CONTEXT,
            JAXRS_CLIENT_TRUST_STORE,
            JAXRS_CLIENT_SERIALIZATION,
            JAXRS_CLIENT_PROTOCOL_VERSION,
    };

    @SuppressWarnings("unchecked")
    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        applySystemPropertiesFallbacks((Hashtable<String, Object>) environment);
        return new RemoteEJBContext((Hashtable<String, Object>) environment);
    }

    private void applySystemPropertiesFallbacks(Hashtable<String, Object> environment) {
        for (String environmentVariable : SYSTEM_PROPERTY_KEYS) {
            if (!environment.containsKey(environmentVariable)) {
                String value = System.getProperty(environmentVariable, null);
                if (value != null) {
                    environment.put(environmentVariable, value);
                }
            }
        }
    }
}
