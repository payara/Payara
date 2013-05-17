/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.DasConfig;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.PostStartupRunLevel;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.kernel.KernelLoggerInfo;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

/**
 * A service wrapper around the dynamic reload processor.
 * <p>
 * The module system will start this service during GlassFish start-up.  In turn
 * it will start the actual reload logic to run periodically.
 * <p>
 * 
 * @author tjquinn
 */
@Service
@RunLevel(PostStartupRunLevel.VAL)
public class DynamicReloadService implements ConfigListener, PostConstruct, PreDestroy {

    @Inject
    DasConfig activeDasConfig;

    @Inject
    Applications applications;
    
    @Inject
    ServiceLocator habitat;
    
    private Logger logger;
    
    private Timer timer;
    
    private TimerTask timerTask;
    
    private DynamicReloader reloader;
    
    private static final String DEFAULT_POLL_INTERVAL_IN_SECONDS = "2";
    
    private static final List<String> configPropertyNames = Arrays.asList(
            "dynamic-reload-enabled", "dynamic-reload-poll-interval-in-seconds"
            );

    public DynamicReloadService() {
    }

    public void postConstruct() {
        logger = KernelLoggerInfo.getLogger();
        /*
         * Create the dynamic reloader right away, even if its use is disabled 
         * currently.  This way any initialization errors will appear early 
         * in the log rather than later if and when the reloader is 
         * enabled.
         */
        try {
            logger.fine("[Reloader] ReloaderService starting");
            reloader = new DynamicReloader(
                    applications,
                    habitat
                    );
            
            if (isEnabled(activeDasConfig)) {
                start(getPollIntervalInSeconds(activeDasConfig));
            } else {
                logger.fine("[Reloader] Reloader is configured as disabled, so NOT starting the periodic task");
            }
            logger.fine("[Reloader] Service start-up complete");
        } catch (Exception e) {
            logger.log(Level.SEVERE, KernelLoggerInfo.exceptionDRS, e); 
        }

    }

    public void preDestroy() {
        stop();
    }

    static String getValue(String value, String defaultValue) {
        return (value == null || value.equals("")) ? defaultValue : value;
    }
    
    private boolean isEnabled(DasConfig config) {
        return Boolean.parseBoolean(config.getDynamicReloadEnabled());
    }
    
    private int getPollIntervalInSeconds(DasConfig config) {
        int result;
        try {
            result = Integer.parseInt(config.getDynamicReloadPollIntervalInSeconds());
        } catch (NumberFormatException e) {
            result = Integer.parseInt(DEFAULT_POLL_INTERVAL_IN_SECONDS);
        }
        return result;
    }
    
    private void start(int pollIntervalInSeconds) {
        long pollIntervalInMS = pollIntervalInSeconds * 1000L;
        reloader.init();
        timer = new Timer("DynamicReloader", true);
        timer.schedule(
                timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            reloader.run();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }, 
                pollIntervalInMS, 
                pollIntervalInMS);
        logger.fine("[Reloader] Started, monitoring every " +
                    pollIntervalInSeconds + " seconds"
                    );
    }

    private void stop() {
        /*
         * Tell the running autodeployer to stop, then cancel the timer task 
         * and the timer.
         */
        logger.fine("[Reloader] Stopping");
        reloader.cancel();
        if(timerTask != null) {
            timerTask.cancel();
        }
        if (timer != null) {
            timer.cancel();
        }
    }
    
    /**
     * Reschedules the autodeployer because a configuration change has altered
     * the frequency.
     */
    private void reschedule(int pollIntervalInSeconds) {
        logger.fine("[Reloader] Restarting...");
        stop();
        try {
            reloader.waitUntilIdle();
        } catch (InterruptedException e) {
            // XXX OK to glide through here?
        }
        start(pollIntervalInSeconds);
    }

    public synchronized UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        /*
         * Deal with any changes to the DasConfig that might affect whether
         * the reloader should be stopped or started or rescheduled with a
         * different frequency.  Those change are handled here, by this
         * class.
         */
       
        /* Record any events we tried to process but could not. */
        List<UnprocessedChangeEvent> unprocessedEvents = new ArrayList<UnprocessedChangeEvent>();

        Boolean newEnabled = null;
        Integer newPollIntervalInSeconds = null;
        
        for (PropertyChangeEvent event : events) {
            String propName = event.getPropertyName();
            if (event.getSource() instanceof DasConfig) {
                if (configPropertyNames.contains(propName) && (event.getOldValue().equals(event.getNewValue()))) {
                    logger.fine("[DynamicReload] Ignoring reconfig of " + propName + 
                            " from " + event.getOldValue() + " to " + event.getNewValue());
                    continue;
                }
                if (propName.equals("dynamic-reload-enabled")) {
                    /*
                     * Either start the currently stopped reloader or stop the
                     * currently running one.
                     */
                    newEnabled = Boolean.valueOf((String) event.getNewValue());
                } else if (propName.equals("dynamic-reload-poll-interval-in-seconds")) {
                    try {
                        newPollIntervalInSeconds = new Integer((String) event.getNewValue());
                    } catch (NumberFormatException ex) {
                        String reason = ex.getClass().getName() + " " + ex.getLocalizedMessage();
                        logger.log(Level.WARNING, reason);
                    }
                }
            }
        }
        if (newEnabled != null) {
            if (newEnabled) {
                start(newPollIntervalInSeconds == null ? getPollIntervalInSeconds(activeDasConfig) : newPollIntervalInSeconds);
            } else {
                stop();
            }
        } else {
            if (newPollIntervalInSeconds != null && isEnabled(activeDasConfig)) {
                /*
                 * There is no change in whether the reloader should be running, only
                 * in how often it should run.  If it is not running now don't
                 * start it.  If it is running now, restart it to use the new
                 * polling interval.
                 */
                reschedule(newPollIntervalInSeconds);
            }
        }
        return (unprocessedEvents.size() > 0) ? new UnprocessedChangeEvents(unprocessedEvents) : null;
    }
}
