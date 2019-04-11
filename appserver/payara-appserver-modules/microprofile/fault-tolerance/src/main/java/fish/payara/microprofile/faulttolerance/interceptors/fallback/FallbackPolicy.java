/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2018 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.microprofile.faulttolerance.interceptors.fallback;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;
import fish.payara.microprofile.faulttolerance.FaultToleranceExecution;
import fish.payara.microprofile.faulttolerance.FaultToleranceExecutionContext;
import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;
import fish.payara.microprofile.faulttolerance.cdi.FaultToleranceCdiUtils;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import javax.enterprise.inject.spi.CDI;

/**
 * Class that executes the fallback policy defined by the {@link Fallback} annotation.
 * @author Andrew Pielage
 */
public class FallbackPolicy {

    private static final Logger logger = Logger.getLogger(FallbackPolicy.class.getName());

    private final Class<? extends FallbackHandler<?>> fallbackClass;
    private final String fallbackMethod;
    private final FaultToleranceExecution execution;
    private final FaultToleranceMetrics metrics;

    public FallbackPolicy(Fallback fallback, FaultToleranceConfig config, FaultToleranceExecution execution, FaultToleranceMetrics metrics,
            InvocationContext context) {
        this.execution = execution;
        this.metrics = metrics;
        this.fallbackClass = config.value(fallback, context);
        this.fallbackMethod = config.fallbackMethod(fallback, context);
    }

    /**
     * Performs the fallback operation defined by the @Fallback annotation.
     * @param context The failing invocation context
     * @return The result of the executed fallback method
     * @throws Exception If the fallback method itself fails.
     */
    public Object fallback(InvocationContext context, Throwable exception) throws Exception {
        Object resultValue = null;
        execution.startTrace("executeFallbackMethod", context);
        try {
            if (fallbackMethod != null && !fallbackMethod.isEmpty()) {
                logger.log(Level.FINE, "Using fallback method: {0}", fallbackMethod);

                resultValue = FaultToleranceCdiUtils
                        .getAnnotatedMethodClass(context, Fallback.class)
                        .getDeclaredMethod(fallbackMethod, context.getMethod().getParameterTypes())
                        .invoke(context.getTarget(), context.getParameters());
            } else {
                logger.log(Level.FINE, "Using fallback class: {0}", fallbackClass.getName());

                ExecutionContext executionContext = new FaultToleranceExecutionContext(context.getMethod(), 
                        context.getParameters(), exception);

                resultValue = CDI.current().select(fallbackClass).get().handle(executionContext);
            }
            metrics.incrementFallbackCallsTotal(context);
        } catch (Exception ex) {
            // Increment the failure counter metric
            metrics.incrementInvocationsFailedTotal(Fallback.class, context);
            throw ex;
        } finally {
            execution.endTrace();
        }
        return resultValue;
    }
}
