/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.metrics;

import static fish.payara.microprofile.metrics.MetricHelper.convertToTags;
import fish.payara.microprofile.metrics.jmx.JMXMetadataConfig;
import fish.payara.microprofile.metrics.jmx.JmxWorker;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.bind.JAXB;
import org.eclipse.microprofile.config.Config;
import static org.eclipse.microprofile.metrics.Metadata.GLOBAL_TAGS_VARIABLE;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Service;

@Service(name = "microprofile-metrics-service")
@RunLevel(StartupRunLevel.VAL)
public class MetricsService implements EventListener {
    
    public static final String METRIC_ENABLED_PROPERTY = "MP_Metrics_Enabled";

    private static final Logger LOGGER = Logger.getLogger(MetricsService.class.getName());

    @Inject
    ServiceLocator habitat;
    
    @Inject
    Events events;
    
    @Inject
    private ServerEnvironment serverEnv;

    private final Map<String, Boolean> enabledMap;

    public MetricsService() {
        enabledMap = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void postConstruct() {
        events.register(this);
        MetricHelper.initMetadataConfig(JAXB.unmarshal(getConfigStream(), JMXMetadataConfig.class));
    }
    
    @Override
    public void event(Event event) {
        if (event.is(Deployment.APPLICATION_UNLOADED)) {
            ApplicationInfo info = (ApplicationInfo) event.hook();
            deregisterApplication(info.getName());
        }
    }
    

    
    private InputStream getConfigStream() {
     
        InputStream configStream = null;
        File metricsResource = new File(serverEnv.getConfigDirPath(), "metrics.xml");
        if (metricsResource.exists()) {
            try {
                configStream = new FileInputStream(metricsResource);
            } catch (FileNotFoundException ex) {
                //ignore
            }
        }
        if (configStream == null) {
            configStream = MetricHelper.class.getResourceAsStream("/metrics.xml");
        }
        return configStream;
    }

    /**
     * Checks whether metric is enabled for a given application
     *
     * @param applicationName The application to check if metric is enabled for.
     * @param config The application config to check for any override values
     * @return true if metric is enabled for the given application name
     */
    public Boolean isMetricEnabled(String applicationName, Config config) {
        if(applicationName == null){
            if (config != null) {
                return config.getOptionalValue(METRIC_ENABLED_PROPERTY, Boolean.class).orElse(Boolean.TRUE);
            } else {
                return Boolean.TRUE;
            }
        }
        if (enabledMap.containsKey(applicationName)) {
            return enabledMap.get(applicationName);
        } else {
            setMetricEnabled(applicationName, config);
            return enabledMap.get(applicationName);
        }
    }

    /**
     * Helper method that sets the enabled status for a given application.
     *
     * @param applicationName The name of the application to register
     * @param config The config to check for override values
     */
    private synchronized void setMetricEnabled(String applicationName, Config config) {
        // Double lock as multiple methods can get inside the calling if at the same time
        LOGGER.log(Level.FINER, "Checking double lock to see if something else has added the application");
        if (!enabledMap.containsKey(applicationName)) {
            if (config != null) {
                // Set the enabled value to the override value from the config, or true if it isn't configured
                enabledMap.put(applicationName,
                        config.getOptionalValue(METRIC_ENABLED_PROPERTY, Boolean.class).orElse(Boolean.TRUE));
            } else {
                LOGGER.log(Level.FINE, "No config found, so enabling metric for application: {0}",
                        applicationName);
                enabledMap.put(applicationName, Boolean.TRUE);
            }
        }
    }

    /**
     * Removes an application from the enabled map, CircuitBreaker map, and
     * bulkhead maps
     *
     * @param applicationName The name of the application to remove
     */
    private void deregisterApplication(String applicationName) {
        enabledMap.remove(applicationName);
    }

    /**
     * Gets the application name from the invocation manager.
     * @return The application name
     */
    public String getApplicationName() {
        InvocationManager invocationManager = Globals.getDefaultBaseServiceLocator()
                .getService(InvocationManager.class);
        if(invocationManager.getCurrentInvocation() == null){
            return null;
        }
        String appName = invocationManager.getCurrentInvocation().getAppName();
        if (appName == null) {
            appName = invocationManager.getCurrentInvocation().getModuleName();

            if (appName == null) {
                appName = invocationManager.getCurrentInvocation().getComponentId();
            }
        }
        
        return appName;
    }
    
}
