/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.elasticity.group.gms;

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.impl.client.*;
import org.glassfish.elasticity.group.GroupMemberEventListener;
import org.glassfish.elasticity.group.GroupService;
import org.glassfish.elasticity.group.ElasticMessageHandler;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mahesh Kannan
 */
public class GroupServiceProvider
        implements GroupService, CallBack {

	public static final Logger logger = Logger.getLogger("elasticity-logger");

    private String myName;

    private String groupName;

    private Properties configProps = new Properties();

    private GroupManagementService gms;

    private GroupHandle groupHandle;

    private ConcurrentHashMap<String, String> aliveInstances = new ConcurrentHashMap<String, String>();

    private List<GroupMemberEventListener> listeners = new ArrayList<GroupMemberEventListener>();

    private boolean createdAndJoinedGMSGroup;

    private AtomicLong previousViewId = new AtomicLong(-100);

    private volatile AliveAndReadyView arView;

    private ConcurrentHashMap<String, Long> lastSendMsgFailNotification = new ConcurrentHashMap<String, Long>();

    private ConcurrentHashMap<String, ElasticMessageHandler> handlers
            = new ConcurrentHashMap<String, ElasticMessageHandler>();

    public GroupServiceProvider(String myName, String groupName, boolean startGMS) {
        init(myName, groupName, startGMS);
    }

    public void processNotification(Signal notification) {
        boolean isJoin = true;
        if ((notification instanceof JoinedAndReadyNotificationSignal)
            || (notification instanceof FailureNotificationSignal)
            || (notification instanceof PlannedShutdownSignal)) {

            isJoin = notification instanceof JoinedAndReadyNotificationSignal;

            checkAndNotifyAboutCurrentAndPreviousMembers(notification.getMemberToken(), isJoin, true);
        } else if (notification instanceof MessageSignal) {
            MessageSignal messageSignal = (MessageSignal) notification;
            byte[] message = ((MessageSignal) notification).getMessage();

            if (messageSignal != null) {
                ElasticMessageHandler handler = handlers.get(messageSignal.getTargetComponent());
                if (handler != null) {
                    handler.handleMessage(messageSignal.getMemberToken(), messageSignal.getMemberToken(), message);
                }
            }
        }
    }

    private synchronized void checkAndNotifyAboutCurrentAndPreviousMembers(String memberName, boolean isJoinEvent, boolean triggeredByGMS) {

        SortedSet<String> currentAliveAndReadyMembers = gms.getGroupHandle().getCurrentAliveAndReadyCoreView().getMembers();
        AliveAndReadyView aView = gms.getGroupHandle().getPreviousAliveAndReadyCoreView();
        SortedSet<String> previousAliveAndReadyMembers = new TreeSet<String>();

        if (aView == null) { //Possible during unit tests when listeners are registered before GMS is started
            return;
        }


        long arViewId = aView.getViewId();
        long knownId = previousViewId.get();
        Signal sig = aView.getSignal();

        if (knownId < arViewId) {
            if (previousViewId.compareAndSet(knownId, arViewId)) {
                this.arView = aView;
                sig = this.arView.getSignal();
                previousAliveAndReadyMembers = this.arView.getMembers();
            } else {
                previousAliveAndReadyMembers = this.arView.getMembers();
            }
        } else {
            previousAliveAndReadyMembers = this.arView.getMembers();
        }

        //Listeners must be notified even if view has not changed.
        //This is because this method is called when a listener
        //  is registered
        for (GroupMemberEventListener listener : listeners) {
            listener.onViewChange(memberName, currentAliveAndReadyMembers,
                    previousAliveAndReadyMembers, isJoinEvent);
        }

        if (triggeredByGMS) {
            StringBuilder sb = new StringBuilder("**VIEW: ");
            sb.append("prevViewId: " + knownId).append("; curViewID: ").append(arViewId)
                    .append("; signal: ").append(sig).append(" ");
            sb.append("[current: ");
            String delim = "";
            for (String member : currentAliveAndReadyMembers) {
                sb.append(delim).append(member);
                delim = ", ";
            }
            sb.append("]  [previous: ");
            delim = "";

            for (String member : previousAliveAndReadyMembers) {
                sb.append(delim).append(member);
                delim = ", ";
            }
            sb.append("]");
            logger.log(Level.INFO, sb.toString());
            logger.log(Level.INFO, "**********************************************************************");
        }

    }

    private void init(String myName, String groupName, boolean startGMS) {
        try {
            gms = GMSFactory.getGMSModule(groupName);
        } catch (Exception e) {
            logger.severe("GMS module for group " + groupName + " not enabled");
        }

        if (gms == null) {
            if (startGMS) {
                logger.info("GroupServiceProvider *CREATING* gms module for group " + groupName);
                GroupManagementService.MemberType memberType = myName.startsWith("monitor-")
                        ? GroupManagementService.MemberType.SPECTATOR
                        : GroupManagementService.MemberType.CORE;

                configProps.put(ServiceProviderConfigurationKeys.MULTICASTADDRESS.toString(),
                        System.getProperty("MULTICASTADDRESS", "229.9.1.1"));
                configProps.put(ServiceProviderConfigurationKeys.MULTICASTPORT.toString(), 2299);
                logger.info("Is initial host=" + System.getProperty("IS_INITIAL_HOST"));
                configProps.put(ServiceProviderConfigurationKeys.IS_BOOTSTRAPPING_NODE.toString(),
                        System.getProperty("IS_INITIAL_HOST", "false"));
                if (System.getProperty("INITIAL_HOST_LIST") != null) {
                    configProps.put(ServiceProviderConfigurationKeys.VIRTUAL_MULTICAST_URI_LIST.toString(),
                            myName.equals("DAS"));
                }
                configProps.put(ServiceProviderConfigurationKeys.FAILURE_DETECTION_RETRIES.toString(),
                        System.getProperty("MAX_MISSED_HEARTBEATS", "3"));
                configProps.put(ServiceProviderConfigurationKeys.FAILURE_DETECTION_TIMEOUT.toString(),
                        System.getProperty("HEARTBEAT_FREQUENCY", "2000"));
                // added for junit testing of send and receive to self.
                // these settings are not used in glassfish config of gms anyways.
                configProps.put(ServiceProviderConfigurationKeys.LOOPBACK.toString(), "true");
                final String bindInterfaceAddress = System.getProperty("BIND_INTERFACE_ADDRESS");
                if (bindInterfaceAddress != null) {
                    configProps.put(ServiceProviderConfigurationKeys.BIND_INTERFACE_ADDRESS.toString(), bindInterfaceAddress);
                }

                gms = (GroupManagementService) GMSFactory.startGMSModule(
                        myName, groupName, memberType, configProps);

                createdAndJoinedGMSGroup = true;
            } else {
                logger.fine("**GroupServiceProvider:: Will not start GMS module for group " + groupName + ". It should have been started by now. But GMS: " + gms);
            }
        } else {
            logger.fine("**GroupServiceProvider:: GMS module for group " + groupName + " should have been started by now GMS: " + gms);
        }

        if (gms != null) {
            this.groupHandle = gms.getGroupHandle();
            this.myName = myName;
            this.groupName = groupName;

            gms.addActionFactory(new JoinNotificationActionFactoryImpl(this));
            gms.addActionFactory(new JoinedAndReadyNotificationActionFactoryImpl(this));
            gms.addActionFactory(new FailureNotificationActionFactoryImpl(this));
            gms.addActionFactory(new PlannedShutdownActionFactoryImpl(this));
            gms.addActionFactory(new MessageActionFactoryImpl(this), groupName);

            logger.info("**GroupServiceProvider:: REGISTERED member event listeners for <group, instance> => <" + groupName + ", " + myName + ">");

        } else {
            throw new IllegalStateException("GMS has not been started yet for group name: " + groupName + ". Is the cluster up and running");
        }

        if (createdAndJoinedGMSGroup) {
            try {
                gms.join();
                Thread.sleep(3000);
                gms.reportJoinedAndReadyState();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Got an exception during reportJoinedAndReadyState?", ex);
            }
        }
    }

    public List<String> getCurrentCoreMembers() {
        return groupHandle.getCurrentCoreMembers();
    }

    public void shutdown() {
        //gms.shutdown();
    }

    @Override
    public String getGroupName() {
        return groupName;
    }

    @Override
    public String getMemberName() {
        return myName;
    }

    @Override
    public boolean sendMessage(String targetMemberName, String token, byte[] data) {
        try {
            groupHandle.sendMessage(targetMemberName, token, data);
            return true;
        } catch (MemberNotInViewException memEx) {
            final String msg = "Error during groupHandle.sendMessage(" + targetMemberName + "," +
                                token + ") failed because " + targetMemberName + " is not alive?";
            logSendMsgFailure(memEx, targetMemberName, msg);
        } catch (GMSException gmsEx) {
            try {
                groupHandle.sendMessage(targetMemberName, token, data);
                return true;
            } catch (GMSException gmsEx2) {
                final String msg = "Error during groupHandle.sendMessage(" + targetMemberName + ", " +
                                   token + "; size=" + (data == null ? -1 : data.length) + ")";
                logSendMsgFailure(gmsEx2, targetMemberName, msg);
            }
        }

        return false;
    }

    // ensure that log is not spammed with these messages.
    // package private so can call from junit test
    void logSendMsgFailure(GMSException t, String targetMemberName, String message) {
        final long SEND_FAILED_NOTIFICATION_PERIOD = 1000 * 60 * 60 * 12 ;  // within a 12 hour period,only notify once.

        final Long lastNotify = lastSendMsgFailNotification.get(targetMemberName);
        final long currentTime = System.currentTimeMillis();
        if (lastNotify == null || currentTime > lastNotify + SEND_FAILED_NOTIFICATION_PERIOD) {
            lastSendMsgFailNotification.put(targetMemberName, new Long(currentTime));
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.WARNING, message, t);
            } else {
                Throwable causeT = t.getCause();
                String cause = causeT == null ? t.getMessage() : causeT.getMessage();
                logger.log(Level.WARNING, message + " Cause:" + cause);
            }
        }
    }

    public void registerGroupMessageReceiver(String serviceName, ElasticMessageHandler handler) {
        logger.info("[GroupServiceProvider]:  REGISTERED A MESSAGE LISTENER: "
                + handler + "; for token: <" + serviceName + ">");
        handlers.put(serviceName, handler);
    }

    @Override
    public void registerGroupMemberEventListener(GroupMemberEventListener listener) {
        listeners.add(listener);
        checkAndNotifyAboutCurrentAndPreviousMembers(myName, true, false);
    }

    @Override
    public void removeGroupMemberEventListener(GroupMemberEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void close() {
        if (createdAndJoinedGMSGroup) {
            shutdown();
        }

    }
}
