/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2024 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

package fish.payara.enterprise.admin.commands;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.JavaConfig;
import com.sun.enterprise.config.serverbeans.JvmOptionBag;
import com.sun.enterprise.universal.xml.MiniXmlParser;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.v3.admin.commands.CLIUtil;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service(name="create-jvm-option")
@PerLookup
@I18n("create.jvm.option")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
@UnknownOptionsAreOperands()
public final class CreateJvmOption implements AdminCommand, AdminCommandSecurity.Preauthorization {

    @Param(name="target", optional=true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;

    @Param(name="assignment", primary=true)
    String jvmAssignment;

    @Param(name="min-jvm", optional = true)
    String minJVM;
    @Param(name="max-jvm", optional = true)
    String maxJVM;

    @Inject
    Target targetService;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    private static final StringManager lsm = StringManager.getManager(CreateJvmOption.class);

    @AccessRequired.To("update")
    private JavaConfig javaConfig;

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        config = CLIUtil.updateConfigIfNeeded(config, targetService, target);
        javaConfig = config.getJavaConfig();
        return true;
    }


    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        try {
            JvmOptionBag bag = javaConfig;


            ActionReport.MessagePart part = report.getTopMessagePart().addChild();
            String validOption = (new MiniXmlParser.JvmOption(jvmAssignment)).option;
            validate(bag, validOption, report);
            validateHeapSize(bag, validOption, report);
            add(bag, jvmAssignment, part);
        }
        catch (IllegalArgumentException iae) {
            report.setMessage(iae.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        catch (Exception e) {
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

    private void validateHeapSize (JvmOptionBag bag, String option, ActionReport report) {
        validateMax(bag, option, report);
        validateMin(bag, option, report);
    }

    private void validateMax (JvmOptionBag bag, String option, ActionReport report) throws IllegalArgumentException {
        if (!option.startsWith("-Xmx")) {
            return;
        }

        //now, opt is something like -Xmx512m or -Xmx2g, or -Xmx=12 i.e. it may contain illegal characters
        Pattern regex = Pattern.compile("-Xmx((\\d)+[m|g|k|M|G|K]?)+");
        boolean matches = regex.matcher(option).matches();
        if (!matches) {
            String msg = lsm.getString("soft.invalid.xmx", option);
            report.getTopMessagePart().addChild().setMessage(msg);
            throw new IllegalArgumentException();
        }


        String existingXmx = bag.getStartingWith("-Xmx");
        if (existingXmx != null) {
            //maybe a different Xmx was given
            String msg = lsm.getString("soft.xmx.exists.singular", existingXmx, option);
            report.getTopMessagePart().addChild().setMessage(msg);
        }

        String existingXms = bag.getStartingWith("-Xms");
        if (existingXms != null) {
            int xmsInConfig = JvmOptionBag.Duck.toMeg(existingXms, "-Xms");
            int xmxGiven    = JvmOptionBag.Duck.toMeg(option, "-Xmx");
            if (xmsInConfig > xmxGiven) { //i.e. domain.xml contains -Xms1g and you ask -Xmx512m to be set
                String msg = lsm.getString("soft.xmx.smaller.than.xms", xmxGiven + " MB", xmsInConfig + " MB");
                report.getTopMessagePart().addChild().setMessage(msg);
                throw new IllegalArgumentException();
            }
        }
    }

    private void validateMin (JvmOptionBag bag, String option, ActionReport report) throws IllegalArgumentException {
        if (!option.startsWith("-Xms")) {
            return;
        }

        //now, opt is something like -Xms512m or -Xms2g, or -Xms=12 i.e. it may contain illegal characters
        Pattern regex = Pattern.compile("-Xms((\\d)+[m|g|k|M|G|K]?)+");
        boolean matches = regex.matcher(option).matches();
        if (!matches) {
            String msg = lsm.getString("soft.invalid.xms", option);
            report.getTopMessagePart().addChild().setMessage(msg);
            throw new IllegalArgumentException();
        }

        String existingXms = bag.getStartingWith("-Xms");
        if (existingXms != null) {
            String msg = lsm.getString("soft.xms.exists.singular", existingXms, option);
            report.getTopMessagePart().addChild().setMessage(msg);
        }
        String existingXmx = bag.getStartingWith("-Xmx");
        if (existingXmx != null) {
            int xmxInConfig = JvmOptionBag.Duck.toMeg(existingXmx, "-Xmx");
            int xmsGiven = JvmOptionBag.Duck.toMeg(option, "-Xms");
            if (xmsGiven > xmxInConfig) { //i.e. domain.xml contains -Xms1g and you ask -Xmx512m to be set
                String msg = lsm.getString("soft.xms.larger.than.xmx", xmsGiven + " MB", xmxInConfig + " MB");
                report.getTopMessagePart().addChild().setMessage(msg);
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Validates a JVM option that it is a valid option.
     * <p>
     * Valid options must either start with a {@code -} or <code>${ENV=}</code>
     * @param bag Bag of JVM options that already exist
     * @param option Option being added.
     * @param report ignored
     * @throws IllegalArgumentException If an invalid options is given
     */
    private void validate(JvmOptionBag bag, String option, ActionReport report) throws IllegalArgumentException {
        if (!option.startsWith("-") && !option.startsWith("${ENV=")) {
            String msg = lsm.getString("joe.invalid.start", option);
            throw new IllegalArgumentException(msg);
        }
        if (bag.contains(option)) {
            // setting an option that already exists is considered an error
            String msg = lsm.getString("joe.exists", option);
            throw new IllegalArgumentException(msg);
        }
    }

    private void add (final JvmOptionBag bag, final String option, final ActionReport.MessagePart part) throws Exception {
        SingleConfigCode<JvmOptionBag> scc = bag1 -> {
            List<String> unversionedCurrentOptions = bag1.getJvmOptions();

            if (!unversionedCurrentOptions.contains(new MiniXmlParser.JvmOption(option).option)) {
                List<String> jvmopts = new ArrayList<>(bag1.getJvmRawOptions());
                if (option.startsWith("-Xms") && bag1.getStartingWith("-Xms") != null) {
                    jvmopts.removeIf(entry -> entry.startsWith("-Xms"));
                }
                else if (option.startsWith("-Xmx") && bag1.getStartingWith("-Xmx") != null) {
                    jvmopts.removeIf(entry -> entry.startsWith("-Xmx"));
                }

                jvmopts.add(
                    MiniXmlParser.JvmOption.hasVersionPattern(option) ?
                        new MiniXmlParser.JvmOption(option).toString() :
                        new MiniXmlParser.JvmOption(option, minJVM, maxJVM).toString()
                );
                bag1.setJvmOptions(jvmopts);
                part.setMessage(lsm.getString("jvm.option.created", option));
            }
            else {
                part.setMessage(lsm.getString("no.option.created"));
            }
            return true;
        };
        ConfigSupport.apply(scc, bag);
    }
}
