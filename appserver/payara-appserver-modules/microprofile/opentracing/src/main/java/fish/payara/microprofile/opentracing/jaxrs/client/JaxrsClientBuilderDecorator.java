/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/master/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */
package fish.payara.microprofile.opentracing.jaxrs.client;

import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.opentracing.OpenTracingService;
import fish.payara.requesttracing.jaxrs.client.JaxrsClientRequestTracingFilter;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;

import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.jersey.client.Initializable;
import org.glassfish.jersey.client.JerseyClientBuilder;

/**
 * Decorator for the default JerseyClientBuilder class to allow us to add our ClientFilter and instrument asynchronous
 * clients.
 * 
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
class JaxrsClientBuilderDecorator extends ClientBuilder {

    public static final String EARLY_BUILDER_INIT = "fish.payara.requesttracing.jaxrs.client.decorators.EarlyBuilderInit";

    protected ClientBuilder clientBuilder;
    
    /**
     * Initialises a new JerseyClientBuilder and sets it as the decorated object.
     * @param clientBuilder
     */
    private JaxrsClientBuilderDecorator(ClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    public static ClientBuilder wrap(ClientBuilder clientBuilder) {
        if (clientBuilder instanceof JaxrsClientBuilderDecorator) {
            return clientBuilder;
        } else {
            return new JaxrsClientBuilderDecorator(clientBuilder);
        }
    }

    @Override
    public ClientBuilder withConfig(Configuration config) {
        clientBuilder = clientBuilder.withConfig(config);
        return this;
    }

    @Override
    public ClientBuilder sslContext(SSLContext sslContext) {
        clientBuilder = clientBuilder.sslContext(sslContext);
        return this;
    }

    @Override
    public ClientBuilder keyStore(KeyStore keyStore, char[] password) {
        clientBuilder = clientBuilder.keyStore(keyStore, password);
        return this;
    }

    @Override
    public ClientBuilder trustStore(KeyStore trustStore) {
        clientBuilder = clientBuilder.trustStore(trustStore);
        return this;
    }

    @Override
    public ClientBuilder hostnameVerifier(HostnameVerifier verifier) {
        clientBuilder = clientBuilder.hostnameVerifier(verifier);
        return this;
    }

    @Override
    public ClientBuilder executorService(ExecutorService executorService) {
        clientBuilder = clientBuilder.executorService(executorService);
        return this;
    }

    @Override
    public ClientBuilder scheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        clientBuilder = clientBuilder.scheduledExecutorService(scheduledExecutorService);
        return this;
    }

    @Override
    public ClientBuilder connectTimeout(long timeout, TimeUnit unit) {
        clientBuilder = clientBuilder.connectTimeout(timeout, unit);
        return this;
    }

    @Override
    public ClientBuilder readTimeout(long timeout, TimeUnit unit) {
        clientBuilder = clientBuilder.readTimeout(timeout, unit);
        return this;
    }

    @Override
    public Client build() {
        if (!requestTracingPresent()) {
            return clientBuilder.build();
        }

        // Build and return a decorated client
        Client client = this.clientBuilder.build();

        // initialize the client if requested
        Object earlyInit = getConfiguration().getProperty(EARLY_BUILDER_INIT);
        if (earlyInit instanceof Boolean && (Boolean)earlyInit) {
            if (client instanceof Initializable) {
                ((Initializable) client).preInitialize();
            }
        }

        return new JaxrsClientDecorator(client);
    }

    private boolean requestTracingPresent() {
        try {
            ServiceLocator serviceLocator = Globals.getDefaultBaseServiceLocator();
            if (serviceLocator != null) {
                ServiceHandle<RequestTracingService> requestTracingHandle = serviceLocator.getServiceHandle(RequestTracingService.class);
                ServiceHandle<OpenTracingService> openTracingHandle = serviceLocator.getServiceHandle(OpenTracingService.class);
                return requestTracingHandle != null && openTracingHandle != null
                        && requestTracingHandle.isActive() && openTracingHandle.isActive();
            }
        } catch (Exception e) {
            // means that we likely cannot do request tracing anyway
        }
        return false;
    }

    @Override
    public Configuration getConfiguration() {
        return this.clientBuilder.getConfiguration();
    }

    @Override
    public ClientBuilder property(String name, Object value) {
        clientBuilder = clientBuilder.property(name, value);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass) {
        clientBuilder = clientBuilder.register(componentClass);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, int priority) {
        clientBuilder = clientBuilder.register(componentClass, priority);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, Class<?>... contracts) {
        clientBuilder = clientBuilder.register(componentClass, contracts);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        clientBuilder = clientBuilder.register(componentClass, contracts);
        return this;
    }

    @Override
    public ClientBuilder register(Object component) {
        clientBuilder = clientBuilder.register(component);
        return this;
    }

    @Override
    public ClientBuilder register(Object component, int priority) {
        clientBuilder = clientBuilder.register(component, priority);
        return this;
    }

    @Override
    public ClientBuilder register(Object component, Class<?>... contracts) {
        clientBuilder = clientBuilder.register(component, contracts);
        return this;
    }

    @Override
    public ClientBuilder register(Object component, Map<Class<?>, Integer> contracts) {
        clientBuilder = clientBuilder.register(component, contracts);
        return this;
    }
    
}
