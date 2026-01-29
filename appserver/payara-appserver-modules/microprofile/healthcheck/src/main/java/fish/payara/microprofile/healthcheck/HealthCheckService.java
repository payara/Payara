/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2017-2025 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.healthcheck;

import com.sun.enterprise.web.WebApplication;
import com.sun.enterprise.web.WebComponentInvocation;
import com.sun.enterprise.web.WebContainer;
import static fish.payara.microprofile.healthcheck.HealthCheckType.LIVENESS;
import static fish.payara.microprofile.healthcheck.HealthCheckType.READINESS;
import static fish.payara.microprofile.healthcheck.HealthCheckType.STARTUP;
import static java.util.Collections.singletonMap;
import static java.util.logging.Level.WARNING;

import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import fish.payara.microprofile.healthcheck.checks.PayaraHealthCheck;
import fish.payara.microprofile.healthcheck.config.MicroprofileHealthCheckConfiguration;
import fish.payara.nucleus.healthcheck.configuration.Checker;
import fish.payara.nucleus.healthcheck.events.PayaraHealthCheckServiceEvents;
import java.util.Optional;
import org.glassfish.api.invocation.InvocationException;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.data.ApplicationInfo;

/**
 * Service that handles the registration, execution, and response of MicroProfile HealthChecks.
 *
 * @author Andrew Pielage
 */
@Service(name = "healthcheck-service")
@RunLevel(StartupRunLevel.VAL)
public class HealthCheckService implements EventListener, ConfigListener {

    @Inject
    private Events events;

    @Inject
    private ApplicationRegistry applicationRegistry;

    @Inject
    private InvocationManager invocationManager;

    @Inject
    private MicroprofileHealthCheckConfiguration configuration;

    private static final Logger LOG = Logger.getLogger(HealthCheckService.class.getName());

    private final Map<String, Set<HealthCheck>> readiness = new ConcurrentHashMap<>();
    private final Map<String, Set<HealthCheck>> liveness = new ConcurrentHashMap<>();
    private final Map<String, Set<HealthCheck>> startup = new ConcurrentHashMap<>();

    private final Map<String, ClassLoader> applicationClassLoaders = new ConcurrentHashMap<>();
    private final List<String> applicationsLoaded = new CopyOnWriteArrayList<>();

    private final AtomicReference<Map<String, Set<String>>> checksCollected = new AtomicReference<>();

    @PostConstruct
    public void postConstruct() {
        if (events == null) {
            events = Globals.getDefaultBaseServiceLocator().getService(Events.class);
        }
        events.register(this);
    }

    @Override
    public void event(Event<?> event) {
        // Remove healthchecks when the app is undeployed.
        Deployment.APPLICATION_UNLOADED.onMatch(event, appInfo -> unregisterHealthCheck(appInfo.getName()));

        // Keep track of all deployed applications.
        Deployment.APPLICATION_STARTED.onMatch(event, appInfo -> applicationsLoaded.add(appInfo.getName()));
        
        PayaraHealthCheckServiceEvents.HEALTHCHECK_SERVICE_CHECKER_ADD_TO_MICROPROFILE_HEALTH.onMatch(event, healthChecker
                ->  registerHealthCheck(healthChecker.getName(),
                        new PayaraHealthCheck(healthChecker.getName(), healthChecker.getCheck()), READINESS));

        PayaraHealthCheckServiceEvents.HEALTHCHECK_SERVICE_CHECKER_REMOVE_FROM_MICROPROFILE_HEALTH.onMatch(event, healthChecker
                -> unregisterHealthCheck(healthChecker.getName()));
        
        if (event.is(PayaraHealthCheckServiceEvents.HEALTHCHECK_SERVICE_DISABLED)) {
            for (Checker checker : getHealthCheckerList()) {
                if (Boolean.valueOf(checker.getAddToMicroProfileHealth())) {
                    unregisterHealthCheck(checker.getName());
                }
            }
        }
    }
    
    private List<Checker> getHealthCheckerList() {
        fish.payara.nucleus.healthcheck.HealthCheckService payaraHealthCheckService
                    = Globals.getDefaultBaseServiceLocator().getService(fish.payara.nucleus.healthcheck.HealthCheckService.class);
        return payaraHealthCheckService.getConfiguration().getCheckerList();       
    }

    public boolean isEnabled() {
        return Boolean.parseBoolean(configuration.getEnabled());
    }

    public boolean isSecurityEnabled() {
        return Boolean.parseBoolean(configuration.getSecurityEnabled());
    }

    /**
     * Register a HealthCheck to the Set of HealthChecks to execute when
     * performHealthChecks is called.
     *
     * @param healthCheckName The name of the HealthCheck
     * @param healthCheck The HealthCheck to register
     */
    public void registerHealthCheck(String healthCheckName, HealthCheck healthCheck, HealthCheckType type) {
        Map<String, Set<HealthCheck>> healthChecks = getHealthChecks(type);

        // If we don't already have the app registered, we need to create a new Set for it
        if (!healthChecks.containsKey(healthCheckName)) {
            // Sync so that we don't get clashes
            synchronized (this) {
                // Check again so that we don't overwrite
                if (!healthChecks.containsKey(healthCheckName)) {
                    // Create a new Map entry and set for the Healthchecks.
                    healthChecks.put(healthCheckName, ConcurrentHashMap.newKeySet());
                }
            }
        }

        // Add the healthcheck to the Set in the Map
        healthChecks.get(healthCheckName).add(healthCheck);
    }
    
    public void unregisterHealthCheck(String appName) {
        readiness.remove(appName);
        liveness.remove(appName);
        startup.remove(appName);
        applicationClassLoaders.remove(appName);
        applicationsLoaded.remove(appName);
    }

    /**
     * Register a ClassLoader for an application.
     *
     * @param appName The name of the application being deployed
     * @param classloader
     */
    public void registerClassLoader(String appName, ClassLoader classloader) {
        // If we don't already have the app registered, we need to create a new Set for it
        if (!applicationClassLoaders.containsKey(appName)) {
            // Sync so that we don't get clashes
            synchronized (this) {
                // Check again so that we don't overwrite
                if (!applicationClassLoaders.containsKey(appName)) {
                    // Create a new Map entry and set for the Healthchecks.
                    applicationClassLoaders.put(appName, classloader);
                }
            }
        }
    }

    private Map<String, Set<HealthCheck>> getHealthChecks(HealthCheckType type) {
        if (type == null) {
            type = HealthCheckType.UNKNOWN;
        }
        switch (type) {
            case LIVENESS:
                return liveness;
            case READINESS:
                return readiness;
            case STARTUP:
                return startup;
            case UNKNOWN:
            default:
                LOG.warning("Unrecognised HealthCheckType: " + type);
                return new HashMap<>();
        }
    }

    private Map<String, Set<HealthCheck>> getCollectiveHealthChecks(HealthCheckType type) {
        final Map<String, Set<HealthCheck>> healthChecks;
        if (type == READINESS) {
            healthChecks = readiness;
        } else if (type == LIVENESS) {
            healthChecks = liveness;
        } else if (type == STARTUP) {
            healthChecks = startup;
        } else {
            // Make sure we do a deep-copy first, otherwise the first map the foreach consumer gets used on will be a
            // shallow copy: the two maps will essentially be the same map (changes to one affecting the other)
            healthChecks = readiness.entrySet().stream().collect(Collectors.toMap(entry ->
                    entry.getKey(), entry -> new HashSet(entry.getValue())));
            BiConsumer<? super String, ? super Set<HealthCheck>> mergeHealthCheckMap
                    = (key, value) -> healthChecks.merge(key, value, (oldValue, newValue) -> {
                        oldValue.addAll(newValue);
                        return oldValue;
                    });
            liveness.forEach(mergeHealthCheckMap);
            startup.forEach(mergeHealthCheckMap);
        }
        return healthChecks;
    }

    /**
     * Execute the call method of every registered HealthCheck and generate the response.
     *
     * @param response The response to return
     * @param type the type of health check
     * @throws IOException If there's an issue writing the response
     */
    public void performHealthChecks(HttpServletResponse response, HealthCheckType type, String enablePrettyPrint) throws IOException {
        Set<HealthCheckResponse> healthCheckResponses = new HashSet<>();

        // Iterate over every HealthCheck stored in the Map
        for (Entry<String, Set<HealthCheck>> healthChecksEntry : getCollectiveHealthChecks(type).entrySet()) {
            for (HealthCheck healthCheck : healthChecksEntry.getValue()) {
                // Execute the call method of the HealthCheck and add its outcome to the set of responses
                try {
                    healthCheckResponses.add(healthCheck.call());
                } catch (IllegalStateException ise) {
                    // If WebComponentInvocation is not present, the app is not ready eg. mid-deployment and the
                    // HealthCheck call should not be made.
                    ApplicationInfo appInfo = applicationRegistry.get(healthChecksEntry.getKey());
                    if (createWebComponentInvocation(appInfo).isPresent()) {
                        healthCheckResponses.add(performHealthCheckInApplicationContext(healthChecksEntry.getKey(), healthCheck));
                    }
                } catch (Exception ex) {
                    LOG.log(WARNING, "Exception executing HealthCheck : " + healthCheck.getClass().getCanonicalName(), ex);
                    // If there's any issue, set the response to an error
                    response.setStatus(500);
                }
            }
        }

        // No applications (yet), server is not ready.
        if (applicationsLoaded.isEmpty()) {
            // Application is not yet deployed
            healthCheckResponses.add(
                    HealthCheckResponse.builder()
                            .name("No Application deployed")
                            .down()
                            .build()
            );
        }

        // If we haven't encountered an exception, construct the JSON response
        if (response.getStatus() != 500) {
            constructResponse(response, healthCheckResponses, type, enablePrettyPrint);
        }
    }

    private HealthCheckResponse performHealthCheckInApplicationContext(
            String appName, HealthCheck healthCheck) {
        Thread currentThread = Thread.currentThread();
        ClassLoader originalClassLoader = currentThread.getContextClassLoader();
        Optional<WebComponentInvocation> wciOpt = Optional.empty();
        try {
            ApplicationInfo appInfo = applicationRegistry.get(appName);
            currentThread.setContextClassLoader(appInfo.getAppClassLoader());
            wciOpt = createWebComponentInvocation(appInfo);
            wciOpt.ifPresent(wci -> invocationManager.preInvoke(wci));
            return healthCheck.call();
        } finally {
            wciOpt.ifPresent(wci -> invocationManager.postInvoke(wci));
            currentThread.setContextClassLoader(originalClassLoader);
        }
    }

    private Optional<WebComponentInvocation> createWebComponentInvocation(ApplicationInfo appInfo) throws InvocationException {
        return appInfo.getModuleInfos().stream()
                .map(mi -> mi.getEngineRefForContainer(WebContainer.class))
                .filter(engineRef -> engineRef != null)
                .flatMap(engineRef -> ((WebApplication) engineRef.getApplicationContainer()).getWebModules().stream())
                .findFirst()
                .map(wm -> new WebComponentInvocation(wm));
    }

    private void constructResponse(HttpServletResponse httpResponse,
            Set<HealthCheckResponse> healthCheckResponses,
            HealthCheckType type, String enablePrettyPrint) throws IOException {
        httpResponse.setContentType("application/json");

        // For each HealthCheckResponse we got from executing the health checks...
        JsonArrayBuilder checksArray = Json.createArrayBuilder();
        for (HealthCheckResponse healthCheckResponse : healthCheckResponses) {
            JsonObjectBuilder healthCheckObject = Json.createObjectBuilder();

            // Add the name and status
            healthCheckObject.add("name", healthCheckResponse.getName());
            healthCheckObject.add("status", healthCheckResponse.getStatus().toString());

            // Add data if present
            JsonObjectBuilder healthCheckData = Json.createObjectBuilder();
            if (healthCheckResponse.getData().isPresent() && !healthCheckResponse.getData().get().isEmpty()) {
                for (Map.Entry<String, Object> dataMapEntry : healthCheckResponse.getData().get().entrySet()) {
                    healthCheckData.add(dataMapEntry.getKey(), dataMapEntry.getValue().toString());
                }
            }
            healthCheckObject.add("data", healthCheckData);

            // Add finished Object to checks array
            checksArray.add(healthCheckObject);

            // Check if we need to set the response as 503. Check against status 200 so we don't repeatedly set it
            if (httpResponse.getStatus() == 200
                    && healthCheckResponse.getStatus().equals(HealthCheckResponse.Status.DOWN)) {
                httpResponse.setStatus(503);
            }
        }
       
        // Create the final aggregate object
        JsonObjectBuilder responseObject = Json.createObjectBuilder();

        // Set the aggregate status
        responseObject.add("status", httpResponse.getStatus() == 200 ? "UP" : "DOWN");

        // Add all of the checks
        responseObject.add("checks", checksArray);

        String prettyPrinting = enablePrettyPrint == null || enablePrettyPrint.equals("false") ? "" : JsonGenerator.PRETTY_PRINTING;
        JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(singletonMap(prettyPrinting, ""));
        StringWriter stringWriter = new StringWriter();

        try (JsonWriter jsonWriter = jsonWriterFactory.createWriter(stringWriter)) {
            jsonWriter.writeObject(responseObject.build());
        }
        
        // Print the outcome
        httpResponse.getOutputStream().print(stringWriter.toString());
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        List<UnprocessedChangeEvent> unchangedList = new ArrayList<>();
        for (PropertyChangeEvent event : events) {
            unchangedList.add(new UnprocessedChangeEvent(event, "Microprofile HealthCheck configuration changed:" + event.getPropertyName()
                    + " was changed from " + event.getOldValue().toString() + " to " + event.getNewValue().toString()));
        }

        return new UnprocessedChangeEvents(unchangedList);
    }
}