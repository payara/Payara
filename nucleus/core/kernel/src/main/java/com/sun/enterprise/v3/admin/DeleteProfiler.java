/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.admin;

import com.sun.enterprise.config.serverbeans.Config;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.internal.api.Target;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import com.sun.enterprise.config.serverbeans.JavaConfig;
import com.sun.enterprise.config.serverbeans.Profiler;
import com.sun.enterprise.config.serverbeans.SystemPropertyBag;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collection;
import org.glassfish.api.admin.*;

/**
* Delete JDBC Resource command
*
*/
@Service(name="delete-profiler")
@PerLookup
@I18n("delete.profiler")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean=Profiler.class,
        opType=RestEndpoint.OpType.DELETE, 
        path="delete-profiler", 
        description="Delete Profiler")
})
public class DeleteProfiler implements AdminCommand, AdminCommandSecurity.Preauthorization {

   final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(DeleteProfiler.class);

    @Param(name="target", optional=true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;

    @Inject
    Target targetService;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    @AccessRequired.To("update")
    private JavaConfig javaConfig;

    
    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        config = CLIUtil.chooseConfig(targetService, config, target);
        javaConfig = config.getJavaConfig();
        return true;
    }

    /**
    * Executes the command with the command parameters passed as Properties
    * where the keys are the paramter names and the values the parameter values
    *
    * @param context information
    */
   public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();
        try {
           ConfigSupport.apply(new SingleConfigCode<JavaConfig>() {
               public Object run(JavaConfig param) throws PropertyVetoException, TransactionFailure {
                   if (param.getProfiler() != null) {
                       param.setProfiler(null);
                       report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
			return param;
                   }
                   // not found
                   report.setMessage(localStrings.getLocalString("delete.profiler.notfound", "delete failed, profiler not found"));
                   report.setActionExitCode(ActionReport.ExitCode.FAILURE);
		    return null;
               }
           }, javaConfig);
       } catch(TransactionFailure e) {
           report.setMessage(localStrings.getLocalString("delete.profiler.fail", "delete failed "));
           report.setActionExitCode(ActionReport.ExitCode.FAILURE);
           report.setFailureCause(e);
       }
   }
}
