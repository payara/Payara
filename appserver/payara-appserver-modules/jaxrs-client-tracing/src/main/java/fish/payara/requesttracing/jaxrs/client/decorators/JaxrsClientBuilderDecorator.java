/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.requesttracing.jaxrs.client.decorators;

import fish.payara.requesttracing.jaxrs.client.JaxrsClientRequestTracingFilter;
import java.security.KeyStore;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;
import org.glassfish.jersey.client.JerseyClientBuilder;

/**
 * Decorator for the default JerseyClientBuilder class to allow us to add our ClientFilter and instrument asynchronous
 * clients.
 * 
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
public class JaxrsClientBuilderDecorator extends ClientBuilder {

    protected ClientBuilder clientBuilder;
    
    /**
     * Initialises a new JerseyClientBuilder and sets it as the decorated object.
     */
    public JaxrsClientBuilderDecorator() {
        this.clientBuilder = new JerseyClientBuilder();
    }
    
    @Override
    public ClientBuilder withConfig(Configuration config) {
        return this.clientBuilder.withConfig(config);
    }

    @Override
    public ClientBuilder sslContext(SSLContext sslContext) {
        return this.clientBuilder.sslContext(sslContext);
    }

    @Override
    public ClientBuilder keyStore(KeyStore keyStore, char[] password) {
        return this.clientBuilder.keyStore(keyStore, password);
    }

    @Override
    public ClientBuilder trustStore(KeyStore trustStore) {
        return this.clientBuilder.trustStore(trustStore);
    }

    @Override
    public ClientBuilder hostnameVerifier(HostnameVerifier verifier) {
        return this.clientBuilder.hostnameVerifier(verifier);
    }

    @Override
    public Client build() {
        // Register the Request Tracing filter
        this.register(JaxrsClientRequestTracingFilter.class);

        // Build and return a decorated client
        return new JaxrsClientDecorator(this.clientBuilder.build());
    }

    @Override
    public Configuration getConfiguration() {
        return this.clientBuilder.getConfiguration();
    }

    @Override
    public ClientBuilder property(String name, Object value) {
        return this.clientBuilder.property(name, value);
    }

    @Override
    public ClientBuilder register(Class<?> componentClass) {
        return this.clientBuilder.register(componentClass);
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, int priority) {
        return this.clientBuilder.register(componentClass, priority);
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, Class<?>... contracts) {
        return this.clientBuilder.register(componentClass, contracts);
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        return this.clientBuilder.register(componentClass, contracts);
    }

    @Override
    public ClientBuilder register(Object component) {
        return this.clientBuilder.register(component);
    }

    @Override
    public ClientBuilder register(Object component, int priority) {
        return this.clientBuilder.register(component, priority);
    }

    @Override
    public ClientBuilder register(Object component, Class<?>... contracts) {
        return this.clientBuilder.register(component, contracts);
    }

    @Override
    public ClientBuilder register(Object component, Map<Class<?>, Integer> contracts) {
        return this.clientBuilder.register(component, contracts);
    }
    
}
