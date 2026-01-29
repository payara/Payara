/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.ejb.http.client.adapter.ClientAdapter;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Configuration;
import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.util.Hashtable;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;
import static fish.payara.ejb.http.client.RemoteEJBContextFactory.*;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import java.util.logging.Level;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.Form;
import static jakarta.ws.rs.core.SecurityContext.BASIC_AUTH;
import static jakarta.ws.rs.core.SecurityContext.DIGEST_AUTH;
import static jakarta.ws.rs.core.SecurityContext.FORM_AUTH;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

/**
 * This is the context used for looking up and invoking remote EJBs via
 * REST in Payara 5.191+.
 * 
 * <p>
 * Note that at the moment only the <code>lookup</code> methods are implemented.
 * 
 * <p>
 * This context supports the following Payara specific properties in its environment:
 * 
 * <ul>
 *     <li>fish.payara.connectTimeout</li>
 *     <li>fish.payara.executorService</li>
 *     <li>fish.payara.hostnameVerifier</li>
 *     <li>fish.payara.keyStore</li>
 *     <li>fish.payara.readTimeout</li>
 *     <li>fish.payara.hostnameVerifier</li>
 *     <li>fish.payara.scheduledExecutorService"</li>
 *     <li>fish.payara.sslContext</li>
 *     <li>fish.payara.trustStore</li>
 *     <li>fish.payara.withConfig</li>
 * </ul>
 * 
 * All properties corresponds to the similarly named settings on the JAX-RS {@link ClientBuilder}.
 * Times are in microseconds, and values can either be given as Strings, number values or object instances.
 * 
 * @author Arjan Tijms
 * @since Payara 5.191
 *
 */
class RemoteEJBContext implements Context {

    private static final Logger logger = Logger.getLogger(LookupV0.class.getName());

    private ClientAdapter clientAdapter;
    private Hashtable<String, Object> environment;
    private Lookup lookup;

    public RemoteEJBContext(Hashtable<String, Object> environment) {
        this.environment = environment;
        if (environment.containsKey(RemoteEJBContextFactory.CLIENT_ADAPTER)) {
            Object adapter =  environment.get(RemoteEJBContextFactory.CLIENT_ADAPTER);
            if (adapter instanceof ClientAdapter) {
                this.clientAdapter = (ClientAdapter)adapter;
            }
        }
    }

    @Override
    public Object lookup(Name name) throws NamingException {
        return lookup(name.toString());
    }

    @Override
    public Object lookup(String name) throws NamingException {
        if (name == null) {
            throw new NullPointerException("Lookup name cannot be null");
        }
        String url = (String) environment.get(PROVIDER_URL);
        try {
            if (clientAdapter != null) {
                Optional<Object> resolvedAdapter = clientAdapter.makeLocalProxy(name, this);
                if (resolvedAdapter.isPresent()) {
                    return resolvedAdapter.get();
                }
            }
            // Get client build with all optional config applied
            URI remotePayaraURI = new URL(url).toURI();

            return lookup(remotePayaraURI).lookup(name);
        } catch (NamingException ne) {
            throw ne;
        } catch (Exception e) {
            throw newNamingException(name, e);
        }
    }



    /**
     * Lazily initialize lookup strategy fit for required serialization and server-side supported protocol.
     * @return
     */
    private synchronized Lookup lookup(URI root) throws NamingException {
        if (lookup == null) {
            lookup = determineLookup(root);
        }
        return lookup;
    }

    /**
     * Determine supported protocol on server side and construct corresponding Lookup instance.
     */
    private Lookup determineLookup(URI root) throws NamingException {
        try {
            Client client = getClientBuilder().build();

            LookupDiscoveryResponse discovery = new LookupDiscoveryServiceImpl().discover(client, root);

            Integer version = getVersion(discovery);
            if (discovery.isV1target() && (version == null || version.intValue() == 1)) {
                logger.log(Level.INFO, "Discovered v1 at {0}", discovery.getV1lookup().getUri());
                return new LookupV1(environment, client, discovery.getV1lookup());
            }
            if (discovery.isV0target() && (version == null || version.intValue() == 0)) {
                logger.log(Level.INFO, "Discovered v0 at {0}", discovery.getV0lookup().getUri());
                return new LookupV0(environment, client.target(discovery.getResolvedRoot()), discovery.getV0lookup());
            }
            throw new NamingException("EJB HTTP client V0 is not supported, out of ideas for now");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw newNamingException("Cannot access lookup endpoint at "+root, e);
        }
    }

    private Integer getVersion(LookupDiscoveryResponse discovery) {
        Object versionSetting = environment.get(RemoteEJBContextFactory.JAXRS_CLIENT_PROTOCOL_VERSION);
        if (versionSetting == null) {
            return null;
        }
        int version = versionSetting instanceof String
                ? Integer.parseInt(versionSetting.toString())
                : ((Number) versionSetting).intValue();
        return version == 1 && discovery.isV1target()
                || version == 0 && discovery.isV0target() ?  version : null;
    }

    private ClientBuilder getClientBuilder() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        ClientBuilder clientBuilder = ClientBuilder.newBuilder();

        if (environment.containsKey(JAXRS_CLIENT_CONNECT_TIMEOUT)) {
            clientBuilder.connectTimeout(getLong(environment.get(JAXRS_CLIENT_CONNECT_TIMEOUT)), MICROSECONDS);
        }

        if (environment.containsKey(JAXRS_CLIENT_EXECUTOR_SERVICE)) {
            clientBuilder.executorService(getInstance(environment.get(JAXRS_CLIENT_EXECUTOR_SERVICE), ExecutorService.class));
        }

        if (environment.containsKey(JAXRS_CLIENT_HOSTNAME_VERIFIER)) {
            clientBuilder.hostnameVerifier(getInstance(environment.get(JAXRS_CLIENT_HOSTNAME_VERIFIER), HostnameVerifier.class));
        }

        if (environment.containsKey(JAXRS_CLIENT_KEY_STORE)) {
            clientBuilder.keyStore(
                    getInstance(environment.get(JAXRS_CLIENT_KEY_STORE), KeyStore.class),
                    getPassword(environment.containsKey("keyStorePassword") ? environment.get("keyStorePassword") : environment.get(JAXRS_CLIENT_KEY_STORE_PASSOWRD))
            );
        }

        if (environment.containsKey(JAXRS_CLIENT_READ_TIMEOUT)) {
            clientBuilder.readTimeout(getLong(environment.get(JAXRS_CLIENT_READ_TIMEOUT)), MICROSECONDS);
        }

        if (environment.containsKey(JAXRS_CLIENT_SCHEDULED_EXECUTOR_SERVICE)) {
            clientBuilder.scheduledExecutorService(getInstance(environment.get(JAXRS_CLIENT_SCHEDULED_EXECUTOR_SERVICE), ScheduledExecutorService.class));
        }

        if (environment.containsKey(JAXRS_CLIENT_SSL_CONTEXT)) {
            clientBuilder.sslContext(getInstance(environment.get(JAXRS_CLIENT_SSL_CONTEXT), SSLContext.class));
        }

        if (environment.containsKey(JAXRS_CLIENT_TRUST_STORE)) {
            clientBuilder.trustStore(getInstance(environment.get(JAXRS_CLIENT_TRUST_STORE), KeyStore.class));
        }

        if (environment.containsKey(JAXRS_CLIENT_CONFIG)) {
            clientBuilder.withConfig(getInstance(environment.get(JAXRS_CLIENT_CONFIG), Configuration.class));
        }

        if (environment.containsKey(JAXRS_CLIENT_REQUEST_FILTER)) {
            Object value = environment.get(JAXRS_CLIENT_REQUEST_FILTER);
            if (value instanceof Class
                    && (ClientRequestFilter.class.isAssignableFrom((Class) value))) {
                clientBuilder.register((Class<ClientRequestFilter>) value);
            } else if (value instanceof ClientRequestFilter) {
                clientBuilder.register((ClientRequestFilter) value);
            }
        }

        if (environment.containsKey(JAXRS_CLIENT_RESPONSE_FILTER)) {
            Object value = environment.get(JAXRS_CLIENT_RESPONSE_FILTER);
            if (value instanceof Class
                    && (ClientResponseFilter.class.isAssignableFrom((Class) value))) {
                clientBuilder.register((Class<ClientResponseFilter>) value);
            } else if (value instanceof ClientResponseFilter) {
                clientBuilder.register((ClientResponseFilter) value);
            }
        }

        String providerPrincipal = (String)environment.get(PROVIDER_PRINCIPAL);
        String providerCredentials = (String)environment.get(PROVIDER_CREDENTIALS);
        if (providerPrincipal != null && providerCredentials != null) {
            String authType = (String)environment.get(PROVIDER_AUTH_TYPE);
            if (null == authType || BASIC_AUTH.equals(authType)) {
                clientBuilder.register(HttpAuthenticationFeature.basic(providerPrincipal, providerCredentials));
            } else if (DIGEST_AUTH.equals(authType)) {
                clientBuilder.register(HttpAuthenticationFeature.digest(providerPrincipal, providerCredentials));
            }
        }
        return clientBuilder;
    }

    private static NamingException newNamingException(String name, Exception cause) {
        NamingException namingException = new NamingException("Could not lookup: " + name);
        namingException.initCause(cause);

        return namingException;
    }

    private static <T> T getInstance(Object value, Class<T> clazz) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }

        if (value instanceof String) {
            return clazz.cast(Class.forName((String)value).newInstance());
        }

        throw new IllegalStateException("Value " + value + " has to be of type String or " + clazz);
    }

    private static Long getLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        if (value instanceof String) {
            return Long.valueOf((String) value);
        }

        throw new IllegalStateException("Value " + value + " has to be of type String or Number");
    }

    private static char[] getPassword(Object value) {
        if (value instanceof String) {
            return ((String) value).toCharArray();
        }

        if (value instanceof char[]) {
            return (char[]) value;
        } 
        throw new IllegalArgumentException("No password provided");

    }

    @Override
    public void bind(Name name, Object obj) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void bind(String name, Object obj) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rebind(Name name, Object obj) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rebind(String name, Object obj) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unbind(Name name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unbind(String name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rename(Name oldName, Name newName) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rename(String oldName, String newName) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroySubcontext(Name name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroySubcontext(String name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Context createSubcontext(Name name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Context createSubcontext(String name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object lookupLink(Name name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object lookupLink(String name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NameParser getNameParser(String name) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String composeName(String name, String prefix) throws NamingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        return environment.put(propName, propVal);
    }

    @Override
    public Object removeFromEnvironment(String propName) throws NamingException {
        return environment.remove(propName);
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return environment;
    }

    @Override
    public void close() throws NamingException {
        environment.clear();
    }

    @Override
    public String getNameInNamespace() throws NamingException {
        throw new UnsupportedOperationException();
    }

}
