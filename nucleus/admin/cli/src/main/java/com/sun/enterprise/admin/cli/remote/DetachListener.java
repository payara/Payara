/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.cli.remote;
import com.sun.enterprise.admin.remote.RemoteRestAdminCommand;
import com.sun.enterprise.admin.remote.sse.GfSseInboundEvent;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.StringUtils;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommandEventBroker;
import org.glassfish.api.admin.AdminCommandState;


/**
 * Whenever a command is executed with --detach
 * this class will close the Server Sent Events for
 * detached commands and give a job id.
 *
 * @author Bhakti Mehta
 */
public class DetachListener implements AdminCommandEventBroker.AdminCommandListener<GfSseInboundEvent> {
    
    private static final LocalStringsImpl strings =
            new LocalStringsImpl(DetachListener.class);

    private final Logger logger;
    private final RemoteRestAdminCommand rac;
    private final boolean terse;

    public DetachListener(Logger logger, RemoteRestAdminCommand rac, boolean terse) {
        this.logger = logger;
        this.rac = rac;
        this.terse = terse;
    }

    @Override
    public void onAdminCommandEvent(String name, GfSseInboundEvent event) {
        try {
            AdminCommandState acs = event.getData(AdminCommandState.class, "application/json");
            String id = acs.getId();
            if (StringUtils.ok(id)) {
                if (terse) {
                    rac.closeSse(id, ActionReport.ExitCode.SUCCESS);
                } else {
                    rac.closeSse(strings.get("detach.jobid", id), ActionReport.ExitCode.SUCCESS);
                }
            } else {
                logger.log(Level.SEVERE, strings.getString("detach.noid", "Command was started but id was not retrieved. Can not detach."));
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

}
