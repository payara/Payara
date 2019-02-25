/*
 * Copyright (c) 2018 Payara Foundation and/or its affiliates. All rights reserved.
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

import static java.lang.reflect.Proxy.newProxyInstance;
import static java.security.AccessController.doPrivileged;
import static java.util.Collections.emptyList;
import static javax.naming.Context.SECURITY_CREDENTIALS;
import static javax.naming.Context.SECURITY_PRINCIPAL;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.glassfish.jersey.internal.util.ReflectionHelper.getClassLoaderPA;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

public final class EjbRestProxyFactory implements InvocationHandler {

    private final WebTarget target;
    private final MultivaluedMap<String, Object> headers;
    private final List<Cookie> cookies;
    private final String lookup;
    private final Map<String, Object> jndiOptions;

    private static final MultivaluedMap<String, Object> EMPTY_MULTI_MAP = new MultivaluedHashMap<>();

    public static <C> C newProxy(Class<C> remoteBusinessInterface, WebTarget target, String lookup, Map<String, Object> jndiOptions) {
        return newProxy(remoteBusinessInterface, target, EMPTY_MULTI_MAP, emptyList(), lookup, jndiOptions);
    }

    @SuppressWarnings("unchecked")
    public static <C> C newProxy(Class<C> remoteBusinessInterface, WebTarget target, MultivaluedMap<String, Object> headers, List<Cookie> cookies, String lookup, Map<String, Object> jndiOptions) {
        return (C) 
            newProxyInstance(doPrivileged(getClassLoaderPA(remoteBusinessInterface)),
                new Class[] { remoteBusinessInterface },
                new EjbRestProxyFactory(addPathFromClass(remoteBusinessInterface, target), headers, cookies, lookup, jndiOptions));
    }

    private EjbRestProxyFactory(WebTarget target, MultivaluedMap<String, Object> headers, List<Cookie> cookies, String lookup, Map<String, Object> jndiOptions) {
        this.target = target;
        this.headers = headers;
        this.cookies = cookies;
        this.lookup = lookup;
        this.jndiOptions = jndiOptions;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] argValues) throws Throwable {
        
        // Check for methods we should not proxy first
        
        if (argValues == null && method.getName().equals("toString")) {
            return toString();
        }

        if (argValues == null && method.getName().equals("hashCode")) {
            // unique instance in the JVM, and no need to override
            return hashCode();
        }

        if (argValues != null && argValues.length == 1 && method.getName().equals("equals")) {
            // unique instance in the JVM, and no need to override
            return equals(argValues[0]);
        }
        
        // Valid method, do invoke it remotely
        return doRemoteInvoke(proxy, method, argValues);
    }
    
    public Object doRemoteInvoke(Object proxy, Method method, Object[] argValues) throws Throwable {
        // HTTP method name; we're always using POST to invoke remote EJBs
        String httpMethod = POST;
        
        // The bare payload being sent 
        Map<String, Object> payload = new HashMap<>();
        payload.put("lookup", lookup);
        payload.put("method", method.getName());
        payload.put("argTypes", method.getParameterTypes());
        payload.put("argValues", argValues);
        
        if (jndiOptions.containsKey(SECURITY_PRINCIPAL)) {
            payload.put(SECURITY_PRINCIPAL, base64Encode(jndiOptions.get(SECURITY_PRINCIPAL)));
        }
        
        if (jndiOptions.containsKey(SECURITY_CREDENTIALS)) {
            payload.put(SECURITY_CREDENTIALS, base64Encode(jndiOptions.get(SECURITY_CREDENTIALS)));
        }
        
        // Payload wrapped as entity so it'll be encoded in JSON
        Entity<?> entity = Entity.entity(payload, APPLICATION_JSON);
        
        // Response type
        Class<?> responseType = method.getReturnType();
        GenericType<?> responseGenericType = new GenericType<>(method.getGenericReturnType());

        // Create a new UriBuilder appending the name from the method
        WebTarget newTarget = addPathFromMethod(method, target);
        
        // Start request
        Invocation.Builder builder = newTarget.request();
                
        // Set optional headers and cookies    
        builder.headers(new MultivaluedHashMap<String, Object>(this.headers));
        for (Cookie cookie : new LinkedList<>(this.cookies)) {
            builder = builder.cookie(cookie);
        }
        
        // Call remote server
        
        if (responseType.isAssignableFrom(CompletionStage.class)) {
            
            // Reactive call - the actual response type is T from CompletionStage<T>
            return builder.rx().method(httpMethod, entity, getResponseParameterizedType(method, responseGenericType));
        } else if (responseType.isAssignableFrom(Future.class)) {
            
            // Asynchronous call - the actual response type is T from Future<T>
            return builder.async().method(httpMethod, entity, getResponseParameterizedType(method, responseGenericType));
        }
        
        // Synchronous call           
        return builder.method(httpMethod, entity, responseGenericType);
    }
    
    private GenericType<?> getResponseParameterizedType(Method method, GenericType<?> responseGenericType) {
        if (method.getGenericReturnType() instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) method.getGenericReturnType();
            
            return new GenericType<>(parameterizedType.getActualTypeArguments()[0]);
        }
        
        return responseGenericType;
    }
    
    private static String base64Encode(Object input) {
        return Base64.getEncoder().encodeToString(input.toString().getBytes());
    }

    private static WebTarget addPathFromMethod(Method method, WebTarget target) {
        return target.path(method.getName());
    }

    private static WebTarget addPathFromClass(Class<?> clazz, WebTarget target) {
        return target.path(clazz.getSimpleName());
    }

    @Override
    public String toString() {
        return target.toString();
    }

}
