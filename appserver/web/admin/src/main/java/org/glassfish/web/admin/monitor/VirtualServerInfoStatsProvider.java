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

package org.glassfish.web.admin.monitor;

import com.sun.enterprise.config.serverbeans.VirtualServer;
import org.glassfish.external.statistics.StringStatistic;
import org.glassfish.external.statistics.impl.StringStatisticImpl;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

@AMXMetadata(type="virtualserverinfo-mon", group="monitoring")
@ManagedObject
@Description("Virtual Server Statistics")
public class VirtualServerInfoStatsProvider {

    private static final String STATE_DESCRIPTION =
        "The state of the virtual server";

    private static final String HOSTS_DESCRIPTION =
        "The host (alias) names of the virtual server";

    private static final String ID_DESCRIPTION =
        "The id of the virtual server";

    private static final String MODE_DESCRIPTION =
        "The mode of the virtual server";

    private VirtualServer host;

    private StringStatisticImpl state = new StringStatisticImpl(
        "State", "String", STATE_DESCRIPTION);

    private StringStatisticImpl hosts = new StringStatisticImpl(
        "Hosts", "String", HOSTS_DESCRIPTION);

    private StringStatisticImpl id = new StringStatisticImpl(
        "Id", "String", ID_DESCRIPTION);

    private StringStatisticImpl mode = new StringStatisticImpl(
        "Mode", "String", MODE_DESCRIPTION);
    
    public VirtualServerInfoStatsProvider(VirtualServer host) {
        this.host = host;
    }

    @ManagedAttribute(id="state")
    @Description(STATE_DESCRIPTION)
    public StringStatistic getState() {
        state.setCurrent(host.getState());
        return state;
    }

    @ManagedAttribute(id="hosts")
    @Description(HOSTS_DESCRIPTION)
    public StringStatistic getHosts() {
        hosts.setCurrent(host.getHosts());
        return hosts;
    }  

    @ManagedAttribute(id="id")
    @Description(ID_DESCRIPTION)
    public StringStatistic getId() {
        id.setCurrent(host.getId());
        return id;
    }

    @ManagedAttribute(id="mode")
    @Description(MODE_DESCRIPTION)
    public StringStatistic getMode() {
        mode.setCurrent(host.getState().equals("on") ? "active" : "unknown");
        return mode;
    }
}
