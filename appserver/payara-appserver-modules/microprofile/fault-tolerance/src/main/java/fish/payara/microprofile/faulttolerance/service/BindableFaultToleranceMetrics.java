/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.faulttolerance.service;

import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.CDI;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;

import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;

/**
 * The {@link BindableFaultToleranceMetrics} works both as a factory where {@link #bindTo(InvocationContext)} is used to
 * create context aware instances of a {@link FaultToleranceMetrics}.
 * 
 * This {@link FaultToleranceMetrics} uses {@link CDI} to resolve the {@link MetricRegistry}. When resolution fails
 * {@link #bindTo(InvocationContext)} will use {@link FaultToleranceMetrics#DISABLED}.
 * 
 * @author Jan Bernitt
 */
final class BindableFaultToleranceMetrics implements FaultToleranceMetrics {

    private static final Logger logger = Logger.getLogger(BindableFaultToleranceMetrics.class.getName());

    private final MetricRegistry metricRegistry;
    /**
     * This is "cached" as soon as an instance is bound using the
     * {@link #FaultToleranceMetricsFactory(MetricRegistry, String)} constructor.
     */
    private final String canonicalMethodName;

    public BindableFaultToleranceMetrics() {
        this.metricRegistry = resolveRegistry();
        this.canonicalMethodName = "(unbound)";
    }

    private BindableFaultToleranceMetrics(MetricRegistry metricRegistry, String canonicalMethodName) {
        this.metricRegistry = metricRegistry;
        this.canonicalMethodName = canonicalMethodName;
    }

    private static MetricRegistry resolveRegistry() {
        logger.log(Level.INFO, "Resolving Fault Tolerance MetricRegistry from CDI.");
        try {
            return CDI.current().select(MetricRegistry.class).get();
        } catch (Exception ex) {
            logger.log(Level.INFO, "No MetricRegistry could be found, disabling metrics.", ex);
            return null;
        }
    }

    /*
     * Factory method
     */

    FaultToleranceMetrics bindTo(InvocationContext context) {
        return metricRegistry == null
                ? FaultToleranceMetrics.DISABLED
                : new BindableFaultToleranceMetrics(metricRegistry, FaultToleranceUtils.getCanonicalMethodName(context));
    }

    /*
     * General Metrics
     */

    @Override
    public void incrementCounter(String keyPattern) {
        metricRegistry.counter(metricName(keyPattern)).inc();
    }

    @Override
    public void addToHistogram(String keyPattern, long duration) {
        metricRegistry.histogram(metricName(keyPattern)).update(duration);
    }

    @Override
    public void linkGauge(String keyPattern, LongSupplier gauge) {
        String metricName = metricName(keyPattern);
        Gauge<?> existingGauge = metricRegistry.getGauges().get(metricName);
        if (existingGauge == null) {
            Gauge<Long> newGauge = gauge::getAsLong;
            metricRegistry.register(metricName, newGauge);
        }
    }

    private String metricName(String keyPattern) {
        return String.format(keyPattern, canonicalMethodName);
    }

}
