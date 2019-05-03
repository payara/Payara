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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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

    public static final String FISH_PAYARA_WITH_CONFIG = "fish.payara.withConfig";
    public static final String FISH_PAYARA_TRUST_STORE = "fish.payara.trustStore";
    public static final String FISH_PAYARA_SSL_CONTEXT = "fish.payara.sslContext";
    public static final String FISH_PAYARA_SCHEDULED_EXECUTOR_SERVICE = "fish.payara.scheduledExecutorService";
    public static final String FISH_PAYARA_READ_TIMEOUT = "fish.payara.readTimeout";
    public static final String FISH_PAYARA_KEY_STORE = "fish.payara.keyStore";
    public static final String FISH_PAYARA_HOSTNAME_VERIFIER = "fish.payara.hostnameVerifier";
    public static final String FISH_PAYARA_EXECUTOR_SERVICE = "fish.payara.executorService";
    public static final String FISH_PAYARA_CONNECT_TIMEOUT = "fish.payara.connectTimeout";

    /**
     * The keys checked when creating a {@link Context} with {@link #getInitialContext(Hashtable)}. If these are not set
     * in the environment we they are initialised to a {@link System#getProperty(String)}. The name of the property is
     * the same except {@code fish.parara.}/{@code java.naming.} is replaced with {@code fish.payara.ejb.http.client}
     * effectively looking for properties with names like {@code fish.payara.ejb.http.client.sslContext} or
     * {@code fish.payara.ejb.http.client.provider.url}.
     */
    private String[] SYSTEM_PROPERTY_KEYS = { Context.PROVIDER_URL, Context.SECURITY_CREDENTIALS,
            Context.SECURITY_PRINCIPAL, FISH_PAYARA_CONNECT_TIMEOUT, FISH_PAYARA_EXECUTOR_SERVICE,
            FISH_PAYARA_HOSTNAME_VERIFIER, FISH_PAYARA_KEY_STORE, FISH_PAYARA_READ_TIMEOUT,
            FISH_PAYARA_SCHEDULED_EXECUTOR_SERVICE, FISH_PAYARA_SSL_CONTEXT, FISH_PAYARA_TRUST_STORE,
            FISH_PAYARA_WITH_CONFIG };

    @SuppressWarnings("unchecked")
    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        updateEnvironmentFromSystemProperties((Hashtable<String, Object>) environment);
        return new RemoteEJBContext(environment);
    }

    private void updateEnvironmentFromSystemProperties(Hashtable<String, Object> environment) {
        for (String key : SYSTEM_PROPERTY_KEYS) {
            if (!environment.containsKey(key)) {
                String systemPropertyName = key.replaceFirst("fish\\.payara\\.|java\\.naming\\.", "fish.payara.ejb.http.client.");
                System.out.println("Checking "+systemPropertyName);
                String value = System.getProperty(systemPropertyName, null);
                if (value != null) {
                    environment.put(key, value);
                }
            }
        }
    }
}
