/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
 *  file and include the License.
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
 */
package fish.payara.healthcheck.mphealth;

import com.sun.enterprise.config.serverbeans.Domain;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import javax.inject.Inject;
import javax.validation.constraints.Pattern;

import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.v3.services.impl.GrizzlyService;
import fish.payara.appserver.micro.services.PayaraInstanceImpl;
import fish.payara.micro.ClusterCommandResult;
import fish.payara.micro.data.InstanceDescriptor;
import fish.payara.nucleus.healthcheck.configuration.MicroProfileHealthCheckerConfiguration;
import fish.payara.microprofile.healthcheck.config.MetricsHealthCheckConfiguration;
import fish.payara.monitoring.collect.MonitoringData;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.monitoring.collect.MonitoringWatchCollector;
import fish.payara.monitoring.collect.MonitoringWatchSource;
import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import fish.payara.notification.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.ProcessingException;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.PropertyResolver;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

/**
 * Health Check service that pokes MicroProfile Healthcheck endpoints of instances in the current domain to see if they
 * are responsive
 *
 * @author jonathan coustick
 * @since 5.184
 */
@Service(name = "healthcheck-mp")
@RunLevel(10)
public class MicroProfileHealthChecker
extends BaseHealthCheck<HealthCheckTimeoutExecutionOptions, MicroProfileHealthCheckerConfiguration>
implements PostConstruct, MonitoringDataSource, MonitoringWatchSource {

    private static final Logger LOGGER = Logger.getLogger(MicroProfileHealthChecker.class.getPackage().getName());

    private static final String UNABLE_TO_CONNECT = "UNABLE TO CONNECT - ";
    private static final String GET_MP_CONFIG_STRING = "get-microprofile-healthcheck-configuration";

    @Inject
    private PayaraExecutorService payaraExecutorService;

    @Inject
    GrizzlyService grizzlyService;

    @Inject
    Domain domain;

    @Inject
    ServerEnvironment envrionment;

    @Inject
    private PayaraInstanceImpl payaraMicro;

    private volatile Map<String, Future<Integer>> priorCollectionTasks;

    @Override
    public void postConstruct() {
        postConstruct(this, MicroProfileHealthCheckerConfiguration.class);
    }

    @Override
    protected HealthCheckResult doCheckInternal() {
        HealthCheckResult result = new HealthCheckResult();

        if (!envrionment.isDas()) {
            // currrently this should only run on DAS
            return result;
        }

        long timeoutMillis = options.getTimeout();
        for (Future<Integer> task : pingAllInstances(timeoutMillis).values()) {
            try {
                int statusCode = task.get(timeoutMillis, MILLISECONDS);
                if (statusCode >= 0) {
                    result.add(entryFromHttpStatusCode(statusCode));
                }
            } catch (InterruptedException | TimeoutException ex) {
                LOGGER.log(Level.FINE, "Error processing MP Healthcheck checker", ex);
                result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CRITICAL,
                        UNABLE_TO_CONNECT + ex.toString()));
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof URISyntaxException) {
                    result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CHECK_ERROR,
                            "INVALID ENDPOINT: " + ((URISyntaxException) cause).getInput()));
                } else if (cause instanceof ProcessingException) {
                    LOGGER.log(Level.FINE, "Error sending JAX-RS Request", cause);
                    result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CRITICAL,
                            UNABLE_TO_CONNECT + cause.getMessage()));
                } else {
                    LOGGER.log(Level.FINE, "Error processing MP Healthcheck checker", cause);
                    result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CRITICAL,
                            UNABLE_TO_CONNECT + cause.toString()));
                }
            }
        }
        return result;
    }

    @Override
    public void collect(MonitoringWatchCollector collector) {
        if (!envrionment.isDas() || options == null || !options.isEnabled()) {
            return;
        }
        collector.watch("ns:health LivelinessUp", "Liveliness UP", "percent")
            .red(-60, null, false, null, null, false)
            .amber(-100, null, false, null, null, false)
            .green(100, null, false, null, null, false);
    }

    /**
     * The idea is that every 12 seconds this data is collected. 
     * This triggers the tasks.
     * If there are tasks from 12 seconds ago these should be done otherwise they are causing a "immediate" timeout
     * as next collection is 12 seconds later.
     * So from the point of view of the collector results are available "immediately" while the asynchronous tasks
     * had 12 seconds to finish.
     */
    @Override
    @MonitoringData(ns = "health", intervalSeconds = 12)
    public void collect(MonitoringDataCollector collector) {
        if (!envrionment.isDas() || options == null || !options.isEnabled()) {
            return;
        }
        Map<String, Future<Integer>> instances = priorCollectionTasks;
        if (instances != null) {
            int upCount = 0;
            for (Entry<String, Future<Integer>> instance : instances.entrySet()) {
                try {
                    int statusCode = instance.getValue().get(10, MILLISECONDS);
                    if (statusCode == HTTP_OK) {
                        upCount++;
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.FINE, "Failed to ping " + instance.getKey(), ex);
                }
            }
            collector.collect("LivelinessUp",100 * upCount / instances.size());
        }
        priorCollectionTasks = pingAllInstances(10000L);
    }

    /**
     * Pings MP health check endpoint of all instances and returns a map containing a {@link Future} returning the ping
     * status code for that instance. Any exceptions thrown in the process will raise a {@link ExecutionException} when
     * the {@link Future} is resolved.
     */
    private Map<String, Future<Integer>> pingAllInstances(long timeoutMillis) {
        Map<String, Future<Integer>> tasks = new ConcurrentHashMap<>();
        Map<String, Future<ClusterCommandResult>> configs = payaraMicro.executeClusteredASAdmin(GET_MP_CONFIG_STRING);

        for (Server server : domain.getServers().getServer()) {

            @Pattern(regexp = "[A-Za-z0-9_][A-Za-z0-9\\-_\\.;]*", message = "{server.invalid.name}", payload = Server.class)
            String instanceName = server.getName();
            tasks.put(instanceName, payaraExecutorService.submit(() -> {
                // get the remote server's MP HealthCheck config
                MetricsHealthCheckConfiguration metricsConfig = server.getConfig()
                        .getExtensionByType(MetricsHealthCheckConfiguration.class);
                if (metricsConfig != null && Boolean.valueOf(metricsConfig.getEnabled())) {
                    return pingHealthEndpoint(buildURI(server, metricsConfig.getEndpoint()));
                }
                return -1;
            }));
        }

        for (InstanceDescriptor instance : payaraMicro.getClusteredPayaras()) {
            String instanceName = instance.getInstanceName();
            if (tasks.containsKey(instanceName)) {
                continue;
            }
            tasks.put(instanceName, payaraExecutorService.submit(() -> {
                ClusterCommandResult mpHealthConfigResult = configs.get(instance.getMemberUUID()) //
                        .get(timeoutMillis, MILLISECONDS);
                String values = mpHealthConfigResult.getOutput().split("\n")[1];
                Boolean enabled = Boolean.parseBoolean(values.split(" ")[0]);
                if (enabled) {
                    String endpoint = values.split(" ", 2)[1].trim();
                    return pingHealthEndpoint(buildURI(instance, endpoint));
                }
                return -1;
            }));
        }
        return tasks;
    }

    @Override
    public HealthCheckTimeoutExecutionOptions constructOptions(MicroProfileHealthCheckerConfiguration c) {
        return new HealthCheckTimeoutExecutionOptions(Boolean.valueOf(c.getEnabled()), Long.parseLong(c.getTime()),
                asTimeUnit(c.getUnit()), Long.parseLong(c.getTimeout()));
    }

    @Override
    protected String getDescription() {
        return "healthcheck.description.MPhealthcheck";
    }

    private static URI buildURI(InstanceDescriptor instance, String endpoint) throws URISyntaxException {
        URL usedURL = instance.getApplicationURLS().get(0);
        return new URI(usedURL.getProtocol(), usedURL.getUserInfo(), usedURL.getHost(), usedURL.getPort(),
                "/" + endpoint, null, null);
    }

    private URI buildURI(Server server, String endpoint) throws URISyntaxException {
        NetworkListener listener = server.getConfig().getNetworkConfig().getNetworkListeners().getNetworkListener()
                .get(0);
        String protocol = Boolean.parseBoolean(listener.findHttpProtocol().getSecurityEnabled()) ? "https" : "http";
        String basePort = listener.getPort();
        Integer truePort = 8080;
        try {
            truePort = Integer.parseInt(basePort);
        } catch (NumberFormatException e) {
            PropertyResolver configProps = new PropertyResolver(domain, server.getName());
            String sysPropsPort = configProps.getPropertyValue(basePort);
            truePort = Integer.parseInt(sysPropsPort);
        }
        return new URI(protocol, null, TranslatedConfigView.expandValue(listener.getAddress()), truePort,
                "/" + endpoint, null, null);
    }

    /**
     * Sends request to remote healthcheck endpoint to get the status
     */
    private static int pingHealthEndpoint(URI remote) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) remote.toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        int statusCode = conn.getResponseCode();
        conn.disconnect();
        return statusCode;
    }

    private static HealthCheckResultEntry entryFromHttpStatusCode(int statusCode) {
        switch (statusCode) {
        case 200:
            return new HealthCheckResultEntry(HealthCheckResultStatus.GOOD, "UP");
        case 503:
            return new HealthCheckResultEntry(HealthCheckResultStatus.WARNING, "DOWN");
        case 500:
            return new HealthCheckResultEntry(HealthCheckResultStatus.CRITICAL, "FAILURE");
        default:
            return new HealthCheckResultEntry(HealthCheckResultStatus.CHECK_ERROR, "UNKNOWN RESPONSE");
        }
    }

}
