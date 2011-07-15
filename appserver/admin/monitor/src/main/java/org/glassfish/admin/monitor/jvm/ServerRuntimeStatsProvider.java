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

package org.glassfish.admin.monitor.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.external.statistics.StringStatistic;
import org.glassfish.external.statistics.impl.StringStatisticImpl;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

/* server.runtime */
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

    @ManagedAttribute(id="uptime")
    @Description( "uptime of the Java virtual machine in milliseconds" )
    public long getUptime() {
        return rtBean.getUptime();
    }

    @ManagedAttribute(id="starttime")
    @Description( "start time of the Java virtual machine" )
    public long getStartTime() {
        return rtBean.getStartTime();
    }

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
