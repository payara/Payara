/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2023 Payara Foundation and/or its affiliates. All rights reserved.
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.eclipse.microprofile.metrics.MetricRegistry;

import fish.payara.microprofile.faulttolerance.FaultToleranceMethodContext;
import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceMethodContextStub;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceServiceStub;
import fish.payara.microprofile.faulttolerance.service.FaultToleranceUtils;
import fish.payara.microprofile.faulttolerance.service.MethodFaultToleranceMetrics;
import fish.payara.microprofile.metrics.impl.MetricRegistryImpl;

/**
 * Base class for FT tests with {@link FaultToleranceMetrics} "enabled" using the actual implementation classes.
 *
 * @author Jan Bernitt
 */
abstract class AbstractMetricTest extends AbstractRecordingTest {

    MetricRegistry registry;

    @Override
    protected FaultToleranceServiceStub createService() {
        registry = new MetricRegistryImpl(MetricRegistry.BASE_SCOPE);
        return new FaultToleranceServiceStub() {
            @Override
            protected FaultToleranceMethodContext stubMethodContext(StubContext ctx) {
                FaultToleranceMetrics metrics = new MethodFaultToleranceMetrics(registry, FaultToleranceUtils.getCanonicalMethodName(ctx.context));
                return new FaultToleranceMethodContextStub(ctx, state, concurrentExecutions, waitingQueuePopulation) {

                    @Override
                    public FaultToleranceMetrics getMetrics() {
                        return metrics;
                    }

                    @Override
                    public Future<?> runDelayed(long delayMillis, Runnable task) throws Exception {
                        return CompletableFuture.completedFuture(null);
                    }
                };
            }
        };
    }

}
