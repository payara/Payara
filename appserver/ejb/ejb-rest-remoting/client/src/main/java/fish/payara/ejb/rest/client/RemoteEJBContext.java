/*
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package fish.payara.ejb.rest.client;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Configuration;

public class RemoteEJBContext implements Context {
    
    private Hashtable<String, Object> environment;
    
    @SuppressWarnings("unchecked")
    public RemoteEJBContext(Hashtable<?, ?> environment) {
        this.environment = (Hashtable<String, Object>) environment;
    }

    @Override
    public Object lookup(Name name) throws NamingException {
        return lookup(name.toString());
    }

    @Override
    public Object lookup(String name) throws NamingException {
        String url = (String) environment.get(PROVIDER_URL);
        
        try {
            // Get client build with all optional config applied
            ClientBuilder clientBuilder = getClientBuilder();
            
            // For the lookup do a call to the remote server first to obtain
            // the remote business interface class name given a JNDI lookup name.
            // The JNDI lookup name normally does not give us this interface name.
            // This also allows us to check the JNDI name indeed resolves before
            // we create a proxy and return it here.
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("lookup", name);
            
            URI remotePayaraURI = new URL(url).toURI();
            
            String className = 
                clientBuilder
                    .build()
                    .target(remotePayaraURI)
                    .path("ejb")
                    .path("lookup")
                    .request()
                    .post(Entity.entity(payload, APPLICATION_JSON))
                    .readEntity(String.class);
            
            // After we have obtained the class name of the remote interface, generate
            // a proxy based on it.
            
            return EjbRestProxyFactory.newProxy(
                    Class.forName(className), 
                    clientBuilder.build()
                                 .target(remotePayaraURI)
                                 .path("ejb")
                                 .path("invoke"), 
                    name,
                    new HashMap<String, Object>(environment)
                    );
            
        } catch (Exception e) {
            throw newNamingException(name, e);
        }
    }
    
    private ClientBuilder getClientBuilder() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        
        if (environment.containsKey("connectTimeout")) {
            clientBuilder.connectTimeout(getLong(environment.get("connectTimeout")).longValue(), MICROSECONDS);
        }
        
        if (environment.contains("executorService")) {
            clientBuilder.executorService(getInstance(environment.get("executorService"), ExecutorService.class));
        }
        
        if (environment.contains("hostnameVerifier")) {
            clientBuilder.hostnameVerifier(getInstance(environment.get("hostnameVerifier"), HostnameVerifier.class));
        }
        
        if (environment.contains("keyStore")) {
            clientBuilder.keyStore(getInstance(environment.get("keyStore"), KeyStore.class), getPassword(environment.get("keyStorePassword")));
        }
        
        if (environment.containsKey("readTimeout")) {
            clientBuilder.readTimeout(getLong(environment.get("readTimeout")).longValue(), MICROSECONDS);
        }
        
        if (environment.contains("hostnameVerifier")) {
            clientBuilder.hostnameVerifier(getInstance(environment.get("hostnameVerifier"), HostnameVerifier.class));
        }
        
        if (environment.contains("scheduledExecutorService")) {
            clientBuilder.scheduledExecutorService(getInstance(environment.get("scheduledExecutorService"), ScheduledExecutorService.class));
        }
        
        if (environment.contains("scheduledExecutorService")) {
            clientBuilder.scheduledExecutorService(getInstance(environment.get("scheduledExecutorService"), ScheduledExecutorService.class));
        }
        
        if (environment.contains("sslContext")) {
            clientBuilder.sslContext(getInstance(environment.get("sslContext"), SSLContext.class));
        }
        
        if (environment.contains("trustStore")) {
            clientBuilder.trustStore(getInstance(environment.get("trustStore"), KeyStore.class));
        }
        
        if (environment.contains("withConfig")) {
            clientBuilder.withConfig(getInstance(environment.get("withConfig"), Configuration.class));
        }
        
        return clientBuilder;
    }
    
    private NamingException newNamingException(String name, Exception cause) {
        NamingException namingException = new NamingException("Could not lookup :" + name);
        namingException.initCause(cause);
        
        return namingException;
    }
    
    private <T> T getInstance(Object value, Class<T> clazz) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        
        if (value instanceof String) {
            return clazz.cast(Class.forName((String)value).newInstance());
        }
        
        throw new IllegalStateException("Value " + value + " has to be of type String or " + clazz);
    }
    
    private Long getLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        
        if (value instanceof String) {
            return Long.valueOf((String) value);
        }
        
        throw new IllegalStateException("Value " + value + " has to be of type String or Number");
    }
    
    private char[] getPassword(Object value) {
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
        
    }

    @Override
    public void bind(String name, Object obj) throws NamingException {
        
    }

    @Override
    public void rebind(Name name, Object obj) throws NamingException {
        
    }

    @Override
    public void rebind(String name, Object obj) throws NamingException {
        
    }

    @Override
    public void unbind(Name name) throws NamingException {
        
    }

    @Override
    public void unbind(String name) throws NamingException {
        
    }

    @Override
    public void rename(Name oldName, Name newName) throws NamingException {
        
    }

    @Override
    public void rename(String oldName, String newName) throws NamingException {
        
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        return null;
    }

    @Override
    public void destroySubcontext(Name name) throws NamingException {
        
    }

    @Override
    public void destroySubcontext(String name) throws NamingException {
        
    }

    @Override
    public Context createSubcontext(Name name) throws NamingException {
        return null;
    }

    @Override
    public Context createSubcontext(String name) throws NamingException {
        return null;
    }

    @Override
    public Object lookupLink(Name name) throws NamingException {
        return null;
    }

    @Override
    public Object lookupLink(String name) throws NamingException {
        return null;
    }

    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        return null;
    }

    @Override
    public NameParser getNameParser(String name) throws NamingException {
        return null;
    }

    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        return null;
    }

    @Override
    public String composeName(String name, String prefix) throws NamingException {
        return null;
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
        return null;
    }

}
