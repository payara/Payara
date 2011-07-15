/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.gms.bootstrap;

import com.sun.enterprise.ee.cms.core.GroupManagementService;
import org.jvnet.hk2.annotations.Contract;
import com.sun.enterprise.ee.cms.core.CallBack;

/**
 * <P>The register methods below replace GroupManagementService.addFactory methods.
 * The remove methods below replace GroupManagementService.removeFactory methods.
 *
 * <P>TODO: Example of a Leaving Listener handling FailureNotificationSignal, PlannedShutdownSignal
 * and the Rejoin subevent of AddNotificationSignal.
 */
@Contract
public interface GMSAdapter {

    GroupManagementService getModule();

    String getClusterName();

    /**
     * Registers a JoinNotification Listener.
     *
     * @param callback processes GMS notification JoinNotificationSignal
     */
    void registerJoinNotificationListener(CallBack callback);

    /**
     * Registers a JoinAndReadyNotification Listener.
     *
     * @param callback processes GMS notification JoinAndReadyNotificationSignal
     */
    void registerJoinedAndReadyNotificationListener(CallBack callback);

    /**
     * Register a listener for all events that represent a member has left the group.
     *
     * @param callback Signal can be either PlannedShutdownSignal, FailureNotificationSignal or JoinNotificationSignal(subevent Rejoin).
     */
    void registerMemberLeavingListener(CallBack callback);

    /**
     * Registers a PlannedShutdown Listener.
     *
     * @param callback processes GMS notification PlannedShutdownSignal
     */
    void registerPlannedShutdownListener(CallBack callback);

    /**
     * Registers a FailureSuspected Listener.
     *
     * @param callback processes GMS notification FailureSuspectedSignal
     */
    void registerFailureSuspectedListener(CallBack callback);

    /**
     * Registers a FailureNotification Listener.
     *
     * @param callback processes GMS notification FailureNotificationSignal
     */
    void registerFailureNotificationListener(CallBack callback);

    /**
     * Registers a FailureRecovery Listener.
     *
     * @param callback      processes GMS notification FailureRecoverySignal
     * @param componentName The name of the parent application's component that should be notified of selected for
     *                      performing recovery operations. One or more components in the parent application may
     *                      want to be notified of such selection for their respective recovery operations.
     */
    void registerFailureRecoveryListener(String componentName, CallBack callback);

    /**
     * Registers a Message Listener.
     *
     * @param componentName   Name of the component that would like to consume
     *                        Messages. One or more components in the parent application would want to
     *                        be notified when messages arrive addressed to them. This registration
     *                        allows GMS to deliver messages to specific components.
     * @param messageListener processes GMS MessageSignal
     */
    void registerMessageListener(String componentName, CallBack messageListener);

    /**
     * Registers a GroupLeadershipNotification Listener.
     *
     * @param callback processes GMS notification GroupLeadershipNotificationSignal. This event occurs when the GMS masters leaves the Group
     *                 and another member of the group takes over leadership. The signal indicates the new leader.
     */
    void registerGroupLeadershipNotificationListener(CallBack callback);

    /**
     * Remove FailureRecoveryListener for <code>componentName</code>
     * @param componentName name of the component to remove its registered CallBack.
     */
    void removeFailureRecoveryListener(String componentName);

    /**
     * Remove MessageListener for <code>componentName</code>
     * @param componentName name of the component to remove its registered CallBack.
     */
    void removeMessageListener(String componentName);

    /**
     * Remove previously registered FailureNotificationListener.
     * @param callback to be removed
     */
    void removeFailureNotificationListener(CallBack callback);

   /**
     * Remove previously registered FailureSuspectedListener.
     * @param callback to be removed
     */
    void removeFailureSuspectedListener(CallBack callback);

    /**
     * Remove previously registered JoinNotificationListener.
     * @param callback to be removed
     */
    void removeJoinNotificationListener(CallBack callback);

    /**
     * Remove previously registered JoinedAndReadyNotificationListener.
     * @param callback to be removed
     */
    void removeJoinedAndReadyNotificationListener(CallBack callback);

    /**
     * Remove previously registered PlannedShutdownListener.
     * @param callback to be removed
     */
    void removePlannedShutdownListener(CallBack callback);

    /**
     * Remove previously registered GroupLeadershipNotificationListener.
     * @param callback to be removed
     */
    void removeGroupLeadershipLNotificationistener(CallBack callback);

    /**
     * Remove previously registered Listeners related to Leaving a Group.
     * Thus, listeners for PlannedShutdown, FailureNotification and Add - Rejoin Subevent are to be removed.
     * @param callback to be removed
     */
    void removeMemberLeavingListener(CallBack callback);

    // only to be called by GMSAdapterService
    boolean initialize(String clusterName);
    void complete();

    /**
     * Returns an object that contains the current health of
     * each instance. 
     */
    HealthHistory getHealthHistory();
    
}
