/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019-2020] Payara Foundation and/or its affiliates. All rights reserved.
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

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;

public class SpanPropagator {
    private static final SpanPropagator INSTANCE = new SpanPropagator();
    private static final ThreadLocal<SpanContext> PROPAGATED_CONTEXT = new ThreadLocal<>();

    SpanPropagator() {
    }

    private SpanContext internalActiveContext() {
        final PayaraTracingServices payaraTracingServices = new PayaraTracingServices();
        final Tracer tracer = payaraTracingServices.getActiveTracer();
        if (tracer == null) {
            return null;
        }
        final Span activeSpan = tracer.activeSpan();
        return activeSpan == null ? null : activeSpan.context();
    }

    public static SpanContext activeContext() {
        return INSTANCE.internalActiveContext();
    }

    public static SpanContext propagateContext(SpanContext context) {
        final SpanContext previous = PROPAGATED_CONTEXT.get();
        PROPAGATED_CONTEXT.set(context);
        return previous;
    }

    public static SpanContext propagatedContext() {
        return PROPAGATED_CONTEXT.get();
    }

    public static void clearPropagatedContext() {
        propagateContext(null);
    }
}
