/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.ee.cms.core.CallBack;
import com.sun.enterprise.ee.cms.core.MessageSignal;
import com.sun.enterprise.ee.cms.core.Signal;
import org.glassfish.api.Startup;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.elasticity.api.AlertContext;
import org.glassfish.elasticity.api.MetricGatherer;
import org.glassfish.elasticity.config.serverbeans.AlertConfig;
import org.glassfish.elasticity.config.serverbeans.ElasticService;
import org.glassfish.elasticity.engine.message.ElasticMessage;
import org.glassfish.elasticity.engine.util.ElasticEngineThreadPool;
import org.glassfish.elasticity.engine.util.EngineUtil;
import org.glassfish.elasticity.engine.util.ExpressionBasedAlert;
import org.glassfish.elasticity.expression.ExpressionNode;
import org.glassfish.elasticity.expression.RemoteExpressionHandler;
import org.glassfish.elasticity.group.ElasticMessageHandler;
import org.glassfish.elasticity.group.GroupMemberEventListener;
import org.glassfish.elasticity.group.gms.GroupServiceProvider;
import org.glassfish.gms.bootstrap.GMSAdapter;
import org.glassfish.gms.bootstrap.GMSAdapterService;
import org.glassfish.hk2.PostConstruct;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@Scoped(PerLookup.class)
public class ElasticServiceContainer
        implements PostConstruct, GroupMemberEventListener, ElasticMessageHandler {

    @Inject
    private Habitat habitat;

    @Inject
    private ElasticEngineThreadPool threadPool;

    @Inject
    private EngineUtil engineUtil;

    @Inject(optional = true)
    private GMSAdapterService gmsAdapterService;

    @Inject
    private ServerEnvironment serverEnvironment;

    private Logger logger;

    private ElasticService service;

    private String name;

    private AtomicBoolean enabled = new AtomicBoolean(true);

    private AtomicInteger minSize = new AtomicInteger();

    private AtomicInteger maxSize = new AtomicInteger();

    private AtomicInteger currentSize = new AtomicInteger();

    private AtomicInteger reconfigurationPeriodInSeconds = new AtomicInteger(3 * 60);

    private ConcurrentHashMap<String, AlertContextImpl> alerts
            = new ConcurrentHashMap<String, AlertContextImpl>();

    private boolean isDAS;

    private GroupServiceProvider gsp;

    private AtomicReference<Set<String>> currentMembers = new AtomicReference<Set<String>>();

    private String remoteExpHandlerToken;

    private RemoteExpressionHandler remoteExpHandler;

    private AtomicInteger messageIdCounter = new AtomicInteger();

    private ConcurrentHashMap<String, Future[]> futureTasks
            = new ConcurrentHashMap<String, Future[]>();

    public Startup.Lifecycle getLifecycle() {
        return Startup.Lifecycle.START;
    }

    public void postConstruct() {
        logger = engineUtil.getLogger();
    }

    public void initialize(ElasticService service) {
        this.service = service;
        this.name = service.getName();
        this.enabled.set(service.getEnabled());
        this.minSize.set(service.getMin());
        this.maxSize.set(service.getMax());

        remoteExpHandlerToken = service.getName() + ":RemoteExpressionHandler";
        remoteExpHandler = new RemoteExpressionHandler(habitat, remoteExpHandlerToken);
    }

    public String getName() {
        return this.name;
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

    public synchronized void setEnabled(boolean value) {
        if (this.enabled.get() == true && value == false) {
            //Request to disable this service
        } else if (this.enabled.get() == false && value == true) {
            //Request to enable
        }

        this.enabled.set(value);
    }

    public synchronized void reconfigureClusterLimits(int minSize, int maxSize) {

        logger.log(Level.INFO, "reconfigure service; service-name=" + service.getName()
                + "; minSize=" + minSize + "; maxSize=" + maxSize);

        this.minSize.set(minSize);
        this.maxSize.set(maxSize);

        if (currentSize.get() < minSize) {
            logger.log(Level.INFO, "SCALE UP: reconfigure service; service-name=" + service.getName()
                    + "; minSize=" + minSize + "; maxSize=" + maxSize);
        } else if (currentSize.get() > maxSize) {
            logger.log(Level.INFO, "SCALE DOWN: reconfigure service; service-name=" + service.getName()
                    + "; minSize=" + minSize + "; maxSize=" + maxSize);
        }
    }

    public void setCurrentSize(int val) {
        this.currentSize.set(val);

        if (val < minSize.get()) {
            //We need to scale up
        } else if (val > maxSize.get()) {
            //We need to scale down
        }
    }

    public GroupServiceProvider getGroupServiceProvider() {
        return gsp;
    }

    public ElasticService getElasticService() {
        return service;
    }

    public void startContainer() {

        isDAS = serverEnvironment.isDas();

        if (gmsAdapterService != null && gmsAdapterService.getGMSAdapter() != null) {
            GMSAdapter gmsAdapter = gmsAdapterService.getGMSAdapter();

            gsp = new GroupServiceProvider(gmsAdapter.getModule().getInstanceName(),
                    gmsAdapter.getClusterName(), false);

            gsp.registerGroupMemberEventListener(this);
            gsp.registerGroupMessageReceiver(service.getName(), this);
        }

        if (isDAS) {
            loadAlerts();
            System.out.println("**Initialized & Loaded Alerts ElasticService = " + this.service.getName());
        }
    }

    public void stopContainer() {
        //Unload even if not DAS
        unloadAlerts();
    }

    public void addAlert(AlertConfig alertConfig) {
        try {
            System.out.println("Creating Alert[" + service.getName() + "]: " + alertConfig.getName());

            String sch = alertConfig.getSchedule().trim();
            long frequencyInSeconds = getFrequencyOfAlertExecutionInSeconds(sch);
            String alertName = alertConfig.getName();
            ExpressionBasedAlert<AlertConfig> alert = new ExpressionBasedAlert<AlertConfig>();
            alert.initialize(habitat, alertConfig);
            AlertContextImpl alertCtx = new AlertContextImpl(this, alertConfig, alert);
            ScheduledFuture<?> future =
                    threadPool.scheduleAtFixedRate(alertCtx, frequencyInSeconds, frequencyInSeconds, TimeUnit.SECONDS);
            alertCtx.setFuture(future);
            alerts.put(alertConfig.getName(), alertCtx);
            logger.log(Level.FINE, "SCHEDULED Alert[name=" + alertName + "; schedule=" + sch
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
                "name='" + name + '\'' +
                ", enabled=" + enabled +
                ", minSize=" + minSize +
                ", maxSize=" + maxSize +
                ", currentSize=" + currentSize +
                ", reconfigurationPeriodInSeconds=" + reconfigurationPeriodInSeconds +
                '}';
    }

    private void loadAlerts() {
        if (this.enabled.get() && service.getAlerts().getAlert() != null) {
            for (AlertConfig alertConfig : service.getAlerts().getAlert()) {
                addAlert(alertConfig);
            }
        }
    }

    private void unloadAlerts() {
        for (String alertName : alerts.keySet()) {
            System.out.println("Stopping alert: " + alertName);
            removeAlert(alertName);
        }
    }

    @Override
    public void onViewChange(String memberName, Collection<String> currentAliveAndReadyMembers, Collection<String> previousView, boolean isJoinEvent) {
        logger.log(Level.FINE, "ElasticEvent[service=" + service.getName() + "]: Member " + memberName
                + (isJoinEvent ? " JOINED" : " LEFT") + " the cluster"
                + "; currentView: " + currentAliveAndReadyMembers);

        Set<String> members = new HashSet<String>();
        members.addAll(currentAliveAndReadyMembers);

        currentMembers.set(members);
        currentSize.set(members.size());
    }


    public ElasticMessage createElasticMessage(String targetInstanceName, String subComponentToken) {
        ElasticMessage message = new ElasticMessage();
        message.setMessageId("" + messageIdCounter.incrementAndGet())
                .setServiceName(service.getName())
                .setSourceMemberName(serverEnvironment.getInstanceName())
                .setSubComponentName(targetInstanceName)
                .setSubComponentName(subComponentToken);

        return message;
    }

    public void sendMessage(ElasticMessage message) {
        byte[] data = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(message);
            oos.close();
            data = bos.toByteArray();

            if (message.getTargetMemberName() == null) {
//                FutureTask[] futures = null;
//                if (! message.isResponseMessage()) {
//                    futures = new FutureTask[currentMembers.get().size()];
//                }
//                futureTasks.put(message.getMessageId(), futures);
                for (String member : currentMembers.get()) {
                    gsp.sendMessage(member, service.getName(), data);
                }
            } else {
                gsp.sendMessage(message.getTargetMemberName(), service.getName(), data);
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Exception during message sending", ex);
        } finally {
            try {
                oos.close();
            } catch (Exception ex) {
            }
        }

    }

    @Override
    public void handleMessage(String senderName, String messageToken, byte[] data) {

        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(bis);
            ElasticMessage message = (ElasticMessage) ois.readObject();
            if (remoteExpHandlerToken.equals(message.getSubComponentName())) {
                if (message.isResponseMessage()) {
                    List<List<Object>> result =
                            (List<List<Object>>) message.getData();

//                    System.out.println("RECEIVED RESPONSE MESSAGE: " + result.get(0).get(0));
                } else {
                    List<List<Object>> result =
                            remoteExpHandler.handleMessage(senderName, message);

                    ElasticMessage responseMessage = new ElasticMessage();
                    responseMessage.setMessageId("" + messageIdCounter.incrementAndGet())
                            .setTargetMemberName(senderName)
                            .setServiceName(service.getName())
                            .setSourceMemberName(serverEnvironment.getInstanceName())
                            .setSubComponentName(message.getSubComponentName())
                            .setInResponseToMessageId(message.getMessageId())
                            .setIsResponseMessage(true)
                            .setData(result);

                    sendMessage(responseMessage);
                }
            } else {
                System.out.println("Received a generic message");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                ois.close();
            } catch (Exception ex) {
            }
            try {
                bis.close();
            } catch (Exception ex) {
            }
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

    private static class ResponseInfo {

        private HashMap<String, Future> response
                = new HashMap<String, Future>();

        public void add(String instanceName) {
            //response.put(instanceName, new FutureTask());
        }

        public HashMap<String, Future> getResponseInfoMap() {
            return response;
        }
    }

}
