/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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
 */
package org.glassfish.elasticity.engine.container;

import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.elasticity.api.AbstractMetricGatherer;
import org.glassfish.elasticity.api.MetricGathererConfigurator;
import org.glassfish.elasticity.config.serverbeans.AlertConfig;
import org.glassfish.elasticity.config.serverbeans.ElasticServiceConfig;
import org.glassfish.elasticity.config.serverbeans.MetricGathererConfig;
import org.glassfish.elasticity.engine.message.MessageProcessor;
import org.glassfish.elasticity.engine.util.ElasticEngineThreadPool;
import org.glassfish.elasticity.engine.util.EngineUtil;
import org.glassfish.elasticity.engine.util.ExpressionBasedAlert;
import org.glassfish.elasticity.group.gms.GroupServiceProvider;
import org.glassfish.gms.bootstrap.GMSAdapter;
import org.glassfish.gms.bootstrap.GMSAdapterService;
import javax.inject.Inject;

import org.glassfish.hk2.Provider;
import org.glassfish.hk2.Services;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.paas.orchestrator.ServiceOrchestrator;

/**
 * For now we associate Alerts and MetricGatherers to a Service. Later,
 *  Alerts will be  associated with an Environment.
 *
 */
@Service
@Scoped(PerLookup.class)
public class ElasticServiceContainer {

    @Inject
    private Services services;

    @Inject
    private ElasticEngineThreadPool threadPool;

    @Inject @Optional
    private GMSAdapterService gmsAdapterService;

    @Inject
    private ServerEnvironment serverEnvironment;

    @Inject
    private ServiceOrchestrator orchestrator;

    private static final Logger _logger = EngineUtil.getLogger();

    private org.glassfish.paas.orchestrator.service.spi.Service provisionedService;

    private AtomicBoolean enabled = new AtomicBoolean(true);

    private AtomicInteger minSize = new AtomicInteger();

    private AtomicInteger maxSize = new AtomicInteger();

    private AtomicInteger reconfigurationPeriodInSeconds = new AtomicInteger(3 * 60);

    private ConcurrentHashMap<String, AlertContextImpl> alerts
            = new ConcurrentHashMap<String, AlertContextImpl>();

    private boolean isDAS;

    private GroupServiceProvider gsp;

    private MessageProcessor messageProcessor;

    private long prevResizeTime = System.currentTimeMillis();

    private long RECONFIG_TIME_IN_MILLIS = 30 * 1000;

    public void start(org.glassfish.paas.orchestrator.service.spi.Service provisionedService) {
        StringBuilder sb = new StringBuilder("ServiceInfo: ");
        sb.append("\n\tEnvironment name                 : ").append(provisionedService.getServiceDescription().getAppName())
                .append("\n\tService Name               : ").append(provisionedService.getName())
                .append("\n\tProperties                 : ").append(provisionedService.getProperties())
                .append("\n\tService Configuration      : ").append(provisionedService.getServiceDescription().getConfigurations())
            .append("\n\t");
        List<MetricGathererConfig> mgConfigs = new LinkedList<MetricGathererConfig>();
        List resolvers = new LinkedList();
        for (Provider<MetricGathererConfigurator> provider : services.forContract(MetricGathererConfigurator.class).all()) {
            MetricGathererConfigurator configurator = provider.get();
            configurator.configure(provisionedService, mgConfigs, resolvers);
        }

        for (MetricGathererConfig cfg : mgConfigs) {
            _logger.log(Level.INFO, "Configuring " + cfg.getName() + "; " + cfg);
            AbstractMetricGatherer mg = services.forContract(AbstractMetricGatherer.class).named(cfg.getName()).get();
            mg.init(provisionedService, cfg);
            threadPool.scheduleAtFixedRate(new MetricGathererWrapper(mg, 300),
                    5, 5, TimeUnit.SECONDS);

        }
        
        System.out.println("Started ServiceContainer for " + sb.toString());
    }

    public String getName() {
        return this.provisionedService.getName();
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public int getMinimumSize() {
        return minSize.get();
    }

    public int getMaximumSize() {
        return maxSize.get();
    }

    public Services getServices() {
        return services;
    }

    public MessageProcessor getMessageProcessor() {
        return messageProcessor;
    }

    public synchronized void setEnabled(boolean value) {
        if (this.enabled.get() == true && value == false) {
            //Request to disable this service
        } else if (this.enabled.get() == false && value == true) {
            //Request to enable
        }

        this.enabled.set(value);
    }

    public synchronized void reconfigureClusterLimits(int minSize, int maxSize) {

        _logger.log(Level.INFO, "reconfigure service; service-name=" + provisionedService.getName()
                + "; minSize=" + minSize + "; maxSize=" + maxSize);

        this.minSize.set(minSize);
        this.maxSize.set(maxSize);

        checkClusterSize();
    }

    private synchronized void checkClusterSize() {
        try {
            if (getCurrentMemberCount() < minSize.get()) {
                _logger.log(Level.INFO, "SCALE UP: reconfigure service; service-name=" + provisionedService.getName()
                        + "; minSize=" + minSize + "; maxSize=" + maxSize);
                scaleUp();
            } else if (getCurrentMemberCount() > maxSize.get()) {
                _logger.log(Level.INFO, "SCALE DOWN: reconfigure service; service-name=" + provisionedService.getName()
                        + "; minSize=" + minSize + "; maxSize=" + maxSize);
                scaleDown();
            }
        } catch (Exception ex) {
            _logger.log(Level.WARNING, "Exception during orchestrator invocation", ex);
        }
    }

    private int getCurrentMemberCount() {
        return messageProcessor.getCurrentMemberCount();
    }
    
    public GroupServiceProvider getGroupServiceProvider() {
        return gsp;
    }

    public ElasticServiceConfig getElasticService() {
        return null;//service;
    }

    public void startContainer() {

        isDAS = "server".equals(serverEnvironment.getInstanceName());

        if (gmsAdapterService != null && gmsAdapterService.getGMSAdapterByName(provisionedService.getName()) != null) {
            GMSAdapter gmsAdapter = gmsAdapterService.getGMSAdapterByName(provisionedService.getName());

            gsp = new GroupServiceProvider(gmsAdapter.getModule().getInstanceName(),
                    gmsAdapter.getClusterName(), false);

            messageProcessor = new MessageProcessor(this, serverEnvironment);
            gsp.registerGroupMemberEventListener(messageProcessor);
            gsp.registerGroupMessageReceiver(provisionedService.getName(), messageProcessor);

//            clusterSizeMonitorTask = threadPool.scheduleAtFixedRate(
//                    new ClusterSizeMonitor(), 30, 30, TimeUnit.SECONDS);
        }

        if (isDAS) {
            loadAlerts();
            _logger.log(Level.FINE, "**Initialized & Loaded Alerts ElasticService = " + this.provisionedService.getName());
        }
    }

    public void stopContainer() {
        gsp.removeGroupMemberEventListener(messageProcessor);

        //Unload even if not DAS
        unloadAlerts();
//        if (clusterSizeMonitorTask != null) {
//            clusterSizeMonitorTask.cancel(false);
//        }
    }

    public void addAlert(AlertConfig alertConfig) {
        try {
            _logger.log(Level.INFO, "Creating Alert[" + provisionedService.getName() + "]: " + alertConfig.getName());

            String sch = alertConfig.getSchedule().trim();
            long frequencyInSeconds = getFrequencyOfAlertExecutionInSeconds(sch);
            String alertName = alertConfig.getName();
            ExpressionBasedAlert<AlertConfig> alert = new ExpressionBasedAlert<AlertConfig>();
            alert.initialize(services, alertConfig);
            AlertContextImpl alertCtx = new AlertContextImpl(this, alertConfig, alert);
            ScheduledFuture<?> future =
                    threadPool.scheduleAtFixedRate(alertCtx, frequencyInSeconds, frequencyInSeconds, TimeUnit.SECONDS);
            alertCtx.setFuture(future);
            alerts.put(alertConfig.getName(), alertCtx);
            _logger.log(Level.INFO, "SCHEDULED Alert[name=" + alertName + "; schedule=" + sch
                    + "; expression=" + alertConfig.getExpression() + "; will be executed every= " + frequencyInSeconds);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void removeAlert(String alertName) {
        AlertContextImpl ctx = alerts.remove(alertName);
        if (ctx != null && ctx.getFuture() != null) {
            ctx.getFuture().cancel(false);
        }
    }

    @Override
    public String toString() {
        return "ElasticServiceContainer{" +
                "name='" + provisionedService.getName() + '\'' +
                ", enabled=" + enabled +
                ", minSize=" + minSize +
                ", maxSize=" + maxSize +
                ", currentSize=" + getCurrentMemberCount() +
                ", reconfigurationPeriodInSeconds=" + reconfigurationPeriodInSeconds +
                '}';
    }

    private void loadAlerts() {
    }

    private void unloadAlerts() {
        for (String alertName : alerts.keySet()) {
            _logger.log(Level.FINE, "Stopping alert: " + alertName);
            removeAlert(alertName);
        }
    }


    private int getFrequencyOfAlertExecutionInSeconds(String sch) {
        String schStr = sch.trim();
        int index = 0;
        for (; index < schStr.length(); index++) {
            if (Character.isDigit(schStr.charAt(index))) {
                break;
            }
        }

        int frequencyInSeconds = 30;
        try {
            frequencyInSeconds = Integer.parseInt(schStr.substring(0, index));
        } catch (NumberFormatException nfEx) {
            //TODO
        }
        if (index < schStr.length()) {
            switch (schStr.charAt(index)) {
                case 's':
                    break;
                case 'm':
                    frequencyInSeconds *= 60;
                    break;
            }
        }

        return frequencyInSeconds;
    }

    public synchronized void scaleUp() {
        if (System.currentTimeMillis() - prevResizeTime > RECONFIG_TIME_IN_MILLIS) {
            orchestrator.scaleService(provisionedService.getServiceDescription().getAppName(), provisionedService.getName(), 1, null);
            prevResizeTime = System.currentTimeMillis();
        }

//        if (System.currentTimeMillis() - prevResizeTime > RECONFIG_TIME_IN_MILLIS) {
//            if (getCurrentMemberCount() < service.getMax()) {
//                if (_logger.isLoggable(Level.INFO)) {
//                    _logger.log(Level.INFO, "scaleUp[" + service.getName() + ": min=" + service.getMin()
//                        + "; max=" +service.getMax() + "; current=" + getCurrentMemberCount() + "]: Invoking orchestrator.scaleService("
//                        + service.getName() + ", " + service.getName() + ", 1, null)");
//                }
//
//                orchestrator.scaleService(provisionedService.getServiceDescription().getAppName(), provisionedService.getName(), 1, null);
//                prevResizeTime = System.currentTimeMillis();
//            } else {
//                if (_logger.isLoggable(Level.FINE)) {
//                   _logger.log(Level.FINE, "scaleUp[" + service.getName() + ": min=" + service.getMin()
//                    + "; max=" + service.getMax() + "; current=" + getCurrentMemberCount() + "]:  Already at max instances ( = " + service.getMax() + " )");
//                }
//            }
//        }
    }

    public synchronized void scaleDown() {
        if (System.currentTimeMillis() - prevResizeTime > RECONFIG_TIME_IN_MILLIS) {
            orchestrator.scaleService(provisionedService.getServiceDescription().getAppName(), provisionedService.getName(), -1, null);
            prevResizeTime = System.currentTimeMillis();
        }

//        if (System.currentTimeMillis() - prevResizeTime > RECONFIG_TIME_IN_MILLIS) {
//            if (getCurrentMemberCount() > service.getMin()) {
//                if (_logger.isLoggable(Level.INFO)) {
//                    _logger.log(Level.INFO, "scaleDown[" + getName() + ": min=" + service.getMin()
//                        + "; max=" +service.getMax() + "; current=" + getCurrentMemberCount() + "]: Invoking orchestrator.scaleService("
//                        + service.getName() + ", " + service.getName() + ", -1, null)");
//                }
//                orchestrator.scaleService(provisionedService.getServiceDescription().getAppName(), provisionedService.getName(), -1, null);
//                prevResizeTime = System.currentTimeMillis();
//            } else {
//                if (_logger.isLoggable(Level.FINE)) {
//                    _logger.log(Level.FINE, "scaleDown[" + service.getName() + ": min=" + service.getMin()
//                        + "; max=" +service.getMax() + "; current=" + getCurrentMemberCount() + "]: Already at min instances ( = " + service.getMin() + " )");
//                }
//            }
//        }
    }

    private class MetricGathererWrapper
            implements Runnable {

        private AbstractMetricGatherer mg;

        private int maxDataHoldingTimeInSeconds;

        private long prevPurgeTime = System.currentTimeMillis();

        private Logger logger;

        MetricGathererWrapper(AbstractMetricGatherer mg, int maxDataHoldingTimeInSeconds) {
            this.mg = mg;
            this.maxDataHoldingTimeInSeconds = maxDataHoldingTimeInSeconds;

            logger = Logger.getLogger(MetricGathererWrapper.class.getName());
        }

        public void run() {
            mg.gatherMetric();

            long now = System.currentTimeMillis();
            if (((now - prevPurgeTime) / 1000) > maxDataHoldingTimeInSeconds) {
                prevPurgeTime = now;
                logger.log(Level.INFO, "Purging data for MetricGatherer: " + mg.getClass().getName());
                mg.purgeDataOlderThan(maxDataHoldingTimeInSeconds, TimeUnit.SECONDS);
            }
        }
    }

}
