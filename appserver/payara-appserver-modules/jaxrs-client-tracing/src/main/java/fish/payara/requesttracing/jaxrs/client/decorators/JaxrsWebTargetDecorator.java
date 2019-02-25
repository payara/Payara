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

import java.net.URI;
import java.util.Map;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

/**
 * Decorator class used for returning a decorated InvocationBuilder.
 * 
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
public class JaxrsWebTargetDecorator implements WebTarget {

    protected WebTarget webTarget;
    
    public JaxrsWebTargetDecorator(WebTarget webTarget) {
        this.webTarget = webTarget;
    }
    
    @Override
    public URI getUri() {
        return this.webTarget.getUri();
    }

    @Override
    public UriBuilder getUriBuilder() {
        return this.webTarget.getUriBuilder();
    }

    @Override
    public WebTarget path(String path) {
        return new JaxrsWebTargetDecorator(this.webTarget.path(path));
    }

    @Override
    public WebTarget resolveTemplate(String name, Object value) {
        return new JaxrsWebTargetDecorator(webTarget.resolveTemplate(name, value));
    }

    @Override
    public WebTarget resolveTemplate(String name, Object value, boolean encodeSlashInPath) {
        return new JaxrsWebTargetDecorator(webTarget.resolveTemplate(name, value, encodeSlashInPath));
    }

    @Override
    public WebTarget resolveTemplateFromEncoded(String name, Object value) {
        return new JaxrsWebTargetDecorator(webTarget.resolveTemplateFromEncoded(name, value));
    }

    @Override
    public WebTarget resolveTemplates(Map<String, Object> templateValues) {
        return new JaxrsWebTargetDecorator(webTarget.resolveTemplates(templateValues));
    }

    @Override
    public WebTarget resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath) {
        return new JaxrsWebTargetDecorator(webTarget.resolveTemplates(templateValues, encodeSlashInPath));
    }

    @Override
    public WebTarget resolveTemplatesFromEncoded(Map<String, Object> templateValues) {
        return new JaxrsWebTargetDecorator(webTarget.resolveTemplatesFromEncoded(templateValues));
    }

    @Override
    public WebTarget matrixParam(String name, Object... values) {
        return new JaxrsWebTargetDecorator(webTarget.matrixParam(name, values));
    }

    @Override
    public WebTarget queryParam(String name, Object... values) {
        return new JaxrsWebTargetDecorator(webTarget.queryParam(name, values));
    }

    @Override
    public Invocation.Builder request() {
        // Return a decorated InvocationBuilder
        return new JaxrsInvocationBuilderDecorator(this.webTarget.request());
    }

    @Override
    public Invocation.Builder request(String... acceptedResponseTypes) {
        // Return a decorated InvocationBuilder
        return new JaxrsInvocationBuilderDecorator(this.webTarget.request(acceptedResponseTypes));
    }

    @Override
    public Invocation.Builder request(MediaType... acceptedResponseTypes) {
        // Return a decorated InvocationBuilder
        return new JaxrsInvocationBuilderDecorator(this.webTarget.request(acceptedResponseTypes));
    }

    @Override
    public Configuration getConfiguration() {
        return this.webTarget.getConfiguration();
    }

    @Override
    public WebTarget property(String name, Object value) {
        webTarget = webTarget.property(name, value);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass) {
        webTarget = webTarget.register(componentClass);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass, int priority) {
        webTarget = webTarget.register(componentClass, priority);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass, Class<?>... contracts) {
        webTarget = webTarget.register(componentClass, contracts);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        webTarget = webTarget.register(componentClass, contracts);
        return this;
    }

    @Override
    public WebTarget register(Object component) {
        webTarget = webTarget.register(component);
        return this;
    }

    @Override
    public WebTarget register(Object component, int priority) {
        webTarget = webTarget.register(component, priority);
        return this;
    }

    @Override
    public WebTarget register(Object component, Class<?>... contracts) {
        webTarget = webTarget.register(component, contracts);
        return this;
    }

    @Override
    public WebTarget register(Object component, Map<Class<?>, Integer> contracts) {
        webTarget = webTarget.register(component, contracts);
        return this;
    }
    
}
