/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
import java.net.URI;
import java.net.URISyntaxException;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.v3.services.impl.GrizzlyService;
import fish.payara.appserver.micro.services.PayaraInstanceImpl;
import fish.payara.micro.ClusterCommandResult;
import fish.payara.micro.data.InstanceDescriptor;
import fish.payara.nucleus.healthcheck.configuration.MicroProfileHealthCheckerConfiguration;
import fish.payara.microprofile.healthcheck.config.MetricsHealthCheckConfiguration;
import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import fish.payara.notification.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * Health Check service that pokes MicroProfile Healthcheck endpoints of instances 
 * in the current domain to see if they are responsive
 *
 * @author jonathan coustick
 * @since 5.184
 */
@Service(name = "healthcheck-mp")
@RunLevel(10)
public class MicroProfileHealthChecker extends BaseHealthCheck<HealthCheckTimeoutExecutionOptions, MicroProfileHealthCheckerConfiguration> implements PostConstruct {

    private static final Logger LOGGER = Logger.getLogger(MicroProfileHealthChecker.class.getPackage().getName());
    private static final String GET_MP_CONFIG_STRING = "get-microprofile-healthcheck-configuration";

    @Inject
    GrizzlyService grizzlyService;

    @Inject
    Domain domain;

    @Inject
    ServerEnvironment envrionment;

    @Inject
    private PayaraInstanceImpl payaraMicro;
    
    private ExecutorService payaraExecutorService;

    @Override
    public void postConstruct() {
        payaraExecutorService = Executors.newSingleThreadExecutor();
        postConstruct(this, MicroProfileHealthCheckerConfiguration.class);
    }

    @Override
    public HealthCheckResult doCheck() {
        HealthCheckResult result = new HealthCheckResult();

        if (!envrionment.isDas()) {
            //currrently this should only run on DAS
            return result;
        }

        Map<String, Future<ClusterCommandResult>> configs = payaraMicro.executeClusteredASAdmin(GET_MP_CONFIG_STRING, new String[0]);

        HashSet<String> usedInstances = new HashSet<String>();
        
        //get all instances that this server knows about
        for (Server server : domain.getServers().getServer()) {
            
            usedInstances.add(server.getName());

            Future taskResult = payaraExecutorService.submit(() -> {

                //get the remote server's MP HealthCheck config
                MetricsHealthCheckConfiguration metricsConfig = server.getConfig().getExtensionByType(MetricsHealthCheckConfiguration.class);
                if (metricsConfig != null && Boolean.valueOf(metricsConfig.getEnabled())) {
                    try {
                        String endpoint = metricsConfig.getEndpoint();

                        URI remote = buildURI(server, endpoint);

                        result.add(pingHealthEndpoint(remote));

                    } catch (URISyntaxException ex) {
                        result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CHECK_ERROR, "INVALID ENDPOINT: " + ex.getInput()));
                    } catch (ProcessingException ex) {
                        LOGGER.log(Level.FINE, "Error sending JAX-RS Request", ex);
                        result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CRITICAL, "UNABLE TO CONNECT - " + ex.getMessage()));
                    }
                }

            });
            try {
                taskResult.get(options.getTimeout(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                LOGGER.log(Level.FINE, "Error processing MP Healthcheck checker", ex);
                result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CRITICAL, "UNABLE TO CONNECT - " + ex.toString()));
            }
        }
        
        for (InstanceDescriptor instance : payaraMicro.getClusteredPayaras()) {
            
            if (usedInstances.contains(instance.getInstanceName())) {
                continue;
            }

            Future taskResult = payaraExecutorService.submit(() -> {
                try {
                    
                    ClusterCommandResult mpHealthConfigResult = configs.get(instance.getMemberUUID()).get();
                    String values = mpHealthConfigResult.getOutput().split("\n")[1];
                    Boolean enabled = Boolean.parseBoolean(values.split(" ")[0]);
                    if (enabled) {
                        
                        String endpoint = values.split(" ", 2)[1].trim();

                        URL usedURL = instance.getApplicationURLS().get(0);
                        URI remote = new URI(usedURL.getProtocol(), usedURL.getUserInfo(), usedURL.getHost(), usedURL.getPort(), "/" + endpoint, null, null);

                        result.add(pingHealthEndpoint(remote));

                    }
                } catch (InterruptedException | ExecutionException ex) {
                    LOGGER.log(Level.FINE, "Error processing MP Healthcheck checker", ex);
                result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CRITICAL, "UNABLE TO CONNECT - " + ex.toString()));
                } catch (URISyntaxException ex) {
                    result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CHECK_ERROR, "INVALID ENDPOINT: " + ex.getInput()));
                } catch (ProcessingException ex) {
                    LOGGER.log(Level.FINE, "Error sending JAX-RS Request", ex);
                    result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CRITICAL, "UNABLE TO CONNECT - " + ex.getMessage()));
                }

            });

            try {
                taskResult.get(options.getTimeout(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                LOGGER.log(Level.FINE, "Error processing MP Healthcheck checker", ex);
                result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CRITICAL, "UNABLE TO CONNECT - " + ex.toString()));
            }

        }

        return result;
    }

    @Override
    public HealthCheckTimeoutExecutionOptions constructOptions(MicroProfileHealthCheckerConfiguration c) {
        return new HealthCheckTimeoutExecutionOptions(Boolean.valueOf(c.getEnabled()), Long.parseLong(c.getTime()), asTimeUnit(c.getUnit()),
                Long.parseLong(c.getTimeout()));
    }

    @Override
    protected String getDescription() {
        return "healthcheck.description.MPhealthcheck";
    }

    private URI buildURI(Server server, String endpoint) throws URISyntaxException {
        NetworkListener listener = server.getConfig().getNetworkConfig().getNetworkListeners().getNetworkListener().get(0);
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
        return new URI(protocol, null, (String) TranslatedConfigView.getTranslatedValue(listener.getAddress()), truePort, "/" + endpoint, null, null);
    }

    //send request to remote healthcheck endpoint to get the status
    private HealthCheckResultEntry pingHealthEndpoint(URI remote) {
        Client jaxrsClient = ClientBuilder.newClient();
        WebTarget target = jaxrsClient.target(remote);

        Response metricsResponse = target.request().accept(MediaType.APPLICATION_JSON).get();
        switch (metricsResponse.getStatus()) {
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
