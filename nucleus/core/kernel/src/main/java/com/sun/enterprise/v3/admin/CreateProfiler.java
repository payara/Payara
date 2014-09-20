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

import java.beans.PropertyVetoException;
import java.util.Map;
import java.util.Properties;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.JavaConfig;
import com.sun.enterprise.config.serverbeans.Profiler;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;

import org.jvnet.hk2.config.types.Property;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.internal.api.Target;

import javax.inject.Inject;
import javax.inject.Named;


import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Create Profiler Command
 *
 */
@Service(name="create-profiler")
@PerLookup
@I18n("create.profiler")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean=JavaConfig.class,
        opType=RestEndpoint.OpType.POST, 
        path="create-profiler", 
        description="Create Profiler")
})
public class CreateProfiler implements AdminCommand, AdminCommandSecurity.Preauthorization {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CreateProfiler.class);

    @Param(optional=true)
    String classpath;

    @Param(optional=true, defaultValue="true")
    Boolean enabled;

    @Param(name="nativelibrarypath", optional=true)
    String nativeLibraryPath;

    @Param(name="profiler_name", primary=true)
    String name;

    @Param(name="property", optional=true, separator=':')
    Properties properties;

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

        if (javaConfig.getProfiler() != null) {
            System.out.println("profiler exists. Please delete it first");
            report.setMessage(
                localStrings.getLocalString("create.profiler.alreadyExists",
                "profiler exists. Please delete it first"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<JavaConfig>() {

                public Object run(JavaConfig param) throws PropertyVetoException, TransactionFailure {
                    Profiler newProfiler = param.createChild(Profiler.class);
                    newProfiler.setName(name);
                    newProfiler.setClasspath(classpath);
                    newProfiler.setEnabled(enabled.toString());
                    newProfiler.setNativeLibraryPath(nativeLibraryPath);
                    if (properties != null) {
                        for ( Map.Entry e : properties.entrySet()) {
                            Property prop = newProfiler.createChild(Property.class);
                            prop.setName((String)e.getKey());
                            prop.setValue((String)e.getValue());
                            newProfiler.getProperty().add(prop);
                        }
                    }
                    param.setProfiler(newProfiler);                    
                    return newProfiler;
                }
            }, javaConfig);

        } catch(TransactionFailure e) {
            report.setMessage(localStrings.getLocalString("create.profiler.fail", "{0} create failed ", name));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
