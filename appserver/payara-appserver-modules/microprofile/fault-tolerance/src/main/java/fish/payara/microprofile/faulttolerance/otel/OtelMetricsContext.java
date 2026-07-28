/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
 * only if the code is changed by the third party and used as a new code
 * in combination with Open Source Software developed by Glassfish/Payara
 * or its successors.
 */
package fish.payara.microprofile.faulttolerance.otel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.metrics.MetricRegistry;
import fish.payara.microprofile.metrics.MetricsService;
import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;
import io.opentelemetry.api.metrics.Meter;

/**
 * OpenTelemetry-backed implementation of {@link MetricsService.MetricsContext}.
 * 
 * This context maintains scoped OTel registries (base, vendor, application) and
 * handles cleanup of observable gauge callbacks when the context is closed.
 */
public final class OtelMetricsContext implements MetricsService.MetricsContext, AutoCloseable {

    private static final Logger logger = Logger.getLogger(OtelMetricsContext.class.getName());

    private final String contextName;
    private final Meter meter;
    private final ConcurrentMap<String, MetricRegistry> registries = new ConcurrentHashMap<>();

    public OtelMetricsContext(String contextName, Meter meter) {
        this.contextName = contextName;
        this.meter = meter;
    }

    @Override
    public String getName() {
        return contextName;
    }

    @Override
    public MetricRegistry getOrCreateRegistry(String registryName) throws NoSuchRegistryException {
        return registries.computeIfAbsent(registryName, scope -> new OtelMetricRegistry(meter, scope));
    }

    @Override
    public ConcurrentMap<String, MetricRegistry> getRegistries() {
        return registries;
    }

    /**
     * Closes all registries and deregisters their observable gauge callbacks.
     * Must be called when this context is no longer needed.
     */
    @Override
    public void close() {
        for (MetricRegistry registry : registries.values()) {
            if (registry instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) registry).close();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to close OTel metric registry for context: " + contextName, e);
                }
            }
        }
        registries.clear();
    }

}
