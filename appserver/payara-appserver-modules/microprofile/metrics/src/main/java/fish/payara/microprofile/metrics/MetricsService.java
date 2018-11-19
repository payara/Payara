/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.microprofile.metrics.admin.MetricsServiceConfiguration;
import fish.payara.microprofile.metrics.cdi.MetricsHelper;
import fish.payara.microprofile.metrics.exception.NoSuchMetricException;
import fish.payara.microprofile.metrics.exception.NoSuchRegistryException;
import fish.payara.microprofile.metrics.impl.MetricRegistryImpl;
import fish.payara.microprofile.metrics.jmx.MBeanMetadata;
import fish.payara.microprofile.metrics.jmx.MBeanMetadataConfig;
import fish.payara.microprofile.metrics.jmx.MBeanMetadataHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.bind.JAXB;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;
import static org.eclipse.microprofile.metrics.MetricRegistry.Type.BASE;
import static org.eclipse.microprofile.metrics.MetricRegistry.Type.VENDOR;
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

    private static final Logger LOGGER = Logger.getLogger(MetricsService.class.getName());

    @Inject
    Events events;

    @Inject
    private ServerEnvironment serverEnv;
    
    @Inject
    ServiceLocator serviceLocator;
    
    @Inject
    private MBeanMetadataHelper helper;
    
    private MetricsServiceConfiguration metricsServiceConfiguration;
    
    private Boolean metricsEnabled;
    
    private Boolean metricsSecure;

    private List<MBeanMetadata> unresolvedBaseMetadataList;

    private List<MBeanMetadata> unresolvedVendorMetadataList;

    private final Map<String, MetricRegistry> REGISTRIES = new ConcurrentHashMap<>();//stores registries of base, vendor, app1, app2, ... app(n) etc

    public MetricsService() {
        
    }

    @PostConstruct
    public void init() {
        events.register(this);
        metricsServiceConfiguration = serviceLocator.getService(MetricsServiceConfiguration.class);
        // Only start if metrics are enabled
        if (isMetricsEnabled()) {
            Executors.newSingleThreadExecutor().submit(new Runnable(){ 
                @Override
                public void run() {
                    bootstrap();
                }
            });
        }
    }

    private void checkSystemCpuLoadIssue(MBeanMetadataConfig metadataConfig) {
        // Could be constant but placed it in method as it is a workaround until fixed in JVM.
        // TODO Make this check dependent on the JDK version (as it hopefully will get solved in the future) -> Azul fix request made?
        String mbeanSystemCPULoad = "java.lang:type=OperatingSystem/SystemCpuLoad";

        long count = 0;
        for (MBeanMetadata metadata : metadataConfig.getBaseMetadata()) {
            if (mbeanSystemCPULoad.equals(metadata.getMBean())) {
                count++;
            }
        }
        for (MBeanMetadata metadata : metadataConfig.getVendorMetadata()) {
            if (mbeanSystemCPULoad.equals(metadata.getMBean())) {
                count++;
            }
        }
        if (count > 1) {

            LOGGER.warning(String.format("Referencing the MBean value %s multiple times possibly leads to inconsistent values for the MBean value.", mbeanSystemCPULoad));
        }
    }

    @Override
    public void event(Event event) {
        if (event.is(Deployment.APPLICATION_LOADED)) {
            ApplicationInfo info = (ApplicationInfo) event.hook();
            registerApplication(info.getName());
        } else if (event.is(Deployment.APPLICATION_UNLOADED)) {
            ApplicationInfo info = (ApplicationInfo) event.hook();
            deregisterApplication(info.getName());
        }
    }

    /**
     * Initialize metrics from the metrics.xml containing the base & vendor
     * metrics metadata.
     *
     * @param metadataConfig
     */
    private void initMetadataConfig(List<MBeanMetadata> baseMetadataList, List<MBeanMetadata> vendorMetadataList, boolean isRetry) {
        Map<String, String> globalTags = MetricsHelper.getGlobalTagsMap();
        if (!baseMetadataList.isEmpty()) {
            unresolvedBaseMetadataList = helper.registerMetadata(
                    getOrAddRegistry(BASE.getName()),
                    baseMetadataList,
                    globalTags, isRetry);
        }
        if (!vendorMetadataList.isEmpty()) {
            unresolvedVendorMetadataList = helper.registerMetadata(
                    getOrAddRegistry(VENDOR.getName()),
                    vendorMetadataList,
                    globalTags, isRetry);
        }
    }

    /**
     * Registers unresolved MBeans if they have been started after the metrics
     * service.
     */
    public void reregisterMetadataConfig() {
        // Initialise the metadata lists if they haven't yet
        if (unresolvedBaseMetadataList == null || unresolvedVendorMetadataList == null) {
            bootstrap();
        } else {
            initMetadataConfig(unresolvedBaseMetadataList, unresolvedVendorMetadataList, true);
        }
    }

    private MBeanMetadataConfig getConfig() {   
        InputStream defaultConfig = MetricsHelper.class.getResourceAsStream("/metrics.xml");
        MBeanMetadataConfig config = JAXB.unmarshal(defaultConfig, MBeanMetadataConfig.class);
          
        File metricsResource = new File(serverEnv.getConfigDirPath(), "metrics.xml");
        if (metricsResource.exists()) {
            try {
                InputStream userMetrics = new FileInputStream(metricsResource);
                MBeanMetadataConfig extraConfig = JAXB.unmarshal(userMetrics, MBeanMetadataConfig.class);
                config.addBaseMetadata(extraConfig.getBaseMetadata());
                config.addVendorMetadata(extraConfig.getVendorMetadata());
                
            } catch (FileNotFoundException ex) {
                //ignore
            }
        }
        return config;
    }

    public Boolean isMetricsEnabled() {
        if (metricsEnabled == null) {
            metricsEnabled = Boolean.valueOf(metricsServiceConfiguration.getEnabled());
        }
        return metricsEnabled;
    }

    public void resetMetricsEnabledProperty() {
        metricsEnabled = null;
    }

    public Boolean isMetricsSecure() {
        if (metricsSecure == null) {
            metricsSecure = Boolean.valueOf(metricsServiceConfiguration.getSecureMetrics());
        }
        return metricsSecure;
    }
    
    public void resetMetricsSecureProperty() {
        metricsSecure = null;
    }

    public Map<String, Metric> getMetricsAsMap(String registryName) throws NoSuchRegistryException {
        MetricRegistry registry = getRegistry(registryName);
        return registry.getMetrics();
    }

    public Map<String, Metadata> getMetadataAsMap(String registryName) throws NoSuchRegistryException {
        MetricRegistry registry = getRegistry(registryName);
        return registry.getMetadata();
    }

    public Map<String, Metric> getMetricsAsMap(String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException {
        MetricRegistry registry = getRegistry(registryName);
        Map<String, Metric> metricMap = registry.getMetrics();
        if (metricMap.containsKey(metricName)) {
            return Collections.singletonMap(metricName, metricMap.get(metricName));
        } else {
            throw new NoSuchMetricException(metricName);
        }
    }
        
    public Map<String, Metadata> getMetadataAsMap(String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException {
        MetricRegistry registry = getRegistry(registryName);
        Map<String, Metadata> metricMetadataMap = registry.getMetadata();
        if (metricMetadataMap.containsKey(metricName)) {
            return Collections.singletonMap(metricName, metricMetadataMap.get(metricName));
        } else {
            throw new NoSuchMetricException(metricName);
        }
    }
    
    /**
     * Returns the Metrics registry based on respective registry name
     * 
     * @param registryName
     * @return 
     * @throws fish.payara.microprofile.metrics.exception.NoSuchRegistryException 
     */ 
    public MetricRegistry getRegistry(String registryName) throws NoSuchRegistryException {
        MetricRegistry registry = REGISTRIES.get(registryName.toLowerCase());
        if (registry == null) {
            throw new NoSuchRegistryException(registryName);
        }
        return registry;
    }
    
    public Set<String> getApplicationRegistryNames() {
        Set<String> applicationRegistries = new HashSet<>(REGISTRIES.keySet());
        applicationRegistries.remove(BASE.getName());
        applicationRegistries.remove(VENDOR.getName());
        return applicationRegistries;
    }
    
    public Set<String> getAllRegistryNames() {
        return REGISTRIES.keySet();
    }
        
    /**
     * Returns the Metrics registry based on respective registry name, 
     * if not available then add the new MetricRegistry instance
     * 
     * @param registryName
     * @return 
     */    
    public MetricRegistry getOrAddRegistry(String registryName) {
        MetricRegistry registry = REGISTRIES.get(registryName.toLowerCase());
        if (registry == null) {
            registry = new MetricRegistryImpl();
            final MetricRegistry raced = REGISTRIES.putIfAbsent(registryName.toLowerCase(), registry);
            if (raced != null) {
                registry = raced;
            }
        }
        return registry;
    }
    
    /**
     * Remove the Metrics registry
     * 
     * @param registryName
     * @return 
     */
    public MetricRegistry removeRegistry(String registryName) {
        return REGISTRIES.remove(registryName);
    }
    
    /**
     * Adds an application to the enabled map
     *
     * @param applicationName The name of the application to remove
     */
    private void registerApplication(String applicationName) {
        getOrAddRegistry(applicationName);
    }

    /**
     * Removes an application from the enabled map
     *
     * @param applicationName The name of the application to remove
     */
    private void deregisterApplication(String applicationName) {
        removeRegistry(applicationName);
    }

    /**
     * Gets the application name from the invocation manager.
     *
     * @return The application name
     */
    public String getApplicationName() {
        InvocationManager invocationManager = Globals.getDefaultBaseServiceLocator()
                .getService(InvocationManager.class);
        if (invocationManager.getCurrentInvocation() == null) {
            return invocationManager.peekAppEnvironment().getName();
        }
        String appName = invocationManager.getCurrentInvocation().getAppName();
        if (appName == null) {
            appName = invocationManager.getCurrentInvocation().getModuleName();
        }
        if (appName == null) {
            appName = invocationManager.getCurrentInvocation().getComponentId();
        }
        return appName;
    }

    private void bootstrap() {
        MBeanMetadataConfig metadataConfig = getConfig();
        checkSystemCpuLoadIssue(metadataConfig); // PAYARA 2938
        initMetadataConfig(metadataConfig.getBaseMetadata(), metadataConfig.getVendorMetadata(), false);
    }
}
