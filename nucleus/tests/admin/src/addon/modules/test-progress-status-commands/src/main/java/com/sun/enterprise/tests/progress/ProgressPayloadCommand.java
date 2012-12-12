/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.tests.progress;

import com.sun.enterprise.util.StringUtils;
import com.sun.logging.LogDomains;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ManagedJob;
import org.glassfish.api.admin.Payload.Outbound;
import org.glassfish.api.admin.Progress;
import org.glassfish.api.admin.ProgressStatus;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/** Doing progress and send some payload.
 *
 * @author mmares
 */
@Service(name = "progress-payload")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("progress")
@Progress(totalStepCount=5)
@ManagedJob
public class ProgressPayloadCommand implements AdminCommand {
    
    private final static Logger logger =
            LogDomains.getLogger(ProgressPayloadCommand.class, LogDomains.ADMIN_LOGGER);
    
    @Param(name = "down", multiple = false, primary = true, optional = true)
    String down;
    
    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        ProgressStatus ps = context.getProgressStatus();
        for (int i = 0; i < 4; i++) {
            doSomeLogic();
            ps.progress(1);
        }
        //Prepare payload
        Outbound out = context.getOutboundPayload();
        StringBuilder msg = new StringBuilder();
        if (down == null || down.isEmpty()) {
            msg.append("No file requested.");
        } else {
            msg.append("You are requesting for ").append(down).append('.').append(StringUtils.EOL);
            File f = new File(down);
            if (!f.exists()) {
                msg.append("But it does not exist!");
            } else {
                try {
                    String canonicalPath = f.getCanonicalPath();
                    canonicalPath = canonicalPath.replace('\\', '/');
                    if (canonicalPath.charAt(1) == ':') {
                        canonicalPath = canonicalPath.substring(2);
                    }
                    if (f.isDirectory()) {
                        msg.append("It is directory - recursive download");
                        out.attachFile("application/octet-stream", URI.create(canonicalPath), f.getName(), f, true);
                    } else {
                        out.attachFile("application/octet-stream", URI.create(canonicalPath), f.getName(), f);
                    }
                } catch (IOException ex) {
                    report.failure(logger, "Can not append " + f.getAbsolutePath());
                }
            }
        }
        if (report.getActionExitCode() == ActionReport.ExitCode.SUCCESS) {
            report.setMessage(msg.toString());
        }
        //Return
        ps.progress(1);
        ps.complete("Finished");
    }
    
    private void doSomeLogic() {
        try {
            Thread.sleep(250L);
        } catch (Exception ex) {
        }
    }
    
}
