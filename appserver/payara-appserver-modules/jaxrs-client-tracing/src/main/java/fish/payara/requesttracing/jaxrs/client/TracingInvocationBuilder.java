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
package fish.payara.requesttracing.jaxrs.client;

import fish.payara.nucleus.requesttracing.domain.PropagationHeaders;
import fish.payara.opentracing.OpenTracingService;

import io.opentracing.Span;

import java.util.logging.Logger;

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.RxInvoker;

import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.JerseyInvocation.Builder;

/**
 * Decorator class used for instrumenting asynchronous clients.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
class TracingInvocationBuilder extends Builder {
    private static final Logger LOG = Logger.getLogger(TracingInvocationBuilder.class.getName());

    private final ServiceLocator serviceLocator;
    private final OpenTracingService openTracing;

    public TracingInvocationBuilder(final ClientRequest clientRequest) {
        super(clientRequest);
        this.serviceLocator = Globals.getDefaultBaseServiceLocator();
        this.openTracing = this.serviceLocator.getService(OpenTracingService.class);
        LOG.finest(() -> "Created " + this);
    }

    @Override
    public AsyncInvoker async() {
        LOG.finest("async()");
        instrumentInvocationBuilder();
        return super.async();
    }

    @Override
    public CompletionStageRxInvoker rx() {
        LOG.finest("rx()");
        instrumentInvocationBuilder();
        return super.rx();
    }

    @Override
    public <T extends RxInvoker> T rx(Class<T> clazz) {
        LOG.finest(() -> "rx(clazz=" + clazz + ")");
        instrumentInvocationBuilder();
        return super.rx(clazz);
    }

    /**
     * Instruments this InvocationBuilder instance with OpenTracing
     */
    private void instrumentInvocationBuilder() {
        if (this.openTracing == null) {
            return;
        }
        final InvocationManager manager = this.serviceLocator.getService(InvocationManager.class);
        final String applicationName = this.openTracing.getApplicationName(manager);
        final Span activeSpan = this.openTracing.getTracer(applicationName).activeSpan();
        LOG.fine(() -> "Found active span=" + activeSpan + ", propagating context.");
        // If there is an active span, add its context to the request as a property so it can be
        // picked up by the filter
        if (activeSpan != null) {
            property(PropagationHeaders.OPENTRACING_PROPAGATED_SPANCONTEXT, activeSpan.context());
        }
    }
}
