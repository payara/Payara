/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2023 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
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

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import fish.payara.opentracing.OpenTelemetryService;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.Extension;
import org.eclipse.microprofile.config.ConfigProvider;
import org.glassfish.internal.api.Globals;

/**
 * Extension class that adds a Producer for OpenTracing Tracers, allowing injection.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
public class TelemetryCdiExtension implements Extension {
    private final OpenTelemetryService openTelemetryService;

    private boolean appManagedOtel;

    public TelemetryCdiExtension() {
        openTelemetryService = Globals.getDefaultBaseServiceLocator().getService(OpenTelemetryService.class);
    }

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        addAnnotatedType(bbd, bm, OpenTracingTracerProducer.class);
        addAnnotatedType(bbd, bm, OpenTelemetryTracerProducer.class);
    }

    static void addAnnotatedType(BeforeBeanDiscovery bbd, BeanManager bm, Class<?> beanClass) {
        var at = bm.createAnnotatedType(beanClass);
        bbd.addAnnotatedType(at, beanClass.getName());
    }

    void initializeOpenTelemetry(@Observes AfterDeploymentValidation adv) {
        try {
            var config = ConfigProvider.getConfig();
            if (config.getOptionalValue("otel.sdk.disabled", Boolean.class).orElse(true)) {
                // app-specific OpenTelemetry is not configured
                return;
            }
            // This is potentially expensive, but Autoconfigure SDK does not have lazy per-key provider, needs a map
            var otelProps = StreamSupport.stream(config.getPropertyNames().spliterator(), false)
                    .filter(key -> key.startsWith("otel."))
                    .collect(Collectors.toMap(k -> k, k -> config.getValue(k, String.class)));
            appManagedOtel = true;
            openTelemetryService.initializeCurrentApplication(otelProps);
        } catch (Exception e) {
            adv.addDeploymentProblem(e);
        }
    }

    void shutdownAppScopedTelemetry(@Observes BeforeShutdown beforeShutdown) {
        if (appManagedOtel) {
            // application may have installed CDI-based components, like the TCK does
            // these need to be shutdown while application scope exists
            openTelemetryService.shutdownCurrentApplication();
        }
    }
}
