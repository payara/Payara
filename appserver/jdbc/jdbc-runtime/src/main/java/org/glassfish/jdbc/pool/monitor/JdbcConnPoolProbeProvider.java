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

package org.glassfish.jdbc.pool.monitor;

import org.glassfish.external.probe.provider.annotations.ProbeParam;
import org.glassfish.external.probe.provider.annotations.Probe;
import org.glassfish.external.probe.provider.annotations.ProbeProvider;
import com.sun.enterprise.resource.pool.monitor.*;
/**
 * Probe provider interface for JDBC connection pool related events to provide 
 * information related to the various objects on jdbc pool monitoring.
 * 
 * @author Shalini M
 */
@ProbeProvider(moduleProviderName="glassfish", moduleName="jdbc", probeProviderName="connection-pool")
public class JdbcConnPoolProbeProvider extends ConnectionPoolProbeProvider {

    /**
     * Emits probe event/notification that the given jdbc connection pool 
     * <code>poolName</code>has got a connection validation failed event.
     * 
     * @param poolName for which connection validation has failed
     * @param increment number of times the validation failed
     */
    @Probe(name="connectionValidationFailedEvent")
    @Override
    public void connectionValidationFailedEvent(
            @ProbeParam("poolName") String poolName, 
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName,
            @ProbeParam("increment") int increment){ }

    /**
     * Emits probe event/notification that a jdbc connection pool with the given
     * name <code>poolName</code> has got a connection timed out event.
     * 
     * @param poolName that has got a connection timed-out event
     */
    @Probe(name="connectionTimedOutEvent")
    @Override
    public void connectionTimedOutEvent(@ProbeParam("poolName") String poolName,
                                        @ProbeParam("appName") String appName,
                                        @ProbeParam("moduleName") String moduleName
                                        ) { }
    
    /**
     * Emits probe event/notification that the pool with the given name 
     * <code>poolName</code> is having a potentialConnLeak event.
     * 
     * @param poolName
     */
    @Probe(name="potentialConnLeakEvent")
    @Override
    public void potentialConnLeakEvent(@ProbeParam("poolName") String poolName,
                                       @ProbeParam("appName") String appName,
                                       @ProbeParam("moduleName") String moduleName
                                       ) { }
    
    /**
     * Emits probe event/notification that the given jdbc connection pool 
     * <code>poolName</code>has got a decrement free connections size event.
     * 
     * @param poolName for which decrement numConnFree is got
     * @param steadyPoolSize 
     */
    @Probe(name="decrementNumConnFreeEvent")
    @Override
    public void decrementNumConnFreeEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName
            ) { }

    /**
     * Emits probe event/notification that the given jdbc connection pool 
     * <code>poolName</code>has got a increment free connections size event.
     * 
     * @param poolName for which increment numConnFree is got
     * @param beingDestroyed if connection is destroyed due to error
     * @param steadyPoolSize 
     */
    @Probe(name="incrementNumConnFreeEvent")
    @Override
    public void incrementNumConnFreeEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName,
            @ProbeParam("beingDestroyed") boolean beingDestroyed,            
            @ProbeParam("steadyPoolSize") int steadyPoolSize) { }

    /**
     * Emits probe event/notification that the given jdbc connection pool 
     * <code>poolName</code>has got a decrement connections used event.
     * 
     * @param poolName for which decrement numConnUsed is got
     */
    @Probe(name="decrementConnectionUsedEvent")
    @Override
    public void decrementConnectionUsedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName
            ) { }
    
    /**
     * Emits probe event/notification that the given jdbc connection pool 
     * <code>poolName</code>has got a increment connections used event.
     * 
     * @param poolName for which increment numConnUsed is got
     */
    @Probe(name="connectionUsedEvent")
    @Override
    public void connectionUsedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName
            ) { }

    /**
     * Emits probe event/notification that the given jdbc connection pool 
     * <code>poolName</code>has got a increment connections free event.
     * 
     * @param poolName for which increment numConnFree is got
     * @param count number of connections freed to pool
     */
    @Probe(name="connectionsFreedEvent")
    @Override
    public void connectionsFreedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName,
            @ProbeParam("count") int count) { }

    /**
     * Emits probe event/notification that a connection request is served in the
     * time <code>timeTakenInMillis</code> for the given jdbc connection pool
     * <code> poolName</code> 
     * 
     * @param poolName 
     * @param timeTakenInMillis time taken to serve a connection
     */
    @Probe(name="connectionRequestServedEvent")
    @Override
    public void connectionRequestServedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName,
            @ProbeParam("timeTakenInMillis") long timeTakenInMillis) { }

    /**
     * Emits probe event/notification that a connection is destroyed for the 
     * given jdbc connection pool <code>poolName</code>
     * 
     * @param poolName 
     */
    @Probe(name="connectionDestroyedEvent")
    @Override
    public void connectionDestroyedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName
            ) { }

    /**
     * Emits probe event/notification that a connection is acquired by application
     * for the given jdbc connection pool <code>poolName</code>
     * 
     * @param poolName
     */
    @Probe(name="connectionAcquiredEvent")
    @Override
    public void connectionAcquiredEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName
            ) { }

    /**
     * Emits probe event/notification that a connection is released for the given
     * jdbc connection pool <code>poolName</code>
     * 
     * @param poolName
     */
    @Probe(name="connectionReleasedEvent")
    @Override
    public void connectionReleasedEvent(@ProbeParam("poolName") String poolName,
                                        @ProbeParam("appName") String appName,
                                        @ProbeParam("moduleName") String moduleName
                                        ) { }

    /**
     * Emits probe event/notification that a new connection is created for the
     * given jdbc connection pool <code>poolName</code>
     * 
     * @param poolName
     */
    @Probe(name="connectionCreatedEvent")
    @Override
    public void connectionCreatedEvent(@ProbeParam("poolName") String poolName,
                                       @ProbeParam("appName") String appName,
                                       @ProbeParam("moduleName") String moduleName
                                       ) { }
    
    @Probe(name="toString", hidden=true)
    @Override
    public void toString(@ProbeParam("poolName") String poolName,
                         @ProbeParam("appName") String appName,
                         @ProbeParam("moduleName") String moduleName,
            @ProbeParam("stackTrace") StringBuffer stackTrace) { }

    /**
     * Emits probe event/notification that a connection under test matches the 
     * current request for the given jdbc connection pool <code>poolName</code>
     * 
     * @param poolName
     */
    @Probe(name="connectionMatchedEvent")    
    @Override
    public void connectionMatchedEvent(@ProbeParam("poolName") String poolName,
                                       @ProbeParam("appName") String appName,
                                       @ProbeParam("moduleName") String moduleName
                                       ) { }

    /**
     * Emits probe event/notification that a connection under test does not 
     * match the current request for the given jdbc connection pool 
     * <code>poolName</code>
     * 
     * @param poolName
     */
    @Probe(name="connectionNotMatchedEvent")
    @Override
    public void connectionNotMatchedEvent(@ProbeParam("poolName") String poolName,
                                          @ProbeParam("appName") String appName,
                                          @ProbeParam("moduleName") String moduleName
                                          ) { }

    /**
     * Emits probe event/notification that the wait queue length has increased 
     * for the given jdbc connection pool <code>poolName</code>
     * 
     * @param poolName
     */
    @Probe(name="connectionRequestQueuedEvent")
    @Override
    public void connectionRequestQueuedEvent(@ProbeParam("poolName") String poolName,
                                             @ProbeParam("appName") String appName,
                                             @ProbeParam("moduleName") String moduleName
                                             ) { }

    /**
     * Emits probe event/notification that the wait queue length has decreased
     * for the given jdbc connection pool <code>poolName</code>
     * 
     * @param poolName
     */
    @Probe(name="connectionRequestDequeuedEvent")
    @Override
    public void connectionRequestDequeuedEvent(@ProbeParam("poolName") String poolName,
                                               @ProbeParam("appName") String appName,
                                               @ProbeParam("moduleName") String moduleName
                                               ) { }
}
