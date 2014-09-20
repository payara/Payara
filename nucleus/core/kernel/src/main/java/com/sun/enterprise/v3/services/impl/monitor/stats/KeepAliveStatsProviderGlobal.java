/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.services.impl.monitor.stats;

import org.glassfish.external.probe.provider.annotations.ProbeListener;
import org.glassfish.external.probe.provider.annotations.ProbeParam;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedObject;

/**
 * Server wide Keep-alive statistics
 *
 * @author Amy Roh
 */
@AMXMetadata(type = "keep-alive-mon", group = "monitoring")
@ManagedObject
@Description("Keep-Alive Statistics")
public class KeepAliveStatsProviderGlobal extends KeepAliveStatsProvider {

    public KeepAliveStatsProviderGlobal(String name) {
        super(name);
    }

    @ProbeListener("glassfish:kernel:connections-keep-alive:setMaxCountRequestsEvent")
    @Override
    public void setMaxCountRequestsEvent(
            @ProbeParam("listenerName") String listenerName,
            @ProbeParam("maxRequests") int max) {
        maxRequestsCount.setCount(max);
    }

    @ProbeListener("glassfish:kernel:connections-keep-alive:setTimeoutInSecondsEvent")
    @Override
    public void setTimeoutInSecondsEvent(
            @ProbeParam("listenerName") String listenerName,
            @ProbeParam("timeoutInSeconds") int timeoutInSeconds) {
        this.timeoutInSeconds.setCount(timeoutInSeconds);
    }

    @ProbeListener("glassfish:kernel:connections-keep-alive:incrementCountConnectionsEvent")
    @Override
    public void incrementCountConnectionsEvent(@ProbeParam("listenerName") String listenerName) {
        keepAliveConnectionsCount.increment();
        totalKeepAliveConnectionsCount.increment();
    }

    @ProbeListener("glassfish:kernel:connections-keep-alive:decrementCountConnectionsEvent")
    @Override
    public void decrementCountConnectionsEvent(@ProbeParam("listenerName") String listenerName) {
        keepAliveConnectionsCount.decrement();
    }

    @ProbeListener("glassfish:kernel:connections-keep-alive:incrementCountFlushesEvent")
    @Override
    public void incrementCountFlushesEvent(@ProbeParam("listenerName") String listenerName) {
//        flushesCount.increment();
    }

    @ProbeListener("glassfish:kernel:connections-keep-alive:incrementCountHitsEvent")
    @Override
    public void incrementCountHitsEvent(@ProbeParam("listenerName") String listenerName) {
        hitsCount.increment();
    }

    @ProbeListener("glassfish:kernel:connections-keep-alive:incrementCountRefusalsEvent")
    @Override
    public void incrementCountRefusalsEvent(@ProbeParam("listenerName") String listenerName) {
        refusalsCount.increment();
    }

    @ProbeListener("glassfish:kernel:connections-keep-alive:incrementCountTimeoutsEvent")
    @Override
    public void incrementCountTimeoutsEvent(@ProbeParam("listenerName") String listenerName) {
        timeoutsCount.increment();
    }
    
}
