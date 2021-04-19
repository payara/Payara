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
// Portions Copyright [2016-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.server.logging.commands;

import com.sun.common.util.logging.LoggingConfigFactory;
import com.sun.enterprise.config.serverbeans.Clusters;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.server.logging.LogManagerService;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;

import fish.payara.enterprise.server.logging.PayaraNotificationFileHandler;
import fish.payara.jul.PayaraLogManager;
import fish.payara.jul.cfg.LogProperty;
import fish.payara.jul.formatter.AnsiColorFormatter;
import fish.payara.jul.formatter.AnsiColorFormatter.AnsiColorFormatterProperty;
import fish.payara.jul.formatter.JSONLogFormatter;
import fish.payara.jul.formatter.JSONLogFormatter.JSONFormatterProperty;
import fish.payara.jul.formatter.ODLLogFormatter;
import fish.payara.jul.formatter.ODLLogFormatter.ODLFormatterProperty;
import fish.payara.jul.formatter.OneLineFormatter;
import fish.payara.jul.formatter.OneLineFormatter.OneLineFormatterProperty;
import fish.payara.jul.formatter.PayaraLogFormatter.PayaraLogFormatterProperty;
import fish.payara.jul.formatter.UniformLogFormatter;
import fish.payara.jul.formatter.UniformLogFormatter.UniformFormatterProperty;
import fish.payara.jul.handler.HandlerConfigurationHelper;
import fish.payara.jul.handler.PayaraLogHandler;
import fish.payara.jul.handler.PayaraLogHandlerConfiguration.PayaraLogHandlerProperty;
import fish.payara.jul.handler.SimpleLogHandler;
import fish.payara.jul.handler.SimpleLogHandler.SimpleLogHandlerProperty;
import fish.payara.jul.handler.SyslogHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.validation.ValidationException;

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
    @RestEndpoint(configBean = Domain.class, opType = RestEndpoint.OpType.POST, path = "set-log-attributes", description = "set-log-attributes")})
public class SetLogAttributes implements AdminCommand {

    private static final String LINE_SEP = System.lineSeparator();
    private static final Logger LOG = Logger.getLogger(SetLogAttributes.class.getName());

    private static final Set<String> VALID_ATTRIBUTES;

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

    static {
        // the set of valid attribute keys affects Admin GUI! Try to save values in Logger settings.
        final Set<String> properties = new HashSet<>();
        properties.add(PayaraLogManager.KEY_ROOT_HANDLERS.getPropertyName());
        properties.add(PayaraLogManager.KEY_RELEASE_PARAMETERS_EARLY.getPropertyName());
        properties.add(SyslogHandler.ENABLED.getPropertyFullName(SyslogHandler.class));

        final Class<?>[] formatters = new Class<?>[] {
            UniformLogFormatter.class, ODLLogFormatter.class, OneLineFormatter.class, JSONLogFormatter.class
        };

        // all handlers with their own properties
        Arrays.stream(PayaraLogHandlerProperty.values())
            .forEach(p -> properties.add(p.getPropertyFullName(PayaraLogHandler.class)));
        Arrays.stream(PayaraLogHandlerProperty.values())
            .forEach(p -> properties.add(p.getPropertyFullName(PayaraNotificationFileHandler.class)));
        Arrays.stream(SimpleLogHandlerProperty.values())
            .forEach(p -> properties.add(p.getPropertyFullName(SimpleLogHandler.class)));

        // all formatters and their own properties
        Arrays.stream(UniformFormatterProperty.values())
            .forEach(p -> properties.add(p.getPropertyFullName(UniformLogFormatter.class)));
        Arrays.stream(ODLFormatterProperty.values())
            .forEach(p -> properties.add(p.getPropertyFullName(ODLLogFormatter.class)));
        Arrays.stream(OneLineFormatterProperty.values())
            .forEach(p -> properties.add(p.getPropertyFullName(OneLineFormatter.class)));
        Arrays.stream(JSONFormatterProperty.values())
            .forEach(p -> properties.add(p.getPropertyFullName(JSONLogFormatter.class)));
        for (Class<?> formatter : formatters) {
            Arrays.stream(PayaraLogFormatterProperty.values())
                .forEach(p -> properties.add(p.getPropertyFullName(formatter)));
            if (AnsiColorFormatter.class.isAssignableFrom(formatter)) {
                Arrays.stream(AnsiColorFormatterProperty.values())
                    .forEach(p -> properties.add(p.getPropertyFullName(formatter)));
            }
        }

        // and finally all formatters used with handlers
        final Class<?>[] handlersWithFormatter = new Class<?>[] {
            PayaraLogHandler.class, SimpleLogHandler.class, PayaraNotificationFileHandler.class
        };
        final Set<LogProperty> formatterParameters = new HashSet<>();
        Arrays.stream(UniformFormatterProperty.values()).forEach(formatterParameters::add);
        Arrays.stream(ODLFormatterProperty.values()).forEach(formatterParameters::add);
        Arrays.stream(OneLineFormatterProperty.values()).forEach(formatterParameters::add);
        Arrays.stream(AnsiColorFormatterProperty.values()).forEach(formatterParameters::add);
        Arrays.stream(PayaraLogFormatterProperty.values()).forEach(formatterParameters::add);

        for (Class<?> handler : handlersWithFormatter) {
            properties.add(HandlerConfigurationHelper.FORMATTER.getPropertyFullName(handler));
        }
        for (LogProperty logProperty : formatterParameters) {
            for (Class<?> handler : handlersWithFormatter) {
                String formatterPrefix = HandlerConfigurationHelper.FORMATTER.getPropertyFullName(handler);
                properties.add(logProperty.getPropertyFullName(formatterPrefix));
            }
        }

        properties.add("java.util.logging.FileHandler.count");
        properties.add("java.util.logging.FileHandler.formatter");
        properties.add("java.util.logging.FileHandler.limit");
        properties.add("java.util.logging.FileHandler.pattern");
        properties.add("java.util.logging.ConsoleHandler.encoding");
        properties.add("java.util.logging.ConsoleHandler.filter");
        properties.add("java.util.logging.ConsoleHandler.formatter");
        properties.add("java.util.logging.ConsoleHandler.level");
        properties.add("java.util.logging.SimpleFormatter.format");

        properties.add(JSONLogFormatter.PAYARA_JSONLOGFORMATTER_UNDERSCORE);
        VALID_ATTRIBUTES = Collections.unmodifiableSet(properties);
        LOG.log(Level.FINE, "Acceptable logging properties for the set-log-attribute command (except loggers): {0}",
            VALID_ATTRIBUTES);
    }

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(SetLogLevel.class);

    @Override
    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();
        final StringBuilder sbfSuccessMsg = new StringBuilder(LINE_SEP);
        boolean success = false;
        boolean invalidAttribute = false;
        Map<String, String> m = new HashMap<>();
        try {
            for (final Object key : properties.keySet()) {
                final String att_name = (String) key;
                final String att_value = (String) properties.get(att_name);
                // that is is a valid level
                if (validate) {
                    final boolean vlAttribute = isValid(att_name, att_value, report);
                    if (vlAttribute) {
                        m.put(att_name, att_value);
                        sbfSuccessMsg.append(localStrings.getLocalString(
                            "set.log.attribute.properties",
                            "{0} logging attribute set with value {1}.",
                            att_name, att_value)).append(LINE_SEP);
                    } else {
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


    private boolean isValid(final String att_name, final String att_value, final ActionReport report) {
        for (final String attrName : VALID_ATTRIBUTES) {
            if (attrName.equals(att_name)) {
                try {
                    logManager.validateProp(att_name, att_value);
                    return true;
                } catch (ValidationException e) {
                    // Add in additional error message information if present
                    if (e.getMessage() != null) {
                        report.setMessage(e.getMessage() + "\n");
                        return false;
                    }
                }
            }
        }
        report.appendMessage(localStrings.getLocalString("set.log.attribute.invalid",
            "Invalid logging attribute name {0} or value {1}.", att_name, att_value));
        return false;
    }

}
