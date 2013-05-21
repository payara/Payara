/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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


import java.io.Serializable;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ExecutionContext;

/**
 * Useful services for administrative commands implementation
 *
 * @author Jerome Dochez
 */
public interface AdminCommandContext extends ExecutionContext, Serializable {
    
    /**
     * Returns the Reporter for this action
     * @return ActionReport implementation suitable for the client
     */
    public ActionReport getActionReport();
    /**
     * Change the Reporter for this action
     * @param newReport The ActionReport to set.
     */
    public void setActionReport(ActionReport newReport);

    /**
     * Returns the Logger
     * @return the logger
     */
    public Logger getLogger();

    /**
     * Returns the inbound payload, from the admin client, that accompanied
     * the command request.
     *
     * @return the inbound payload
     */
    public Payload.Inbound getInboundPayload();

    /**
     * Changes the inbound payload for this action.
     *
     * @param newInboundPayload inbound payload to set.
     */
    public void setInboundPayload(Payload.Inbound newInboundPayload);

    /**
     * Returns a reference to the outbound payload so a command implementation
     * can populate the payload for return to the admin client.
     *
     * @return the outbound payload
     */
    public Payload.Outbound getOutboundPayload();

    /**
     * Changes the outbound payload for this action.
     *
     * @param newOutboundPayload outbound payload to set.
     */
    public void setOutboundPayload(Payload.Outbound newOutboundPayload);

    /**
     * Returns the Subject associated with this command context.
     *
     * @return the Subject
     */
    public Subject getSubject();

    /**
     * Sets the Subject to be associated with this command context.
     *
     * @param subject
     */
    public void setSubject(Subject subject);
    
    /** 
     * ProgressStatus can be used to inform about step by step progress 
     * of the command. It is always ready to use but propagated to 
     * the client only if {@code @Progress} annotation is on the command
     * implementation.
     */
    public ProgressStatus getProgressStatus();
    
    /** Simple event broker for inter command communication mostly
     * from server to client. (Command to caller).
     */
    public AdminCommandEventBroker getEventBroker();
    
    
    /** Id of current job. Only managed commands has job id.
     */
    public String getJobId();

}
