/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.admin.commands;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.JavaConfig;
import com.sun.enterprise.config.serverbeans.JvmOptionBag;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.i18n.StringManager;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.UnknownOptionsAreOperands;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.AdminCommandSecurity;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Deletes given JVM options in server's configuration.
 * @author &#2325;&#2375;&#2342;&#2366;&#2352 (km@dev.java.net)
 * @author Kin-man Chung
 * @since GlassFish V3
 */
@Service(name="delete-jvm-options")   //implements the cli command by this "name"
@PerLookup            //should be provided "per lookup of this class", not singleton
@I18n("delete.jvm.options")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
@UnknownOptionsAreOperands()
public final class DeleteJvmOptions implements AdminCommand, AdminCommandSecurity.Preauthorization {

    @Param(name="target", optional=true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;

    @Param(name="profiler", optional=true)
    Boolean fromProfiler = false;
        
    @Param(name="jvm_option_name", primary=true, separator=':')
    List<String> jvmOptions;
    
    @Inject
    Target targetService;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    private static final StringManager lsm = StringManager.getManager(ListJvmOptions.class); 

    @AccessRequired.To("update")
    private JavaConfig jc;
    
    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        config = CLIUtil.updateConfigIfNeeded(config, targetService, target);
        jc = config.getJavaConfig();
        return true;
    }

    public void execute(AdminCommandContext context) {
        //validate the target first
        final ActionReport report = context.getActionReport();

        try {
            JvmOptionBag bag;
            if (fromProfiler) {
                if (jc.getProfiler() == null) {
                    report.setMessage(lsm.getString("create.profiler.first"));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
                bag = jc.getProfiler();
            } else
                bag = jc;
            ActionReport.MessagePart part = report.getTopMessagePart().addChild();
            deleteX(bag, jvmOptions, part);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : 
                lsm.getStringWithDefault("delete.jvm.options.failed",
                    "Command: delete-jvm-options failed", new String[]{e.getMessage()});
            report.setMessage(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);        
    }


    /** Adds the JVM option transactionally.
     * @throws java.lang.Exception
     */
    // following should work in the fullness of time ...
    /*
    private static void addX(JavaConfig jc, final String option) throws Exception {
        SingleConfigCode<JavaConfig> scc = new SingleConfigCode<JavaConfig> () {
            public Object run(JavaConfig jc) throws PropertyVetoException, TransactionFailure {
                List<String> jvmopts = jc.getJvmOptions();
                jvmopts.add(option);
                return ( jc.getJvmOptions() );
            }
        };
        ConfigSupport.apply(scc, jc);
    }
    */
    //@ForTimeBeing :)
    private void deleteX(final JvmOptionBag bag, final List<String> toRemove, final ActionReport.MessagePart part) throws Exception {
        SingleConfigCode<JvmOptionBag> scc = new SingleConfigCode<JvmOptionBag> () {
            public Object run(JvmOptionBag bag) throws PropertyVetoException, TransactionFailure {
                List<String> jvmopts = new ArrayList<String>(bag.getJvmOptions());
                int orig = jvmopts.size();
                boolean removed = jvmopts.removeAll(toRemove);
                bag.setJvmOptions(jvmopts);
                int now = jvmopts.size();
                if (removed) {
                    part.setMessage(lsm.getString("deleted.message", (orig-now)));
                } else {
                    part.setMessage(lsm.getString("no.option.deleted"));
                }
                return true;
            }
        };
        ConfigSupport.apply(scc, bag);
    }
}
