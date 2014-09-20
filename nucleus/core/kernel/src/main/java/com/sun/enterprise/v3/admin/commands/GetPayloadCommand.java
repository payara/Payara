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
package com.sun.enterprise.v3.admin.commands;

import com.sun.enterprise.util.LocalStringManagerImpl;
import javax.inject.Inject;
import org.glassfish.admin.payload.PayloadImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.Payload.Outbound;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/** Retrieve outbound payload from finished managed job.
 *
 * @author mmares
 */
@Service(name = "_get-payload")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("getpayload")
@AccessRequired(resource="jobs/job/$jobID", action="read")
public class GetPayloadCommand implements AdminCommand {
    
    private final static LocalStringManagerImpl strings = new LocalStringManagerImpl(GetPayloadCommand.class);
    
    @Inject
    JobManager registry;
    
    @Param(primary=true, optional=false, multiple=false)
    String jobID;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport ar = context.getActionReport();
        Job job = registry.get(jobID);
        if (job == null) {
            ar.setActionExitCode(ActionReport.ExitCode.FAILURE);
            ar.setMessage(strings.getLocalString("getPayload.wrong.commandinstance.id", "Command instance {0} does not exist.", jobID));
            return;
        }
        Outbound jobPayload = job.getPayload();
        if (jobPayload == null) {
            ar.setMessage(strings.getLocalString("getPayload.nopayload", "Outbound payload does not exist."));
            return; //Just return. This is OK.
        }
        Outbound paylaod = context.getOutboundPayload();
        if ((paylaod instanceof PayloadImpl.Outbound) && (jobPayload instanceof PayloadImpl.Outbound)) {
            PayloadImpl.Outbound destination = (PayloadImpl.Outbound) paylaod;
            PayloadImpl.Outbound source = (PayloadImpl.Outbound) jobPayload;
            destination.getParts().addAll(source.getParts());
        } else {
            ar.setActionExitCode(ActionReport.ExitCode.FAILURE);
            ar.setMessage(strings.getLocalString("getPayload.unsupported", "Payload type is not supported. Can not download data."));
        }
        
    }
    
    
}
