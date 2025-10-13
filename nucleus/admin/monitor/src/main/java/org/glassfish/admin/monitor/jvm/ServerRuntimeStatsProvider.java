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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package org.glassfish.admin.monitor.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

/**
 * Class providing the MBean for server runtime statistics
 * <p>
 * The MBean will be of the format
 * {@code amx:pp=/mon/server-mon[server],type=server-runtime-mon}
 * and can be enabled by turning the Jvm monitoring level in the admin console to LOW
 */
@AMXMetadata(type="server-runtime-mon", group="monitoring", isSingleton=true)
@ManagedObject
@Description( "Server Runtime Statistics" )
public class ServerRuntimeStatsProvider {
    
    private final RuntimeMXBean rtBean = ManagementFactory.getRuntimeMXBean();

    public static final int STARTING_STATE = 0;
    public static final int RUNNING_STATE = 1;
    public static final int STOPPING_STATE = 2;
    public static final int STOPPED_STATE = 3;
    public static final int FAILED_STATE = 4;
    private int state = STOPPED_STATE;

    /**
     * Gets the uptime of the Java virtual machine
     * @return time in milliseconds
     */
    @ManagedAttribute(id="uptime")
    @Description( "uptime of the Java virtual machine in milliseconds" )
    public long getUptime() {
        return rtBean.getUptime();
    }

    /**
     * Gets the start time of the Java virtual machine
     * @return time in milliseconds since the epoch (1st January 1970)
     */
    @ManagedAttribute(id="starttime")
    @Description( "start time of the Java virtual machine" )
    public long getStartTime() {
        return rtBean.getStartTime();
    }

    /**
     * Gets the state of the server such as Running, Stopped, Failed.
     * @return a number representing the state of the server
     * @see #STARTING_STATE
     * @see #RUNNING_STATE
     * @see #STOPPING_STATE
     * @see #STOPPED_STATE
     * @see #FAILED_STATE
     */
    @ManagedAttribute(id="state")
    @Description( "state of the server such as Running, Stopped, Failed" )
    public synchronized long getState() {
        if (rtBean != null) {
            return RUNNING_STATE;
        }
        return state;
    }

    //TODO: set state based on server events
    public synchronized void setState(int state) {
        this.state = state;
    }

}
