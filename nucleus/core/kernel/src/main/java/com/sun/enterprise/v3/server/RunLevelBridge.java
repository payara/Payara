/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.v3.server;

import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base for all run level bridges.
 * 
 * @author Jeff Trent
 * @author Tom Beerbower
 */
@SuppressWarnings("rawtypes")
abstract class RunLevelBridge implements PostConstruct, PreDestroy {

    private final static Logger logger = Logger.getLogger(RunLevelBridge.class.getName());

    private final static Level level = AppServerStartup.level;

    @Inject
    private ServiceLocator locator;

    /**
     * The legacy type we are bridging to.
     */
    private final Class bridgeClass;

    /**
     * The service handles for the services that have been activated.
     */
    private List<ServiceHandle<?>> services = new LinkedList<ServiceHandle<?>>();


    // ----- Constructors ----------------------------------------------------

    RunLevelBridge(Class bridgeClass) {
        this.bridgeClass = bridgeClass;
    }


    // ----- PostConstruct ---------------------------------------------------

    @SuppressWarnings("unchecked")
    @Override
    public void postConstruct() {
        List<ServiceHandle<?>> serviceHandles = locator.getAllServiceHandles(bridgeClass);
        for (ServiceHandle<?> serviceHandle : serviceHandles) {
            long start = System.currentTimeMillis();
            logger.log(level, "starting {0}", serviceHandle);
            try {
                activate(serviceHandle);
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "problem starting {0}: {1}", new Object[] {serviceHandle, e.getMessage()});
                logger.log(Level.SEVERE, "nested error", e);
                throw e;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "problem starting {0}: {1}", new Object[] {serviceHandle, e.getMessage()});
                logger.log(Level.SEVERE, "nested error", e);
            }
            if (logger.isLoggable(level)) {
                logger.log(level, "start of " + serviceHandle + " done in "
                    + (System.currentTimeMillis() - start) + " ms");
            }
        }
    }


    // ----- PreDestroy ------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Override
    public void preDestroy() {
        for (ServiceHandle serviceHandle : services) {
            logger.log(level, "releasing {0}", serviceHandle);
            try {
                serviceHandle.getActiveDescriptor().dispose(serviceHandle.getService());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "problem releasing {0}: {1}", new Object[] {serviceHandle, e.getMessage()});
                logger.log(level, "nested error", e);
            }
        }
    }


    // ----- helper methods --------------------------------------------------

    /**
     * Activate a service handle.
     *
     * @param serviceHandle  the service handle
     *
     * @return the service
     */
    protected Object activate(ServiceHandle<?> serviceHandle) {
        Object service = serviceHandle.getService();
        services.add(0, serviceHandle);
        return service;
    }

}
