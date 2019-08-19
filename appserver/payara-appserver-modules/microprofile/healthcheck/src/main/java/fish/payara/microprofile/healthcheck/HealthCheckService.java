/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017-2019] Payara Foundation and/or its affiliates. All rights reserved.
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

import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletResponse;

import fish.payara.microprofile.healthcheck.config.MetricsHealthCheckConfiguration;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;

import static java.util.logging.Level.WARNING;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
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

/**
 * Service that handles the registration, execution, and response of MicroProfile HealthChecks.
 *
 * @author Andrew Pielage
 */
@Service(name = "healthcheck-service")
@RunLevel(StartupRunLevel.VAL)
public class HealthCheckService implements EventListener, ConfigListener, MonitoringDataSource {

    @Inject
    Events events;

    @Inject
    ApplicationRegistry applicationRegistry;

    @Inject
    MetricsHealthCheckConfiguration configuration;

    private static final Logger LOG = Logger.getLogger(HealthCheckService.class.getName());

    private final Map<String, Set<HealthCheck>> healthChecks = new ConcurrentHashMap<>();
    private final Map<String, ClassLoader> applicationClassLoaders = new ConcurrentHashMap<>();
    private final List<String> applicationsLoaded = new CopyOnWriteArrayList<>();

    @Override
    public void collect(MonitoringDataCollector collector) {
        MonitoringDataCollector nsCollector = collector.in("health-check").type("check")
                .collect("size", healthChecks.size())
                .collect("enabled", isEnabled())
                .collect("securityEnabled", isSecurityEnabled());
        for (java.util.Map.Entry<String, Set<HealthCheck>> entry : healthChecks.entrySet()) {
            nsCollector.app(entry.getKey()).collect("size", entry.getValue().size());
        }
    }

    @PostConstruct
    public void postConstruct() {
        if (events == null) {
            events = Globals.getDefaultBaseServiceLocator().getService(Events.class);
        }
        events.register(this);
    }

    @Override
    public void event(Event event) {
        // Remove healthchecks when the app is undeployed.
        if (event.is(Deployment.APPLICATION_UNLOADED)) {
            ApplicationInfo appInfo = Deployment.APPLICATION_UNLOADED.getHook(event);
            if (appInfo != null) {
                healthChecks.remove(appInfo.getName());
                applicationClassLoaders.remove(appInfo.getName());
                applicationsLoaded.remove(appInfo.getName());
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
     * Register a HealthCheck to the Set of HealthChecks to execute when performHealthChecks is called.
     *
     * @param appName The name of the application being deployed
     * @param healthCheck The HealthCheck to register
     */
    public void registerHealthCheck(String appName, HealthCheck healthCheck) {
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

    /**
     * Execute the call method of every registered HealthCheck and generate the response.
     *
     * @param response The response to return
     * @throws IOException If there's an issue writing the response
     */
    public void performHealthChecks(HttpServletResponse response) throws IOException {
        Set<HealthCheckResponse> healthCheckResponses = new HashSet<>();

        // Iterate over every HealthCheck stored in the Map
        for (Map.Entry<String, Set<HealthCheck>> healthChecksEntry : healthChecks.entrySet()) {
            for (HealthCheck healthCheck : healthChecksEntry.getValue()) {
                // Execute the call method of the HealthCheck and add its outcome to the set of responses
                try {
                    healthCheckResponses.add(healthCheck.call());
                } catch (IllegalStateException ise) {
                    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
                    try {
                        Thread.currentThread().setContextClassLoader(
                                applicationRegistry.get(healthChecksEntry.getKey()).getAppClassLoader());
                        healthCheckResponses.add(healthCheck.call());
                    } finally {
                        Thread.currentThread().setContextClassLoader(originalClassLoader);
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
            healthCheckResponses.add(HealthCheckResponse.builder().name("No Application deployed").down().build());
        }

        // If we haven't encountered an exception, construct the JSON response
        if (response.getStatus() != 500) {
            constructResponse(response, healthCheckResponses);
        }
    }

    private void constructResponse(HttpServletResponse httpResponse,
            Set<HealthCheckResponse> healthCheckResponses) throws IOException {
        httpResponse.setContentType("application/json");

        // For each HealthCheckResponse we got from executing the health checks...
        JsonArrayBuilder checksArray = Json.createArrayBuilder();
        for (HealthCheckResponse healthCheckResponse : healthCheckResponses) {
            JsonObjectBuilder healthCheckObject = Json.createObjectBuilder();

            // Add the name and state
            healthCheckObject.add("name", healthCheckResponse.getName());
            healthCheckObject.add("state", healthCheckResponse.getState().toString());

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

        // Set the aggregate outcome
        if (httpResponse.getStatus() == 200) {
            responseObject.add("outcome", "UP");
        } else {
            responseObject.add("outcome", "DOWN");
        }

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
