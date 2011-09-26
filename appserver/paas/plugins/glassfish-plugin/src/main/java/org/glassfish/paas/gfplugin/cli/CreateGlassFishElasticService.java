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

package org.glassfish.paas.gfplugin.cli;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.scopes.PerLookup;
import org.glassfish.paas.gfplugin.GlassFishPlugin;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;


@Service(name="_create-glassfish-elastic-service")
@Supplemental(value="_create-glassfish-service", ifFailure= FailurePolicy.Warn, on= Supplemental.Timing.After)
@Scoped(PerLookup.class)
@ExecuteOn(value={RuntimeType.DAS})
@CommandLock(CommandLock.LockType.NONE)
public class CreateGlassFishElasticService implements AdminCommand {

    @Param(name = "servicename", primary = true)
    private String serviceName;

    @Param(name="appname", optional=true)
    private String appName;

    @Inject
    CommandRunner commandRunner;

    @Inject
    GlassFishServiceUtil gfServiceUtil;

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        ParameterMap parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT",serviceName);

        String min = gfServiceUtil.getProperty(serviceName, GlassFishPlugin.MIN_CLUSTER_PROPERTY_NAME);
        if(min != null){
            parameterMap.add("min", min);
        }
        String max = gfServiceUtil.getProperty(serviceName, GlassFishPlugin.MAX_CLUSTER_PROPERTY_NAME);
        if(max != null){
            parameterMap.add("max", max);
        }
        commandRunner.getCommandInvocation("_create-elastic-service", report).parameters(parameterMap).execute();
    }
}
