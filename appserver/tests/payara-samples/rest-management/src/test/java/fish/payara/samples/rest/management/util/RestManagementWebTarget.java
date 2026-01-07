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

import java.net.URI;
import java.util.Map;

import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;

public class RestManagementWebTarget implements WebTarget {

    private final WebTarget proxy;

    protected RestManagementWebTarget(WebTarget proxy) {
        this.proxy = proxy;
    }

    @Override
    public Configuration getConfiguration() {
        return proxy.getConfiguration();
    }

    @Override
    public WebTarget property(String name, Object value) {
        return new RestManagementWebTarget(proxy.property(name, value));
    }

    @Override
    public WebTarget register(Class<?> componentClass) {
        return new RestManagementWebTarget(proxy.register(componentClass));
    }

    @Override
    public WebTarget register(Class<?> componentClass, int priority) {
        return new RestManagementWebTarget(proxy.register(componentClass, priority));
    }

    @Override
    public WebTarget register(Class<?> componentClass, Class<?>... contracts) {
        return new RestManagementWebTarget(proxy.register(componentClass, contracts));
    }

    @Override
    public WebTarget register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        return new RestManagementWebTarget(proxy.register(componentClass, contracts));
    }

    @Override
    public WebTarget register(Object component) {
        return new RestManagementWebTarget(proxy.register(component));
    }

    @Override
    public WebTarget register(Object component, int priority) {
        return new RestManagementWebTarget(proxy.register(component, priority));
    }

    @Override
    public WebTarget register(Object component, Class<?>... contracts) {
        return new RestManagementWebTarget(proxy.register(component, contracts));
    }

    @Override
    public WebTarget register(Object component, Map<Class<?>, Integer> contracts) {
        return new RestManagementWebTarget(proxy.register(component, contracts));
    }

    @Override
    public URI getUri() {
        return proxy.getUri();
    }

    @Override
    public UriBuilder getUriBuilder() {
        return proxy.getUriBuilder();
    }

    @Override
    public WebTarget path(String path) {
        return new RestManagementWebTarget(proxy.path(path));
    }

    @Override
    public WebTarget resolveTemplate(String name, Object value) {
        return new RestManagementWebTarget(proxy.resolveTemplate(name, value));
    }

    @Override
    public WebTarget resolveTemplate(String name, Object value, boolean encodeSlashInPath) {
        return new RestManagementWebTarget(proxy.resolveTemplate(name, value, encodeSlashInPath));
    }

    @Override
    public WebTarget resolveTemplateFromEncoded(String name, Object value) {
        return new RestManagementWebTarget(proxy.resolveTemplateFromEncoded(name, value));
    }

    @Override
    public WebTarget resolveTemplates(Map<String, Object> templateValues) {
        return new RestManagementWebTarget(proxy.resolveTemplates(templateValues));
    }

    @Override
    public WebTarget resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath) {
        return new RestManagementWebTarget(proxy.resolveTemplates(templateValues, encodeSlashInPath));
    }

    @Override
    public WebTarget resolveTemplatesFromEncoded(Map<String, Object> templateValues) {
        return new RestManagementWebTarget(proxy.resolveTemplatesFromEncoded(templateValues));
    }

    @Override
    public WebTarget matrixParam(String name, Object... values) {
        return new RestManagementWebTarget(proxy.matrixParam(name, values));
    }

    @Override
    public WebTarget queryParam(String name, Object... values) {
        return new RestManagementWebTarget(proxy.queryParam(name, values));
    }

    @Override
    public Builder request() {
        return new RestManagementRequestBuilder(proxy.request());
    }

    @Override
    public Builder request(String... acceptedResponseTypes) {
        return new RestManagementRequestBuilder(proxy.request(acceptedResponseTypes));
    }

    @Override
    public Builder request(MediaType... acceptedResponseTypes) {
        return new RestManagementRequestBuilder(proxy.request(acceptedResponseTypes));
    }
    
}