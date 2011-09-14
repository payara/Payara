/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.admin;

import com.sun.enterprise.admin.util.ClusterOperationUtil;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import java.util.Collections;
import java.util.logging.Logger;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.FailurePolicy;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.common.util.admin.ParameterMapExtractor;
import org.jvnet.hk2.component.Habitat;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Utility methods useful from deployment-related commands.
 *
 * @author Tim Quinn
 */
public class DeploymentCommandUtils {

    /**
     * Replicates an enable or disable command to all instances in the cluster
     * of which the target is a member.  If the target is not cluster member
     * this method is a no-op.
     * @param commandName name of the command to replicate to cluster instances
     * @param domain domain containing the relevant configuration
     * @param target name of the target being enabled or disabled
     * @param appName name of the application being enabled or disabled
     * @param habitat hk2 habitat
     * @param context command context passed to the running enable or disable command 
     * @param command command object
     * @return
     */
    public static ActionReport.ExitCode replicateEnableDisableToContainingCluster(
            final String commandName,
            final Domain domain,
            final String target,
            final String appName,
            final Habitat habitat,
            final AdminCommandContext context,
            final AdminCommand command) throws IllegalArgumentException, IllegalAccessException {
        /*
         * If the target is a cluster instance, the DAS will broadcast the command
         * to all instances in the cluster so they can all update their configs.
         */
        final Cluster containingCluster = domain.getClusterForInstance(target);
        if (containingCluster != null) {
            final ParameterMapExtractor extractor = new ParameterMapExtractor(command);
            final ParameterMap pMap = extractor.extract(Collections.EMPTY_LIST);
            pMap.set("DEFAULT", appName);

            return ClusterOperationUtil.replicateCommand(
                    commandName,
                    FailurePolicy.Error,
                    FailurePolicy.Warn,
                    FailurePolicy.Ignore,
                    containingCluster.getInstances(),
                    context,
                    pMap,
                    habitat);
        }
        return ActionReport.ExitCode.SUCCESS;
    }

    public static String getLocalHostName() {
        String defaultHostName = "localhost";
        try {
            InetAddress host = InetAddress.getLocalHost();
            defaultHostName = host.getCanonicalHostName();
        } catch(UnknownHostException uhe) {
           // ignore
        }
        return defaultHostName;
    }

    public static String getTarget(ParameterMap parameters, OpsParams.Origin origin, Deployment deployment) {
        String appName = parameters.getOne("DEFAULT");
        String targetName = deployment.getDefaultTarget(appName, origin);
        parameters.set("target", targetName);
        return targetName;
    }
}
