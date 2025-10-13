/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2025] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.server.logging.commands;

import com.sun.common.util.logging.LoggingConfigFactory;
import com.sun.enterprise.config.serverbeans.Clusters;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.server.logging.LogManagerService;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.config.UnprocessedConfigListener;
import org.jvnet.hk2.annotations.Service;

/**
 * Set Log Attributes Command
 *
 * Updates one or more loggers' attributes
 * 
 * @author naman mehta
 * @since 3.1
 */
@ExecuteOn({ RuntimeType.DAS, RuntimeType.INSTANCE })
@TargetType({ CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CONFIG })
@CommandLock(CommandLock.LockType.NONE)
@Service(name = "set-log-attributes")
@PerLookup
@I18n("set.log.attributes")
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class, opType = RestEndpoint.OpType.POST, path = "set-log-attributes", description = "set-log-attributes") })
public class SetLogAttributes implements AdminCommand {

    private static final String LINE_SEP = System.lineSeparator();

    @Param(name = "name_value", primary = true, separator = ':')
    Properties properties;

    @Param(optional = true)
    String target = SystemPropertyConstants.DAS_SERVER_NAME;

    @Param(optional = true, defaultValue = "true")
    boolean validate;

    @Inject
    private LoggingConfigFactory loggingConfigFactory;

    @Inject
    private LogManagerService logManager;

    @Inject
    Domain domain;

    @Inject
    Servers servers;

    @Inject
    Clusters clusters;

    @Inject
    UnprocessedConfigListener ucl;

    String[] validAttributes = {"handlers", "handlerServices",
        "java.util.logging.ConsoleHandler.formatter",
        "com.sun.enterprise.server.logging.GFFileHandler.file",
        "com.sun.enterprise.server.logging.GFFileHandler.rotationTimelimitInMinutes",
        "com.sun.enterprise.server.logging.GFFileHandler.flushFrequency",
        "java.util.logging.FileHandler.formatter",
        "com.sun.enterprise.server.logging.GFFileHandler.formatter",
        "java.util.logging.FileHandler.limit",
        "com.sun.enterprise.server.logging.GFFileHandler.logtoFile",
        "com.sun.enterprise.server.logging.GFFileHandler.logtoConsole",
        "com.sun.enterprise.server.logging.GFFileHandler.rotationLimitInBytes",
        "com.sun.enterprise.server.logging.SyslogHandler.host",
        "com.sun.enterprise.server.logging.SyslogHandler.useSystemLogging",
        "com.sun.enterprise.server.logging.GFFileHandler.alarms",
        "java.util.logging.FileHandler.count",
        "com.sun.enterprise.server.logging.GFFileHandler.retainErrorsStasticsForHours",
        "log4j.logger.org.hibernate.validator.util.Version",
        "com.sun.enterprise.server.logging.GFFileHandler.maxHistoryFiles",
        "java.util.logging.FileHandler.pattern",
        "com.sun.enterprise.server.logging.GFFileHandler.rotationOnDateChange",
        "com.sun.enterprise.server.logging.GFFileHandler.logFormatDateFormat",
        "com.sun.enterprise.server.logging.GFFileHandler.excludeFields",
        "com.sun.enterprise.server.logging.GFFileHandler.multiLineMode",
        "com.sun.enterprise.server.logging.GFFileHandler.compressOnRotation",
        "com.sun.enterprise.server.logging.GFFileHandler.logStandardStreams",
        "com.sun.enterprise.server.logging.GFFileHandler.fastLogging",
        "com.sun.enterprise.server.logging.UniformLogFormatter.ansiColor",
        "com.sun.enterprise.server.logging.UniformLogFormatter.infoColor",
        "com.sun.enterprise.server.logging.UniformLogFormatter.warnColor",
        "com.sun.enterprise.server.logging.UniformLogFormatter.severeColor",
        "com.sun.enterprise.server.logging.UniformLogFormatter.loggerColor",
        "com.sun.enterprise.server.logging.ODLLogFormatter.ansiColor",
        "com.sun.enterprise.server.logging.ODLLogFormatter.loggerColor",
        "com.sun.enterprise.server.logging.ODLLogFormatter.infoColor",
        "com.sun.enterprise.server.logging.ODLLogFormatter.warnColor",
        "com.sun.enterprise.server.logging.ODLLogFormatter.severeColor",
        "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.file",
        "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.logtoFile",
        "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.formatter",
        "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.rotationTimelimitInMinutes",
        "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.rotationOnDateChange",
        "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.rotationLimitInBytes",
        "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.maxHistoryFiles",
        "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.compressOnRotation",
        "fish.payara.deprecated.jsonlogformatter.underscoreprefix"};

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(SetLogLevel.class);

    @Override
    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();
        StringBuilder sbfSuccessMsg = new StringBuilder(LINE_SEP);
        boolean success = false;
        boolean invalidAttribute = false;

        Map<String, String> m = new HashMap<String, String>();
        try {
            for (final Object key : properties.keySet()) {
                final String att_name = (String) key;
                final String att_value = (String) properties.get(att_name);
                // that is is a valid level
                if (validate) {
                    boolean vlAttribute = false;
                    for (String attrName : validAttributes) {
                        if (attrName.equals(att_name)) {
                            try {
                                logManager.validateProp(att_name, att_value);
                            } catch (ValidationException e) {
                                // Add in additional error message information if present
                                if (e.getMessage() != null) {
                                    report.setMessage(e.getMessage() + "\n");
                                }

                                break;
                            }
                            m.put(att_name, att_value);
                            vlAttribute = true;
                            sbfSuccessMsg.append(localStrings.getLocalString(
                                    "set.log.attribute.properties",
                                    "{0} logging attribute set with value {1}.",
                                    att_name, att_value)).append(LINE_SEP);
                        }
                    }

                    if (!vlAttribute) {
                        report.appendMessage(localStrings.getLocalString("set.log.attribute.invalid",
                                "Invalid logging attribute name {0} or value {1}.", att_name, att_value));
                        invalidAttribute = true;
                        break;
                    }
                } else {
                    m.put(att_name, att_value);
                    sbfSuccessMsg.append(localStrings.getLocalString(
                            "set.log.attribute.properties",
                            "{0} logging attribute set with value {1}.",
                            att_name, att_value)).append(LINE_SEP);
                }
            }

            if (invalidAttribute) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            TargetInfo targetInfo = new TargetInfo(domain, target);
            String targetConfigName = targetInfo.getConfigName();
            boolean isDas = targetInfo.isDas();

            loggingConfigFactory.provide(targetConfigName).setLoggingProperties(m);
            success = true;

            if (success) {
                String effectiveTarget = (isDas ? SystemPropertyConstants.DAS_SERVER_NAME : targetConfigName);
                sbfSuccessMsg.append(localStrings.getLocalString(
                        "set.log.attribute.success",
                        "These logging attributes are set for {0}.", effectiveTarget)).append(LINE_SEP);
                report.setMessage(sbfSuccessMsg.toString());
                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            } else {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                String msg = localStrings.getLocalString("invalid.target.sys.props",
                        "Invalid target: {0}. Valid default target is a server named ''server'' (default) or cluster name.", target);
                report.setMessage(msg);
                return;
            }

        } catch (IOException e) {
            report.setMessage(localStrings.getLocalString("set.log.attribute.failed",
                    "Could not set logging attributes for {0}.", target));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }

}
