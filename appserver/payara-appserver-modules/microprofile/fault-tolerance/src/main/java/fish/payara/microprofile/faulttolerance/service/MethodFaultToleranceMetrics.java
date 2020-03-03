/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2020 Payara Foundation and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.MetricRegistry;

import fish.payara.microprofile.faulttolerance.FaultToleranceMetrics;

/**
 * The {@link MethodFaultToleranceMetrics} is a {@link FaultToleranceMetrics} for a particular {@link Method}.
 * 
 * @author Jan Bernitt
 */
final class MethodFaultToleranceMetrics implements FaultToleranceMetrics {

    /**
     * This is "cached" as soon as an instance is bound using the
     * {@link #FaultToleranceMetricsFactory(MetricRegistry, String)} constructor.
     */
    private final String canonicalMethodName;
    private final MetricRegistry registry;
    private final Map<String, Counter> countersByKeyPattern = new ConcurrentHashMap<>();
    private final Map<String, Histogram> histogramsByKeyPattern = new ConcurrentHashMap<>();
    private final Map<String, Gauge<Long>> gaugesByKeyPattern = new ConcurrentHashMap<>();

    MethodFaultToleranceMetrics(MetricRegistry registry, String canonicalMethodName) {
        this.registry = registry;
        this.canonicalMethodName = canonicalMethodName;
    }

    /*
     * General Metrics
     */

    @Override
    public void incrementCounter(String keyPattern) {
        countersByKeyPattern.computeIfAbsent(keyPattern, 
                pattern -> registry.counter(metricName(pattern))).inc();
    }

    @Override
    public void addToHistogram(String keyPattern, long duration) {
        histogramsByKeyPattern.computeIfAbsent(keyPattern, 
                pattern -> registry.histogram(metricName(pattern))).update(duration);
    }

    @Override
    public void linkGauge(String keyPattern, LongSupplier gauge) {
        gaugesByKeyPattern.computeIfAbsent(keyPattern, pattern -> {
            String metricName = metricName(keyPattern);
            Gauge<Long> newGauge = gauge::getAsLong; 
            try {
                registry.register(metricName, newGauge);
            } catch (IllegalArgumentException ex) {
                // gauge already exists. Its ugly but this is the only way to make sure it does exist.
            }
            return newGauge;
        });
    }

    private String metricName(String keyPattern) {
        return String.format(keyPattern, canonicalMethodName);
    }

}
