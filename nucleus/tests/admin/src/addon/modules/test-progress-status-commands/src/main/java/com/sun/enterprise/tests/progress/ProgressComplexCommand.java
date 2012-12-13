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
package com.sun.enterprise.tests.progress;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.logging.LogDomains;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ManagedJob;
import org.glassfish.api.admin.Progress;
import org.glassfish.api.admin.ProgressStatus;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/** Basic progress status example.
 * Contains 10 steps 
 *
 * @author mmares
 */
@Service(name = "progress-complex")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("progress")
@ManagedJob
@Progress(name="complex", totalStepCount=20)
public class ProgressComplexCommand implements AdminCommand {
    
    private final static Logger logger =
            LogDomains.getLogger(ProgressComplexCommand.class, LogDomains.ADMIN_LOGGER);
    
    @Override
    public void execute(AdminCommandContext context) {
        ProgressStatus ps = context.getProgressStatus();
        ProgressStatus ch1 = ps.createChild("ch1", 5);
        ProgressStatus ch2 = ps.createChild("ch2-paral", 5);
        ProgressStatus ch3 = ps.createChild("ch3", 6);
        //Prepare ch1
        ch1.setTotalStepCount(10);
        ProgressStatus ch11 = ch1.createChild("ch11", 5);
        ch11.setTotalStepCount(5);
        ProgressStatus ch12 = ch1.createChild("ch12", 5);
        //Prepare ch2
        ch2.setTotalStepCount(50);
        ProgressStatus ch21 = ch2.createChild("ch21", 10);
        ch21.setTotalStepCount(25);
        ProgressStatus ch22 = ch2.createChild("ch22", 10);
        ch22.setTotalStepCount(25);
        ProgressStatus ch23 = ch2.createChild("ch23", 10);
        ch23.setTotalStepCount(25);
        ProgressStatus ch24 = ch2.createChild("ch24", 15);
        ch24.setTotalStepCount(25);
        //First move ch1
        doProgress(ch11, 4, 200, "progress ch1.1");
        //Init ch3
        ch3.setTotalStepCount(112);
        ProgressStatus ch31 = ch3.createChild("ch31", 100);
        ch31.setTotalStepCount(5);
        ProgressStatus ch32 = ch3.createChild("ch32", 8);
        ch32.setTotalStepCount(5);
        //Move ch3 then ch1 and then ch3 and then finish ch1
        doProgress(ch3, 4, 150, "progress ch3");
        doProgress(ch11, 1, 150, "progress ch1.1");
        doProgress(ch32, 5, 150, "progress ch3.2");
        ch12.setTotalStepCount(6);
        doProgress(ch31, 5, 150, "progress ch3.1");
        doProgress(ch12, 6, 150, "progress ch1.2");
        //Do paralel progress of ch2.x
        Thread th21 = new Thread(new ProgressRunnable(ch21, 25, 100, "progress ch2.1"));
        Thread th22 = new Thread(new ProgressRunnable(ch22, 25, 100, "progress ch2.2"));
        Thread th23 = new Thread(new ProgressRunnable(ch23, 25, 100, "progress ch2.3"));
        Thread th24 = new Thread(new ProgressRunnable(ch24, 25, 100, "progress ch2.4"));
        th21.start();
        th22.start();
        th23.start();
        th24.start();
        try {
            th21.join();
            th22.join();
            th23.join();
            th24.join();
        } catch (InterruptedException ex) {
            context.getActionReport().failure(Logger.global, "Unexpected interrupt", ex);
            return;
        }
        doProgress(ps, 4, 100, "progress main");
        doProgress(ch2, 5, 100, "progress ch2");
        context.getActionReport().appendMessage("All done");
    }
    
    private static void doProgress(ProgressStatus ps, int count, long interval, String message) {
        for (int i = 0; i < count; i++) {
            try {
                Thread.sleep(interval);
            } catch (Exception ex) {
            }
            if (message != null) {
                int rsc = ps.getRemainingStepCount() - 1;
                ps.progress(1, message + ", remaining " + rsc);
            } else {
                ps.progress(1);
            }
        }
    }
    
    static class ProgressRunnable implements Runnable {
        
        private final ProgressStatus ps;
        private final int count;
        private final long interval;
        private final String message;

        public ProgressRunnable(ProgressStatus ps, int count, long interval, String message) {
            this.ps = ps;
            this.count = count;
            this.interval = interval;
            this.message = message;
        }

        public void run() {
            doProgress(ps, count, interval, message);
        }
        
        
    }
    
}
