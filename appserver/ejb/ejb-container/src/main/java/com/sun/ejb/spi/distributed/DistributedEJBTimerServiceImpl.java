/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ejb.spi.distributed;

import com.sun.ejb.containers.EjbContainerUtil;
import com.sun.ejb.containers.EJBTimerService;
import com.sun.enterprise.transaction.api.RecoveryResourceRegistry;
import com.sun.enterprise.transaction.spi.RecoveryEventListener;
import org.glassfish.ejb.api.DistributedEJBTimerService;

import org.glassfish.gms.bootstrap.GMSAdapter;
import org.glassfish.gms.bootstrap.GMSAdapterService;
import com.sun.enterprise.ee.cms.core.CallBack;
import com.sun.enterprise.ee.cms.core.GMSConstants;
import com.sun.enterprise.ee.cms.core.PlannedShutdownSignal;
import com.sun.enterprise.ee.cms.core.Signal;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.PostConstruct;

import java.util.logging.Logger;
import java.util.logging.Level;

@Service
public class DistributedEJBTimerServiceImpl
    implements DistributedEJBTimerService, RecoveryEventListener, PostConstruct, CallBack {

    @Inject
    private EjbContainerUtil ejbContainerUtil;

    @Inject
    GMSAdapterService gmsAdapterService;

    @Inject
    RecoveryResourceRegistry recoveryResourceRegistry;

    public void postConstruct() {
        if (!ejbContainerUtil.isDas()) {
            if (gmsAdapterService != null) {
                GMSAdapter gmsAdapter = gmsAdapterService.getGMSAdapter();
                if (gmsAdapter != null) {
                    // We only register interest in the Planned Shutdown event here.
                    // Because of the dependency between transaction recovery and
                    // timer migration, the timer migration operation during an
                    // unexpected failure is initiated by the transaction recovery
                    // subsystem.
                    gmsAdapter.registerPlannedShutdownListener(this);
                }
            }
            // Do DB read before timeout in a cluster
            setPerformDBReadBeforeTimeout(true);

            // Register for transaction recovery events
            recoveryResourceRegistry.addEventListener(this);
        }
    }

    @Override
    public void processNotification(Signal signal) {
        Logger logger = ejbContainerUtil.getLogger();
        if (signal instanceof PlannedShutdownSignal) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "[DistributedEJBTimerServiceImpl] planned shutdown signal: " + signal);
            }
            PlannedShutdownSignal pssig = (PlannedShutdownSignal)signal;
            if (pssig.getEventSubType() == GMSConstants.shutdownType.INSTANCE_SHUTDOWN) {
                migrateTimers(signal.getMemberToken());
            }
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "[DistributedEJBTimerServiceImpl] ignoring signal: " + signal);
            }
        }
    }

    @Override
    public void beforeRecovery(boolean delegated, String instance) {}

    @Override
    public void afterRecovery(boolean success, boolean delegated, String instance) {
        if (!delegated) {
            return; // nothing to do
        }

        Logger logger = ejbContainerUtil.getLogger();
        if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "[DistributedEJBTimerServiceImpl] afterRecovery event for instance " + instance);
        }

        if (instance != null && !instance.equals(ejbContainerUtil.getServerEnvironment().getInstanceName())) {
            if (success) {
                migrateTimers(instance);
            } else {
                logger.log(Level.WARNING, "[DistributedEJBTimerServiceImpl] Cannot perform automatic timer migration after failed transaction recovery");
            }
        }
    }

    /**
     *--------------------------------------------------------------
     * Methods implemented for DistributedEJBTimerService
     *--------------------------------------------------------------
     */
    public int migrateTimers( String serverId ) {
        Logger logger = ejbContainerUtil.getLogger();
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "[DistributedEJBTimerServiceImpl] migrating timers from " + serverId);
        }

        int result = 0;
        EJBTimerService ejbTimerService = ejbContainerUtil.getEJBTimerService();
        if (ejbTimerService != null) {
            result = ejbTimerService.migrateTimers( serverId );
        } else {
            //throw new IllegalStateException("EJB Timer service is null. "
                    //+ "Cannot migrate timers for: " + serverId);
        }
        
        return result;
    }

    public String[] listTimers( String[] serverIds ) {
        String[] result = new String[serverIds.length];
        EJBTimerService ejbTimerService = ejbContainerUtil.getEJBTimerService();
        if (ejbTimerService != null) {
            result = ejbTimerService.listTimers( serverIds );
        } else {
            //FIXME: Should throw IllegalStateException
            for (int i=0; i<serverIds.length; i++) {
                result[i] = "0";
            }
            //throw new com.sun.enterprise.admin.common.exception.AFException("EJB Timer service is null. "
                    //+ "Cannot list timers.");
        }
        
        return result;
    }

    public void setPerformDBReadBeforeTimeout( boolean defaultDBReadValue ) {
        // Set it if and when EJBTimerService is available
        ejbContainerUtil.setEJBTimerServiceDBReadBeforeTimeout(defaultDBReadValue);
    }

} //DistributedEJBTimerServiceImpl.java

