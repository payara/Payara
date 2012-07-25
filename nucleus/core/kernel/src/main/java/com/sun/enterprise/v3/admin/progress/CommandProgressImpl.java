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
package com.sun.enterprise.v3.admin.progress;

import java.util.Date;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandProgress;
import org.glassfish.api.admin.Payload;
import org.glassfish.api.admin.ProgressStatus;
import org.glassfish.api.admin.progress.ProgressStatusEvent;
import org.glassfish.api.admin.progress.ProgressStatusImpl;
import org.glassfish.api.admin.progress.ProgressStatusMirroringImpl;

/** Basic and probably only implementation of {@code CommandProgress}.
 *
 * @author mmares
 */
public class CommandProgressImpl extends ProgressStatusImpl implements CommandProgress {

    public class LastChanged {
        
        private ProgressStatus source;
        private String message;

        private LastChanged(ProgressStatus source, String message) {
            this.source = source;
            if (message != null && message.isEmpty()) {
                message = null;
            }
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public ProgressStatus getSource() {
            return source;
        }
        
    }
    
    private LastChanged lastChanged;
    private LastChanged lastMessage;
    private long eTag = 0;
    private Date startTime;
    private Date endTime;
    private CommandResult commandResult;
    
    public CommandProgressImpl(String name) {
        super(name, -1, null, null);
        startTime = new Date();
        lastChanged = new LastChanged(this, null);
    }
    
    @Override
    protected synchronized void fireEvent(ProgressStatusEvent event) {
        if (event == null) {
            return;
        }
        lastChanged = new LastChanged(event.getSource(), event.getMessage());
        if (event.getMessage() != null && !event.getMessage().isEmpty()) {
            lastMessage = lastChanged;
        }
        eTag++;
    }
    
    @Override
    public synchronized void complete(ActionReport actionReport, Payload.Outbound payload) {
        this.commandResult = new CommandResult(actionReport, payload);
        this.endTime = new Date();
        super.complete();
    }
    
    @Override
    public synchronized ProgressStatusMirroringImpl createMirroringChild(int allocatedSteps) {
        allocateStapsForChildProcess(allocatedSteps);
        String childId = (id == null ? "" : id) + "." + (childs.size() + 1);
        ProgressStatusMirroringImpl result = new ProgressStatusMirroringImpl(null, this, childId);
        childs.add(new ChildProgressStatus(allocatedSteps, result));
        fireEvent(new ProgressStatusEvent(result, allocatedSteps));
        return result;
    }

    @Override
    public CommandResult getCommandResult() {
        return commandResult;
    }

    @Override
    public Date getEndTime() {
        return endTime;
    }

    @Override
    public Date getStartTime() {
        return startTime;
    }
    
    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public float computeCompletePortion() {
        return super.computeCompletePortion();
    }
    
    @Override
    public String getLastMessage() {
        if (lastMessage != null) {
            return lastMessage.getMessage();
        } else {
            return null;
        }
    }
    
}
