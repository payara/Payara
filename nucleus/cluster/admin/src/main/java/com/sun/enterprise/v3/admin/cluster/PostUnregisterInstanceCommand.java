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

package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.admin.util.ClusterOperationUtil;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.internal.api.Target;
import org.glassfish.common.util.admin.ParameterMapExtractor;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Causes InstanceRegisterInstanceCommand executions on the correct remote instances.
 *
 * @author Jennifer Chou
 */
@Service(name="_post-unregister-instance")
@Supplemental(value="_unregister-instance", ifFailure=FailurePolicy.Warn)
@PerLookup
@ExecuteOn(value={RuntimeType.DAS})
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.POST, 
        path="_post-unregister-instance", 
        description="_post-unregister-instance")
})
public class PostUnregisterInstanceCommand implements AdminCommand {
    @Param(name = "node", optional = true)
    public String node;
    @Param(name = "name", primary = true)
    public String instanceName;

    @Inject
    private ServiceLocator habitat;

    @Inject
    private Target target;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        final Logger logger = context.getLogger();
        final String clusterName = context.getActionReport().getResultType(String.class);
        if (clusterName != null) {
            try {
                ParameterMapExtractor pme = new ParameterMapExtractor(this);
                final ParameterMap paramMap = pme.extract();
                List<String> targets = new ArrayList<String>();
                List<Server> instances = target.getInstances(clusterName);
                for (Server s : instances) {
                    targets.add(s.getName());
                }

                ClusterOperationUtil.replicateCommand(
                        "_unregister-instance",
                        FailurePolicy.Warn,
                        FailurePolicy.Warn,
                        FailurePolicy.Ignore,
                        targets,
                        context,
                        paramMap,
                        habitat);
            } catch (Exception e) {
                report.failure(logger, e.getMessage());
            }
        }
    }
}
