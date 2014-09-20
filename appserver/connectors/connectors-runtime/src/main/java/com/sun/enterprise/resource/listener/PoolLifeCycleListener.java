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

package com.sun.enterprise.resource.listener;

/**
 * Pool Life cycle listener that can be implemented by listeners for pool monitoring
 *
 * @author Jagadish Ramu
 */
public interface PoolLifeCycleListener {

    /**
     * Print stack trace in server.log
     * @param stackTrace
     */
    void toString(StringBuffer stackTrace);
    
    /**
     * indicates that a connection is acquired by application
     */
    void connectionAcquired(long resourceHandleId);

    /**
     * indicates that a connection request is server in the time
     * @param timeTakenInMillis time taken to serve a connection
     */
    void connectionRequestServed(long timeTakenInMillis);

    /**
     * indicates that a connection is timed-out
     */
    void connectionTimedOut();

    /**
     * indicates that a connection under test does not match the current request
     */
    void connectionNotMatched();

    /**
     * indicates that a connection under test matches the current request
     */
    void connectionMatched();

    /**
     * indicates that a connection is being used
     */
    void connectionUsed(long resourceHandleId);

    /**
     * indicates that a connection is destroyed
     */
    void connectionDestroyed(long resourceHandleId);

    /**
     * indicates that a connection is released
     */
    void connectionReleased(long resourceHandleId);

    /**
     * indicates that a new connection is created
     */
    void connectionCreated();

    /**
     * indicates that a potential connection leak happened
     */
    void foundPotentialConnectionLeak();

    /**
     * indicates that a number of connections have failed validation
     * @param count number of connections
     */
    void connectionValidationFailed(int count);

    /**
     * indicates the number of connections freed to pool
     * @param count number of connections
     */
    void connectionsFreed(int count);

    /**
     * indicates that connection count that is used has to be decremented.
     */
    void decrementConnectionUsed(long resourceHandleId);

    /**
     * indicates that free connections count in the pool has to be decremented.
     */
    void decrementNumConnFree();    
    
    /**
     * indicates that a connection is freed and the count is to be incremented.
     * @param beingDestroyed in case of an error.
     * @param steadyPoolSize
     */
    void incrementNumConnFree(boolean beingDestroyed, int steadyPoolSize);
    
    /**
     * indicates that the wait queue length has increased.
     */
    void connectionRequestQueued();
    
    /**
     * indicates that the wait queue length has decreased.
     */
    void connectionRequestDequeued();

}
