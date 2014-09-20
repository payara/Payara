/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.connectors.work;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.resource.spi.work.Work;
import org.glassfish.logging.annotation.LogMessageInfo;
import com.sun.enterprise.connectors.work.context.WorkContextHandlerImpl;

/**
 * Represents one piece of work that will be submitted to the workqueue.
 *
 * @author Binod P.G
 */
public final class OneWork implements com.sun.corba.ee.spi.threadpool.Work {

    private final Work work;
    private final WorkCoordinator coordinator;
    private long nqTime;
    private static final Logger logger = LogFacade.getLogger();

    private String name = "Resource adapter work";
    private boolean nameSet = false;
    
    //Store the client's TCC so that when the work is executed,
    //TCC is set appropriately.
    private ClassLoader tcc = null;

    /**
     * Creates a work object that can be submitted to a workqueue.
     *
     * @param work Actual work submitted by Resource adapter.
     * @param coordinator <code>WorkCoordinator</code> object.
     */
    OneWork (Work work, WorkCoordinator coordinator, ClassLoader tcc) {
        this.work = work;
        this.coordinator = coordinator;
        this.tcc = tcc;
    }

    /**
     * This method is executed by thread pool as the basic work operation.
     */
    public void doWork() {
        ClassLoader callerCL = Thread.currentThread().getContextClassLoader();
        if(tcc != null && tcc != callerCL){
            Thread.currentThread().setContextClassLoader(tcc);
        }
        try{
        coordinator.preInvoke(); // pre-invoke will set work state to "started",
        boolean timedOut = coordinator.isTimedOut();

        // validation of work context should be after this
        //so as to throw WorkCompletedException in case of error.
        if (coordinator.proceed()) {
            try {
                coordinator.setupContext(this);
                //work-name will be set (if specified via HintsContext "javax.resources.spi.HintsContext.NAME_HINT")
                log("Start of Work");
            } catch (Throwable e) {
                coordinator.setException(e);
            }
        }

        //there may be failure in context setup
        if(coordinator.proceed()){
            try {
                work.run();
                log("Work Executed");
            } catch (Throwable t) {
                log("Execution has thrown exception " + t.getMessage());
                coordinator.setException(t);
            }
        }

        if(!timedOut){
            coordinator.postInvoke();
        }
        log("End of Work");
        }finally{
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if(cl != callerCL){
                Thread.currentThread().setContextClassLoader(callerCL);
            }
            tcc = null;
        }
    }

    @LogMessageInfo(
            message = "The Work named [ {0} ], progress [ {1} ].",
            comment = "Print Work status",
            level = "INFO",
            publish = false)
    private static final String RAR_WORK_PROGRESS_INFO = "AS-RAR-05004";

    public void log(String message){
        if(nameSet){
            Object args[] = new Object[]{name, message};
            logger.log(Level.INFO, RAR_WORK_PROGRESS_INFO, args);
        }
    }

    /**
     * Time at which this work is enqueued.
     *
     * @param tme Time in milliseconds.
     */
    public void setEnqueueTime(long tme) {
        this.nqTime = tme;
    }

    /**
     * Retrieves the time at which this work is enqueued
     *
     * @return Time in milliseconds.
     */
    public long getEnqueueTime() {
        return nqTime;
    }

    /**
     * Retrieves the name of the work.
     *
     * @return Name of the work.
     */
    public String getName() {
        return name;
    }

    public void setName(String name){
        this.name = name;
        nameSet = true;
    }

    /**
     * Retrieves the string representation of work.
     *
     * @return String representation of work.
     */
    public String toString() {
        StringBuffer result = new StringBuffer();
        if(nameSet){
            result.append("[Work : " + name + "] ");
        }
        result.append(work.toString());
        return result.toString();
    }
}
