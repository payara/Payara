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
package fish.payara.healthcheck.mp;

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
import fish.payara.nucleus.healthcheck.configuration.MPCheckerConfiguration;
import fish.payara.microprofile.healthcheck.config.MetricsHealthCheckConfiguration;
import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import fish.payara.notification.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.ProcessingException;
import org.glassfish.config.support.PropertyResolver;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.api.PostConstruct;;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

/**
 * Health Check service that pokes MP Healthcheck endpoints of instances 
 * in the current domain to see if they are responsive
 *
 * @author jonathan coustick
 * @since 5.184
 */
@Service(name = "healthcheck-mp")
@RunLevel(10)
public class MPChecker extends BaseHealthCheck<TimeoutHealthCheckExecutionOptions, MPCheckerConfiguration> implements PostConstruct {

    private static final Logger LOGGER = Logger.getLogger(MPChecker.class.getPackage().getName());
    
    @Inject
    private Target targetUtil;

    @Inject
    private PayaraExecutorService payaraExecutorService;
    
    @Inject
    GrizzlyService grizzlyService;
    
    @Inject
    Domain domain;

    @Override
    public void postConstruct() {
        postConstruct(this, MPCheckerConfiguration.class);
    }

    @Override
    public HealthCheckResult doCheck() {
        HealthCheckResult result = new HealthCheckResult();

        //get all instances that this server knows about
        for (Server server : domain.getServers().getServer()) {

            Future taskResult = payaraExecutorService.submit(() -> {

                //get the remote server's MP HealthCheck config
                MetricsHealthCheckConfiguration metricsConfig = server.getConfig().getExtensionByType(MetricsHealthCheckConfiguration.class);
                if (metricsConfig != null && Boolean.valueOf(metricsConfig.getEnabled())) {
                    try {
                        String endpoint = metricsConfig.getEndpoint();

                        URI remote = buildURI(server, endpoint);

                        //send request to remote healthcheck endpoint to get the status
                        Client jaxrsClient = ClientBuilder.newClient();
                        WebTarget target = jaxrsClient.target(remote);

                        Response metricsResponse = target.request().accept(MediaType.APPLICATION_JSON).get();
                        switch (metricsResponse.getStatus()) {
                            case 200:
                                result.add(new HealthCheckResultEntry(HealthCheckResultStatus.GOOD, "UP"));
                                break;
                            case 503:
                                result.add(new HealthCheckResultEntry(HealthCheckResultStatus.WARNING, "DOWN"));
                                break;
                            case 500:
                                result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CRITICAL, "FAILURE"));
                                break;
                            default:
                                result.add(new HealthCheckResultEntry(HealthCheckResultStatus.CHECK_ERROR, "UNKNOWN RESPONSE"));
                                break;
                        }

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

        return result;
    }

    @Override
    public TimeoutHealthCheckExecutionOptions constructOptions(MPCheckerConfiguration c) {
        return new TimeoutHealthCheckExecutionOptions(Boolean.valueOf(c.getEnabled()), Long.parseLong(c.getTime()), asTimeUnit(c.getUnit()), 
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

}
