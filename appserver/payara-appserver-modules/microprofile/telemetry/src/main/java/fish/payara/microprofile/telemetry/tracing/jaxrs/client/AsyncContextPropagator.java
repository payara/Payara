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
package fish.payara.microprofile.telemetry.tracing.jaxrs.client;

import fish.payara.opentracing.PropagationHelper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import org.eclipse.microprofile.rest.client.ext.AsyncInvocationInterceptor;
import org.eclipse.microprofile.rest.client.ext.AsyncInvocationInterceptorFactory;

public class AsyncContextPropagator implements AsyncInvocationInterceptor {

    private PropagationHelper propagationHelper;

    private Context context;

    private Span span;

    private Exception e;

    @Override
    public void prepareContext() {
        span = Span.current();
        context = Context.current();
//        try (var propagationHelper = PropagationHelper.startMultiThreaded(span, context)) {
//            this.propagationHelper = propagationHelper;
//            singleThreaded = false;
//        } catch (Exception e) {
//            this.e = e;
//        }

    }

    @Override
    public void applyContext() {
        try (var propagationHelper = PropagationHelper.startMultiThreaded(span, context)) {
            this.propagationHelper = propagationHelper;
        } catch (Exception e) {
            this.e = e;
        }
//        // install span and context as current for partial execution
//        try(var ignore = propagationHelper.localScope()) {
//            // no-op. do nothing else here, as the context has already been applied
//        } catch (Exception e) {
//            this.e = e;
//        }

    }

    @Override
    public void removeContext() {
        // end the span with OK or ERROR status depending on whether an error was reported
        propagationHelper.end(e);
    }

    static class Factory implements AsyncInvocationInterceptorFactory {

        @Override
        public AsyncInvocationInterceptor newInterceptor() {
            return new AsyncContextPropagator();
        }
    }
}
