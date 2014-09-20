/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ejb.containers;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.logging.LogDomains;

import org.glassfish.ejb.config.EjbTimerService;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

/**
 * ConfigListener class for the EJB Timer Service changes
 *
 * @author Marina Vatkina
 */
@Service
public class EJBTimerServiceConfigListener implements ConfigListener {

    private static final Logger _logger = LogDomains.getLogger(
            EJBTimerServiceConfigListener.class, LogDomains.EJB_LOGGER);

    // Injecting @Configured type triggers the corresponding change
    // events to be sent to this instance
    @Inject private EjbTimerService ejbt;

    /****************************************************************************/
    /** Implementation of org.jvnet.hk2.config.ConfigListener *********************/
    /****************************************************************************/
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {

        // Events that we can't process now because they require server restart.
        List<UnprocessedChangeEvent> unprocessedEvents = new ArrayList<UnprocessedChangeEvent>();

        for (PropertyChangeEvent event : events) {
            if (event.getSource() instanceof EjbTimerService) {
                Object oldValue = event.getOldValue();
                Object newValue = event.getNewValue();

                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "Got EjbTimerService change event ==== "
                            + event.getSource() + " "
                            + event.getPropertyName() + " " + oldValue + " " + newValue);
                }

                if (oldValue != null && oldValue.equals(newValue)) {
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, "Event " + event.getPropertyName()
                                + " did not change existing value of " + oldValue);
                    }
                } else {
                    unprocessedEvents.add(new UnprocessedChangeEvent(event, "Restart required to reconfigure EJB Timer Service"));
                }
            }
        }

        return (unprocessedEvents.size() > 0)
                ? new UnprocessedChangeEvents(unprocessedEvents) : null;

    }
}
