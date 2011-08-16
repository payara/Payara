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

package org.glassfish.virtualization.commands;

import com.sun.enterprise.config.serverbeans.*;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.logging.Level;

/**
 * Deletes a virtual machine
 * @author Jerome Dochez
 */
@Service(name="delete-vm")
@Scoped(PerLookup.class)
public class DeleteVirtualMachine implements AdminCommand {

    @Param(primary=true)
    String clusterName;

    @Inject(optional = true)
    IAAS groups=null;

    @Inject
    RuntimeContext rtContext;

    @Inject
    Domain domain;

    @Override
    public void execute(AdminCommandContext context) {

        Cluster cluster = domain.getClusterNamed(clusterName);
        if (cluster==null) {
            context.getActionReport().failure(RuntimeContext.logger, "Cannot find cluster named " + clusterName);
            return;
        }
        if (cluster.getServerRef().size()==0) {
            context.getActionReport().failure(RuntimeContext.logger, "Cluster is empty");
            return;
        }
        // so far, this is simplistic, we remove the first node. We should eventually
        // have more intelligence, for instance, killing the node on the machine which is
        // the most used to offload it, or killing the node in a machine that does nothing else
        // so we can switch off the machine.
        deleteServerRef(context, cluster.getServerRef().get(0));

    }

    private void deleteServerRef(AdminCommandContext context, ServerRef serverRef) {

        String instanceName = serverRef.getRef();
        Server instance = domain.getServerNamed(instanceName);
        if (instance!=null) {
            String serverPoolName = instance.getProperty("ServerPool").getValue();
            rtContext.executeAdminCommand(context.getActionReport(), "stop-instance", instanceName, "_vmShutdown", "false");
            rtContext.executeAdminCommand(context.getActionReport(), "delete-instance", instanceName);

            // stop and delete the virtual machine.
            try {
                groups.byName(serverPoolName).delete(instance);
            } catch (VirtException e) {
                RuntimeContext.logger.log(Level.WARNING,
                        "Exception while deleting virtual machine " + instanceName, e);
            }
        }
    }
}
