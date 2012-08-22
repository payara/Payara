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

import com.sun.enterprise.admin.progress.CommandProgressImpl;
import com.sun.enterprise.admin.remote.AdminCommandStateImpl;
import com.sun.enterprise.util.LocalStringManagerImpl;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandEventBroker.AdminCommandListener;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/** Gets CommandInstance from registry based on given id and forwards all events.
 *
 * @author mmares
 */
@Service(name = "_attach")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("attach")
public class AttachCommand implements AdminCommand, AdminCommandListener {
    
    private final static LocalStringManagerImpl strings = new LocalStringManagerImpl(AttachCommand.class);
    
    private AdminCommandEventBroker eventBroker;
    private Job attached;
    
    @Inject
    JobManager registry;
    
    @Param(primary=true, optional=false, multiple=false)
    String commandId;

    @Override
    public void execute(AdminCommandContext context) {
        eventBroker = context.getEventBroker();
        ActionReport ar = context.getActionReport();
        attached = registry.get(commandId);
        if (attached == null) {
            ar.setActionExitCode(ActionReport.ExitCode.FAILURE);
            ar.setMessage(strings.getLocalString("attach.wrong.commandinstance.id", "Command instance {0} does not exist.", commandId));
            return;
        }
        AdminCommandEventBroker attachedBroker = attached.getEventBroker();
        synchronized (attachedBroker) {
            eventBroker.fireEvent(AdminCommandStateImpl.EVENT_STATE_CHANGED, attached);
            if (attached.getCommandProgress() != null) {
                eventBroker.fireEvent(CommandProgressImpl.EVENT_PROGRESSSTAUS_STATE, attached.getCommandProgress());
            }
            attachedBroker.registerListener(".*", this);
        }
        synchronized (attached) {
            while(attached.getState() == AdminCommandState.State.PREPARED ||
                    attached.getState() == AdminCommandState.State.RUNNING) {
                try {
                    attached.wait(1000*60*5); //5000L just to be sure
                } catch (InterruptedException ex) {}
            }
        }
        ar.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        ar.setMessage(strings.getLocalString("attach.finished", "Attach command finished successfully."));
    }

    @Override
    public void onAdminCommandEvent(String name, Object event) {
        if (name == null || name.startsWith("client.")) { //Skip nonsence or own events
            return;
        }
        eventBroker.fireEvent(name, event); //Forward
        if (AdminCommandStateImpl.EVENT_STATE_CHANGED.equals(name)) {
            synchronized (attached) {
                attached.notifyAll();
            }
        }
    }

    
}
