/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2023] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.microprofile.telemetry.tracing.jaxrs.client;

import fish.payara.opentracing.PropagationHelper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.eclipse.microprofile.rest.client.ext.AsyncInvocationInterceptor;
import org.eclipse.microprofile.rest.client.ext.AsyncInvocationInterceptorFactory;

/**
 * An implementation of the {@link AsyncInvocationInterceptor} interface that propagates OpenTelemetry
 * {@link io.opentelemetry.api.trace.Span} and {@link io.opentelemetry.context.Context}
 * across asynchronous invocation boundaries.
 */
public class AsyncContextPropagator implements AsyncInvocationInterceptor {

    private PropagationHelper helper;

    private Scope scope;

    /**
     * Prepares the context for propagation by starting the PropagationHelper object.
     */
    @Override
    public void prepareContext() {
        this.helper = PropagationHelper.startMultiThreaded(Span.current(), Context.current());
    }

    /**
     * Applies the propagated context and span to the current thread's context and span using a local scope.
     */
    @Override
    public void applyContext() {
        this.scope = helper.localScope();
    }

    /**
     * Removes the propagated context and span from the current thread's context and span.
     */
    @Override
    public void removeContext() {
        if (this.scope != null) {
            this.scope.close();
        }

        if (this.helper != null) {
            this.helper.close();
        }
    }

    static class Factory implements AsyncInvocationInterceptorFactory {

        @Override
        public AsyncInvocationInterceptor newInterceptor() {
            return new AsyncContextPropagator();
        }
    }
}
