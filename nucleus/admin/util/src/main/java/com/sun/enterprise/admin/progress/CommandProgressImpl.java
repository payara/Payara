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
package com.sun.enterprise.admin.progress;

import com.sun.enterprise.util.StringUtils;
import java.io.Serializable;
import java.util.Date;
import org.glassfish.api.admin.AdminCommandEventBroker;
import org.glassfish.api.admin.CommandProgress;
import org.glassfish.api.admin.progress.ProgressStatusBase;
import org.glassfish.api.admin.progress.ProgressStatusEvent;
import org.glassfish.api.admin.progress.ProgressStatusEventCreateChild;
import org.glassfish.api.admin.progress.ProgressStatusEventProgress;
import org.glassfish.api.admin.progress.ProgressStatusImpl;
import org.glassfish.api.admin.progress.ProgressStatusMessage;
import org.glassfish.api.admin.progress.ProgressStatusMirroringImpl;

/** Basic and probably only implementation of {@code CommandProgress}.
 *
 * @author mmares
 */
public class CommandProgressImpl extends ProgressStatusImpl implements CommandProgress, Serializable {
    
    private static final long serialVersionUID = 1;

    public class LastChangedMessage implements ProgressStatusMessage, Serializable {
        
        private String sourceId;
        private String message;
        private String contextString;

        private LastChangedMessage(String sourceId, String message) {
            this.sourceId = sourceId;
            if (message != null && message.isEmpty()) {
                message = null;
            }
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public String getSourceId() {
            return sourceId;
        }
        
        public String getContextString() {
            if (contextString == null) {
                StringBuilder result = new StringBuilder();
                ProgressStatusBase fnd = findById(sourceId);
                if (StringUtils.ok(fnd.getName())) {
                    result.append(fnd.getName());
                }
                ProgressStatusBase parent;
                while((parent = fnd.getParrent()) != null) {
                    if (StringUtils.ok(parent.getName())) {
                        if (result.length() > 0) {
                            result.insert(0, '.');
                        }
                        result.insert(0, parent.getName());
                    }
                    fnd = parent;
                }
                contextString = result.toString();
            }
            return contextString;
        }
        
    }
    
    private LastChangedMessage lastMessage;
    private long eTag = 0;
    private Date startTime;
    private Date endTime;
    //TODO: Set after resurection
    private transient AdminCommandEventBroker eventBroker;
    private boolean spinner = false;
    
    public CommandProgressImpl(String name, String id) {
        super(name, -1, null, id);
        startTime = new Date();
    }
    
    @Override
    protected synchronized void fireEvent(ProgressStatusEvent event) {
        if (event == null) {
            return;
        }
        if (event instanceof ProgressStatusMessage) {
            ProgressStatusMessage msgEvent = (ProgressStatusMessage) event;
            if (StringUtils.ok(msgEvent.getMessage())) {
                lastMessage = new LastChangedMessage(msgEvent.getSourceId(), msgEvent.getMessage());
            }
        }
        if (event instanceof ProgressStatusEventProgress) {
            this.spinner = ((ProgressStatusEventProgress) event).isSpinner();
        }
        eTag++;
        if (eventBroker != null) {
            eventBroker.fireEvent(EVENT_PROGRESSSTATUS_CHANGE, event);
        }
    }
    
    @Override
    public void setEventBroker(AdminCommandEventBroker eventBroker) {
        this.eventBroker = eventBroker;
        if (eventBroker != null) {
            eventBroker.fireEvent(EVENT_PROGRESSSTATUS_STATE, this);
        }
    }
    
    @Override
    public synchronized ProgressStatusMirroringImpl createMirroringChild(int allocatedSteps) {
        allocateStapsForChildProcess(allocatedSteps);
        String childId = (id == null ? "" : id) + "." + (children.size() + 1);
        ProgressStatusMirroringImpl result = new ProgressStatusMirroringImpl(null, this, childId);
        children.add(new ChildProgressStatus(allocatedSteps, result));
        fireEvent(new ProgressStatusEventCreateChild(id, null, result.getId(), allocatedSteps, -1));
        return result;
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
    public String getLastMessage() {
        if (lastMessage != null) {
            StringBuilder result = new StringBuilder();
            result.append(lastMessage.getContextString());
            if (result.length() > 0) {
                result.append(": ");
            }
            result.append(lastMessage.getMessage());
            return result.toString();
        } else {
            return null;
        }
    }
    
    @Override
    public void complete() {
        complete(null);
    }
    
    @Override
    public void complete(String message) {
        this.endTime = new Date();
        super.complete(message);
    }
    
    @Override
    public boolean isSpinnerActive() {
        return this.spinner;
    }
    
}
