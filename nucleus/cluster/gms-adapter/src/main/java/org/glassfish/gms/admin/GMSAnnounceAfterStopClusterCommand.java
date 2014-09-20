/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.gms.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.ee.cms.core.GMSConstants;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import org.glassfish.api.logging.LogLevel;
import java.util.logging.Logger;


@Service(name = "_gms-announce-after-stop-cluster-command")
@Supplemental(value = "stop-cluster", on = Supplemental.Timing.After, ifFailure = FailurePolicy.Warn)
@PerLookup
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.POST, 
        path="_gms-announce-after-stop-cluster-command", 
        description="_gms-announce-after-stop-cluster-command")
})
public class GMSAnnounceAfterStopClusterCommand implements AdminCommand {

    //private static final Logger logger = LogDomains.getLogger(
    //    GMSAnnounceAfterStopClusterCommand.class, LogDomains.GMS_LOGGER);

    @LoggerInfo(subsystem = "CLSTR", description="Group Management Service Admin Logger", publish=true)
    private static final String GMS_LOGGER_NAME = "javax.enterprise.cluster.gms.admin";


    @LogMessagesResourceBundle
    private static final String LOG_MESSAGES_RB = "org.glassfish.cluster.gms.LogMessages";

    static final Logger GMS_LOGGER = Logger.getLogger(GMS_LOGGER_NAME, LOG_MESSAGES_RB);

    //# GMSAnnounceAfterStopClusterCommand
    //group.stop.exception=GMSAD3003: An exception occurred while announcing GMS group shutdown: {0}
    //GMSAD3003.diag.cause.1=An unexpected exception occurred in the GMS implementation.
    //GMSAD3003.diag.check.1=Check the server log file for more information from Shoal-GMS.
    @LogMessageInfo(message = "An exception occurred while announcing GMS group shutdown: {0}",
        level="WARNING",
        cause="An unexpected exception occurred in the GMS implementation.",
        action="Check the server log file for more information from Shoal-GMS.")
    private static final String GMS_GROUP_STOP_EXCEPTION="NCLS-CLSTR-30003";

    @Param(optional = false, primary = true)
    private String clusterName;

    @Param(optional = true, defaultValue = "false")
    private boolean verbose;


    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        announceGMSGroupStopComplete(clusterName, report);
    }

    static public void announceGMSGroupStopComplete(String clusterName, ActionReport report) {
        if (report != null) {
            GMSAnnounceSupplementalInfo gmsInfo = report.getResultType(GMSAnnounceSupplementalInfo.class);
            if (gmsInfo != null && gmsInfo.gmsInitiated) {
                GMSConstants.shutdownState groupShutdownState = GMSConstants.shutdownState.COMPLETED;
                try {
                    if (gmsInfo.gms != null) {
                        gmsInfo.gms.announceGroupShutdown(clusterName, groupShutdownState);
                    }
                } catch (Throwable t) {
                    // ensure gms group startup announcement does not interfere with starting cluster.
                    GMS_LOGGER.log(LogLevel.WARNING, GMS_GROUP_STOP_EXCEPTION,
                        t.getLocalizedMessage());
                }
            }
        }
    }

}
