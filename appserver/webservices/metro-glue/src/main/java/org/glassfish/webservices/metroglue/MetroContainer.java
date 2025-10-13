/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 * Portions Copyright [2017-2021] Payara Foundation and/or affiliates
 *
 */
package org.glassfish.webservices.metroglue;

import static com.sun.enterprise.config.serverbeans.ServerTags.OBJECT_TYPE;
import static com.sun.enterprise.util.SystemPropertyConstants.HOST_NAME_PROPERTY;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static org.glassfish.api.admin.ServerEnvironment.DEFAULT_INSTANCE_NAME;
import static org.glassfish.deployment.common.DeploymentProperties.SYSTEM_ALL;
import static org.glassfish.webservices.metroglue.LogUtils.ENDPOINT_EVENT_DEPLOYED;
import static org.glassfish.webservices.metroglue.LogUtils.ENDPOINT_EVENT_LISTENER_REGISTERED;
import static org.glassfish.webservices.metroglue.LogUtils.ENDPOINT_EVENT_UNDEPLOYED;
import static org.glassfish.webservices.metroglue.LogUtils.METRO_HA_ENVIRONEMT_INITIALIZED;
import static org.glassfish.webservices.metroglue.LogUtils.WSTX_SERVICE_CANNOT_DEPLOY;
import static org.glassfish.webservices.metroglue.LogUtils.WSTX_SERVICE_DEPLOYED_EXPLICITLY;
import static org.glassfish.webservices.metroglue.LogUtils.WSTX_SERVICE_LOADING;
import static org.glassfish.webservices.metroglue.LogUtils.WSTX_SERVICE_PORT_CONFIGURATION_EXCEPTION;
import static org.glassfish.webservices.metroglue.LogUtils.WSTX_SERVICE_STARTED;
import static org.glassfish.webservices.metroglue.LogUtils.WSTX_SERVICE_UNEXPECTED_EXCEPTION;

import java.io.File;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.glassfish.api.ActionReport;
import org.glassfish.api.container.Container;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.Deployer;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.server.ServerEnvironmentImpl;
import org.glassfish.webservices.WebServiceDeploymentListener;
import org.glassfish.webservices.WebServicesDeployer;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.types.Property;

import com.sun.enterprise.config.serverbeans.AvailabilityService;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.transaction.api.RecoveryResourceRegistry;
import com.sun.enterprise.transaction.spi.RecoveryEventListener;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.xml.ws.api.ha.HighAvailabilityProvider;
import com.sun.xml.ws.tx.dev.WSATRuntimeConfig;
import com.sun.xml.wss.impl.config.SecurityConfigProvider;

import fish.payara.nucleus.hazelcast.HazelcastCore;

/**
 * @author Marek Potociar
 */
@Service(name = "org.glassfish.webservices.metroglue.MetroContainer")
@Singleton
public class MetroContainer implements PostConstruct, Container, WebServiceDeploymentListener {

    private static final Logger logger = LogUtils.getLogger();
    private static final ResourceBundle rb = logger.getResourceBundle();

    private static final String WSTX_SERVICES_APP_NAME = "wstx-services";
    private static final String METRO_APPS_INSTALL_ROOT = "lib/install/applications/metro";
    private static final Object lock = new Object();
    private final AtomicBoolean wstxServicesDeployed = new AtomicBoolean(false);
    private final AtomicBoolean wstxServicesDeploying = new AtomicBoolean(false);

    @Inject
    private ServiceLocator serviceLocator;

    @Inject
    private ServerContext serverContext;

    @Inject
    private ServerEnvironmentImpl env;

    @Inject
    private RecoveryResourceRegistry recoveryRegistry;

    @Inject
    JavaEETransactionManager txManager;

    @Inject
    HazelcastCore hazelcastCore;

    @Inject
    @Named(DEFAULT_INSTANCE_NAME)
    @Optional
    private AvailabilityService availabilityService;

    @Inject
    @Named(DEFAULT_INSTANCE_NAME)
    private SecurityService securityService;

    @Override
    public void postConstruct() {
        WebServicesDeployer.getDeploymentNotifier().addListener(this);
        logger.info(ENDPOINT_EVENT_LISTENER_REGISTERED);

        if (isCluster() && isHaEnabled()) {
            HighAvailabilityProvider.INSTANCE.initHaEnvironment(
                hazelcastCore.getMemberGroup(),
                hazelcastCore.getMemberName());

            logger.info(METRO_HA_ENVIRONEMT_INITIALIZED);
        }

        Property maxNonceAgeProperty = securityService.getProperty("MAX_NONCE_AGE");

        if (maxNonceAgeProperty != null) {
            SecurityConfigProvider.INSTANCE.init(Long.parseLong(maxNonceAgeProperty.getValue()));
        }
    }

    @Override
    public Class<? extends Deployer> getDeployer() {
        return MetroDeployer.class;
    }

    @Override
    public String getName() {
        return "metro";
    }

    @Override
    public void onDeployed(WebServiceEndpoint endpoint) {
        logger.finest(ENDPOINT_EVENT_DEPLOYED);

        if (!wstxServicesDeployed.get() && !wstxServicesDeploying.get()) {
            deployWsTxServices();
            initializeWsTxRuntime();
        }
    }

    @Override
    public void onUndeployed(WebServiceEndpoint endpoint) {
        logger.finest(ENDPOINT_EVENT_UNDEPLOYED);
        // noop
    }

    public void deployWsTxServices() {
        deployWsTxServices(null);
    }

    public void deployWsTxServices(String target) {
        synchronized (lock) {
            if (wstxServicesDeployed.get() || !wstxServicesDeploying.compareAndSet(false, true)) {
                return;
            }

            Deployment deployment = serviceLocator.getService(Deployment.class);
            boolean isRegistered = deployment.isRegistered(WSTX_SERVICES_APP_NAME);

            if (isRegistered) {
                logger.log(WARNING, WSTX_SERVICE_DEPLOYED_EXPLICITLY);
            } else {
                logger.log(INFO, WSTX_SERVICE_LOADING);

                File root = serverContext.getInstallRoot();
                File app = null;
                try {
                    app = FileUtils.getManagedFile(WSTX_SERVICES_APP_NAME + ".war", new File(root, METRO_APPS_INSTALL_ROOT));
                } catch (Exception e) {
                    logger.log(WARNING, WSTX_SERVICE_UNEXPECTED_EXCEPTION, e);
                }

                if (app == null || !app.exists()) {
                    // TODO
                    logger.log(WARNING, format(WSTX_SERVICE_CANNOT_DEPLOY,
                            "Required WAR file (" + WSTX_SERVICES_APP_NAME + ".war) is not installed"));
                } else {
                    ActionReport report = serviceLocator.getService(ActionReport.class, "plain");
                    DeployCommandParameters params = new DeployCommandParameters(app);
                    String appName = WSTX_SERVICES_APP_NAME;
                    params.name = appName;

                    try {
                        File rootScratchDir = env.getApplicationStubPath();
                        File appScratchDir = new File(rootScratchDir, appName);

                        if (isDas() && appScratchDir.createNewFile()) {
                            params.origin = OpsParams.Origin.deploy;
                            if (target != null) {
                                params.target = target;
                            }
                        } else {
                            params.origin = OpsParams.Origin.load;
                            params.target = env.getInstanceName();
                        }

                        ExtendedDeploymentContext dc = deployment.getBuilder(logger, params, report).source(app).build();
                        Properties appProps = dc.getAppProps();
                        appProps.setProperty(OBJECT_TYPE, SYSTEM_ALL);

                        deployment.deploy(dc);

                        if (report.getActionExitCode() != ActionReport.ExitCode.SUCCESS) {
                            logger.log(WARNING, format(WSTX_SERVICE_CANNOT_DEPLOY, report.getMessage()),
                                    report.getFailureCause());
                        }

                        logger.log(INFO, WSTX_SERVICE_STARTED);

                    } catch (Exception ex) {
                        logger.log(WARNING, format(WSTX_SERVICE_CANNOT_DEPLOY, ex.getLocalizedMessage()), ex);
                    }
                }
            }

            wstxServicesDeployed.set(true);
            wstxServicesDeploying.set(false);
        }
    }

    private String format(String key, String... values) {
        return MessageFormat.format(rb.getString(key), (Object[]) values);
    }

    /**
     * Embedded is a single-instance like DAS
     */
    private boolean isDas() {
        return env.isDas() || env.isEmbedded();
    }

    private boolean isCluster() {
        return !env.isDas() && !env.isEmbedded() && hazelcastCore.isEnabled();
    }

    private boolean isHaEnabled() {
        boolean haEnabled = false;
        if (availabilityService != null) {
            haEnabled = Boolean.valueOf(availabilityService.getAvailabilityEnabled());
        }

        return haEnabled;
    }

    /**
     * Initialization of WS-TX runtime configuration
     */
    private void initializeWsTxRuntime() {
        final String serverName = serverContext.getInstanceName();
        final Config config = serverContext.getConfigBean().getConfig();

        final WSATRuntimeConfig.TxlogLocationProvider txlogLocationProvider = new WSATRuntimeConfig.TxlogLocationProvider() {
            @Override
            public String getTxLogLocation() {
                return txManager.getTxLogLocation();
            }
        };

        WSATRuntimeConfig
            .initializer()
            .hostName(getHostName())
            .httpPort(getHttpPort(false, serverName, config))
            .httpsPort(getHttpPort(true, serverName, config))
            .txLogLocation(txlogLocationProvider).done();

        final WSATRuntimeConfig.RecoveryEventListener metroListener = WSATRuntimeConfig.getInstance().new WSATRecoveryEventListener();
        recoveryRegistry.addEventListener(new RecoveryEventListener() {

            @Override
            public void beforeRecovery(boolean delegated, String instance) {
                metroListener.beforeRecovery(delegated, instance);
            }

            @Override
            public void afterRecovery(boolean success, boolean delegated, String instance) {
                metroListener.afterRecovery(success, delegated, instance);
            }
        });
    }

    /**
     * Lookup the canonical host name of the system this server instance is running on.
     *
     * @return the canonical host name or null if there was an error retrieving it
     */
    private String getHostName() {
        // this value is calculated from InetAddress.getCanonicalHostName when the AS is
        // installed. asadmin then passes this value as a system property when the server
        // is started.
        return System.getProperty(HOST_NAME_PROPERTY);
    }

    /**
     * Get the http/https port number for the default virtual server of this server instance.
     * <p/>
     * If the 'secure' parameter is true, then return the secure http listener port, otherwise return
     * the non-secure http listener port.
     *
     * @param secure true if you want the secure port, false if you want the non-secure port
     * @return the port or null if there was an error retrieving it.
     */
    private String getHttpPort(boolean secure, String serverName, Config config) {
        try {
            String networkListeners = config.getHttpService().getVirtualServerByName(serverName).getNetworkListeners();
            if (networkListeners == null || networkListeners.isEmpty()) {
                return null;
            }

            for (String listenerName : networkListeners.split(",")) {
                if (listenerName == null || listenerName.isEmpty()) {
                    continue;
                }

                NetworkListener listener = config.getNetworkConfig().getNetworkListener(listenerName.trim());

                if (secure == Boolean.valueOf(listener.findHttpProtocol().getSecurityEnabled())) {
                    return listener.getPort();
                }
            }
        } catch (Throwable t) {
            // Error condition handled in wsit code
            logger.log(FINEST, WSTX_SERVICE_PORT_CONFIGURATION_EXCEPTION, t);
        }

        return null;
    }
}
