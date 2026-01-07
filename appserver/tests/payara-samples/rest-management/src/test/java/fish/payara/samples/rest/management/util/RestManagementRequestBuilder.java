/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.rest.management.util;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.Locale;

import jakarta.ws.rs.client.AsyncInvoker;
import jakarta.ws.rs.client.CompletionStageRxInvoker;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.RxInvoker;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

public class RestManagementRequestBuilder implements Builder {

    private final Builder proxy;

    protected RestManagementRequestBuilder(Builder proxy) {
        this.proxy = proxy;
        accept(APPLICATION_JSON, TEXT_PLAIN);
        header("X-Requested-By", "Payara");
    }

    @Override
    public Response get() {
        return proxy.get();
    }

    @Override
    public <T> T get(Class<T> responseType) {
        return proxy.get(responseType);
    }

    @Override
    public <T> T get(GenericType<T> responseType) {
        return proxy.get(responseType);
    }

    @Override
    public Response put(Entity<?> entity) {
        return proxy.put(entity);
    }

    @Override
    public <T> T put(Entity<?> entity, Class<T> responseType) {
        return proxy.put(entity, responseType);
    }

    @Override
    public <T> T put(Entity<?> entity, GenericType<T> responseType) {
        return proxy.put(entity, responseType);
    }

    @Override
    public Response post(Entity<?> entity) {
        return proxy.post(entity);
    }

    @Override
    public <T> T post(Entity<?> entity, Class<T> responseType) {
        return proxy.post(entity, responseType);
    }

    @Override
    public <T> T post(Entity<?> entity, GenericType<T> responseType) {
        return proxy.post(entity, responseType);
    }

    @Override
    public Response delete() {
        return proxy.delete();
    }

    @Override
    public <T> T delete(Class<T> responseType) {
        return proxy.delete(responseType);
    }

    @Override
    public <T> T delete(GenericType<T> responseType) {
        return proxy.delete(responseType);
    }

    @Override
    public Response head() {
        return proxy.head();
    }

    @Override
    public Response options() {
        return proxy.options();
    }

    @Override
    public <T> T options(Class<T> responseType) {
        return proxy.options(responseType);
    }

    @Override
    public <T> T options(GenericType<T> responseType) {
        return proxy.options(responseType);
    }

    @Override
    public Response trace() {
        return proxy.trace();
    }

    @Override
    public <T> T trace(Class<T> responseType) {
        return proxy.trace(responseType);
    }

    @Override
    public <T> T trace(GenericType<T> responseType) {
        return proxy.trace(responseType);
    }

    @Override
    public Response method(String name) {
        return proxy.method(name);
    }

    @Override
    public <T> T method(String name, Class<T> responseType) {
        return proxy.method(name, responseType);
    }

    @Override
    public <T> T method(String name, GenericType<T> responseType) {
        return proxy.method(name, responseType);
    }

    @Override
    public Response method(String name, Entity<?> entity) {
        return proxy.method(name, entity);
    }

    @Override
    public <T> T method(String name, Entity<?> entity, Class<T> responseType) {
        return proxy.method(name, entity, responseType);
    }

    @Override
    public <T> T method(String name, Entity<?> entity, GenericType<T> responseType) {
        return proxy.method(name, entity, responseType);
    }

    @Override
    public Invocation build(String method) {
        return proxy.build(method);
    }

    @Override
    public Invocation build(String method, Entity<?> entity) {
        return proxy.build(method, entity);
    }

    @Override
    public Invocation buildGet() {
        return proxy.buildGet();
    }

    @Override
    public Invocation buildDelete() {
        return proxy.buildDelete();
    }

    @Override
    public Invocation buildPost(Entity<?> entity) {
        return proxy.buildPost(entity);
    }

    @Override
    public Invocation buildPut(Entity<?> entity) {
        return proxy.buildPut(entity);
    }

    @Override
    public AsyncInvoker async() {
        return proxy.async();
    }

    @Override
    public Builder accept(String... mediaTypes) {
        return proxy.accept(mediaTypes);
    }

    @Override
    public Builder accept(MediaType... mediaTypes) {
        return proxy.accept(mediaTypes);
    }

    @Override
    public Builder acceptLanguage(Locale... locales) {
        return proxy.acceptLanguage(locales);
    }

    @Override
    public Builder acceptLanguage(String... locales) {
        return proxy.acceptLanguage(locales);
    }

    @Override
    public Builder acceptEncoding(String... encodings) {
        return proxy.acceptEncoding(encodings);
    }

    @Override
    public Builder cookie(Cookie cookie) {
        return proxy.cookie(cookie);
    }

    @Override
    public Builder cookie(String name, String value) {
        return proxy.cookie(name, value);
    }

    @Override
    public Builder cacheControl(CacheControl cacheControl) {
        return proxy.cacheControl(cacheControl);
    }

    @Override
    public Builder header(String name, Object value) {
        return proxy.header(name, value);
    }

    @Override
    public Builder headers(MultivaluedMap<String, Object> headers) {
        return proxy.headers(headers);
    }

    @Override
    public Builder property(String name, Object value) {
        return proxy.property(name, value);
    }

    @Override
    public CompletionStageRxInvoker rx() {
        return proxy.rx();
    }

    @Override
    public <T extends RxInvoker> T rx(Class<T> clazz) {
        return proxy.rx(clazz);
    }
    
}