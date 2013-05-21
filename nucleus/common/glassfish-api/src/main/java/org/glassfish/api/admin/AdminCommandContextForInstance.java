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
package org.glassfish.api.admin;

import java.util.logging.Logger;
import javax.security.auth.Subject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.Payload.Inbound;
import org.glassfish.api.admin.Payload.Outbound;

/** Most of {@link AdminCommandContext} attributes are used in any phase 
 * of command execution (supplemental commands, replication) but some 
 * of them must be different for every instance. This wrapper provides 
 * such staff.
 *
 * @author mmares
 */
public class AdminCommandContextForInstance implements AdminCommandContext {
    
    private AdminCommandContext wrapped;
    private ProgressStatus progressStatus;

    public AdminCommandContextForInstance(AdminCommandContext wrapped, ProgressStatus progressStatus) {
        if (wrapped == null) {
            throw new IllegalArgumentException("Argument wrapped can not be null");
        }
        this.wrapped = wrapped;
        this.progressStatus = progressStatus;
    }

    @Override
    public ActionReport getActionReport() {
        return wrapped.getActionReport();
    }

    @Override
    public void setActionReport(ActionReport newReport) {
        wrapped.setActionReport(newReport);
    }

    @Override
    public Logger getLogger() {
        return wrapped.getLogger();
    }

    @Override
    public Inbound getInboundPayload() {
        return wrapped.getInboundPayload();
    }

    @Override
    public void setInboundPayload(Inbound newInboundPayload) {
        wrapped.setInboundPayload(newInboundPayload);
    }

    @Override
    public Outbound getOutboundPayload() {
        return wrapped.getOutboundPayload();
    }

    @Override
    public void setOutboundPayload(Outbound newOutboundPayload) {
        wrapped.setOutboundPayload(newOutboundPayload);
    }

    @Override
    public Subject getSubject() {
        return wrapped.getSubject();
    }

    @Override
    public void setSubject(Subject subject) {
        wrapped.setSubject(subject);
    }

    @Override
    public ProgressStatus getProgressStatus() {
        if (progressStatus == null) {
            return wrapped.getProgressStatus();
        } else {
            return progressStatus;
        }
    }

    @Override
    public AdminCommandEventBroker getEventBroker() {
        return wrapped.getEventBroker();
    }
    @Override
    public String getJobId() {
        return wrapped.getJobId();
    }

    
}
