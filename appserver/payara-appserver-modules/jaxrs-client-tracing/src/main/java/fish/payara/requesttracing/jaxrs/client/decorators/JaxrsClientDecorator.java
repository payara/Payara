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
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

/**
 * Decorator class used to return a decorated WebTarget.
 * 
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
public class JaxrsClientDecorator implements Client {

    protected Client client;
    
    public JaxrsClientDecorator(Client client) {
        this.client = client;
    }
    
    @Override
    public void close() {
        this.client.close();
    }

    @Override
    public WebTarget target(String uri) {
        return new JaxrsWebTargetDecorator(this.client.target(uri));
    }

    @Override
    public WebTarget target(URI uri) {
        return new JaxrsWebTargetDecorator(this.client.target(uri));
    }

    @Override
    public WebTarget target(UriBuilder uriBuilder) {
        return new JaxrsWebTargetDecorator(this.client.target(uriBuilder));
    }

    @Override
    public WebTarget target(Link link) {
        // Return a decorated WebTarget
        return new JaxrsWebTargetDecorator(this.client.target(link));
    }

    @Override
    public Invocation.Builder invocation(Link link) {
        return this.client.invocation(link);
    }

    @Override
    public SSLContext getSslContext() {
        return this.client.getSslContext();
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return this.client.getHostnameVerifier();
    }

    @Override
    public Configuration getConfiguration() {
        return this.client.getConfiguration();
    }

    @Override
    public Client property(String name, Object value) {
        client = client.property(name, value);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass) {
        client = client.register(componentClass);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass, int priority) {
        client = client.register(componentClass, priority);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass, Class<?>... contracts) {
        client = client.register(componentClass, contracts);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        client = client.register(componentClass, contracts);
        return this;
    }

    @Override
    public Client register(Object component) {
        client = client.register(component);
        return this;
    }

    @Override
    public Client register(Object component, int priority) {
        client = client.register(component, priority);
        return this;
    }

    @Override
    public Client register(Object component, Class<?>... contracts) {
        client = client.register(component, contracts);
        return this;
    }

    @Override
    public Client register(Object component, Map<Class<?>, Integer> contracts) {
        client = client.register(component, contracts);
        return this;
    }
    
}