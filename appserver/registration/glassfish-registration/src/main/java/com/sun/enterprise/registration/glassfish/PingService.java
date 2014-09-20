/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.registration.glassfish;

import java.util.logging.Level;

import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import org.glassfish.hk2.api.PostConstruct;
import java.util.logging.Logger;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;
import java.io.File;
import com.sun.enterprise.registration.RegistrationException;

import com.sun.pkg.client.Image;
import com.sun.pkg.client.SystemInfo;
import org.glassfish.internal.api.PostStartupRunLevel;

import com.sun.appserv.server.util.Version;


@Service(name = "PingService")
@RunLevel(PostStartupRunLevel.VAL)
public class PingService implements PostConstruct {

    @Inject
    Logger logger;

    //@Inject
    //private ModulesRegistry modulesRegistry;

    private static final long ONE_WEEK =  7 * 24 * 60 * 60 * 1000;
    private static final String JVM_OPTION = 
            "com.sun.enterprise.registration.PING_TIMER_INTERVAL";
    private static final long TIMER_INTERVAL = 
            Long.getLong(JVM_OPTION, 7 * 24 * 60) * 60 * 1000;
    private static final String UC_PING_TIME_STAMP_FILE = ".ping";
    private static final String CONTEXT = "ping";
    //private ActiveModules activeModules;


    @Override
    public void postConstruct() {

        if (TIMER_INTERVAL <=0) {
            logger.fine("Domain Ping disabled : " + JVM_OPTION + " <= 0");
            return;                             
        }

        SystemInfo.UpdateCheckFrequency frequency =
                SystemInfo.getUpdateCheckFrequency();

        if (frequency.equals(SystemInfo.UpdateCheckFrequency.NEVER)) {
            logger.fine("Domain Ping disabled by Update Center option");
            return;                             
        }

        try {
            RegistrationUtil.synchUUID();
        } catch (RegistrationException ex) {
            logger.fine("Domain Ping disabled due to UUID exception.");
            logger.fine(ex.getMessage());
            return; 
        }

        //activeModules = new ActiveModules(logger, modulesRegistry);

        final Timer pingTimer = new Timer("PingService", true); //Mark the timer as daemon so that it does not hold up appserver shutdown

        TimerTask pingTask = new TimerTask() {
            public void run() {
                Image img = null;
                try {
                    HashMap<String,String> map = new HashMap<String,String>();

                    map.put("product", Version.getProductName().replace(";", ":"));
                    map.put("version", getVersionNumber());
                    map.put("context", CONTEXT);
                    // Disable module status usage tracking.
                    //map.put("modules", activeModules.generateModuleStatus());


                    img = RegistrationUtil.getUpdateCenterImage();
                    img.setMetaData(map);
                    img.refreshCatalog(img.getPreferredAuthorityName());
                    logger.log(Level.INFO, "Domain Pinged: {0}",
                        img.getPreferredAuthorityName());
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("X-JPkg-Metadata: product: " +
                            map.get("product"));
                        logger.fine("X-JPkg-Metadata: version: " +
                            map.get("version"));
                        logger.fine("X-JPkg-Metadata: context: " +
                            map.get("context"));
                        //logger.fine("X-JPkg-Metadata: modules: " + map.get("modules"));
                    }

                } catch (Exception e) {
                    // should the timer schedule be changed in case of
                    // exception?
                    if (img != null)
                        logger.log(Level.FINE, "Domain Ping: Unable to refresh catalog: {0}", img.getPreferredAuthorityName());
                    else
                        logger.log(Level.FINE, "Domain Ping: Unable to refresh catalog.  Null image.");
                    logger.fine(e.getMessage());
                }
                // set the time stamp even in case of failure to ping, 
                // so that next attempt to ping remains startup agnostic.                
                finally { 
                    try {
                        setTimeStamp();
                    } catch (Exception ex) {
                        logger.fine(ex.getMessage());
                    }
                }
            }
        };

        // nextPing is the time after which an initial ping would
        // be attempted during the current server run.  If we are due to
        // ping now we delay the ping by 120 seconds to allow the domain
        // to fully initialize.
        long nextPing = 2 * 60 * 1000;
        try {
            long current = System.currentTimeMillis();
            long lastPing = getTimeStamp();
            // This is to ensure that we do only one ping within a 24 hour
            // period, regardless of server restarts.
            if (current - lastPing <= ONE_WEEK)
                nextPing = lastPing - current + ONE_WEEK;
            if (nextPing < 0)
                nextPing = 2 * 60 * 1000;
        } catch(Exception ex) {
            logger.fine("Domain Ping: exception computing next ping time.");
            logger.fine(ex.getMessage());
            nextPing = 2 * 60 * 1000L;
        }
        
        logger.fine("Domain Ping: next ping in " + nextPing/(60 * 1000) +
            " minutes");
        // ping after nextPing milliseconds and subsequenlty after 
        // TIMER_INTERVAL intervals
        pingTimer.schedule(pingTask, nextPing, TIMER_INTERVAL);
    }

    private void setTimeStamp() throws Exception {
        File f = new File(RegistrationUtil.getRegistrationHome(), 
                UC_PING_TIME_STAMP_FILE);
        if (!f.createNewFile())
            if (!f.setLastModified(System.currentTimeMillis()))
                logger.fine("PingService: Could not update timestamp for : " + f.getAbsolutePath());
    }

    private long getTimeStamp() throws Exception {
        File f = new File(RegistrationUtil.getRegistrationHome(), 
                UC_PING_TIME_STAMP_FILE);
        if (!f.exists())
            return 0L;
        return f.lastModified();        
    }

    /*
     * Construct a cononical version number.
     */
    private String getVersionNumber() {
        StringBuilder versionNumber = new StringBuilder();

        if (Version.getMajorVersion() != null)
            versionNumber.append(Version.getMajorVersion()).append(".");
        else
            versionNumber.append("0.");

        if (Version.getMinorVersion() != null)
            versionNumber.append(Version.getMinorVersion()).append(".");
        else
            versionNumber.append("0.");

        if (Version.getUpdateVersion() != null)
            versionNumber.append(Version.getUpdateVersion()).append("-");
        else
            versionNumber.append("0-");

        if (Version.getBuildVersion() != null)
            versionNumber.append(Version.getBuildVersion());
        else
            versionNumber.append("0");

        return versionNumber.toString();
    }
}

