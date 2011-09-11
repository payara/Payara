/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.paas.lbplugin.cli;

import java.util.Collection;
import java.util.logging.Level;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.paas.lbplugin.LBServiceUtil;
import org.glassfish.paas.lbplugin.logger.LBPluginLogger;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.glassfish.virtualization.runtime.VirtualCluster;
import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.spi.VirtException;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

/**
 *
 * @author kshitiz
 */
@Service
@Scoped(PerLookup.class)
public class BaseLBService {

    @Param(name = "servicename", primary = true)
    String serviceName;
    @Param(name = "appname", optional = true)
    String appName;
    @Inject
    LBServiceUtil lbServiceUtil;
    // TODO :: remove dependency on VirtualCluster(s).
    @Inject(optional = true) // // made it optional for non-virtual scenario to work
    VirtualClusters virtualClusters;

    VirtualCluster virtualCluster;
    VirtualMachine virtualMachine;

    public void execute(AdminCommandContext context) {
    }
    
    void retrieveVirtualMachine() throws VirtException {
        if (virtualClusters != null && serviceName != null) {
            virtualCluster = virtualClusters.byName(serviceName);
            String vmId = lbServiceUtil.getInstanceID(
                    serviceName, appName, ServiceType.LOAD_BALANCER);
            if (vmId != null) {
                LBPluginLogger.getLogger().log(Level.INFO,"Found VirtualMachine for load-balancer with id : " + vmId);
                virtualMachine = virtualCluster.vmByName(vmId);
                // TODO :: IMS should give differnt way to get hold of VM using the vmId
                return;
            }
            LBPluginLogger.getLogger().log(Level.INFO,"Unable to find VirtualMachine for load-balancer with vmId : " + vmId);
        } else {
            LBPluginLogger.getLogger().log(Level.INFO,"Unable to find VirtualMachine for load-balancer as virtualCluters or serviceName is null");
        }
    }

}
