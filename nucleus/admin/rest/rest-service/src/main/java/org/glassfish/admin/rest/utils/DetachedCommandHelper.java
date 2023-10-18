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
package org.glassfish.admin.rest.utils;

import com.sun.enterprise.admin.remote.AdminCommandStateImpl;
import com.sun.enterprise.v3.admin.JobManagerService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.glassfish.admin.rest.RestLogging;
import org.glassfish.api.admin.AdminCommandEventBroker;
import org.glassfish.api.admin.AdminCommandState;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.internal.api.Globals;

/**
 *
 * @author jdlee
 */
public class DetachedCommandHelper implements Runnable, AdminCommandEventBroker.AdminCommandListener {

    private final CommandRunner.CommandInvocation commandInvocation;
    private CountDownLatch latch;
    private String jobId;
    private AdminCommandEventBroker broker;

    private DetachedCommandHelper(final CommandRunner.CommandInvocation commandInvocation, CountDownLatch latch) {
        this.commandInvocation = commandInvocation;
        this.latch = latch;
    }

    @Override
    public void run() {
        commandInvocation.execute();
    }

    public static String invokeAsync(CommandRunner.CommandInvocation commandInvocation) {
        if (commandInvocation == null) {
            throw new IllegalArgumentException("commandInvocation");
        }
        CountDownLatch latch = new CountDownLatch(1);
        DetachedCommandHelper helper = new DetachedCommandHelper(commandInvocation, latch);
        commandInvocation.listener(".*", helper);
        JobManagerService jobManagerService = Globals.getDefaultHabitat().getService(JobManagerService.class);
        jobManagerService.getThreadPool().execute(helper);
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                RestLogging.restLogger.log(Level.FINE, "latch.await() returned false");
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        return helper.jobId;
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

        if (AdminCommandStateImpl.EVENT_STATE_CHANGED.equals(name)) {
            unregister();
            AdminCommandState acs = (AdminCommandState) event;
            jobId = acs.getId();
            latch.countDown();
        }
    }

    private void unregister() {
        if (broker != null) {
            broker.unregisterListener(this);
        }
    }
}