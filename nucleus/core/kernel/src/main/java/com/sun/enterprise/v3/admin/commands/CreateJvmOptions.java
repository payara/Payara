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
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
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
 * Creates given JVM options in server's configuration.
 * @author &#2325;&#2375;&#2342;&#2366;&#2352 (km@dev.java.net)
 * @author Kin-man Chung
 * @since GlassFish V3
 */

@Service(name="create-jvm-options")   //implements the cli command by this "name"
@PerLookup            //should be provided "per lookup of this class", not singleton
@I18n("create.jvm.options")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
@UnknownOptionsAreOperands()
public final class CreateJvmOptions implements AdminCommand, AdminCommandSecurity.Preauthorization {

    @Param(name="target", optional=true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;

    @Param(name="profiler", optional=true)
    Boolean addToProfiler=false;
    
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
        final ActionReport report = context.getActionReport();
        try {
            JvmOptionBag bag;
            if (addToProfiler) { //make sure profiler element exists before creating a JVM option for profiler
                if (jc.getProfiler() == null) {
                    report.setMessage(lsm.getString("create.profiler.first"));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
                bag = jc.getProfiler();
            } else
                bag = jc;
            ActionReport.MessagePart part = report.getTopMessagePart().addChild();
            List<String> validOptions = new ArrayList<String>(jvmOptions);
            validate(bag, validOptions, report); //Note: method mutates the given list
            validateSoft(bag, validOptions, report); //Note: method does not mutate the given list
            addX(bag, validOptions, part);
        } catch (IllegalArgumentException iae) {
            report.setMessage(iae.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : 
                lsm.getStringWithDefault("create.jvm.options.failed",
                    "Command: create-jvm-options failed", new String[]{e.getMessage()});
            report.setMessage(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);        
    }

    private void validateSoft(JvmOptionBag bag, List<String> opts, ActionReport report) {
        //Note: These are only recommendations!
        Iterator<String> siter = opts.iterator();
        while (siter.hasNext()) {
            String opt = siter.next();
            validateSoftXmx(bag, opt, report);
            validateSoftXms(bag, opt, report);
        }
    }

    private void validateSoftXmx(JvmOptionBag bag, String opt, ActionReport report) {
        if (!opt.startsWith("-Xmx"))
            return;
        //now, opt is something like -Xmx512m or -Xmx2g, or -Xmx=12 i.e. it may contain illegal characters
        try {
            Pattern regex = Pattern.compile("-Xmx((\\d)+[m|g|k|M|G|K]?)+");
            boolean matches = regex.matcher(opt).matches();
            if (!matches) {
                String msg = lsm.getString("soft.invalid.xmx", opt);
                report.getTopMessagePart().addChild().setMessage(msg);
            }
        } catch(Exception e) {
            //squelch
            //e.printStackTrace();
        }
        String existingXmx = bag.getStartingWith("-Xmx");
        if (existingXmx != null) {
            //maybe a different Xmx was given
            String msg = lsm.getString("soft.xmx.exists", existingXmx);
            report.getTopMessagePart().addChild().setMessage(msg);
        }
        String existingXms = bag.getStartingWith("-Xms");
        if (existingXms != null) {
            int xmsInConfig = JvmOptionBag.Duck.toMeg(existingXms, "-Xms");
            int xmxGiven    = JvmOptionBag.Duck.toMeg(opt, "-Xmx");
            if (xmsInConfig > xmxGiven) { //i.e. domain.xml contains -Xms1g and you ask -Xmx512m to be set
                String msg = lsm.getString("soft.xmx.smaller.than.xms", xmxGiven + " MB", xmsInConfig + " MB");
                report.getTopMessagePart().addChild().setMessage(msg);
            }
        }
    }

    private void validateSoftXms(JvmOptionBag bag, String opt, ActionReport report) {
        if (!opt.startsWith("-Xms"))
            return;
        //now, opt is something like -Xms512m or -Xms2g, or -Xms=12 i.e. it may contain illegal characters
        try {
            Pattern regex = Pattern.compile("-Xms((\\d)+[m|g|k|M|G|K]?)+");
            boolean matches = regex.matcher(opt).matches();
            if (!matches) {
                String msg = lsm.getString("soft.invalid.xms", opt);
                report.getTopMessagePart().addChild().setMessage(msg);
            }
        } catch(Exception e) {
            //squelch
            //e.printStackTrace();
        }
        String existingXms = bag.getStartingWith("-Xms");
        if (existingXms != null) {
            //maybe a different Xmx was given
            String msg = lsm.getString("soft.xms.exists", existingXms);
            report.getTopMessagePart().addChild().setMessage(msg);
        }
        String existingXmx = bag.getStartingWith("-Xmx");
        if (existingXmx != null) {
            int xmxInConfig = JvmOptionBag.Duck.toMeg(existingXmx, "-Xmx");
            int xmsGiven    = JvmOptionBag.Duck.toMeg(opt, "-Xms");
            if (xmsGiven > xmxInConfig) { //i.e. domain.xml contains -Xms1g and you ask -Xmx512m to be set
                String msg = lsm.getString("soft.xms.larger.than.xmx", xmsGiven + " MB", xmxInConfig + " MB");
                report.getTopMessagePart().addChild().setMessage(msg);
            }
        }
    }

    private void validate(JvmOptionBag bag, List<String> opts, ActionReport report) 
            throws IllegalArgumentException {
        Iterator<String> siter = opts.iterator();
        while (siter.hasNext()) {
            String opt = siter.next();
            if (!opt.startsWith("-")) {
                String msg = lsm.getString("joe.invalid.start", opt);
                throw new IllegalArgumentException(msg);
            }
            if (bag.contains(opt)) {
                // setting an option that already exists is considered an error
                String msg = lsm.getString("joe.exists", opt);
                throw new IllegalArgumentException(msg);
            }
        }
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
    private void addX(final JvmOptionBag bag, final List<String> newOpts, final ActionReport.MessagePart part) throws Exception {
        SingleConfigCode<JvmOptionBag> scc = new SingleConfigCode<JvmOptionBag> () {
            public Object run(JvmOptionBag bag) throws PropertyVetoException, TransactionFailure {
                newOpts.removeAll(bag.getJvmOptions());  //"prune" the given list first to avoid duplicates
                List<String> jvmopts = new ArrayList<String>(bag.getJvmOptions());
                int orig = jvmopts.size();
                boolean added = jvmopts.addAll(newOpts);
                bag.setJvmOptions(jvmopts);
                int now = jvmopts.size();
                if (added) {
                    part.setMessage(lsm.getString("created.message", (now-orig)));
                } else {
                    part.setMessage(lsm.getString("no.option.created"));
                }
                return true;
            }
        };
        ConfigSupport.apply(scc, bag);
    }
}
