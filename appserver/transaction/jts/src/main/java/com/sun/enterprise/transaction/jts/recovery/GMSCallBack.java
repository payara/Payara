/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.transaction.jts.recovery;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.jts.jta.TransactionServiceProperties;
import com.sun.jts.CosTransactions.Configuration;

import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;

import com.sun.enterprise.transaction.api.ResourceRecoveryManager;
import com.sun.enterprise.transaction.jts.api.DelegatedTransactionRecoveryFence;

import org.glassfish.gms.bootstrap.GMSAdapter;
import org.glassfish.gms.bootstrap.GMSAdapterService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocator;

import com.sun.enterprise.ee.cms.core.CallBack;
import com.sun.enterprise.ee.cms.core.DistributedStateCache;
import com.sun.enterprise.ee.cms.core.GroupManagementService;
import com.sun.enterprise.ee.cms.core.FailureRecoverySignal;
import com.sun.enterprise.ee.cms.core.Signal;

import com.sun.logging.LogDomains;

public class GMSCallBack implements CallBack {

    private static final String component = "TRANSACTION-RECOVERY-SERVICE";
    private static final String TXLOGLOCATION = "TX_LOG_DIR";
    private static final String MEMBER_DETAILS = "MEMBERDETAILS";

    // Use a class from com.sun.jts subpackage
    static Logger _logger = LogDomains.getLogger(TransactionServiceProperties.class, LogDomains.TRANSACTION_LOGGER);

    private Servers servers;
    private ServiceLocator serviceLocator;

    private int waitTime;
    private DelegatedTransactionRecoveryFence fence;
    private GroupManagementService gms;
    private final long startTime;
    private final static Object lock = new Object();

    public GMSCallBack(int waitTime, ServiceLocator serviceLocator) {
        GMSAdapterService gmsAdapterService = serviceLocator.getService(GMSAdapterService.class);
        if (gmsAdapterService != null) {
            GMSAdapter gmsAdapter = gmsAdapterService.getGMSAdapter();
            if (gmsAdapter != null) {
                gmsAdapter.registerFailureRecoveryListener(component, this);

                this.serviceLocator = serviceLocator;
                servers = serviceLocator.getService(Servers.class);

                this.waitTime = waitTime;

                Properties props = TransactionServiceProperties.getJTSProperties(serviceLocator, false);
                if (!Configuration.isDBLoggingEnabled()) {
                    if (Configuration.getORB() == null) {
                        // IIOP listeners are not setup yet,
                        // Create recoveryfile file so that automatic recovery will find it even 
                        // if no XA transaction is envolved.
                        fence = RecoveryLockFile.getDelegatedTransactionRecoveryFence(this);
                    }

                    gms = gmsAdapter.getModule();
                    // Set the member details when GMS service is ready to store it
                    String instanceName = props.getProperty(Configuration.INSTANCE_NAME);
                    String logdir = props.getProperty(Configuration.LOG_DIRECTORY);
                    try {
                         _logger.log(Level.INFO, "Storing GMS instance " + instanceName +
                                 " data " + TXLOGLOCATION + " : " + logdir);
                         gms.updateMemberDetails(instanceName, TXLOGLOCATION, logdir);
                    } catch (Exception e) {
                        _logger.log(Level.WARNING, "jts.error_updating_gms", e);
                    }
                }
            }
        }
        startTime = System.currentTimeMillis();
    }

    @Override
    public void processNotification(Signal signal) {
        if (signal instanceof FailureRecoverySignal) {
            long timestamp = System.currentTimeMillis();

            if (_logger.isLoggable(Level.INFO)) {
                _logger.log(Level.INFO, "[GMSCallBack] failure recovery signal: " + signal);
            }

            // Waiting for 1 minute (or the user set value) to ensure that indoubt xids are updated into
            // the database, otherwise while doing the recovery an instance may not
            // get all the correct indoubt xids.
            try {
                Thread.sleep(waitTime*1000L);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }

            String instance = signal.getMemberToken();
            String logdir = null;
            if (Configuration.isDBLoggingEnabled()) {
                logdir = instance; // this is how logdir will be used inside the db recovery
            } else {
                Map<Serializable, Serializable> failedMemberDetails = signal.getMemberDetails();
                if (failedMemberDetails != null) {
                    logdir = (String)failedMemberDetails.get(TXLOGLOCATION);
                }
            }

            synchronized(lock) {
                _logger.log(Level.INFO, "[GMSCallBack] Recovering for instance: " + instance + 
                        " logdir: " + logdir);
                doRecovery(logdir, instance, timestamp);

                if (!Configuration.isDBLoggingEnabled()) {
                    // Find records of not finished delegated recovery and do delegated recovery on those instances.
                    while (logdir != null) {
                        logdir = finishDelegatedRecovery(logdir, timestamp);
                    }
                }
                _logger.log(Level.INFO, "[GMSCallBack] Finished recovery for instance: " + instance);
            }
        } else {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "[GMSCallBack] ignoring signal: " + signal);
            }
        }
    }

    /**
     * Find records of not finished delegated recovery in the recovery lock file on
     * this path, and do delegated recovery if such record exists
     */
    String finishDelegatedRecovery(String logdir) {
        return finishDelegatedRecovery(logdir, startTime);
    }

    /**
     * Find records of not finished delegated recovery in the recovery lock file on
     * this path and recorded before specified timestamp, and do delegated recovery if such record exists
     */
    String finishDelegatedRecovery(String logdir, long timestamp) {
        String delegatedLogDir = null;
        String instance = fence.getInstanceRecoveredFor(logdir, timestamp);
        if (_logger.isLoggable(Level.INFO)) {
            _logger.log(Level.INFO, "[GMSCallBack] Instance " + instance + " need to finish delegated recovering");
        }
        if (instance != null) {
            DistributedStateCache dsc=gms.getGroupHandle().getDistributedStateCache();
            Map<Serializable, Serializable> memberDetails = dsc.getFromCacheForPattern(MEMBER_DETAILS, instance );
            delegatedLogDir = (String)memberDetails.get(TXLOGLOCATION); 
            if (_logger.isLoggable(Level.INFO)) {
                _logger.log(Level.INFO, "[GMSCallBack] Tx log dir for instance " + instance + " is " + delegatedLogDir);
            }

            doRecovery(delegatedLogDir, instance, timestamp);
        }

        return delegatedLogDir;
    }

    private void doRecovery(String logdir, String instance, long timestamp) {
        if (isInstanceRunning(instance)) {
            return;
        }

        if (!Configuration.isDBLoggingEnabled()) {
            if (logdir == null) {
                // Could happen if instance fails BEFORE actually getting this info into distributed state cache.
                // Could also be a gms distributed state cache bug.
                _logger.log(Level.WARNING, "jts.error_getting_member_details", instance);
                return;
            }

            if (fence.isFenceRaised(logdir, instance, timestamp)) {
                if (_logger.isLoggable(Level.INFO)) {
                    _logger.log(Level.INFO, "Instance " + instance + " is already recovering");
                }
                return;
            }
        }

        try {
            if (!Configuration.isDBLoggingEnabled()) {
                fence.raiseFence(logdir, instance);
            }

            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Transaction log directory for " + instance + " is " + logdir);
                _logger.log(Level.FINE, "Starting transaction recovery of " + instance);
            }

            ResourceRecoveryManager recoveryManager = serviceLocator.getService(ResourceRecoveryManager.class);
            recoveryManager.recoverIncompleteTx(true, logdir, instance, true);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Transaction recovery of " + instance + " is completed");
            }
        } catch (Throwable e) {
            _logger.log(Level.WARNING, "jts.recovery_error", e);
        } finally {
            if (!Configuration.isDBLoggingEnabled()) {
                fence.lowerFence(logdir, instance);
            }
        }
    }

    private boolean isInstanceRunning(String instance) {
        boolean rs = false;
        for(Server server : servers.getServer()) {
            if(instance.equals(server.getName())) {
                rs = server.isRunning();
                break;
            }
        }

        return rs;
    }
}
