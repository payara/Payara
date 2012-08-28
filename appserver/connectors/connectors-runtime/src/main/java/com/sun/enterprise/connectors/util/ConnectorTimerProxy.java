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

package com.sun.enterprise.connectors.util;

import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.logging.LogDomains;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectorTimerProxy extends Timer {
    
    private volatile static ConnectorTimerProxy connectorTimer;
    private Timer timer;
    private boolean timerException = false;
    private final Object getTimerLock = new Object();
    
    private final static Logger _logger = LogDomains.getLogger(ConnectorTimerProxy.class, 
            LogDomains.RSR_LOGGER);

    private ConnectorTimerProxy(boolean isDaemon) {
        super(isDaemon);
    }

    private Timer getTimer() {
        synchronized (getTimerLock) {
            if (timer == null || timerException) {
                ClassLoader loader = null;
                try {
                    loader = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(
                            ConnectorRuntime.getRuntime().getConnectorClassLoader());
                    timer = new Timer("connector-timer-proxy", true);
                } finally {
                    Thread.currentThread().setContextClassLoader(loader);
                    timerException = false;
                }
            }
        }
        return timer;        
    }

    public static final ConnectorTimerProxy getProxy() {
        if(connectorTimer == null) {
            synchronized (ConnectorTimerProxy.class) {
                if (connectorTimer == null) {
                    connectorTimer = new ConnectorTimerProxy(true);
                }
            }
        }
        return connectorTimer;
    }
    
    /**
     * Proxy method to schedule a timer task at fixed rate.
     * The unchecked exceptions are caught here and in such cases, the timer
     * is recreated and task is rescheduled.
     * @param task
     * @param delay
     * @param period
     */
    @Override
    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
        timer = getTimer();
        try {
            timer.scheduleAtFixedRate(task, delay, period);
        } catch(Exception ex) {
            handleTimerException(ex);
            timer.scheduleAtFixedRate(task, delay, period);
        }
    }

    @Override
    public void cancel() {
        timer = getTimer();
        try {
            timer.cancel();
        } catch(Exception ex) {
            _logger.log(Level.WARNING, "exception_cancelling_timer", ex.getMessage());
        }
    }

    @Override
    public int purge() {
        int status = 0;
        timer = getTimer();
        try {
            status = timer.purge();
        } catch(Exception ex) {
            _logger.log(Level.WARNING, "exception_purging_timer",  ex.getMessage());
        }        
        return status;
    }

    /**
     * Proxy method to schedule a timer task after a specified delay.
     * The unchecked exceptions are caught here and in such cases, the timer
     * is recreated and task is rescheduled.
     * @param task
     * @param delay
     * @param period
     */
    @Override
    public void schedule(TimerTask task, long delay) {
        timer = getTimer();
        try {
            timer.schedule(task, delay);
        } catch(Exception ex) {
            handleTimerException(ex);
            timer.schedule(task, delay);
        }
    }

    /**
     * Proxy method to schedule a timer task at the specified time.
     * The unchecked exceptions are caught here and in such cases, the timer
     * is recreated and task is rescheduled.
     * @param task
     * @param delay
     * @param period
     */    
    @Override
    public void schedule(TimerTask task, Date time) {
        timer = getTimer();
        try {
            timer.schedule(task, time);
        } catch(Exception ex) {
            handleTimerException(ex);
            timer.schedule(task, time);
        }        
    }

    /**
     * Proxy method to schedule a timer task for repeated fixed-delay execution, 
     * beginning after the specified delay.
     * The unchecked exceptions are caught here and in such cases, the timer
     * is recreated and task is rescheduled.
     * @param task
     * @param delay
     * @param period
     */
    @Override
    public void schedule(TimerTask task, long delay, long period) {
        timer = getTimer();
        try {
            timer.schedule(task, delay, period);
        } catch(Exception ex) {
            handleTimerException(ex);
            timer.schedule(task, delay, period);
        }        
    }

    /**
     * Proxy method to schedule a timer task for repeated fixed-delay execution, 
     * beginning after the specified delay.
     * The unchecked exceptions are caught here and in such cases, the timer
     * is recreated and task is rescheduled.
     * @param task
     * @param delay
     * @param period
     */
    @Override
    public void schedule(TimerTask task, Date firstTime, long period) {
        timer = getTimer();
        try {
            timer.schedule(task, firstTime, period);
        } catch(Exception ex) {
            handleTimerException(ex);
            timer.schedule(task, firstTime, period);
        }        
    }

    /**
     * Proxy method to schedule a timer task for repeated fixed-rate execution, 
     * beginning after the specified delay.
     * The unchecked exceptions are caught here and in such cases, the timer
     * is recreated and task is rescheduled.
     * @param task
     * @param delay
     * @param period
     */
    @Override
    public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period) {
        timer = getTimer();
        try {
            timer.scheduleAtFixedRate(task, firstTime, period);
        } catch(Exception ex) {
            handleTimerException(ex);
            timer.scheduleAtFixedRate(task, firstTime, period);
        }        
    }

    /**
     * Handle any exception occured during scheduling timer. 
     * 
     * In case of unchecked exceptions, the timer is recreated to be used 
     * by the subsequent requests for scheduling.
     * @param ex exception that was caught
     */
    private void handleTimerException(Exception ex) {
        _logger.log(Level.WARNING, "exception_scheduling_timer", ex.getMessage());
        
        //In case of unchecked exceptions, timer needs to recreated.
        _logger.info("Recreating Timer and scheduling at fixed rate");
        timerException = true;
        timer = getTimer();
    }
}
