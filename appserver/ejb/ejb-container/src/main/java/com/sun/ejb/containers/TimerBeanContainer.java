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

package com.sun.ejb.containers;


import java.util.logging.*;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.ejb.containers.EjbContainerUtil;
import com.sun.ejb.containers.EjbContainerUtilImpl;
import com.sun.logging.LogDomains;

public class TimerBeanContainer
    extends StatelessSessionContainer {
    
    private EJBTimerService ejbTimerService;

    private EjbContainerUtil ejbContainerUtil = EjbContainerUtilImpl.getInstance();

    private static final Logger _logger = LogDomains.getLogger(
            TimerBeanContainer.class, LogDomains.EJB_LOGGER);
        
    /**
     * This constructor is called when the timer service system application is
     * loaded.
     * @exception Exception on error
     */
    protected TimerBeanContainer(EjbDescriptor desc, ClassLoader loader)
        throws Exception
    {
        super(desc, loader);

        //_logger.log(Level.FINE,"[TimerBeanContainer] Created "
        _logger.log(Level.INFO,"[TimerBeanContainer] Created "
                + " TimerBeanContainer: " + logParams[0]);

    }

    protected void doConcreteContainerShutdown(boolean appBeingUndeployed) {
        _logger.log(Level.INFO,"[TimerBeanContainer] Shutdown() called....");

        if (ejbTimerService != null) {
            ejbTimerService.onShutdown();
        }

        super.doConcreteContainerShutdown(appBeingUndeployed);        

        ejbContainerUtil.unsetEJBTimerService();
    }

    /**
     * Called after all the components in the container's application
     * have deployed successfully.
     */
    public void startApplication(boolean deploy) {

        super.startApplication(deploy);

        try {

            String timerLocalIntf = "com.sun.ejb.containers.TimerLocal";
            TimerLocal timerLocal = (TimerLocal) createEJBLocalBusinessObjectImpl(timerLocalIntf).
                    getClientObject(timerLocalIntf);

            //descriptor object representing this application or module
            Application application = ejbDescriptor.getApplication();
        
            //registration name of the applicaton
            String appID = application.getRegistrationName();

            // Create EJB Timer service. 
            ejbTimerService = new EJBTimerService(appID, timerLocal);
            ejbContainerUtil.setEJBTimerService(ejbTimerService);

        } catch (Exception ex) {
            _logger.log(Level.WARNING, "ejb.timer_service_init_error",
                        logParams);
            _logger.log(Level.WARNING, "", ex);
            throw new RuntimeException(ex.getMessage(), ex);
        } 

    }

    /**
     * Call setSessionContext even though TimerBean doesn't implement
     * SessionBean interface.
     */
    @Override
    void setSessionContext(Object ejb, SessionContextImpl context) {
        if( ejb instanceof TimerBean ) {
            ((TimerBean)ejb).setSessionContext(context);
        }
    }

} //TimerBeanContainer.java
