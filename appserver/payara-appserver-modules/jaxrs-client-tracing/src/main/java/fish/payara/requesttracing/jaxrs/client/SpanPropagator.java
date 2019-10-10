/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/master/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */

package fish.payara.requesttracing.jaxrs.client;

import fish.payara.opentracing.OpenTracingService;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

public class SpanPropagator {
    private static final SpanPropagator INSTANCE = new SpanPropagator();
    private static ThreadLocal<SpanContext> propagatedContext = new ThreadLocal<>();

    private final OpenTracingService openTracing;
    private final InvocationManager invocationManager;

    SpanPropagator() {
        // Get the ServiceLocator and OpenTracing services
        ServiceLocator serviceLocator = Globals.getDefaultBaseServiceLocator();
        if (serviceLocator != null) {
            openTracing = serviceLocator.getService(OpenTracingService.class);
            invocationManager = serviceLocator.getService(InvocationManager.class);
        } else {
            openTracing = null;
            invocationManager = null;
        }
    }

    private SpanContext _activeContext() {
        if (openTracing != null) {
            Span activeSpan = openTracing.getTracer(
                    openTracing.getApplicationName(invocationManager))
                    .activeSpan();
            return activeSpan != null ? activeSpan.context() : null;
        }
        return null;
    }

    public static SpanContext activeContext() {
        return INSTANCE._activeContext();
    }

    public static SpanContext propagateContext(SpanContext context) {
        SpanContext previous = propagatedContext.get();
        propagatedContext.set(context);
        return previous;
    }

    public static SpanContext propagatedContext() {
        return propagatedContext.get();
    }

    public static void clearPropagatedContext() {
        propagateContext(null);
    }
}
