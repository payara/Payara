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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import java.io.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.xml.bind.*;

import com.sun.enterprise.admin.util.InstanceStateService;
import org.glassfish.api.admin.*;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import org.glassfish.hk2.api.PerLookup;

import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.cluster.SyncRequest;
import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * Synchronize files.  Accepts an XML document containing files
 * and mod times and sends the client new versions of anything
 * that's out of date.
 *
 * @author Bill Shannon
 */
@Service(name="_synchronize-files")
@PerLookup
@CommandLock(CommandLock.LockType.EXCLUSIVE)
@I18n("synchronize.command")
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.POST, 
        path="_synchronize-files", 
        description="_synchronize-files")
})
public class SynchronizeFiles implements AdminCommand {

    @Param(name = "file_list", primary = true)
    private File fileList;

    @Inject @Optional
    private Applications applications;

    @Inject @Optional
    private Servers servers;

    @Inject
    private InstanceStateService stateService;

    @Inject
    private ServerSynchronizer sync;

    private Logger logger;

    private final static LocalStringManagerImpl strings =
        new LocalStringManagerImpl(SynchronizeFiles.class);

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        logger = context.getLogger();
        SyncRequest sr = null;
        try {
            // read the input document
            JAXBContext jc = JAXBContext.newInstance(SyncRequest.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            unmarshaller.setSchema(null);       // XXX - needed?
            sr = (SyncRequest)unmarshaller.unmarshal(fileList);
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "SynchronizeFiles: synchronize dir {0}", sr.dir);
            }
        } catch (Exception ex) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("SynchronizeFiles: Exception reading request");
                logger.fine(ex.toString());
            }
            report.setActionExitCode(ExitCode.FAILURE);
            report.setMessage(
                        strings.getLocalString("sync.exception.reading",
                            "SynchronizeFiles: Exception reading request"));
            report.setFailureCause(ex);
            return;
        }

        try {
            // verify the server instance is valid
            Server server = null;
            if (servers != null)
                server = servers.getServer(sr.instance);
            if (server == null) {
                report.setActionExitCode(ExitCode.FAILURE);
                report.setMessage(
                        strings.getLocalString("sync.unknown.instance",
                            "Unknown server instance: {0}", sr.instance));
                return;
            }

            sync.synchronize(server, sr, context.getOutboundPayload(), report,
                                logger);
            stateService.setState(server.getName(), InstanceState.StateType.NO_RESPONSE, true);
            stateService.removeFailedCommandsForInstance(server.getName());
        } catch (Exception ex) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("SynchronizeFiles: Exception processing request");
                logger.fine(ex.toString());
            }
            report.setActionExitCode(ExitCode.FAILURE);
            report.setMessage(
                        strings.getLocalString("sync.exception.processing",
                            "SynchronizeFiles: Exception processing request"));
            report.setFailureCause(ex);
        }
    }
}
