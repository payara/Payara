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

package org.glassfish.jdbc.pool.monitor;

import org.glassfish.external.probe.provider.annotations.Probe;
import org.glassfish.external.probe.provider.annotations.ProbeParam;
import org.glassfish.external.probe.provider.annotations.ProbeProvider;
import com.sun.enterprise.resource.pool.monitor.*;
/**
 * Probe provider interface for JDBC connection pool related events to provide
 * information related to the various objects on jdbc pool monitoring grouped
 * by applications.
 *
 * @author Shalini M
 */
@ProbeProvider(moduleProviderName="glassfish", moduleName="jdbc-pool", probeProviderName="applications")
public class JdbcConnPoolAppProbeProvider extends ConnectionPoolAppProbeProvider {

    /**
     * Emits probe event/notification that the given jdbc connection pool
     * <code>poolName</code> for the <code>appName</code> has got a
     * decrement connections used event.
     *
     * @param poolName for which decrement numConnUsed is got
     * @param appName for which decrement numConnUsed is got
     */
    @Probe(name="decrementConnectionUsedEvent")
    public void decrementConnectionUsedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName) { }

    /**
     * Emits probe event/notification that the given jdbc connection pool
     * <code>poolName</code> for the <code>appName</code> has got an
     * increment connections used event.
     *
     * @param poolName for which increment numConnUsed is got
     * @param appName for which increment numConnUsed is got
     */
    @Probe(name="connectionUsedEvent")
    public void connectionUsedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName) { }

    /**
     * Emits probe event/notification that a connection is acquired by application
     * for the given jdbc connection pool <code>poolName</code> by
     * <code>appName</code>
     *
     * @param poolName
     * @param appName
     */
    @Probe(name="connectionAcquiredEvent")
    public void connectionAcquiredEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName) { }

    /**
     * Emits probe event/notification that a connection is released for the given
     * jdbc connection pool <code>poolName</code> by the
     * <code>appName</code>
     *
     * @param poolName
     * @param appName
     */
    @Probe(name="connectionReleasedEvent")
    public void connectionReleasedEvent(@ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName) { }


}
