/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.faulttolerance.policy;

import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;

/**
 * The resolved "cached" information of a {@link Asynchronous} annotation an a specific method.
 * 
 * @author Jan Bernitt
 */
public final class AsynchronousPolicy extends Policy {

    private static final AsynchronousPolicy FUTURE = new AsynchronousPolicy();
    private static final AsynchronousPolicy COMPLETION_STAGE = new AsynchronousPolicy();

    private AsynchronousPolicy() {
        // hide
    }

    public static AsynchronousPolicy create(InvocationContext context, FaultToleranceConfig config) {
        if ((config.isAnnotationPresent(Asynchronous.class) || config.isAlternativeAsynchronousAnnoationPresent())
                && config.isEnabled(Asynchronous.class)) {
            checkReturnsFutureOrCompletionStage(context.getMethod());
            return context.getMethod().getReturnType() == Future.class ? FUTURE : COMPLETION_STAGE;
        }
        return null;
    }

    static void checkReturnsFutureOrCompletionStage(Method annotated) {
        Class<?> returnType = annotated.getReturnType();
        if (returnType != Future.class && returnType != CompletionStage.class) {
            throw new FaultToleranceDefinitionException(describe(annotated, Asynchronous.class, "")
                    + "does not return a Future or CompletionStage but: " + returnType.getName());
        }
    }

    public static Future<?> toFuture(Object asyncResult) {
        return asyncResult instanceof CompletionStage
                ? ((CompletionStage<?>) asyncResult).toCompletableFuture()
                : (Future<?>) asyncResult;
    }

    public boolean isSuccessWhenCompletedExceptionally() {
        return this == FUTURE;
    }
}
