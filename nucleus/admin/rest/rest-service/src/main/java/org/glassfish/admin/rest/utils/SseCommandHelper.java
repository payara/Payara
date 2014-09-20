/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.utils;

import com.sun.enterprise.admin.remote.AdminCommandStateImpl;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.v3.admin.JobManagerService;
import com.sun.enterprise.v3.common.PropsFileActionReporter;
import com.sun.logging.LogDomains;
import java.io.IOException;
import java.util.logging.Level;
import javax.ws.rs.core.MediaType;
import org.glassfish.admin.rest.RestLogging;
import org.glassfish.admin.rest.resources.admin.CommandResource;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommandEventBroker;
import org.glassfish.api.admin.AdminCommandState;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.internal.api.Globals;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;

/**
 * Provides bridge between CommandInvocation and ReST Response for SSE. Create
 * it and call execute.
 *
 * @author martinmares
 */
public class SseCommandHelper implements Runnable, AdminCommandEventBroker.AdminCommandListener {

    /** If implementation of this interface is registered then it's process()
     * method is used to convert ActionReport before it is transfered to the
     * client.
     */
    public static interface ActionReportProcessor {

        /** Framework calls this method to process report before it is send
         * to the client. Implementation also can send custom events using
         * provided event channel.
         */
        public ActionReport process(ActionReport report, EventOutput ec);

    }
    private final static LocalStringManagerImpl strings = new LocalStringManagerImpl(CommandResource.class);

    private final CommandRunner.CommandInvocation commandInvocation;
    private final ActionReportProcessor processor;
    private final EventOutput eventOuptut = new EventOutput();
    private AdminCommandEventBroker broker;

    private SseCommandHelper(final CommandInvocation commandInvocation,
                             final ActionReportProcessor processor) {
        this.commandInvocation = commandInvocation;
        this.processor = processor;
    }

    @Override
    public void run() {
        try {
            commandInvocation.execute();
        } catch (Throwable thr) {
            RestLogging.restLogger.log(Level.WARNING, RestLogging.UNEXPECTED_EXCEPTION,
                    thr);
            ActionReport actionReport = new PropsFileActionReporter(); //new RestActionReporter();
            actionReport.setFailureCause(thr);
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            AdminCommandState acs = new AdminCommandStateImpl(AdminCommandState.State.COMPLETED, actionReport, true, "unknown");
            onAdminCommandEvent(AdminCommandStateImpl.EVENT_STATE_CHANGED, acs);
        } finally {
            try {
                eventOuptut.close();
            } catch (IOException ex) {
                RestLogging.restLogger.log(Level.WARNING, RestLogging.IO_EXCEPTION, 
                        ex.getMessage());
            }
        }
    }

    private void unregister() {
        if (broker != null) {
            broker.unregisterListener(this);
        }
    }

    private Object process(final String name, Object event) {
        if (processor != null && AdminCommandStateImpl.EVENT_STATE_CHANGED.equals(name)) {
            AdminCommandState acs = (AdminCommandState) event;
            ActionReport report = processor.process(acs.getActionReport(), eventOuptut);
            event = new AdminCommandStateImpl(acs.getState(), report, acs.isOutboundPayloadEmpty(), acs.getId());
        }
        return event;
    }

    @Override
    public void onAdminCommandEvent(final String name, Object event) {
        if (name == null || event == null) {
            return;
        }
        if (AdminCommandEventBroker.BrokerListenerRegEvent.EVENT_NAME_LISTENER_REG.equals(name)) {
            AdminCommandEventBroker.BrokerListenerRegEvent blre = (AdminCommandEventBroker.BrokerListenerRegEvent) event;
            broker = blre.getBroker();
            return;
        }
        if (name.startsWith(AdminCommandEventBroker.LOCAL_EVENT_PREFIX)) {
            return; //Prevent events from client to be send back to client
        }
        if (eventOuptut.isClosed()) {
            unregister();
            return;
        }
        if ((event instanceof Number)
                || (event instanceof CharSequence)
                || (event instanceof Boolean)) {
            event = String.valueOf(event);
        }
        event = process(name, event);
        OutboundEvent outEvent = new OutboundEvent.Builder()
                .name(name)
                .mediaType(event instanceof String
                ? MediaType.TEXT_PLAIN_TYPE
                : MediaType.APPLICATION_JSON_TYPE)
                .data(event.getClass(), event)
                .build();
        try {
            eventOuptut.write(outEvent);
        } catch (Exception ex) {
            if (RestLogging.restLogger.isLoggable(Level.FINE)) {
                RestLogging.restLogger.log(Level.FINE, null, ex);
            }
            if (eventOuptut.isClosed()) {
                unregister();
            }
        }
    }

    public static EventOutput invokeAsync(CommandInvocation commandInvocation, ActionReportProcessor processor) {
        if (commandInvocation == null) {
            throw new IllegalArgumentException("commandInvocation");
        }
        SseCommandHelper helper = new SseCommandHelper(commandInvocation, processor);
        commandInvocation.listener(".*", helper);
        JobManagerService jobManagerService = Globals.getDefaultHabitat().getService(JobManagerService.class);
        jobManagerService.getThreadPool().execute(helper);
        return helper.eventOuptut;
    }
}
