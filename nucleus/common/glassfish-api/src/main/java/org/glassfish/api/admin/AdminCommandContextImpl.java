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

package org.glassfish.api.admin;


import java.util.UUID;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.progress.ProgressStatusImpl;

/**
 * Useful services for administrative commands implementation
 *
 * @author Jerome Dochez
 */
public class AdminCommandContextImpl implements  AdminCommandContext {
    
    private  ActionReport report;
    private final Logger logger;
    private final Payload.Inbound inboundPayload;
    private final Payload.Outbound outboundPayload;
    private Subject subject;
    private ProgressStatus progressStatus; //new ErrorProgressStatus();
    private final AdminCommandEventBroker eventBroker;

    public AdminCommandContextImpl(Logger logger, ActionReport report) {
        this(logger, report, null, null, null);
    }
    
    public AdminCommandContextImpl(Logger logger, ActionReport report,
                                   final Payload.Inbound inboundPayload,
                                   final Payload.Outbound outboundPayload,
                                   final AdminCommandEventBroker eventBroker) {
        this.logger = logger;
        this.report = report;
        this.inboundPayload = inboundPayload;
        this.outboundPayload = outboundPayload;
        this.eventBroker = eventBroker;
    }
    
    @Override
    public ActionReport getActionReport() {
        return report;
    }

    @Override
    public void setActionReport(ActionReport newReport) {
        report = newReport;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Payload.Inbound getInboundPayload() {
        return inboundPayload;
    }

    @Override
    public Payload.Outbound getOutboundPayload() {
        return outboundPayload;
    }

    @Override
    public Subject getSubject() {
        return subject;
    }

    @Override
    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    @Override
    public ProgressStatus getProgressStatus() {
        if (progressStatus == null) {
            progressStatus = new ProgressStatusImpl();
        }
        return progressStatus;
    }

    @Override
    public AdminCommandEventBroker getEventBroker() {
        return this.eventBroker;
    }
    
    static class ErrorProgressStatus implements ProgressStatus {
        
        private static final String EXC_MESSAGE = "@Progress annotation is not present.";
        private String id = null;
        
        
        @Override
        public void setTotalStepCount(int totalStepCount) {
            throw new IllegalStateException(EXC_MESSAGE);
        }

        @Override
        public int getTotalStepCount() {
            return 0;
        }

        @Override
        public int getRemainingStepCount() {
            return 0;
        }

        @Override
        public void progress(int steps, String message) {
            throw new IllegalStateException(EXC_MESSAGE);
        }

        @Override
        public void progress(int steps) {
            throw new IllegalStateException(EXC_MESSAGE);
        }

        @Override
        public void progress(String message) {
            throw new IllegalStateException(EXC_MESSAGE);
        }
        
        @Override
        public void progress(int steps, String message, boolean spinner) {
            throw new IllegalStateException(EXC_MESSAGE);
        }

        @Override
        public void setCurrentStepCount(int stepCount) {
            throw new IllegalStateException(EXC_MESSAGE);
        }

        @Override
        public void complete(String message) {
        }

        @Override
        public void complete() {
        }

        @Override
        public boolean isComplete() {
            return true;
        }

        @Override
        public ProgressStatus createChild(String name, int allocatedSteps) {
            throw new IllegalStateException(EXC_MESSAGE);
        }

        @Override
        public ProgressStatus createChild(int allocatedSteps) {
            throw new IllegalStateException(EXC_MESSAGE);
        }

        @Override
        public synchronized String getId() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return id;
        }

    }

}
