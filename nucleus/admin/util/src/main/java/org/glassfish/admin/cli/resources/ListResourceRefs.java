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

package org.glassfish.admin.cli.resources;

import com.sun.enterprise.config.serverbeans.Clusters;
import com.sun.enterprise.config.serverbeans.ConfigBeansUtilities;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.util.List;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.PerLookup;

/**
 * List Resource Refs Command
 * 
 */
@TargetType(value={CommandTarget.DAS, CommandTarget.CLUSTER, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTERED_INSTANCE})
@ExecuteOn(value={RuntimeType.DAS})
@Service(name="list-resource-refs")
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.resource.refs")
public class ListResourceRefs implements AdminCommand {
    
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ListResourceRefs.class);

    @Param(optional=true, primary=true)
    private String target = SystemPropertyConstants.DAS_SERVER_NAME;

    @Inject
    private Server[] servers;

    @Inject
    private Clusters clusters;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        
        try {
            Server targetServer = ConfigBeansUtilities.getServerNamed(target);
            if(targetServer != null){
                List<ResourceRef> resourceRefs = targetServer.getResourceRef();
                processResourceRefs(report, resourceRefs);
            } else {
                List<com.sun.enterprise.config.serverbeans.Cluster> clusterList = clusters.getCluster();
                if(clusterList != null){
                    for(com.sun.enterprise.config.serverbeans.Cluster cl : clusterList){
                        if(cl.getName().equals(target)){
                            List<ResourceRef> resourceRefs = cl.getResourceRef();
                            processResourceRefs(report, resourceRefs);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            report.setMessage(localStrings.getLocalString("list.resource.refs.failed",
                    "list-resource-refs failed"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    private void processResourceRefs(ActionReport report, List<ResourceRef> resourceRefs) {
        if (resourceRefs.isEmpty()) {
            final ActionReport.MessagePart part = report.getTopMessagePart().addChild();
            part.setMessage(localStrings.getLocalString(
                    "NothingToList", "Nothing to List."));
        } else {
            for (ResourceRef ref : resourceRefs) {
                final ActionReport.MessagePart part = report.getTopMessagePart().addChild();
                part.setMessage(ref.getRef());
            }
        }
    }
}
