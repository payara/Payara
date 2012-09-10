/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.loadbalancer.admin.cli;

import java.util.logging.Logger;
import java.util.Map;
import java.util.Iterator;

import java.beans.PropertyVetoException;

import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.*;
import org.glassfish.api.Param;
import org.glassfish.api.I18n;
import org.glassfish.api.ActionReport;
import com.sun.enterprise.util.LocalStringManagerImpl;

import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Cluster;

import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.PerLookup;

/**
 * This is a remote command that enables lb-enabled attribute of an application
 * for cluster or instance
 * @author Yamini K B
 */
@Service(name = "configure-lb-weight")
@PerLookup
@I18n("configure.lb.weight")
@org.glassfish.api.admin.ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
    @RestEndpoint(configBean=Cluster.class,
        opType=RestEndpoint.OpType.POST, 
        path="configure-lb-weight", 
        description="Configure LB Weight",
        params={
            @RestParam(name="target", value="$parent")
        }),
    @RestEndpoint(configBean=Server.class,
        opType=RestEndpoint.OpType.POST, 
        path="configure-lb-weight", 
        description="Configure LB Weight",
        params={
            @RestParam(name="target", value="$parent")
        })
})
public final class ConfigureLBWeightCommand extends LBCommandsBase
                                            implements AdminCommand {

    @Param(optional=false)
    String cluster;

    @Param(primary=true)
    String weights;

    final private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(ConfigureLBWeightCommand.class);


    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        final Logger logger = context.getLogger();
        
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

        Map<String,Integer> instanceWeights = null;
        try {
            instanceWeights = getInstanceWeightsMap(weights);
        } catch (CommandException ce) {
            report.setMessage(localStrings.getLocalString("InvalidWeightValue", "Invalid weight value"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(ce);
            return;
        }

        Cluster cl = domain.getClusterNamed(cluster);
        if ( cl == null){
            String msg = localStrings.getLocalString("NoSuchCluster", "No such cluster {0}", cluster);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        for (Iterator it = instanceWeights.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            String instance = (String)entry.getKey();

            try {
                Server s = domain.getServerNamed(instance);
                if (s == null) {
                    String msg = localStrings.getLocalString("NoSuchInstance", "No such instance {0}", instance);
                    logger.warning(msg);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage(msg);
                    return;
                }

                Cluster c = domain.getClusterForInstance(s.getName());

                if (c == null) {
                   String msg = localStrings.getLocalString("InstanceDoesNotBelongToCluster",
                            "Instance {0} does not belong to cluster {1}.", instance,cluster);
                    logger.warning(msg);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage(msg);
                    return;
                }

                if (!c.getName().equals(cluster)) {
                    String msg = localStrings.getLocalString("InstanceDoesNotBelongToCluster",
                            "Instance {0} does not belong to cluster {1}.", instance,cluster);
                    logger.warning(msg);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage(msg);
                    return;
                }
                updateLBWeight(s, entry.getValue().toString());                
            } catch (TransactionFailure ex) {
                report.setMessage(ex.getMessage());
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(ex);
                return;
            }
        }
        
    }

    private void updateLBWeight(final Server s, final String w)
                                throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode<Server>() {
                @Override
                public Object run(Server param) throws PropertyVetoException, TransactionFailure {                    
                    param.setLbWeight(w);
                    return Boolean.TRUE;
                }
            }, s);
    }
}
