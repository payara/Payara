/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2026] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.telemetry.service;


import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.glassfish.common.util.Constants;
import org.glassfish.hk2.api.Rank;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.InitRunLevel;
import org.jvnet.hk2.annotations.Service;

import static fish.payara.telemetry.service.PayaraTelemetryConstants.*;

@Service(name = "telemetry-runtime-config-service")
@RunLevel(InitRunLevel.VAL)
@Rank(Constants.IMPORTANT_RUN_LEVEL_SERVICE)
public class PayaraTelemetryBootstrapFactoryServiceImpl implements PayaraTelemetryBootstrapFactoryService {
    
    private OpenTelemetry runtimeSdk = null;

    @PostConstruct
    public void init() {
        runtimeSdk = createTelemetryRuntimeInstance();
    }

    @Override
    public OpenTelemetry createTelemetryRuntimeInstance() {
        if (!isRuntimeOtelEnabled()) {
            // need to read otel properties
            final Map<String, String> props = new HashMap<>(readOtelProperties());
            return AutoConfiguredOpenTelemetrySdk.builder()
                    //Need to provide custom Resources to start impl
                    .addResourceCustomizer(provideDefaultResourceCustomizer(!isRuntimeOtelEnabled()))
                    //Need to provide properties read from the system and env
                    .addPropertiesCustomizer(p -> props)
                    .setServiceClassLoader(Thread.currentThread().getContextClassLoader())
                    .disableShutdownHook()
                    .setResultAsGlobal()
                    .build().getOpenTelemetrySdk();
        }
        return runtimeSdk;
    }

    @Override
    public OpenTelemetry getAvailableRuntimeReference() {
        return runtimeSdk;
    }

    @Override
    public boolean isRuntimeOtelEnabled() {
        if (System.getProperty(OTEL_SYSTEM_PROPERTY_NAME) != null) {
            return !"false".equalsIgnoreCase(System.getProperty(OTEL_SYSTEM_PROPERTY_NAME, "true"));
        }

        if (System.getenv(OTEL_ENVIRONMENT_PROPERTY_NAME) != null) {
            return !"false".equalsIgnoreCase(System.getenv(OTEL_ENVIRONMENT_PROPERTY_NAME));
        }
        return true;
    }

    private Map<String, String> readOtelProperties() {
        Map<String, String> props = new HashMap<>();
        Map<String, String> systemOtelPropsAvailable = System.getProperties().entrySet().stream()
                .filter(e -> ((String) e.getKey()).startsWith(OTEL_PROPERTIES_PREFIX))
                .map(e -> Map.entry((String) e.getKey(), (String) e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, String> environmentOtelPropsAvailable = System.getenv().entrySet().stream()
                .filter(e -> e.getKey().startsWith(OTEL_PROPERTIES_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        props.putAll(systemOtelPropsAvailable);
        props.putAll(environmentOtelPropsAvailable);
        return props;
    }
    
    private BiFunction<? super Resource, ConfigProperties, ?extends Resource> provideDefaultResourceCustomizer(boolean runtimeOtelEnabled) {
        return (Resource resource, ConfigProperties configProperties) -> {
            try {
                return this.createDefaultResources(resource, configProperties, runtimeOtelEnabled).build();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        };
    }
    
    private ResourceBuilder createDefaultResources(Resource resource, ConfigProperties configProperties, boolean runtimeOtelEnabled) throws UnknownHostException {
        ResourceBuilder builder = resource.toBuilder();
        builder.put(OTEL_SERVICE_NAME, PAYARA_OTEL_RUNTIME_INSTANCE_NAME);
        builder.put("service.name", PAYARA_OTEL_RUNTIME_INSTANCE_NAME);
        //indicating metrics exporter as none to prevent warning from logs
        builder.put(OTEL_METRICS_EXPORTER, "none");
        //set semantic attribute for OS name and version
        builder.put("os.name", System.getProperty("os.name"));
        builder.put("os.version", System.getProperty("os.version"));
        //set semantic attribute for host information
        builder.put("host.name", InetAddress.getLocalHost().getHostName());
        builder.put("host.arch", System.getProperty("os.arch"));
        //set semantic attribute for jvm information
        builder.put("jvm.name", System.getProperty("java.vm.name"));
        builder.put("jvm.vendor", System.getProperty("java.vendor"));
        builder.put("jvm.version", System.getProperty("java.version"));
        return builder;
    }
    


}
