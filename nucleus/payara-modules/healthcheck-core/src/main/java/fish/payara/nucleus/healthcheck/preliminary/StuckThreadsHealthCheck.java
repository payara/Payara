/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.nucleus.healthcheck.preliminary;

import fish.payara.nucleus.healthcheck.HealthCheckResult;
import fish.payara.nucleus.healthcheck.HealthCheckResultEntry;
import fish.payara.nucleus.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.HealthCheckStuckThreadExecutionOptions;
import fish.payara.nucleus.healthcheck.configuration.StuckThreadsChecker;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

/**
 * @since 4.1.2.173
 * @author jonathan coustick
 */
@Service(name = "healthcheck-stuckthreads")
@RunLevel(11)
public class StuckThreadsHealthCheck extends BaseHealthCheck<HealthCheckStuckThreadExecutionOptions,
        StuckThreadsChecker>{

    Map<Thread,StackTraceElement[]> stackTraces;
    
    @PostConstruct
    void postConstruct() {
        stackTraces = new HashMap<Thread,StackTraceElement[]>();
        postConstruct(this, StuckThreadsChecker.class);
    }
    
    @Override
    public HealthCheckResult doCheck() {
        
        
        HealthCheckResult result = new HealthCheckResult();
        result.add(new HealthCheckResultEntry(HealthCheckResultStatus.GOOD, "Running stuck threads checker"));
        Logger.getGlobal().log(Level.INFO, "Running stuck threads checker");
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] deadlockthreadids = bean.findDeadlockedThreads();
        if (deadlockthreadids != null) {
            ThreadInfo[] lockedThreads = bean.getThreadInfo(deadlockthreadids);
            for (ThreadInfo info : lockedThreads) {
                result.add(new HealthCheckResultEntry(HealthCheckResultStatus.WARNING, "Stuck Thread: " + info.toString()));

            }
        }
        
        Map<Thread,StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        for (Thread thread: allStackTraces.keySet()){
            thread.getState();
            
            if (!thread.isAlive()) {
                //To make sure that old threads aren't held on to and are garbage collected
                allStackTraces.remove(thread);
            } else {

                checkForStuck(thread, result);
                
            }

        }
        stackTraces = allStackTraces;
        return result;
    }
    
    private void checkForStuck(Thread thread, HealthCheckResult result) {
        if (stackTraces.containsKey(thread)) {
            StackTraceElement[] previousStackTraceArray = stackTraces.get(thread);
            
            if (Arrays.equals(thread.getStackTrace(), previousStackTraceArray)) {
                
                String message = "Stuck Thread " + thread.getName() + "at\n";
                
                for (StackTraceElement element : thread.getStackTrace()) {
                    //A parked thread is not stuck
                    if (element.toString().equals("sun.misc.Unsafe.park(Native Method)")) {
                        return;
                    }

                    message += element + "\n";
                }
                result.add(new HealthCheckResultEntry(HealthCheckResultStatus.WARNING, message));

            }
        }
    }


    @Override
    public HealthCheckStuckThreadExecutionOptions constructOptions(StuckThreadsChecker checker) {
        return new HealthCheckStuckThreadExecutionOptions(Boolean.valueOf(checker.getEnabled()), Long.parseLong(checker.getTime()), asTimeUnit(checker.getUnit()),
                Long.parseLong(checker.getThresholdTime()), asTimeUnit(checker.getThresholdTimeUnit()));
    }    
    
}
