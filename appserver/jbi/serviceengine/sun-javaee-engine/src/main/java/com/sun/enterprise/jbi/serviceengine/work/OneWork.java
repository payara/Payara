/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.jbi.serviceengine.work;

import java.util.logging.Logger;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.DeliveryChannel;
import com.sun.enterprise.jbi.serviceengine.ServiceEngineException;
import com.sun.corba.se.spi.orbutil.threadpool.Work;
import com.sun.enterprise.jbi.serviceengine.core.JavaEEServiceEngineContext;
import com.sun.logging.LogDomains;

/**
 * Represents one piece of work that will be submitted to the workqueue.
 *
 * @author Binod P.G	
 */
public abstract class OneWork implements Work {

    private long nqTime;
    protected static final Logger logger =
        LogDomains.getLogger(OneWork.class, LogDomains.SERVER_LOGGER);
    private MessageExchange me = null;
    private DeliveryChannel channel = null;
    private boolean useCurrentThread = true;
    private JavaEEServiceEngineContext seContext; 
    private WorkManager wm = null;
    private Exception exception = null;


    /**
     * Initializes the work. Save a local copy of delivery channel
     * and work manager.
     */
    public OneWork() {
        this.channel = JavaEEServiceEngineContext.getInstance().getDeliveryChannel();
        this.wm = JavaEEServiceEngineContext.getInstance().getWorkManager();
    }

    /**
     * This method is executed by thread pool as the basic work operation.
     */
    public abstract void doWork(); 
    
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
     * Set the MEP associated with this piece of work.
     */
    public void setMessageExchange(MessageExchange me) {
        this.me = me;
    }

    /**
     * Retrieves the MEP.
     */
    public MessageExchange getMessageExchange() {
        return me;
    }

    /**
     * Set a boolean indicating whether current thread should be used
     * for execution of this work.
     *
     * @param flag If set to true, then the current callng thread will
     * be used for executing the thread. If set to false, the work will
     * be submitted to the Queue of the thread pool.
     */
    public void setUseCurrentThread(boolean flag) {
        this.useCurrentThread = flag;
    }

    /**
     * Retrieves the flag indicating whether the current thread should 
     * be used for work execution or not.
     */
    public boolean getUseCurrentThread() {
        return this.useCurrentThread;
    }

    /**
     * Retrieves the work manager instance.
     */
    public WorkManager getWorkManager() {
        return this.wm;
    }

    /**
     * Retrieves the delivery channel object.
     */
    public DeliveryChannel getDeliveryChannel() {
        return this.channel;
    }

    /**
     * Get the exception, if any produced while executng
     * this work.
     */
    public Exception getException() {
        return this.exception;
    }

    /**
     * Convenience method to set the execption object 
     * produced while executing the work.
     */
    public void setException(Exception ex) {
        this.exception = ex;
    }

    /**
     * Execute the work. If current thread should be used,
     * doWork is called directly. Otherwise, work is submitted
     * to the thread pool.
     */
    protected void execute() {
        if (getUseCurrentThread()) {
            doWork();
        } else {
            getWorkManager().submitWork(this);
        }
    }

    /**
     * Retrieves the name of the work.
     *
     * @return Name of the work.
     */
    public String getName() {
        return "One JBI Work";
    }
}
