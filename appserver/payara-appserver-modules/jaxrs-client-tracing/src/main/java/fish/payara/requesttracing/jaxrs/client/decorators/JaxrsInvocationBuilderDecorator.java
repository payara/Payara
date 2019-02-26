/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018-2019] Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.nucleus.requesttracing.domain.PropagationHeaders;
import fish.payara.opentracing.OpenTracingService;
import io.opentracing.Span;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Locale;

/**
 * Decorator class used for instrumenting asynchronous clients.
 * 
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
public class JaxrsInvocationBuilderDecorator implements Invocation.Builder {

    protected Invocation.Builder invocationBuilder;

    private ServiceLocator serviceLocator;
    private OpenTracingService openTracing;

    public JaxrsInvocationBuilderDecorator(Invocation.Builder invocationBuilder) {
        this.invocationBuilder = invocationBuilder;

        // Get the ServiceLocator and OpenTracing services
        serviceLocator = Globals.getDefaultBaseServiceLocator();
        openTracing = serviceLocator.getService(OpenTracingService.class);
    }

    @Override
    public Invocation build(String method) {
        return this.invocationBuilder.build(method);
    }

    @Override
    public Invocation build(String method, Entity<?> entity) {
        return this.invocationBuilder.build(method, entity);
    }

    @Override
    public Invocation buildGet() {
        return this.invocationBuilder.buildGet();
    }

    @Override
    public Invocation buildDelete() {
        return this.invocationBuilder.buildDelete();
    }

    @Override
    public Invocation buildPost(Entity<?> entity) {
        return this.invocationBuilder.buildPost(entity);
    }

    @Override
    public Invocation buildPut(Entity<?> entity) {
        return this.invocationBuilder.buildPut(entity);
    }

    @Override
    public AsyncInvoker async() {
        // Instrument invocation builder with OpenTracing
        instrumentInvocationBuilder();
        
        // Return the asynchronous invoker - end of the decorated chain.
        return this.invocationBuilder.async();
    }

    @Override
    public Invocation.Builder accept(String... mediaTypes) {
        invocationBuilder = invocationBuilder.accept(mediaTypes);
        return this;
    }

    @Override
    public Invocation.Builder accept(MediaType... mediaTypes) {
        invocationBuilder = invocationBuilder.accept(mediaTypes);
        return this;
    }

    @Override
    public Invocation.Builder acceptLanguage(Locale... locales) {
        invocationBuilder = invocationBuilder.acceptLanguage(locales);
        return this;
    }

    @Override
    public Invocation.Builder acceptLanguage(String... locales) {
        invocationBuilder = invocationBuilder.acceptLanguage(locales);
        return this;
    }

    @Override
    public Invocation.Builder acceptEncoding(String... encodings) {
        invocationBuilder = invocationBuilder.acceptEncoding(encodings);
        return this;
    }

    @Override
    public Invocation.Builder cookie(Cookie cookie) {
        invocationBuilder = invocationBuilder.cookie(cookie);
        return this;
    }

    @Override
    public Invocation.Builder cookie(String name, String value) {
        invocationBuilder = invocationBuilder.cookie(name, value);
        return this;
    }

    @Override
    public Invocation.Builder cacheControl(CacheControl cacheControl) {
        invocationBuilder = invocationBuilder.cacheControl(cacheControl);
        return this;
    }

    @Override
    public Invocation.Builder header(String name, Object value) {
        invocationBuilder = invocationBuilder.header(name, value);
        return this;
    }

    @Override
    public Invocation.Builder headers(MultivaluedMap<String, Object> headers) {
        invocationBuilder = invocationBuilder.headers(headers);
        return this;
    }

    @Override
    public Invocation.Builder property(String name, Object value) {
        invocationBuilder = invocationBuilder.property(name, value);
        return this;
    }

    @Override
    public Response get() {
        return this.invocationBuilder.get();
    }

    @Override
    public <T> T get(Class<T> responseType) {
        return this.invocationBuilder.get(responseType);
    }

    @Override
    public <T> T get(GenericType<T> responseType) {
        return this.invocationBuilder.get(responseType);
    }

    @Override
    public Response put(Entity<?> entity) {
        return this.invocationBuilder.put(entity);
    }

    @Override
    public <T> T put(Entity<?> entity, Class<T> responseType) {
        return this.invocationBuilder.put(entity, responseType);
    }

    @Override
    public <T> T put(Entity<?> entity, GenericType<T> responseType) {
        return this.invocationBuilder.put(entity, responseType);
    }

    @Override
    public Response post(Entity<?> entity) {
        return this.invocationBuilder.post(entity);
    }

    @Override
    public <T> T post(Entity<?> entity, Class<T> responseType) {
        return this.invocationBuilder.post(entity, responseType);
    }

    @Override
    public <T> T post(Entity<?> entity, GenericType<T> responseType) {
        return this.invocationBuilder.post(entity, responseType);
    }

    @Override
    public Response delete() {
        return this.invocationBuilder.delete();
    }

    @Override
    public <T> T delete(Class<T> responseType) {
        return this.invocationBuilder.delete(responseType);
    }

    @Override
    public <T> T delete(GenericType<T> responseType) {
        return this.invocationBuilder.delete(responseType);
    }

    @Override
    public Response head() {
        return this.invocationBuilder.head();
    }

    @Override
    public Response options() {
        return this.invocationBuilder.options();
    }

    @Override
    public <T> T options(Class<T> responseType) {
        return this.invocationBuilder.options(responseType);
    }

    @Override
    public <T> T options(GenericType<T> responseType) {
        return this.invocationBuilder.options(responseType);
    }

    @Override
    public Response trace() {
        return this.invocationBuilder.trace();
    }

    @Override
    public <T> T trace(Class<T> responseType) {
        return this.invocationBuilder.trace(responseType);
    }

    @Override
    public <T> T trace(GenericType<T> responseType) {
        return this.invocationBuilder.trace(responseType);
    }

    @Override
    public Response method(String name) {
        return this.invocationBuilder.method(name);
    }

    @Override
    public <T> T method(String name, Class<T> responseType) {
        return this.invocationBuilder.method(name, responseType);
    }

    @Override
    public <T> T method(String name, GenericType<T> responseType) {
        return this.invocationBuilder.method(name, responseType);
    }

    @Override
    public Response method(String name, Entity<?> entity) {
        return this.invocationBuilder.method(name, entity);
    }

    @Override
    public <T> T method(String name, Entity<?> entity, Class<T> responseType) {
        return this.invocationBuilder.method(name, entity, responseType);
    }

    @Override
    public <T> T method(String name, Entity<?> entity, GenericType<T> responseType) {
        return this.invocationBuilder.method(name, entity, responseType);
    }

    /**
     * Instruments this InvocationBuilder instance with OpenTracing
     */
    private void instrumentInvocationBuilder() {
        // Get the currently active span if present
        if (openTracing != null) {
            Span activeSpan = openTracing.getTracer(
                    openTracing.getApplicationName(serviceLocator.getService(InvocationManager.class)))
                    .activeSpan();

            // If there is an active span, add its context to the request as a property so it can be picked up by the filter
            if (activeSpan != null) {
                this.property(PropagationHeaders.OPENTRACING_PROPAGATED_SPANCONTEXT, activeSpan.context());
            }
        }
    }
}
