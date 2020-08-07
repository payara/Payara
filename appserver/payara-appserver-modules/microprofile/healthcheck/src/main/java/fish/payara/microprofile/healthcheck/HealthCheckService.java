/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2017-2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
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

import fish.payara.microprofile.healthcheck.config.MetricsHealthCheckConfiguration;
import fish.payara.monitoring.collect.MonitoringData;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.monitoring.collect.MonitoringWatchCollector;
import fish.payara.monitoring.collect.MonitoringWatchSource;

import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.State;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import static fish.payara.microprofile.healthcheck.HealthCheckType.HEALTH;
import static fish.payara.microprofile.healthcheck.HealthCheckType.LIVENESS;
import static fish.payara.microprofile.healthcheck.HealthCheckType.READINESS;
import static java.util.Collections.emptyList;
import static java.util.logging.Level.WARNING;
import static java.util.stream.Collectors.joining;

/**
 * Service that handles the registration, execution, and response of MicroProfile HealthChecks.
 *
 * @author Andrew Pielage
 */
@Service(name = "healthcheck-service")
@RunLevel(StartupRunLevel.VAL)
public class HealthCheckService implements EventListener, ConfigListener, MonitoringDataSource, MonitoringWatchSource {

    @Inject
    private Events events;

    @Inject
    private ApplicationRegistry applicationRegistry;

    @Inject
    private MetricsHealthCheckConfiguration configuration;

    @Inject
    private ConfigProviderResolver microConfigResolver;

    private boolean backwardCompEnabled;

    private static final Logger LOG = Logger.getLogger(HealthCheckService.class.getName());

    private final Map<String, Set<HealthCheck>> health = new ConcurrentHashMap<>();
    private final Map<String, Set<HealthCheck>> readiness = new ConcurrentHashMap<>();
    private final Map<String, Set<HealthCheck>> liveness = new ConcurrentHashMap<>();

    private final Map<String, ClassLoader> applicationClassLoaders = new ConcurrentHashMap<>();
    private final List<String> applicationsLoaded = new CopyOnWriteArrayList<>();

    private final AtomicReference<Map<String, Set<String>>> checksCollected = new AtomicReference<>();

    private static final String BACKWARD_COMP_ENABLED_PROPERTY = "MP_HEALTH_BACKWARD_COMPATIBILITY_ENABLED";

    @PostConstruct
    public void postConstruct() {
        if (events == null) {
            events = Globals.getDefaultBaseServiceLocator().getService(Events.class);
        }
        events.register(this);
        this.backwardCompEnabled = microConfigResolver.getConfig()
                .getOptionalValue(BACKWARD_COMP_ENABLED_PROPERTY, Boolean.class)
                .orElse(false);
    }

    @Override
    @MonitoringData(ns = "health", intervalSeconds = 12)
    public void collect(MonitoringDataCollector collector) {
        Map<String, Set<String>> collected = new HashMap<>();
        Map<String, List<HealthCheckResponse>> healthResponsesByAppName = collectChecks(collector, health, collected);
        Map<String, List<HealthCheckResponse>> readinessResponsesByAppName = collectChecks(collector, readiness, collected);
        Map<String, List<HealthCheckResponse>> livenessResponsesByAppName = collectChecks(collector, liveness, collected);
        checksCollected.set(collected);
        if (!collected.isEmpty()) {
            List<HealthCheckResponse> overall = new ArrayList<>();
            overall.addAll(collectJointType(collector, "Health", healthResponsesByAppName));
            overall.addAll(collectJointType(collector, "Readiness", readinessResponsesByAppName));
            overall.addAll(collectJointType(collector, "Liveness", livenessResponsesByAppName));
            collectUpDown(collector, computeJointState("Overall", overall));
        }
        for (String appName : collected.keySet()) {
            List<HealthCheckResponse> overallByApp = new ArrayList<>();
            overallByApp.addAll(healthResponsesByAppName.getOrDefault(appName, emptyList()));
            overallByApp.addAll(readinessResponsesByAppName.getOrDefault(appName, emptyList()));
            overallByApp.addAll(livenessResponsesByAppName.getOrDefault(appName, emptyList()));
            collectUpDown(collector.group(appName), computeJointState("Overall", overallByApp));
        }
    }

    private static void collectUpDown(MonitoringDataCollector collector, HealthCheckResponse response) {
        collector.collect(response.getName(), response.getState() == State.UP ? 1 : 0);
    }

    private static List<HealthCheckResponse> collectJointType(MonitoringDataCollector collector, String type,
            Map<String, List<HealthCheckResponse>> healthResponsesByAppName) {
        List<HealthCheckResponse> allForType = new ArrayList<>();
        for (Entry<String, List<HealthCheckResponse>> e : healthResponsesByAppName.entrySet()) {
            HealthCheckResponse joint = computeJointState(type, e.getValue());
            collectUpDown(collector.group(e.getKey()), joint);
            allForType.addAll(e.getValue());
        }
        collectUpDown(collector, computeJointState(type, allForType));
        return allForType;
    }

    @Override
    public void collect(MonitoringWatchCollector collector) {
        Map<String, Set<String>> collected = checksCollected.get();
        if (collected != null) {
            for (Entry<String, Set<String>> e : collected.entrySet()) {
                String appName = e.getKey();
                for (String metric : e.getValue()) {
                    addWatch(collector, appName, metric);
                }
                addWatch(collector, appName, "Health");
                addWatch(collector, appName, "Readiness");
                addWatch(collector, appName, "Liveness");
                addWatch(collector, appName, "Overall");
            }
            if (!collected.isEmpty()) {
                addWatch(collector, null, "Health");
                addWatch(collector, null, "Readiness");
                addWatch(collector, null, "Liveness");
                addWatch(collector, null, "Overall");
            }
        }
    }

    private static void addWatch(MonitoringWatchCollector collector, String appName, String metric) {
        String series = appName == null ? "ns:health " + metric : "ns:health @:" + appName + " " + metric;
        String watchName = "RAG "+ metric + (appName == null ? "" : " @" + appName);
        collector.watch(series, watchName, "updown")
            .red(-1L, 5, false, null, 1, false)
            .amber(-1L, 1, false, null, 1, false)
            .green(1L, 1, false, null, 1, false);
    }

    private Map<String, List<HealthCheckResponse>> collectChecks(MonitoringDataCollector collector,
            Map<String, Set<HealthCheck>> checks,            Map<String, Set<String>> collected) {
        Map<String, List<HealthCheckResponse>> stateByApp = new HashMap<>();
        for (Entry<String, Set<HealthCheck>> entry : checks.entrySet()) {
            String appName = entry.getKey();
            MonitoringDataCollector appCollector = collector.group(appName);
            for (HealthCheck check : entry.getValue()) {
                HealthCheckResponse response = performHealthCheckInApplicationContext(appName, check);
                String metric = response.getName();
                Set<String> appCollected = collected.get(appName);
                // prevent adding same check more then once, unfortunately we have to run it to find that out
                if (appCollected == null || !appCollected.contains(metric)) {
                    stateByApp.computeIfAbsent(appName, key -> new ArrayList<>()).add(response);
                    collectUpDown(appCollector, response);
                    if (response.getState() == State.DOWN && response.getData().isPresent()) {
                        appCollector.annotate(metric, 0L, createAnnotation(response.getData().get()));
                    }
                    collected.computeIfAbsent(appName, key -> new HashSet<>()).add(metric);
                }
            }
        }
        return stateByApp;
    }

    private static HealthCheckResponse computeJointState(String name, Collection<HealthCheckResponse> responses) {
        long ups = responses.stream().filter(response -> response.getState() == State.UP).count();
        if (ups == responses.size()) {
            return HealthCheckResponse.up(name);
        }
        String upNames = responses.stream()
                .filter(r -> r.getState() == State.UP)
                .map(r -> r.getName())
                .collect(joining(","));
        String downNames = responses.stream()
                .filter(r -> r.getState() == State.DOWN)
                .map(r -> r.getName())
                .collect(joining(","));
        return HealthCheckResponse.named(name).down()
                .withData("up", upNames)
                .withData("down", downNames)
                .build();
    }

    private static String[] createAnnotation(Map<String, Object> data) {
        List<String> attrs = new ArrayList<>();
        for (Entry<String, Object> e : data.entrySet()) {
            Object value = e.getValue();
            if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                attrs.add(e.getKey());
                attrs.add(value.toString());
            }
        }
        return attrs.toArray(new String[0]);
    }

    @Override
    public void event(Event event) {
        // Remove healthchecks when the app is undeployed.
        if (event.is(Deployment.APPLICATION_UNLOADED)) {
            ApplicationInfo appInfo = Deployment.APPLICATION_UNLOADED.getHook(event);
            if (appInfo != null) {
                String appName = appInfo.getName();
                readiness.remove(appName);
                liveness.remove(appName);
                health.remove(appName);
                applicationClassLoaders.remove(appName);
                applicationsLoaded.remove(appName);
            }
        }

        // Keep track of all deployed applications.
        if (event.is(Deployment.APPLICATION_STARTED)) {
            ApplicationInfo appInfo = Deployment.APPLICATION_STARTED.getHook(event);
            if (appInfo != null) {
                applicationsLoaded.add(appInfo.getName());
            }
        }
    }

    public boolean isEnabled() {
        return Boolean.parseBoolean(configuration.getEnabled());
    }

    public boolean isSecurityEnabled() {
        return Boolean.parseBoolean(configuration.getSecurityEnabled());
    }

    /**
     * Register a HealthCheck to the Set of HealthChecks based on appName to execute when
     * performHealthChecks is called.
     *
     * @param appName The name of the application being deployed
     * @param healthCheck The HealthCheck to register
     */
    public void registerHealthCheck(String appName, HealthCheck healthCheck, HealthCheckType type) {
        Map<String, Set<HealthCheck>> healthChecks = getHealthChecks(type);

         // If we don't already have the app registered, we need to create a new Set for it
        if (!healthChecks.containsKey(appName)) {
            // Sync so that we don't get clashes
            synchronized (this) {
                // Check again so that we don't overwrite
                if (!healthChecks.containsKey(appName)) {
                    // Create a new Map entry and set for the Healthchecks.
                    healthChecks.put(appName, ConcurrentHashMap.newKeySet());
                }
            }
        }

        // Add the healthcheck to the Set in the Map
        healthChecks.get(appName).add(healthCheck);
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
        final Map<String, Set<HealthCheck>> healthChecks;
        if (type == READINESS) {
            healthChecks = readiness;
        } else if (type == LIVENESS) {
            healthChecks = liveness;
        } else {
            healthChecks = health;
        }
        return healthChecks;
    }

    private Map<String, Set<HealthCheck>> getCollectiveHealthChecks(HealthCheckType type) {
        final Map<String, Set<HealthCheck>> healthChecks;
        if (type == READINESS) {
            healthChecks = readiness;
        } else if (type == LIVENESS) {
            healthChecks = liveness;
        } else {
            healthChecks = new HashMap<>(health);
            BiConsumer<? super String, ? super Set<HealthCheck>> mergeHealthCheckMap
                    = (key, value) -> healthChecks.merge(key, value, (oldValue, newValue) -> {
                        oldValue.addAll(newValue);
                        return oldValue;
                    });
            //FIXME most likely this unintentionally changes the fields
            readiness.forEach(mergeHealthCheckMap);
            liveness.forEach(mergeHealthCheckMap);
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
    public void performHealthChecks(HttpServletResponse response, HealthCheckType type) throws IOException {
        Set<HealthCheckResponse> healthCheckResponses = new HashSet<>();

        // Iterate over every HealthCheck stored in the Map
        for (Entry<String, Set<HealthCheck>> healthChecksEntry : getCollectiveHealthChecks(type).entrySet()) {
            for (HealthCheck healthCheck : healthChecksEntry.getValue()) {
                // Execute the call method of the HealthCheck and add its outcome to the set of responses
                try {
                    healthCheckResponses.add(healthCheck.call());
                } catch (IllegalStateException ise) {
                    healthCheckResponses.add(performHealthCheckInApplicationContext(healthChecksEntry.getKey(), healthCheck));
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
            constructResponse(response, healthCheckResponses, type);
        }
    }

    private HealthCheckResponse performHealthCheckInApplicationContext(
            String appName, HealthCheck healthCheck) {
        Thread currentThread = Thread.currentThread();
        ClassLoader originalClassLoader = currentThread.getContextClassLoader();
        try {
            currentThread.setContextClassLoader(applicationRegistry.get(appName).getAppClassLoader());
            return healthCheck.call();
        } finally {
            currentThread.setContextClassLoader(originalClassLoader);
        }
    }

    private void constructResponse(HttpServletResponse httpResponse,
            Set<HealthCheckResponse> healthCheckResponses,
            HealthCheckType type) throws IOException {
        httpResponse.setContentType("application/json");

        // For each HealthCheckResponse we got from executing the health checks...
        JsonArrayBuilder checksArray = Json.createArrayBuilder();
        for (HealthCheckResponse healthCheckResponse : healthCheckResponses) {
            JsonObjectBuilder healthCheckObject = Json.createObjectBuilder();

            // Add the name and state
            healthCheckObject.add("name", healthCheckResponse.getName());
            healthCheckObject.add(
                    backwardCompEnabled && type == HEALTH ? "state" : "status",
                    healthCheckResponse.getState().toString()
            );

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
                    && healthCheckResponse.getState().equals(HealthCheckResponse.State.DOWN)) {
                httpResponse.setStatus(503);
            }
        }

        // Create the final aggregate object
        JsonObjectBuilder responseObject = Json.createObjectBuilder();

        // Set the aggregate status
        responseObject.add(
                backwardCompEnabled && type == HEALTH ? "outcome" : "status",
                httpResponse.getStatus() == 200 ? "UP" : "DOWN"
        );

        // Add all of the checks
        responseObject.add("checks", checksArray);

        // Print the outcome
        httpResponse.getOutputStream().print(responseObject.build().toString());
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
