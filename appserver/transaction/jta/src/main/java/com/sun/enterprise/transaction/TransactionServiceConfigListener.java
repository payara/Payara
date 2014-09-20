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

package com.sun.enterprise.transaction;

import com.sun.enterprise.config.serverbeans.Config;
import java.util.*;
import java.util.logging.*;
import java.beans.PropertyChangeEvent;

import com.sun.logging.LogDomains;
import com.sun.enterprise.util.i18n.StringManager;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import javax.inject.Named;

import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;
import org.jvnet.hk2.config.types.Property;

import com.sun.enterprise.config.serverbeans.ModuleMonitoringLevels;
import com.sun.enterprise.transaction.config.TransactionService;
import com.sun.enterprise.config.serverbeans.ServerTags;

import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ObservableBean;

/**
 * ConfigListener class for TransactionService and TransactionService 
 * monitoring level changes
 *
 * @author Marina Vatkina
 */
@Service
public class TransactionServiceConfigListener implements ConfigListener, PostConstruct {

    private static final Logger _logger = LogDomains.getLogger(
            TransactionServiceConfigListener.class, LogDomains.JTA_LOGGER);

    private TransactionService ts;

    @Inject
    private ServiceLocator habitat;

    private JavaEETransactionManager tm;

    // Sting Manager for Localization
    private static StringManager sm 
           = StringManager.getManager(TransactionServiceConfigListener.class);

    /**
     * Clears the transaction associated with the caller thread
     */
    public void setTM(JavaEETransactionManager tm) {
        this.tm = tm;
    }

    @Override
    public void postConstruct() {
        // Listen to monitoring level changes
        Config c = habitat.getService(Config.class, ServerEnvironment.DEFAULT_INSTANCE_NAME);
        ts = c.getExtensionByType(TransactionService.class);
        ModuleMonitoringLevels mml = c.getMonitoringService().getModuleMonitoringLevels();
        ((ObservableBean)ConfigSupport.getImpl(mml)).addListener(this);
    }

    /****************************************************************************/
/** Implementation of org.jvnet.hk2.config.ConfigListener *********************/
/****************************************************************************/
    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {

        // Events that we can't process now because they require server restart.
        List<UnprocessedChangeEvent> unprocessedEvents = new ArrayList<UnprocessedChangeEvent>();

        for (PropertyChangeEvent event : events) {
            String eventName = event.getPropertyName();
            Object oldValue = event.getOldValue();
            Object newValue = event.getNewValue();
            boolean accepted = true;

            _logger.log(Level.FINE, "Got TransactionService change event ==== {0} {1} {2} {3}", 
                    new Object[]{event.getSource(), eventName, oldValue, newValue});

            if (oldValue != null && oldValue.equals(newValue)) {
                _logger.log(Level.FINE, "Event {0} did not change existing value of {1}", 
                        new Object[]{eventName, oldValue});
                continue;
            }

           if (event.getSource() instanceof ModuleMonitoringLevels) {
                if (eventName.equals(ServerTags.TRANSACTION_SERVICE)) {
                    String newlevel = newValue.toString();
                    _logger.log(Level.FINE, "Changing transaction monitoring level"); 
                    if ("OFF".equals(newlevel)) {
                        tm.setMonitoringEnabled(false);
                    } else if ("LOW".equals(newlevel) || "HIGH".equals(newlevel)) {
                        tm.setMonitoringEnabled(true);
                    } 
                } // else skip
           } else if (eventName.equals(ServerTags.TIMEOUT_IN_SECONDS)) {
                try {
                    tm.setDefaultTransactionTimeout(Integer.parseInt((String)newValue,10));
                    _logger.log(Level.FINE, " Transaction Timeout interval event processed for: {0}", newValue);
                } catch (Exception ex) {
                    _logger.log(Level.WARNING,"transaction.reconfig_txn_timeout_failed",ex);
                } // timeout-in-seconds

            } else if (eventName.equals(ServerTags.KEYPOINT_INTERVAL)
                    || eventName.equals(ServerTags.RETRY_TIMEOUT_IN_SECONDS)) {
                tm.handlePropertyUpdate(eventName, newValue);
                _logger.log(Level.FINE, "{0} reconfig event processed for new value: {1}", 
                        new Object[]{eventName, newValue});

            } else if (event.getPropertyName().equals("value")) {
                eventName = ((Property)event.getSource()).getName();
                _logger.log(Level.FINE, "Got Property change event for {0}", eventName);
                if (eventName.equals("purge-cancelled-transactions-after")) {
                    String v = (String)newValue;
                    if (v == null || v.length() == 0) {
                        tm.setPurgeCancelledTtransactionsAfter(0);
                    } else {
                        tm.setPurgeCancelledTtransactionsAfter(Integer.parseInt(v,10));
                    }
                } else {
                    // Not handled dynamically. Restart is required.
                    accepted = false;
                }

            } else if (event.getPropertyName().equals("name")
                    || event.getPropertyName().equals("property")) {
                // skip - means a new property added, was processed above as "value".
                _logger.log(Level.FINE, "...skipped");

            } else {
                // Not handled dynamically. Restart is required.
                accepted = false;
            }

            if (!accepted) {
                String msg = sm.getString("enterprise_distributedtx.restart_required",
                        eventName);
                _logger.log(Level.INFO, msg);
                unprocessedEvents.add(new UnprocessedChangeEvent(event, msg));
            }
        }
        return (unprocessedEvents.size() > 0) 
                ? new UnprocessedChangeEvents(unprocessedEvents) : null;
    }

}
