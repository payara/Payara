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
package com.sun.enterprise.v3.admin;

import java.io.*;
import static com.sun.enterprise.util.StringUtils.ok;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.JavaConfig;
import java.util.Properties;
import org.glassfish.internal.api.Globals;
import static com.sun.enterprise.util.StringUtils.ok;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.OS;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import java.util.logging.Level;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.lang.management.RuntimeMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import com.sun.enterprise.module.bootstrap.StartupContext;
import java.util.logging.Logger;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.*;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.types.Property;
import static org.glassfish.api.ActionReport.ExitCode.SUCCESS;

/**
 * https://glassfish.dev.java.net/issues/show_bug.cgi?id=12483
 * @author Byron Nevins
 * @author Ludovic Champenois
 */
@Service(name = "_get-runtime-info")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTERED_INSTANCE})
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.GET, 
        path="get-runtime-info", 
        description="Get Runtime Info")
})
@AccessRequired(resource="domain", action="read")
public class RuntimeInfo implements AdminCommand {
    public RuntimeInfo() {
    }

    @Override
    public void execute(AdminCommandContext context) {
        report = context.getActionReport();
        report.setActionExitCode(SUCCESS);
        top = report.getTopMessagePart();
        logger = context.getLogger();
        jpdaEnabled = Boolean.parseBoolean(ctx.getArguments().getProperty("-debug"));
        javaConfig = config.getJavaConfig();
        int debugPort = parsePort(javaConfig.getDebugOptions());
        top.addProperty("debug", Boolean.toString(jpdaEnabled));
        top.addProperty("debugPort", Integer.toString(debugPort));
        final OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        top.addProperty("os.arch", osBean.getArch());
        top.addProperty("os.name", osBean.getName());
        top.addProperty("os.version", osBean.getVersion());
        top.addProperty("availableProcessorsCount", "" + osBean.getAvailableProcessors());

        // getTotalPhysicalMemorySize is from com.sun.management.OperatingSystemMXBean and cannot easily access it via OSGi
        // also if we are not on a sun jdk, we will not return this attribute.
        if ( !OS.isAix()) {
            try {
                final Method jm = osBean.getClass().getMethod("getTotalPhysicalMemorySize");
                AccessController.doPrivileged(
                        new PrivilegedExceptionAction() {
                            public Object run() throws Exception {
                                if (!jm.isAccessible()) {
                                    jm.setAccessible(true);
                                }
                                return null;
                            }
                        });

                top.addProperty("totalPhysicalMemorySize", "" + jm.invoke(osBean));

            }
            catch (Exception ex) {
                logger.log(Level.SEVERE, null, ex);
            }

        }
        RuntimeMXBean rmxb = ManagementFactory.getRuntimeMXBean();
        top.addProperty("startTimeMillis", "" + rmxb.getStartTime());
        top.addProperty("pid", "" + rmxb.getName());
        checkDtrace();
        setDasName();
        top.addProperty("java.vm.name", System.getProperty("java.vm.name"));
        setRestartable();
        reportMessage.append(Strings.get("runtime.info.debug", jpdaEnabled ? "enabled" : "not enabled"));
        report.setMessage(reportMessage.toString());
    }

    private void checkDtrace() {
        try {
            Class.forName("com.sun.tracing.ProviderFactory");
            top.addProperty("dtrace", "true");
        }
        catch (Exception ex) {
            top.addProperty("dtrace", "false");
        }
    }

    private void setDasName() {
        try {
            String name = env.getInstanceRoot().getName();
            top.addProperty("domain_name", name);
        }
        catch (Exception ex) {
            // ignore
        }
    }

    /**
     * March 11 2011 -- See JIRA 16197
     * Say the user started the server with a passwordfile arg.  After they started it
     * they deleted the password file. If we don't do anything special restart-server
     * will take down the server -- but it will not startup again.  The user will have no clue why.
     * We can NOT tell the user directly because the restart server command is asynchronous
     * (@Async annotation).
     * So -- this method was added as a pre-flight check.  The client restart commands
     * should run this command and check the restartable flag to make sure
     * the restart doesn't fail because of a missing password file.
     */
    private void setRestartable() {
        // false positive is MUCH better than false negative.  Err on the side of
        // trying to restart if in doubt.  No harm can result from that.
        restartable = true;
        String passwordFile = null;

        try {
            Properties props = Globals.get(StartupContext.class).getArguments();
            String argsString = props.getProperty("-asadmin-args");

            if (ok(argsString) && argsString.indexOf("--passwordfile") >= 0) {
                String[] args = argsString.split(",,,");

                for (int i = 0; i < args.length; i++) {
                    if (args[i].equals("--passwordfile")) {
                        if ((i + 1) < args.length && ok(args[i + 1])) {
                            passwordFile = args[i + 1];
                        }
                        break;
                    }
                }
            }
        }
        catch (Exception e) {
            // nothing to do, but I'll do this anyway because I'm paranoid
            restartable = true;
        }

        if (ok(passwordFile)) {
            // the --passwordfile is here -- so it had best point to a file that
            // exists and can be read!  In all other cases -- restartable is true
            File pwf = new File(passwordFile);
            restartable = pwf.canRead();
        }
        top.addProperty("restartable", Boolean.toString(restartable));
    }

    private int parsePort(String s) {
        //"-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9009"
        int port = -1;
        String[] ss = s.split(",");

        for (String sub : ss) {
            if (sub.startsWith("address=")) {
                try {
                    port = Integer.parseInt(sub.substring(8));
                }
                catch (Exception e) {
                    port = -1;
                }
                break;
            }
        }
        return port;
    }
    @Inject
    ServerEnvironment env;
    @Inject
    private StartupContext ctx;
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;
    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.SERVER_NAME)
    String target;
    private boolean jpdaEnabled;
    private JavaConfig javaConfig;
    private ActionReport report;
    private ActionReport.MessagePart top;
    private Logger logger;
    private StringBuilder reportMessage = new StringBuilder();

    private boolean restartable;
}
