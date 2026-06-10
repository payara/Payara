/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */
package fish.payara.microprofile.telemetry.tracing;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import java.lang.reflect.Method;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Guards the interceptor advice type of {@link TracedInterceptor}.
 *
 * <p>The whole point of {@link TracedInterceptor} is to open a span around <em>each business
 * method invocation</em> of a {@link Traced} bean. That requires {@link AroundInvoke}.</p>
 *
 * <p>If the advice is declared as {@link AroundConstruct} instead, the interceptor fires only
 * once, when the bean is constructed, so:</p>
 * <ul>
 *   <li>no business method is ever traced (the feature silently stops working); and</li>
 *   <li>{@code InvocationContext.getMethod()} returns {@code null} during construction, while
 *       the body dereferences it in {@code isJaxRsMethod}/{@code isWebServiceMethod}/
 *       {@code getOperationName} &rarr; {@link NullPointerException} as soon as request tracing
 *       is enabled and a {@code @Traced} bean is instantiated.</li>
 * </ul>
 *
 * <p>This test fails on purpose while the advice is {@code @AroundConstruct} (FISH-13069), and
 * should turn green once it is changed back to {@code @AroundInvoke}.</p>
 */
public class TracedInterceptorTest {

    @Test
    public void traceCdiCallMustInterceptMethodInvocationsNotConstruction() throws NoSuchMethodException {
        final Method traceCdiCall =
                TracedInterceptor.class.getDeclaredMethod("traceCdiCall", InvocationContext.class);

        assertNotNull(
                "TracedInterceptor#traceCdiCall must be annotated with @AroundInvoke so that a span is "
                        + "created around every @Traced method call; without it no business method is traced.",
                traceCdiCall.getAnnotation(AroundInvoke.class));

        assertNull(
                "TracedInterceptor#traceCdiCall must NOT be annotated with @AroundConstruct: it would only fire "
                        + "on bean construction, where InvocationContext.getMethod() is null and the interceptor "
                        + "dereferences it, throwing a NullPointerException once request tracing is enabled.",
                traceCdiCall.getAnnotation(AroundConstruct.class));
    }
}