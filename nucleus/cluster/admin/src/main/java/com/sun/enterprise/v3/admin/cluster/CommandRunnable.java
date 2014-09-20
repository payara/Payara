/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.admin.cluster;

import java.util.concurrent.BlockingQueue;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;

/**
 * This class wraps a CommandInvocation so that it can be run via a
 * thread pool. On construction you pass it the CommandInvocation
 * to execute as well as a response queue and the ActionReport
 * that was set on the CommandInvocation. When the run() method
 * is called the CommandInvocation is executed (which sets its results
 * in the ActionReport) and then it adds itself to the response queue
 * where it can be picked up and the ActionReport inspected for the results.
 *
 * @author dipol
 */
public class CommandRunnable implements Runnable {

    BlockingQueue<CommandRunnable> responseQueue = null;
    String name = "";
    CommandInvocation ci = null;
    ActionReport report = null;

    private CommandRunnable() {
    }

    /**
     * Construct a CommandRunnable. This class wraps a CommandInvocation
     * so that it can be executed via a thread pool.
     *
     * @param ci        A CommandInvocation containing the command you want
     *                  to run.
     * @param report    The ActionReport you used with the CommandInvocation
     * @param q         A blocking queue that this class will add itself to
     *                  when its run method has completed.
     *
     * After dispatching this class to a thread pool the caller can block
     * on the response queue where it will dequeue CommandRunnables and then
     * use the getActionReport() method to retrieve the results.
     */
    public CommandRunnable(CommandInvocation ci, ActionReport report,
            BlockingQueue<CommandRunnable> q) {
        this.responseQueue = q;
        this.report = report;
        this.ci = ci;
    }

    @Override
    public void run() {
        ci.execute();
        if (responseQueue != null) {
            responseQueue.add(this);
        }
    }

    /**
     * Set a name on the runnable. The name is not interpreted to mean
     * anything so the caller can use it for whatever it likes.
     *
     * @param s The name
     */
    public void setName(String s) {
        this.name = s;
    }

    /**
     * Get the name that was previously set.
     *
     * @return  A name that was previously set or null if no name was set.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the CommandInvocation that was passed on the constructor
     *
     * @return  The CommandInvocation that was passed on the constructor
     */
    public CommandInvocation getCommandInvocation() {
        return ci;
    }

    /**
     * Returns the ActionReport that was passed on the constructor.
     *
     * @return  the ActionReport that was passed on the constructor.
     */
    public ActionReport getActionReport() {
        return report;
    }

    @Override
    public String toString() {
        if (name == null) {
            return "null";
        } else {
            return name;
        }
    }
}
