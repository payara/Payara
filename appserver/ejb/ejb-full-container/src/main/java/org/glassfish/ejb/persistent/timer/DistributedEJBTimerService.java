/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ejb.persistent.timer;

import com.sun.ejb.PersistentTimerService;
import com.sun.ejb.containers.EjbContainerUtil;
import com.sun.ejb.containers.EjbContainerUtilImpl;
import com.sun.ejb.containers.EJBTimerService;
import com.sun.enterprise.transaction.api.RecoveryResourceRegistry;
import com.sun.enterprise.transaction.spi.RecoveryEventListener;

import org.glassfish.gms.bootstrap.GMSAdapter;
import org.glassfish.gms.bootstrap.GMSAdapterService;
import com.sun.enterprise.ee.cms.core.CallBack;
import com.sun.enterprise.ee.cms.core.GMSConstants;
import com.sun.enterprise.ee.cms.core.PlannedShutdownSignal;
import com.sun.enterprise.ee.cms.core.Signal;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import org.glassfish.hk2.api.PostConstruct;

import java.util.logging.Logger;
import java.util.logging.Level;

@Service
public class DistributedEJBTimerService
    implements PersistentTimerService, RecoveryEventListener, PostConstruct, CallBack {

    private static Logger logger = EjbContainerUtilImpl.getLogger();

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
            // Register for transaction recovery events
            recoveryResourceRegistry.addEventListener(this);
        }
    }

    public void initPersistentTimerService(String target) {
        PersistentEJBTimerService.initEJBTimerService(target);
    }

    @Override
    public void processNotification(Signal signal) {
        if (signal instanceof PlannedShutdownSignal) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "[DistributedEJBTimerService] planned shutdown signal: " + signal);
            }
            PlannedShutdownSignal pssig = (PlannedShutdownSignal)signal;
            if (pssig.getEventSubType() == GMSConstants.shutdownType.INSTANCE_SHUTDOWN) {
                migrateTimers(signal.getMemberToken());
            }
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "[DistributedEJBTimerService] ignoring signal: " + signal);
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

        if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "[DistributedEJBTimerService] afterRecovery event for instance {0}", instance);
        }

        if (instance != null && !instance.equals(ejbContainerUtil.getServerEnvironment().getInstanceName())) {
            if (success) {
                migrateTimers(instance);
            } else {
                logger.log(Level.WARNING, "[DistributedEJBTimerService] Cannot perform automatic timer migration after failed transaction recovery");
            }
        }
    }

    /**
     *--------------------------------------------------------------
     * Private methods for DistributedEJBTimerService
     *--------------------------------------------------------------
     */
    private int migrateTimers( String serverId ) {
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "[DistributedEJBTimerService] migrating timers from {0}", serverId);
        }

        int result = 0;
        // Force loading TimerService if it hadn't been started
        EJBTimerService ejbTimerService = EJBTimerService.getPersistentTimerService();
        if (ejbTimerService != null && ejbTimerService.isPersistent()) {
            result = ejbTimerService.migrateTimers( serverId );
        } else {
            //throw new IllegalStateException("EJB Timer service is null. "
                    //+ "Cannot migrate timers for: " + serverId);
        }
        
        return result;
    }

} //DistributedEJBTimerService.java

