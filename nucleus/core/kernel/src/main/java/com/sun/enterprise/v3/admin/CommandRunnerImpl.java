/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2017-2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.v3.admin;

import com.sun.enterprise.admin.event.AdminCommandEventBrokerImpl;
import com.sun.enterprise.admin.util.CachedCommandModel;
import com.sun.enterprise.admin.util.ClusterOperationUtil;
import com.sun.enterprise.admin.util.CommandSecurityChecker;
import com.sun.enterprise.admin.util.InstanceStateService;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.universal.collections.ManifestUtils;
import com.sun.enterprise.universal.glassfish.AdminCommandResponse;
import com.sun.enterprise.util.AnnotationUtil;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.admin.report.XMLContentActionReporter;
import fish.payara.api.admin.config.NameGenerator;
import org.glassfish.admin.payload.PayloadFilesManager;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.Payload;
import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.admin.AdminCommandEventBroker.AdminCommandListener;
import org.glassfish.api.admin.SupplementalCommandExecutor.SupplementalCommand;
import org.glassfish.api.logging.LogHelper;
import org.glassfish.common.util.admin.CommandModelImpl;
import org.glassfish.common.util.admin.ManPageFinder;
import org.glassfish.common.util.admin.MapInjectionResolver;
import org.glassfish.common.util.admin.UnacceptableValueException;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.GenericCrudCommand;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.api.UndoableCommand;
import org.glassfish.internal.deployment.DeploymentTargetResolver;
import org.glassfish.kernel.KernelLoggerInfo;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.MultiMap;
import org.jvnet.hk2.config.InjectionManager;
import org.jvnet.hk2.config.InjectionResolver;
import org.jvnet.hk2.config.MessageInterpolatorImpl;
import org.jvnet.hk2.config.UnsatisfiedDependencyException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Scope;
import javax.inject.Singleton;
import javax.security.auth.Subject;
import javax.validation.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates the logic needed to execute a server-side command (for example,
 * a descendant of AdminCommand) including injection of argument values into the
 * command.
 *
 * @author dochez
 * @author tjquinn
 * @author Bill Shannon
 */
@Service
public class CommandRunnerImpl implements CommandRunner {

    private static final Logger logger = KernelLoggerInfo.getLogger();
    // This is used only for backword compatibility with old behavior
    private static final String OLD_PASSWORD_PARAM_PREFIX = "AS_ADMIN_";

    private static final InjectionManager injectionMgr = new InjectionManager();

    @Inject
    private ServiceLocator habitat;

    @Inject
    private ServerContext sc;

    @Inject
    private Domain domain;

    @Inject
    private ServerEnvironment serverEnv;

    @Inject
    private ProcessEnvironment processEnv;

    @Inject
    private InstanceStateService state;

    @Inject
    private AdminCommandLock adminLock;

    @Inject @Named("SupplementalCommandExecutorImpl")
    SupplementalCommandExecutor supplementalExecutor;

    private final Map<NameCommandClassPair, String> commandModelEtagMap = new IdentityHashMap<NameCommandClassPair, String>();

    @Inject
    private CommandSecurityChecker commandSecurityChecker;

    private static final LocalStringManagerImpl adminStrings = new LocalStringManagerImpl(CommandRunnerImpl.class);
    private static volatile Validator beanValidator;

    /**
     * Returns an initialized ActionReport instance for the passed type or
     * null if it cannot be found.
     *
     * @param name action report type name
     * @return uninitialized action report or null
     */
    @Override
    public ActionReport getActionReport(String name) {
        return habitat.getService(ActionReport.class, name);
    }

    /**
     * Returns the command model for a command name.
     *
     * @param commandName command name
     * @param logger logger to log any error messages
     * @return model for this command (list of parameters,etc...),
     *          or null if command is not found
     */
    @Override
    public CommandModel getModel(String commandName, Logger logger) {
        return getModel(null, commandName, logger);
    }

    /**
     * Returns the command model for a command name.
     *
     * @param commandName command name
     * @param logger logger to log any error messages
     * @return model for this command (list of parameters,etc...),
     *          or null if command is not found
     */
    @Override
    public CommandModel getModel(String scope, String commandName, Logger logger) {
        AdminCommand command;
        try {
            String commandServiceName = (scope != null) ? scope + commandName : commandName;
            command = habitat.getService(AdminCommand.class, commandServiceName);
        } catch (MultiException e) {
            LogHelper.log(logger, Level.SEVERE, KernelLoggerInfo.cantInstantiateCommand,
                    e, commandName);
            return null;
        }
        return command == null ? null : getModel(command);
    }

    @Override
    public boolean validateCommandModelETag(AdminCommand command, String eTag) {
        if (command == null) {
            return true; //Everithing is ok for unexisting command
        }
        if (eTag == null || eTag.isEmpty()) {
            return false;
        }
        CommandModel model = getModel(command);
        return validateCommandModelETag(model, eTag);
    }

    @Override
    public boolean validateCommandModelETag(CommandModel model, String eTag) {
        if (model == null) {
            return true; //Unexisting model => it is ok (but weard in fact)
        }
        if (eTag == null || eTag.isEmpty()) {
            return false;
        }
        String actualETag = CachedCommandModel.computeETag(model);
        return eTag.equals(actualETag);
    }

    /**
     * Obtain and return the command implementation defined by
     * the passed commandName for the null scope.
     *
     * @param commandName command name as typed by users
     * @param report report used to communicate command status back to the user
     * @param logger logger to log
     * @return command registered under commandName or null if not found
     */
    @Override
    public AdminCommand getCommand(String commandName,
            ActionReport report, Logger logger) {
        return getCommand(null, commandName, report, logger);
    }

    private static Class<? extends Annotation> getScope(Class<?> onMe) {
        for (Annotation anno : onMe.getAnnotations()) {
            if (anno.annotationType().isAnnotationPresent(Scope.class)) {
                return anno.annotationType();
            }

        }

        return null;
    }

    /**
     * Obtain and return the command implementation defined by
     * the passed commandName.
     *
     * @param commandName command name as typed by users
     * @param report report used to communicate command status back to the user
     * @param logger logger to log
     * @return command registered under commandName or null if not found
     */
    @Override
    public AdminCommand getCommand(String scope, String commandName,
            ActionReport report, Logger logger) {

        AdminCommand command = null;
        String commandServiceName = (scope != null) ? scope + commandName : commandName;

        try {
            command = habitat.getService(AdminCommand.class, commandServiceName);
        } catch (MultiException e) {
            report.setFailureCause(e);
        }
        if (command == null) {
            String msg;

            if (!ok(commandName)) {
                msg = adminStrings.getLocalString("adapter.command.nocommand",
                        "No command was specified.");
            } else {
                // this means either a non-existent command or
                // an ill-formed command
                if (habitat.getServiceHandle(AdminCommand.class, commandServiceName)
                        == null) // somehow it's in habitat
                {
                    msg = adminStrings.getLocalString("adapter.command.notfound", "Command {0} not found", commandName);
                } else {
                    msg = adminStrings.getLocalString("adapter.command.notcreated",
                            "Implementation for the command {0} exists in "
                            + "the system, but it has some errors, "
                            + "check server.log for details", commandName);
                }
            }
            report.setMessage(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            KernelLoggerInfo.getLogger().fine(msg);
            return null;
        }

        Class<? extends Annotation> myScope = getScope(command.getClass());
        if (myScope == null) {
            String msg = adminStrings.getLocalString("adapter.command.noscope",
                    "Implementation for the command {0} exists in the "
                    + "system,\nbut it has no @Scoped annotation", commandName);
            report.setMessage(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            KernelLoggerInfo.getLogger().fine(msg);
            command = null;
        } else if (Singleton.class.equals(myScope)) {
            // check that there are no parameters for this command
            CommandModel model = getModel(command);
            if (model.getParameters().size() > 0) {
                String msg =
                        adminStrings.getLocalString("adapter.command.hasparams",
                        "Implementation for the command {0} exists in the "
                        + "system,\nbut it's a singleton that also has "
                        + "parameters", commandName);
                report.setMessage(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                KernelLoggerInfo.getLogger().fine(msg);
                command = null;
            }
        }

        return command;
    }

    @Override
    public CommandInvocation getCommandInvocation(String name, ActionReport report, Subject subject) {
        return getCommandInvocation(name,report,subject,false);
    }

    @Override
    public CommandInvocation getCommandInvocation(String scope, String name, ActionReport report, Subject subject) {
        return getCommandInvocation(scope,name,report,subject,false);
    }

    /**
     * Obtain a new command invocation object for the null scope.
     * Command invocations can be configured and used
     * to trigger a command execution.
     *
     * @param name name of the requested command to invoke
     * @param report where to place the status of the command execution
     * @param subject the Subject under which to execute the command
     * @return a new command invocation for that command name
     */
    @Override
    public CommandInvocation getCommandInvocation(String name,
            ActionReport report, Subject subject,boolean isNotify) {
        return getCommandInvocation(null, name, report, subject,false);
    }

    /**
     * Obtain a new command invocation object.
     * Command invocations can be configured and used
     * to trigger a command execution.
     *
     * @param scope the scope (or name space) for the command
     * @param name name of the requested command to invoke
     * @param report where to place the status of the command execution
     * @param subject the Subject under which to execute the command
     * @param isNotify  Should notification be enabled
     * @return a new command invocation for that command name
     */
    @Override
    public CommandInvocation getCommandInvocation(String scope, String name,
            ActionReport report, Subject subject, boolean isNotify) {
        return new ExecutionContext(scope, name, report, subject, isNotify);
    }

    public static boolean injectParameters(final CommandModel model, final Object injectionTarget,
            final InjectionResolver<Param> injector,
            final ActionReport report) {

        if (injectionTarget instanceof GenericCrudCommand) {
            GenericCrudCommand c = GenericCrudCommand.class.cast(injectionTarget);
            c.setInjectionResolver(injector);
        }

        // inject
        try {
            injectionMgr.inject(injectionTarget, injector);
        } catch (UnsatisfiedDependencyException e) {
            Param param = e.getAnnotation(Param.class);
            CommandModel.ParamModel paramModel = null;
            for (CommandModel.ParamModel pModel : model.getParameters()) {
                if (pModel.getParam().equals(param)) {
                    paramModel = pModel;
                    break;
                }
            }
            String errorMsg;
            final String usage = getUsageText(model);
            if (paramModel != null) {
                String paramName = paramModel.getName();
                String paramDesc = paramModel.getLocalizedDescription();

                if (param.primary()) {
                    errorMsg = adminStrings.getLocalString("commandrunner.operand.required",
                            "Operand required.");
                } else if (param.password()) {
                    errorMsg = adminStrings.getLocalString("adapter.param.missing.passwordfile",
                            "{0} command requires the passwordfile "
                            + "parameter containing {1} entry.",
                            model.getCommandName(), paramName);
                } else if (paramDesc != null) {
                    errorMsg = adminStrings.getLocalString("admin.param.missing",
                            "{0} command requires the {1} parameter ({2})",
                            model.getCommandName(), paramName, paramDesc);

                } else {
                    errorMsg = adminStrings.getLocalString("admin.param.missing.nodesc",
                            "{0} command requires the {1} parameter",
                            model.getCommandName(), paramName);
                }
            } else {
                errorMsg = adminStrings.getLocalString("admin.param.missing.nofound",
                        "Cannot find {1} in {0} command model, file a bug",
                        model.getCommandName(), e.getUnsatisfiedName());
            }
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(errorMsg);
            report.setFailureCause(e);
            ActionReport.MessagePart childPart =
                    report.getTopMessagePart().addChild();
            childPart.setMessage(usage);
            return false;
        }
        catch (MultiException e) {
            // If the cause is UnacceptableValueException -- we want the message
            // from it.  It is wrapped with a less useful Exception.

            Exception exception = null;
            for (Throwable th : e.getErrors()) {
                Throwable cause = th;
                while (cause != null) {
                    if ((cause instanceof UnacceptableValueException) ||
                            (cause instanceof IllegalArgumentException)) {
                        exception = (Exception) th;
                        break;
                    }

                    cause = cause.getCause();
                }
            }

            if (exception == null) {
                // Not an UnacceptableValueException or IllegalArgumentException
                exception = e;
            }

            logger.log(Level.SEVERE, KernelLoggerInfo.invocationException, exception);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(exception.getMessage());
            report.setFailureCause(exception);
            ActionReport.MessagePart childPart =
                    report.getTopMessagePart().addChild();
            childPart.setMessage(getUsageText(model));
            return false;
        }

        checkAgainstBeanConstraints(injectionTarget, model.getCommandName());
        return true;
    }

    private static synchronized void initBeanValidator() {
        if (beanValidator != null) {
            return;
        }
        ClassLoader cl = System.getSecurityManager() == null ?
                Thread.currentThread().getContextClassLoader():
                AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                    @Override
                    public ClassLoader run() {
                        return Thread.currentThread().getContextClassLoader();
                    }
                });
        try {
            Thread.currentThread().setContextClassLoader(Validation.class.getClassLoader());
            Configuration<?> configuration = Validation.byDefaultProvider().providerResolver(
                    new HibernateValidationProviderResolver()
            ).configure();
            ValidatorFactory validatorFactory = configuration.buildValidatorFactory();
            ValidatorContext validatorContext = validatorFactory.usingContext();
            validatorContext.messageInterpolator(new MessageInterpolatorImpl());
            beanValidator = validatorContext.getValidator();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    private static void checkAgainstBeanConstraints(Object component, String cname) {
        initBeanValidator();

        Set<ConstraintViolation<Object>> constraintViolations = beanValidator.validate(component);
        if (constraintViolations == null || constraintViolations.isEmpty()) {
            return;
        }
        StringBuilder msg = new StringBuilder(adminStrings.getLocalString("commandrunner.unacceptableBV",
                "Parameters for command {0} violate the following constraints: ",
                cname));
        boolean addc = false;
        String violationMsg = adminStrings.getLocalString("commandrunner.unacceptableBV.reason",
                "on parameter [ {1} ] violation reason [ {0} ]");
        for (ConstraintViolation cv : constraintViolations) {
            if (addc) {
                msg.append(", ");
            }
            msg.append(MessageFormat.format(violationMsg, cv.getMessage(), cv.getPropertyPath()));
            addc = true;
        }
        throw new UnacceptableValueException(msg.toString());
    }

    /**
     * Executes the provided command object.
     *
     * @param model model of the command (used for logging and reporting)
     * @param command the command service to execute
     * @param context the AdminCommandcontext that has the payload and report
     */
    private ActionReport doCommand(
            final CommandModel model,
            final AdminCommand command,
            final AdminCommandContext context,
            final CommandRunnerProgressHelper progressHelper) {

        ActionReport report = context.getActionReport();
        report.setActionDescription(model.getCommandName() + " AdminCommand");

        // We need to set context CL to common CL before executing
        // the command. See issue #5596
        final Thread thread = Thread.currentThread();
        final ClassLoader origCL = thread.getContextClassLoader();
        final ClassLoader ccl = sc.getCommonClassLoader();

        AdminCommand wrappedCommand = new WrappedAdminCommand(command) {
            @Override
            public void execute(final AdminCommandContext context) {
                try {
                    if (origCL != ccl) {
                        thread.setContextClassLoader(ccl);
                    }
                    /*
                     * Execute the command in the security context of the
                     * previously-authenticated subject.
                     */
                    Subject.doAs(context.getSubject(),
                            new PrivilegedAction<Void> () {

                        @Override
                        public Void run() {
                            command.execute(context);
                            return null;
                        }

                    });
                } finally {
                    if (origCL != ccl) {
                        thread.setContextClassLoader(origCL);
                    }
                }
            }
        };

        // look for other wrappers using CommandAspect annotation
        final AdminCommand otherWrappedCommand = CommandSupport.createWrappers(habitat, model, wrappedCommand, report);

        try {
            Subject.doAs(context.getSubject(),
                            new PrivilegedAction<Void> () {

                        @Override
                        public Void run() {
                            try {
                                if (origCL != ccl) {
                                    thread.setContextClassLoader(ccl);
                                }
                                otherWrappedCommand.execute(progressHelper.wrapContext4MainCommand(context));
                                return null;
                            } finally {
                                if (origCL != ccl) {
                                    thread.setContextClassLoader(origCL);
                                }
                            }
                        }

                    });

        } catch (Throwable e) {
            logger.log(Level.SEVERE, KernelLoggerInfo.invocationException, e);
            report.setMessage(e.toString());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            }

        return context.getActionReport();
    }

    /**
     * Get the usage-text of the command.
     * Check if <command-name>.usagetext is defined in LocalString.properties.
     * If defined, then use the usagetext from LocalString.properties else
     * generate the usagetext from Param annotations in the command class.
     *
     * @param model command model
     * @return usagetext
     */
    static String getUsageText(CommandModel model) {
        StringBuilder usageText = new StringBuilder();

        String usage;
        if (ok(usage = model.getUsageText())) {
            usageText.append(
                    adminStrings.getLocalString("adapter.usage", "Usage: "));
            usageText.append(usage);
            return usageText.toString();
        } else {
            return generateUsageText(model);
        }
    }

    /**
     * Generate the usage-text from the annotated Param in the command class.
     *
     * @param model command model
     * @return generated usagetext
     */
    private static String generateUsageText(CommandModel model) {
        StringBuilder usageText = new StringBuilder();
        usageText.append(
                adminStrings.getLocalString("adapter.usage", "Usage: "));
        usageText.append(model.getCommandName());
        usageText.append(" ");
        StringBuilder operand = new StringBuilder();
        for (CommandModel.ParamModel pModel : model.getParameters()) {
            final Param param = pModel.getParam();
            final String paramName =
                    pModel.getName().toLowerCase(Locale.ENGLISH);
            // skip "hidden" options
            if (paramName.startsWith("_")) {
                continue;
            }
            // do not want to display password as an option
            if (param.password()) {
                continue;
            }
            // do not want to display obsolete options
            if (param.obsolete()) {
                continue;
            }
            final boolean optional = param.optional();
            final Class<?> ftype = pModel.getType();
            Object fvalue = null;
            String fvalueString = null;
            try {
                fvalue = param.defaultValue();
                if (fvalue != null) {
                    fvalueString = fvalue.toString();
                }
            } catch (Exception e) {
                // just leave it as null...
            }
            // this is a param.
            if (param.primary()) {
                if (optional) {
                    operand.append("[").append(paramName).append("] ");
                } else {
                    operand.append(paramName).append(" ");
                }
                continue;
            }

            if (optional) {
                usageText.append("[");
            }

            usageText.append("--").append(paramName);
            if (ok(param.defaultValue())) {
                usageText.append("=").append(param.defaultValue());
            } else if (ftype.isAssignableFrom(String.class)) {
                // check if there is a default value assigned
                if (ok(fvalueString)) {
                    usageText.append("=").append(fvalueString);
                } else {
                    usageText.append("=").append(paramName);
                }
            } else if (ftype.isAssignableFrom(Boolean.class)) {
                // note: There is no defaultValue for this param.  It might
                // hava  value -- but we don't care -- it isn't an official
                // default value.
                usageText.append("=").append("true|false");
            } else {
                usageText.append("=").append(paramName);
            }

            if (optional) {
                usageText.append("] ");
            } else {
                usageText.append(" ");
            }
        }
        usageText.append(operand);
        return usageText.toString();
    }

    @Override
    public BufferedReader getHelp(CommandModel model) throws CommandNotFoundException {
        BufferedReader manPage = getManPage(model.getCommandName(), model);
        if (manPage != null) {
            return manPage;
        } else {
            StringBuilder hlp = new StringBuilder(256);
            StringBuilder part = new StringBuilder(64);
            hlp.append("NAME").append(ManifestUtils.EOL);
            part.append(model.getCommandName());
            String description = model.getLocalizedDescription();
            if (ok(description)) {
                part.append(" - ").append(model.getLocalizedDescription());
            }
            hlp.append(formatGeneratedManPagePart(part.toString(), 5, 65)).append(ManifestUtils.EOL);
            //Usage
            hlp.append(ManifestUtils.EOL).append("SYNOPSIS").append(ManifestUtils.EOL);
            hlp.append(formatGeneratedManPagePart(getUsageText(model), 5, 65));
            //Options
            hlp.append(ManifestUtils.EOL).append(ManifestUtils.EOL);
            hlp.append("OPTIONS").append(ManifestUtils.EOL);
            CommandModel.ParamModel operand = null;
            for (CommandModel.ParamModel paramModel : model.getParameters()) {
                Param param = paramModel.getParam();
                if (param == null || paramModel.getName().startsWith("_") ||
                        param.password() || param.obsolete()) {
                    continue;
                }
                if (param.primary()) {
                    operand = paramModel;
                    continue;
                }
                hlp.append("     --").append(paramModel.getName().toLowerCase(Locale.ENGLISH));
                hlp.append(ManifestUtils.EOL);
                if (ok(param.shortName())) {
                    hlp.append("      -").append(param.shortName().toLowerCase(Locale.ENGLISH));
                    hlp.append(ManifestUtils.EOL);
                }
                String descr = paramModel.getLocalizedDescription();
                if (ok(descr)) {
                    hlp.append(formatGeneratedManPagePart(descr, 9, 65));
                }
                hlp.append(ManifestUtils.EOL);
            }
            //Operand
            if (operand != null) {
                hlp.append("OPERANDS").append(ManifestUtils.EOL);
                hlp.append("     ").append(operand.getName().toLowerCase(Locale.ENGLISH));
                hlp.append(ManifestUtils.EOL);
                String descr = operand.getLocalizedDescription();
                if (ok(descr)) {
                    hlp.append(formatGeneratedManPagePart(descr, 9, 65));
                }
            }
            return new BufferedReader(new StringReader(hlp.toString()));
        }
    }

    private String formatGeneratedManPagePart(String part, int prefix, int lineLength) {
        if (part == null) {
            return null;
        }
        if (prefix < 0) {
            prefix = 0;
        }
        //Prepare prefix
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < prefix; i++) {
            sb.append(' ');
        }
        String prfx = sb.toString();
        StringBuilder result = new StringBuilder(part.length() + prefix + 16);
        boolean newLine = true;
        boolean lastWasCR = false;
        int counter = 0;
        for (int i = 0; i < part.length(); i++) {
            boolean addPrefix = newLine;
            char ch = part.charAt(i);
            switch (ch) {
                case '\n':
                    if (!lastWasCR) {
                        newLine = true;
                    } else {
                        lastWasCR = false;
                    }
                    counter = 0;
                    break;
                case '\r':
                    newLine = true;
                    lastWasCR = true;
                    counter = 0;
                    break;
                default:
                    newLine = false;
                    lastWasCR = false;
                    counter++;
            }
            if (addPrefix && !newLine) {
                result.append(prfx);
                counter += prefix;
            }
            result.append(ch);
            if (lineLength > 0 && counter >= lineLength && !newLine) {
                newLine = true;
                result.append(ManifestUtils.EOL);
                counter = 0;
            }
        }
        return result.toString();
    }

    public void getHelp(AdminCommand command, ActionReport report) {

        CommandModel model = getModel(command);
        report.setActionDescription(model.getCommandName() + " help");

        // XXX - this is a hack for now.  if the request mapped to an
        // XMLContentActionReporter, that means we want the command metadata.
        if (report instanceof XMLContentActionReporter) {
            getMetadata(command, model, report);
        } else {
            report.setMessage(model.getCommandName() + " - "
                    + model.getLocalizedDescription());
            report.getTopMessagePart().addProperty("SYNOPSIS",
                    encodeManPage(new BufferedReader(new StringReader(
                    getUsageText(model)))));
            for (CommandModel.ParamModel param : model.getParameters()) {
                addParamUsage(report, param);
            }
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        }
    }

    /**
     * Return the metadata for the command.  We translate the parameter
     * and operand information to parts and properties of the ActionReport,
     * which will be translated to XML elements and attributes by the
     * XMLContentActionReporter.
     *
     * @param command the command
     * @param model the CommandModel describing the command
     * @param report	the (assumed to be) XMLContentActionReporter
     */
    private void getMetadata(AdminCommand command, CommandModel model,
            ActionReport report) {
        ActionReport.MessagePart top = report.getTopMessagePart();
        ActionReport.MessagePart cmd = top.addChild();
        // <command name="name">
        cmd.setChildrenType("command");
        cmd.addProperty("name", model.getCommandName());
        if (model.unknownOptionsAreOperands()) {
            cmd.addProperty("unknown-options-are-operands", "true");
        }
        String usage = model.getUsageText();
        if (ok(usage)) {
            cmd.addProperty("usage", usage);
        }
        CommandModel.ParamModel primary = null;
        // for each parameter add
        // <option name="name" type="type" short="s" default="default"
        //   acceptable-values="list"/>
        for (CommandModel.ParamModel p : model.getParameters()) {
            Param param = p.getParam();
            if (param.primary()) {
                primary = p;
                continue;
            }
            ActionReport.MessagePart ppart = cmd.addChild();
            ppart.setChildrenType("option");
            ppart.addProperty("name", p.getName());
            ppart.addProperty("type", typeOf(p));
            ppart.addProperty("optional", Boolean.toString(param.optional()));
            if (param.obsolete()) // don't include it if it's false
            {
                ppart.addProperty("obsolete", "true");
            }
            String paramDesc = p.getLocalizedDescription();
            if (ok(paramDesc)) {
                ppart.addProperty("description", paramDesc);
            }
            if (ok(param.shortName())) {
                ppart.addProperty("short", param.shortName());
            }
            if (ok(param.defaultValue())) {
                ppart.addProperty("default", param.defaultValue());
            }
            if (ok(param.acceptableValues())) {
                ppart.addProperty("acceptable-values", param.acceptableValues());
            }
            if (ok(param.alias())) {
                ppart.addProperty("alias", param.alias());
            }
        }

        // are operands allowed?
        if (primary != null) {
            // for the operand(s), add
            // <operand type="type" min="0/1" max="1"/>
            ActionReport.MessagePart primpart = cmd.addChild();
            primpart.setChildrenType("operand");
            primpart.addProperty("name", primary.getName());
            primpart.addProperty("type", typeOf(primary));
            primpart.addProperty("min",
                    primary.getParam().optional() ? "0" : "1");
            primpart.addProperty("max", primary.getParam().multiple()
                    ? "unlimited" : "1");
            String desc = primary.getLocalizedDescription();
            if (ok(desc)) {
                primpart.addProperty("description", desc);
            }
        }
    }

    /**
     * Map a Java type to one of the types supported by the asadmin client.
     * Currently supported types are BOOLEAN, FILE, PROPERTIES, PASSWORD, and
     * STRING.  (All of which should be defined constants on some class.)
     *
     * @param p the Java type
     * @return	the string representation of the asadmin type
     */
    private static String typeOf(CommandModel.ParamModel p) {
        Class t = p.getType();
        if (t == Boolean.class || t == boolean.class) {
            return "BOOLEAN";
        } else if (t == File.class || t == File[].class) {
            return "FILE";
        } else if (t == Properties.class) { // XXX - allow subclass?
            return "PROPERTIES";
        } else if (p.getParam().password()) {
            return "PASSWORD";
        } else {
            return "STRING";
        }
    }

    /**
     * Return an InputStream for the man page for the named command.
     */
    public static BufferedReader getManPage(String commandName,
            CommandModel model) {
        Class clazz = model.getCommandClass();
        if (clazz == null) {
            return null;
        }
        return ManPageFinder.getCommandManPage(commandName, clazz.getName(),
                Locale.getDefault(), clazz.getClassLoader(), logger);
    }

    private void addParamUsage(
            ActionReport report,
            CommandModel.ParamModel model) {
        Param param = model.getParam();
        if (param != null) {
            // this is a param.
            String paramName = model.getName().toLowerCase(Locale.ENGLISH);
            // skip "hidden" options
            if (paramName.startsWith("_")) {
                return;
            }
            // do not want to display password in the usage
            if (param.password()) {
                return;
            }
            // do not want to display obsolete options
            if (param.obsolete()) {
                return;
            }
            if (param.primary()) {
                // if primary then it's an operand
                report.getTopMessagePart().addProperty(paramName + "_operand",
                        model.getLocalizedDescription());
            } else {
                report.getTopMessagePart().addProperty(paramName,
                        model.getLocalizedDescription());
            }
        }
    }

    private static boolean ok(String s) {
        return s != null && s.length() > 0;
    }

    /**
     * Validate the parameters with the Param annotation.  If parameter is
     * not defined as a Param annotation then it's an invalid option.
     * If parameter's key is "DEFAULT" then it's a operand.
     *
     * @param model command model
     * @param parameters parameters from URL
     *
     */
    static void validateParameters(final CommandModel model,
            final ParameterMap parameters) throws MultiException {

        ParameterMap adds = null; // renamed password parameters
        boolean autoname = false;

        // loop through parameters and make sure they are
        // part of the Param declared field
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            String key = entry.getKey();

            // to do, we should validate meta-options differently.
            if (key.equals("DEFAULT")) {
                continue;
            }

            // help and Xhelp are meta-options that are handled specially
            if (key.equals("help") || key.equals("Xhelp") || key.equals("notify")) {
                continue;
            }

            if (key.startsWith(OLD_PASSWORD_PARAM_PREFIX)) {
                // This is an old prefixed password parameter being passed in.
                // Strip the prefix and lowercase the name
                key = key.substring(OLD_PASSWORD_PARAM_PREFIX.length()).toLowerCase(Locale.ENGLISH);
                if (adds == null) adds = new ParameterMap();
                adds.add(key, entry.getValue().get(0));
            }

            // check if key is a valid Param Field
            boolean validOption = false;
            // loop through the Param field in the command class
            // if either field name or the param name is equal to
            // key then it's a valid option
            for (CommandModel.ParamModel pModel : model.getParameters()) {
                validOption = pModel.isParamId(key);
                if (validOption) {
                    break;
                }
            }

            if (!validOption) {
                throw new MultiException(new IllegalArgumentException(" Invalid option: " + key));
            }

            if ((key.equals("autoname") || key.equals("a"))
                    && entry.getValue().get(0).equalsIgnoreCase("true")) {
                autoname = true;
            }
        }

        if (!parameters.containsKey("DEFAULT") && autoname) {
            parameters.add("DEFAULT", NameGenerator.generateName());
        }

        parameters.mergeAll(adds);
    }

    /**
     * Check if the variable, "skipParamValidation" is defined in the command
     * class.  If defined and set to true, then parameter validation will be
     * skipped from that command.
     * This is used mostly for command referencing.  For example the
     * list-applications command references list-components command and you
     * don't want to define the same params from the class that implements
     * list-components.
     *
     * @param command - AdminCommand class
     * @return true if to skip param validation, else return false.
     */
    static boolean skipValidation(AdminCommand command) {
        try {
            final Field f =
                    command.getClass().getDeclaredField("skipParamValidation");
            AccessController.doPrivileged(new PrivilegedAction<Object>() {

                @Override
                public Object run() {
                    f.setAccessible(true);
                    return null;
                }
            });
            if (f.getType().isAssignableFrom(boolean.class)) {
                return f.getBoolean(command);
            }
        } catch (NoSuchFieldException e) {
            return false;
        } catch (IllegalAccessException e) {
            return false;
        }
        //all else return false
        return false;
    }

    private static String encodeManPage(BufferedReader br) {
        if (br == null) {
            return null;
        }

        try {
            String line;
            StringBuilder sb = new StringBuilder();

            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append(ManifestUtils.EOL_TOKEN);
            }
            return sb.toString();
        } catch (Exception ex) {
            return null;
        } finally {
            try {
                br.close();
            } catch (IOException ioex) {
            }
        }
    }

    private static CommandModel getModel(AdminCommand command) {

        if (command instanceof CommandModelProvider) {
            return ((CommandModelProvider) command).getModel();
        } else {
            return new CommandModelImpl(command.getClass());
        }
    }

    /**
     * Called from ExecutionContext.execute.
     */
    private void doCommand(ExecutionContext inv, AdminCommand command,
            final Subject subject, final Job job) {

        boolean fromCheckpoint = job != null &&
                (job.getState() == AdminCommandState.State.REVERTING ||
                job.getState() == AdminCommandState.State.FAILED_RETRYABLE);
        CommandModel model;
        try {
            CommandModelProvider c = CommandModelProvider.class.cast(command);
            model = c.getModel();
        } catch (ClassCastException e) {
            model = new CommandModelImpl(command.getClass());
        }
        UploadedFilesManager ufm = null;
        ActionReport report = inv.report();
        if (!fromCheckpoint) {
            report.setActionDescription(model.getCommandName() + " command");
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        }
        ParameterMap parameters;
        final AdminCommandContext context = new AdminCommandContextImpl(
                logger, report, inv.inboundPayload(), inv.outboundPayload(),
                job.getEventBroker(),
                job.getId());
        context.setSubject(subject);
        List<RuntimeType> runtimeTypes = new ArrayList<RuntimeType>();
        FailurePolicy fp = null;
        Set<CommandTarget> targetTypesAllowed = new HashSet<CommandTarget>();
        ActionReport.ExitCode preSupplementalReturn = ActionReport.ExitCode.SUCCESS;
        ActionReport.ExitCode postSupplementalReturn = ActionReport.ExitCode.SUCCESS;
        CommandRunnerProgressHelper progressHelper =
                new CommandRunnerProgressHelper(command, model.getCommandName(), job, inv.progressStatusChild);

        // If this glassfish installation does not have stand alone instances / clusters at all, then
        // lets not even look Supplemental command and such. A small optimization
        boolean doReplication = false;
        if ((domain.getServers().getServer().size() > 1) || (!domain.getClusters().getCluster().isEmpty())) {
            doReplication = true;
        } else {
            logger.fine(adminStrings.getLocalString("dynamicreconfiguration.diagnostics.devmode",
                    "The GlassFish environment does not have any clusters or instances present; Replication is turned off"));
        }
        try {
            //Get list of supplemental commands
            Collection<SupplementalCommand> supplementalCommands =
                    supplementalExecutor.listSupplementalCommands(model.getCommandName());
            try {
                /*
                 * Extract any uploaded files and build a map from parameter names
                 * to the corresponding extracted, uploaded file.
                 */
                ufm = new UploadedFilesManager(inv.report, logger,
                        inv.inboundPayload());

                if (inv.typedParams() != null) {
                    logger.fine(adminStrings.getLocalString("dynamicreconfiguration.diagnostics.delegatedcommand",
                            "This command is a delegated command. Dynamic reconfiguration will be bypassed"));
                    InjectionResolver<Param> injectionTarget =
                            new DelegatedInjectionResolver(model, inv.typedParams(),
                            ufm.optionNameToFileMap());
                    if (injectParameters(model, command, injectionTarget, report)) {
                        inv.setReport(doCommand(model, command, context, progressHelper));
                    }
                    return;
                }

                parameters = inv.parameters();
                if (parameters == null) {
                    // no parameters, pass an empty collection
                    parameters = new ParameterMap();
                }

                if (isSet(parameters, "help") || isSet(parameters, "Xhelp")) {
                    BufferedReader in = getManPage(model.getCommandName(), model);
                    String manPage = encodeManPage(in);

                    if (manPage != null && isSet(parameters, "help")) {
                        inv.report().getTopMessagePart().addProperty("MANPAGE", manPage);
                    } else {
                        report.getTopMessagePart().addProperty(
                                AdminCommandResponse.GENERATED_HELP, "true");
                        getHelp(command, report);
                    }
                    return;
                }

                try {
                    if (!fromCheckpoint && !skipValidation(command)) {
                        validateParameters(model, parameters);
                    }
                } catch (MultiException e) {
                    // If the cause is UnacceptableValueException -- we want the message
                    // from it.  It is wrapped with a less useful Exception.

                    Exception exception = e;
                    for (Throwable cause : e.getErrors()) {
                        if (cause != null
                                && (cause instanceof UnacceptableValueException)) {
                            // throw away the wrapper.
                            exception = (Exception) cause;
                            break;
                        }

                    }

                    logger.log(Level.SEVERE, KernelLoggerInfo.invocationException, exception);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage(exception.getMessage());
                    report.setFailureCause(exception);
                    ActionReport.MessagePart childPart =
                            report.getTopMessagePart().addChild();
                    childPart.setMessage(getUsageText(model));
                    return;
                }

                // initialize the injector and inject
                MapInjectionResolver injectionMgr =
                        new MapInjectionResolver(model, parameters,
                        ufm.optionNameToFileMap());
                injectionMgr.setContext(context);
                if (!injectParameters(model, command, injectionMgr, report)) {
                    return;
                }

                CommandSupport.init(habitat, command, context, job);

                /*
                 * Now that parameters have been injected into the command object,
                 * decide if the current Subject should be permitted to execute
                 * the command.  We need to wait until after injection is done
                 * because the class might implement its own authorization check
                 * and that logic might need the injected values.
                 */
                final Map<String,Object> env = buildEnvMap(parameters);
                try {
                    if ( ! commandSecurityChecker.authorize(context.getSubject(), env, command, context)) {
                        /*
                         * If the command class tried to prepare itself but
                         * could not then the return is false and the command has
                         * set the action report accordingly.  Don't process
                         * the command further and leave the action report alone.
                         */
                        return;
                    }
                } catch (SecurityException ex) {
                    report.setFailureCause(ex);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage(adminStrings.getLocalString("commandrunner.noauth",
                            "User is not authorized for this command"));
                    return;
                } catch (Exception ex) {
                    report.setFailureCause(ex);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage(adminStrings.getLocalString("commandrunner.errAuth",
                            "Error during authorization"));
                    return;
                }


                logger.fine(adminStrings.getLocalString("dynamicreconfiguration.diagnostics.injectiondone",
                        "Parameter mapping, validation, injection completed successfully; Starting paramater injection"));

                // Read cluster annotation attributes
                org.glassfish.api.admin.ExecuteOn clAnnotation = model.getClusteringAttributes();
                if (clAnnotation == null) {
                    runtimeTypes.add(RuntimeType.DAS);
                    runtimeTypes.add(RuntimeType.INSTANCE);
                    fp = FailurePolicy.Error;
                } else {
                    if (clAnnotation.value().length == 0) {
                        runtimeTypes.add(RuntimeType.DAS);
                        runtimeTypes.add(RuntimeType.INSTANCE);
                    } else {
                        runtimeTypes.addAll(Arrays.asList(clAnnotation.value()));
                    }
                    if (clAnnotation.ifFailure() == null) {
                        fp = FailurePolicy.Error;
                    } else {
                        fp = clAnnotation.ifFailure();
                    }
                }
                TargetType tgtTypeAnnotation = command.getClass().getAnnotation(TargetType.class);

                //@ExecuteOn(RuntimeType.SINGLE_INSTANCE) cannot be combined with
                //@TargetType since we do not want to replicate the command
                if (runtimeTypes.contains(RuntimeType.SINGLE_INSTANCE)) {
                   if (tgtTypeAnnotation != null) {

                       report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                       report.setMessage(adminStrings.getLocalString("commandrunner.executor.targettype.unallowed",
                               "Target type is not allowed on single instance command {0}  ,"
                                       , model.getCommandName()));
                       return;
                   }
                   //Do not replicate the command when there is
                   //@ExecuteOn(RuntimeType.SINGLE_INSTANCE)
                   doReplication = false;
                }

                String targetName = parameters.getOne("target");
                if (targetName == null || model.getModelFor("target").getParam().obsolete()) {
                    if (command instanceof DeploymentTargetResolver) {
                        targetName = ((DeploymentTargetResolver) command).getTarget(parameters);
                    } else {
                        targetName = "server";
                    }
                }

                logger.fine(adminStrings.getLocalString("dynamicreconfiguration.diagnostics.target",
                        "@ExecuteOn parsing and default settings done; Current target is {0}", targetName));

                if (serverEnv.isDas()) {

                    //Do not replicate this command if it has @ExecuteOn(RuntimeType.SINGLE_INSTANCE)
                    //and the user is authorized to execute on DAS
                    // TODO add authorization check
                    /*if (runtimeTypes.contains(RuntimeType.SINGLE_INSTANCE)) {
                        //If authorization fails
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setMessage(adminStrings.getLocalString("commandrunner.executor.das.unallowed",
                                "Not authorized to execute command {0} on DAS"
                                        , model.getCommandName()));
                        progressHelper.complete(context);
                        return;
                    }*/

                    // Check if the command allows this target type; first read the annotation
                    //TODO : See is @TargetType can also be moved to the CommandModel

                    if (tgtTypeAnnotation != null) {
                        targetTypesAllowed.addAll(Arrays.asList(tgtTypeAnnotation.value()));
                    }
                    //If not @TargetType, default it
                    if (targetTypesAllowed.isEmpty()) {
                        targetTypesAllowed.add(CommandTarget.DAS);
                        targetTypesAllowed.add(CommandTarget.STANDALONE_INSTANCE);
                        targetTypesAllowed.add(CommandTarget.CLUSTER);
                        targetTypesAllowed.add(CommandTarget.CONFIG);
                    }

                    // If the target is "server" and the command is not marked for DAS,
                    // add DAS to RuntimeTypes; This is important because those class of CLIs that
                    // do not always have to be run on DAS followed by applicable instances
                    // will have @ExecuteOn(RuntimeType.INSTANCE) and they have to be run on DAS
                    // ONLY if the target is "server"
                    if (CommandTarget.DAS.isValid(habitat, targetName)
                            && !runtimeTypes.contains(RuntimeType.DAS)) {
                        runtimeTypes.add(RuntimeType.DAS);
                    }

                    logger.fine(adminStrings.getLocalString("dynamicreconfiguration.diagnostics.runtimeTypes",
                            "RuntimeTypes are: {0}", runtimeTypes.toString()));
                    logger.fine(adminStrings.getLocalString("dynamicreconfiguration,diagnostics.targetTypes",
                            "TargetTypes are: {0}", targetTypesAllowed.toString()));

                    // Check if the target is valid
                    //Is there a server or a cluster or a config with given name ?
                    if ((!CommandTarget.DOMAIN.isValid(habitat, targetName))
                            && (domain.getServerNamed(targetName) == null)
                            && (domain.getClusterNamed(targetName) == null)
                            && (domain.getConfigNamed(targetName) == null)
                            && (domain.getDeploymentGroupNamed(targetName) == null)) {
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setMessage(adminStrings.getLocalString("commandrunner.executor.invalidtarget",
                                "Unable to find a valid target with name {0}", targetName));
                        return;
                    }
                    //Does this command allow this target type
                    boolean isTargetValidType = false;
                    Iterator<CommandTarget> it = targetTypesAllowed.iterator();
                    while (it.hasNext()) {
                        if (it.next().isValid(habitat, targetName)) {
                            isTargetValidType = true;
                            break;
                        }
                    }
                    if (!isTargetValidType) {
                        StringBuilder validTypes = new StringBuilder();
                        it = targetTypesAllowed.iterator();
                        while (it.hasNext()) {
                            validTypes.append(it.next().getDescription()).append(", ");
                        }
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setMessage(adminStrings.getLocalString("commandrunner.executor.invalidtargettype",
                                "Target {0} is not a supported type. Command {1} supports these types of targets only : {2}",
                                targetName, model.getCommandName(), validTypes.toString()));
                        return;
                    }
                    //If target is a clustered instance and the allowed types does not allow operations on clustered
                    //instance, return error
                    if ((CommandTarget.CLUSTERED_INSTANCE.isValid(habitat, targetName))
                            && (!targetTypesAllowed.contains(CommandTarget.CLUSTERED_INSTANCE))) {
                        Cluster c = domain.getClusterForInstance(targetName);
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setMessage(adminStrings.getLocalString("commandrunner.executor.instanceopnotallowed",
                                "The {0} command is not allowed on instance {1} because it is part of cluster {2}",
                                model.getCommandName(), targetName, c.getName()));
                        return;
                    }
                    logger.fine(adminStrings.getLocalString("dynamicreconfiguration.diagnostics.replicationvalidationdone",
                            "All @ExecuteOn attribute and type validation completed successfully. Starting replication stages"));
                }

                /**
                 * We're finally ready to actually execute the command instance.
                 * Acquire the appropriate lock.
                 */
                Lock lock = null;
                boolean lockTimedOut = false;
                try {
                    // XXX: The owner of the lock should not be hardcoded.  The
                    //      value is not used yet.
                    lock = adminLock.getLock(command, "asadmin");

                    //Set there progress statuses
                    if (!fromCheckpoint) {
                        for (SupplementalCommand supplementalCommand : supplementalCommands) {
                            progressHelper.addProgressStatusToSupplementalCommand(supplementalCommand);
                        }
                    }

                    // If command is undoable, then invoke prepare method
                    if (command instanceof UndoableCommand) {
                        UndoableCommand uCmd = (UndoableCommand) command;
                        logger.fine(adminStrings.getLocalString("dynamicreconfiguration.diagnostics.prepareunodable",
                                "Command execution stage 1 : Calling prepare for undoable command {0}", inv.name()));
                        if (!uCmd.prepare(context, parameters).equals(ActionReport.ExitCode.SUCCESS)) {
                            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                            report.setMessage(adminStrings.getLocalString("commandrunner.executor.errorinprepare",
                                    "The command {0} cannot be completed because the preparation for the command failed "
                                    + "indicating potential issues : {1}", model.getCommandName(), report.getMessage()));
                            return;
                        }
                    }

                    ClusterOperationUtil.clearInstanceList();

                    // Run Supplemental commands that have to run before this command on this instance type
                    if (!fromCheckpoint) {
                        logger.fine(adminStrings.getLocalString("dynamicreconfiguration.diagnostics.presupplemental",
                                "Command execution stage 2 : Call pre supplemental commands for {0}", inv.name()));
                        preSupplementalReturn = supplementalExecutor.execute(supplementalCommands,
                                Supplemental.Timing.Before, context, parameters, ufm.optionNameToFileMap());
                        if (preSupplementalReturn.equals(ActionReport.ExitCode.FAILURE)) {
                            report.setActionExitCode(preSupplementalReturn);
                            if (!StringUtils.ok(report.getTopMessagePart().getMessage())) {
                                report.setMessage(adminStrings.getLocalString("commandrunner.executor.supplementalcmdfailed",
                                    "A supplemental command failed; cannot proceed further"));
                            }
                            return;
                        }
                    }

                    //Run main command if it is applicable for this instance type
                    if ((runtimeTypes.contains(RuntimeType.ALL))
                            || (serverEnv.isDas() &&
                                (CommandTarget.DOMAIN.isValid(habitat, targetName) || runtimeTypes.contains(RuntimeType.DAS)))
                            || runtimeTypes.contains(RuntimeType.SINGLE_INSTANCE)
                            || (serverEnv.isInstance() && runtimeTypes.contains(RuntimeType.INSTANCE))) {
                        logger.fine(adminStrings.getLocalString("dynamicreconfiguration.diagnostics.maincommand",
                                "Command execution stage 3 : Calling main command implementation for {0}", inv.name()));
                        report = doCommand(model, command, context, progressHelper);
                        inv.setReport(report);
                    }



                    if (!FailurePolicy.applyFailurePolicy(fp,
                            report.getActionExitCode()).equals(ActionReport.ExitCode.FAILURE)) {
                        //Run Supplemental commands that have to be run after this command on this instance type
                        logger.fine(adminStrings.getLocalString("dynamicreconfiguration.diagnostics.postsupplemental",
                                "Command execution stage 4 : Call post supplemental commands for {0}", inv.name()));
                        postSupplementalReturn = supplementalExecutor.execute(supplementalCommands,
                                Supplemental.Timing.After, context, parameters, ufm.optionNameToFileMap());
                        if (postSupplementalReturn.equals(ActionReport.ExitCode.FAILURE)) {
                            report.setActionExitCode(postSupplementalReturn);
                            report.setMessage(adminStrings.getLocalString("commandrunner.executor.supplementalcmdfailed",
                                    "A supplemental command failed; cannot proceed further"));
                            return;
                        }
                    }
                } catch (AdminCommandLockTimeoutException ex) {
                    lockTimedOut = true;
                    String lockTime = formatSuspendDate(ex.getTimeOfAcquisition());
                    String logMsg = "Command: " + model.getCommandName()
                            + " failed to acquire a command lock.  REASON: time out "
                            + "(current lock acquired on " + lockTime + ")";
                    String msg = adminStrings.getLocalString("lock.timeout",
                            "Command timed out.  Unable to acquire a lock to access "
                            + "the domain.  Another command acquired exclusive access "
                            + "to the domain on {0}.  Retry the command at a later "
                            + "time.", lockTime);
                    report.setMessage(msg);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                } catch (AdminCommandLockException ex) {
                    lockTimedOut = true;
                    String lockTime = formatSuspendDate(ex.getTimeOfAcquisition());
                    String lockMsg = ex.getMessage();
                    String logMsg;

                    logMsg = "Command: " + model.getCommandName()
                            + " was blocked.  The domain was suspended by a "
                            + "user on:" + lockTime;

                    if (lockMsg != null && !lockMsg.isEmpty()) {
                        logMsg += " Reason: " + lockMsg;
                    }

                    String msg = adminStrings.getLocalString("lock.notacquired",
                            "The command was blocked.  The domain was suspended by "
                            + "a user on {0}.", lockTime);

                    if (lockMsg != null && !lockMsg.isEmpty()) {
                        msg += " "
                                + adminStrings.getLocalString("lock.reason", "Reason:")
                                + " " + lockMsg;
                    }

                    report.setMessage(msg);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                } finally {
                    // command is done, release the lock
                    if (lock != null && lockTimedOut == false) {
                        lock.unlock();
                    }
                }

            } catch (Exception ex) {
                logger.log(Level.SEVERE, KernelLoggerInfo.invocationException, ex);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(ex.getMessage());
                report.setFailureCause(ex);
                ActionReport.MessagePart childPart =
                        report.getTopMessagePart().addChild();
                childPart.setMessage(getUsageText(model));
                return;
            }
            /*
             * Command execution completed; If this is DAS and the command succeeded,
             * time to replicate; At this point we will get the appropriate ClusterExecutor
             * and give it complete control; We will let the executor take care all considerations
             * (like FailurePolicy settings etc)
             * and just give the final execution results which we will set as is in the Final
             * Action report returned to the caller.
             */

            if (processEnv.getProcessType().isEmbedded()) {
                return;
            }
            if (preSupplementalReturn == ActionReport.ExitCode.WARNING
                    || postSupplementalReturn == ActionReport.ExitCode.WARNING) {
                report.setActionExitCode(ActionReport.ExitCode.WARNING);
            }
            if (doReplication
                    && (!FailurePolicy.applyFailurePolicy(fp, report.getActionExitCode()).equals(ActionReport.ExitCode.FAILURE))
                    && (serverEnv.isDas())
                    && (runtimeTypes.contains(RuntimeType.INSTANCE) || runtimeTypes.contains(RuntimeType.ALL))) {
                logger.fine(adminStrings.getLocalString("dynamicreconfiguration.diagnostics.startreplication",
                        "Command execution stages completed on DAS; Starting replication on remote instances"));
                ClusterExecutor executor = null;
                // This try-catch block is a fix for 13838
                try {
                    if (model.getClusteringAttributes() != null && model.getClusteringAttributes().executor() != null) {
                        executor = habitat.getService(model.getClusteringAttributes().executor());
                    } else {
                        executor = habitat.getService(ClusterExecutor.class, "GlassFishClusterExecutor");
                    }
                } catch (UnsatisfiedDependencyException usdepex) {
                    logger.log(Level.WARNING, KernelLoggerInfo.cantGetClusterExecutor, usdepex);
                }
                if (executor != null) {
                    report.setActionExitCode(executor.execute(model.getCommandName(), command, context, parameters));
                    if (report.getActionExitCode().equals(ActionReport.ExitCode.FAILURE)) {
                        report.setMessage(adminStrings.getLocalString("commandrunner.executor.errorwhilereplication",
                                "An error occurred during replication"));
                    } else {
                        if (!FailurePolicy.applyFailurePolicy(fp,
                                report.getActionExitCode()).equals(ActionReport.ExitCode.FAILURE)) {
                            logger.fine(adminStrings.getLocalString("dynamicreconfiguration.diagnostics.afterreplsupplemental",
                                    "Command execution stage 5 : Call post-replication supplemental commands for {0}", inv.name()));
                            ActionReport.ExitCode afterReplicationSupplementalReturn = supplementalExecutor.execute(supplementalCommands,
                                    Supplemental.Timing.AfterReplication, context, parameters, ufm.optionNameToFileMap());
                            if (afterReplicationSupplementalReturn.equals(ActionReport.ExitCode.FAILURE)) {
                                report.setActionExitCode(afterReplicationSupplementalReturn);
                                report.setMessage(adminStrings.getLocalString("commandrunner.executor.supplementalcmdfailed",
                                        "A supplemental command failed; cannot proceed further"));
                                return;
                            }
                        }
                    }
                }
            }
            if (report.getActionExitCode().equals(ActionReport.ExitCode.FAILURE)) {
                // If command is undoable, then invoke undo method method
                if (command instanceof UndoableCommand) {
                    UndoableCommand uCmd = (UndoableCommand) command;
                    logger.fine(adminStrings.getLocalString("dynamicreconfiguration.diagnostics.undo",
                            "Command execution failed; calling undo() for command {0}", inv.name()));
                    uCmd.undo(context, parameters, ClusterOperationUtil.getCompletedInstances());
                }
            } else {
                //TODO : Is there a better way of doing this ? Got to look into it
                if ("_register-instance".equals(model.getCommandName())) {
                    state.addServerToStateService(parameters.getOne("DEFAULT"));
                }
                if ("_unregister-instance".equals(model.getCommandName())) {
                    state.removeInstanceFromStateService(parameters.getOne("DEFAULT"));
                }
            }
        } finally {
            if (ufm != null) {
                ufm.close();
            }
        }
    }

    private Map<String,Object> buildEnvMap(final ParameterMap params) {
        final Map<String,Object> result = new HashMap<String,Object>();
        for (Map.Entry<String,List<String>> entry : params.entrySet()) {
            final List<String> values = entry.getValue();
            if (values != null && values.size() > 0) {
                result.put(entry.getKey(), values.get(0));
            }
        }
        return result;
    }

    public void executeFromCheckpoint(JobManager.Checkpoint checkpoint, boolean revert, AdminCommandEventBroker eventBroker) {
        ExecutionContext ec = new ExecutionContext(null, null, null, null,false);
        ec.executeFromCheckpoint(checkpoint, revert, eventBroker);
    }

    /*
     * Some private classes used in the implementation of CommandRunner.
     */
    /**
     * ExecutionContext is a CommandInvocation, which
     * defines a command excecution context like the requested
     * name of the command to execute, the parameters of the command, etc.
     */
    class ExecutionContext implements CommandInvocation {

        private class NameListerPair {

            private String nameRegexp;
            private AdminCommandEventBroker.AdminCommandListener listener;

            public NameListerPair(String nameRegexp, AdminCommandListener listener) {
                this.nameRegexp = nameRegexp;
                this.listener = listener;
            }

        }

        protected String scope;
        protected String name;
        protected ActionReport report;
        protected ParameterMap params;
        protected CommandParameters paramObject;
        protected Payload.Inbound inbound;
        protected Payload.Outbound outbound;
        protected Subject subject;
        protected ProgressStatus progressStatusChild;
        protected boolean isManagedJob;
        protected boolean isNotify;
        private   List<NameListerPair> nameListerPairs = new ArrayList<NameListerPair>();

        private ExecutionContext(String scope, String name, ActionReport report, Subject subject, boolean isNotify) {
            this.scope = scope;
            this.name = name;
            this.report = report;
            this.subject = subject;
            this.isNotify = isNotify;
        }

        @Override
        public CommandInvocation parameters(CommandParameters paramObject) {
            this.paramObject = paramObject;
            return this;
        }

        @Override
        public CommandInvocation parameters(ParameterMap params) {
            this.params = params;
            return this;
        }

        @Override
        public CommandInvocation inbound(Payload.Inbound inbound) {
            this.inbound = inbound;
            return this;
        }

        @Override
        public CommandInvocation outbound(Payload.Outbound outbound) {
            this.outbound = outbound;
            return this;
        }

        @Override
        public CommandInvocation listener(String nameRegexp, AdminCommandEventBroker.AdminCommandListener listener) {
            nameListerPairs.add(new NameListerPair(nameRegexp, listener));
            return this;
        }

        @Override
        public CommandInvocation progressStatusChild(ProgressStatus ps) {
            this.progressStatusChild = ps;
            return this;
        }

        @Override
        public CommandInvocation managedJob() {
            this.isManagedJob = true;
            return this;
        }

        @Override
        public void execute() {
            execute(null);
        }

        private ParameterMap parameters() {
            return params;
        }

        private CommandParameters typedParams() {
            return paramObject;
        }

        private String name() {
            return name;
        }

        private String scope() {
            return scope;
        }

        @Override
        public ActionReport report() {
            return report;
        }

        private void setReport(ActionReport ar) {
            report = ar;
        }

        private Payload.Inbound inboundPayload() {
            return inbound;
        }

        private Payload.Outbound outboundPayload() {
            return outbound;
        }

        private void executeFromCheckpoint(JobManager.Checkpoint checkpoint, boolean revert, AdminCommandEventBroker eventBroker) {
            Job job = checkpoint.getJob();
            if (subject == null) {
                subject = checkpoint.getContext().getSubject();
            }
            parameters(job.getParameters());
            AdminCommandContext context = checkpoint.getContext();
            this.report = context.getActionReport();
            this.inbound = context.getInboundPayload();
            this.outbound = context.getOutboundPayload();
            this.scope = job.getScope();
            this.name = job.getName();
            if (eventBroker == null) {
                eventBroker = job.getEventBroker() == null ? new AdminCommandEventBrokerImpl() : job.getEventBroker();
            }
            ((AdminCommandInstanceImpl) job).setEventBroker(eventBroker);
            ((AdminCommandInstanceImpl) job).setState(revert ? AdminCommandState.State.REVERTING : AdminCommandState.State.RUNNING_RETRYABLE);
            JobManager jobManager = habitat.getService(JobManagerService.class);
            jobManager.registerJob(job);
            //command
            AdminCommand command = checkpoint.getCommand();
            if (command == null) {
                command = getCommand(job.getScope(), job.getName(), report(), logger);
                if (command == null) {
                    return;
                }
            }
            //execute
            CommandRunnerImpl.this.doCommand(this, command, subject, job);
            job.complete(report(), outboundPayload());
            if (progressStatusChild != null) {
                progressStatusChild.complete();
            }
            CommandSupport.done(habitat, command, job);
        }

        @Override
        public void execute(AdminCommand command) {
            if (command == null) {
                command = getCommand(scope(), name(), report(), logger);
                if (command == null) {
                    return;
                }
            }
            /*
             * The caller should have set the subject explicitly.  In case
             * it didn't, try setting it from the current access controller context
             * since the command framework will have set that before invoking
             * the original command's execute method.
             */
            if (subject == null) {
                subject = AccessController.doPrivileged(new PrivilegedAction<Subject>() {
                    @Override
                    public Subject run() {
                        return Subject.getSubject(AccessController.getContext());
                    }
                });
            }

            if(!isManagedJob) {
                isManagedJob = AnnotationUtil.presentTransitive(ManagedJob.class, command.getClass());
            }
            JobCreator jobCreator = null;
            JobManager jobManager = null;

            jobCreator = habitat.getService(JobCreator.class,scope+"job-creator");
            jobManager = habitat.getService(JobManagerService.class);

            if (jobCreator == null ) {
                jobCreator = habitat.getService(JobCreatorService.class);

            }

            Job job = null;
            if (isManagedJob) {
                job = jobCreator.createJob(jobManager.getNewId(), scope(), name(), subject, isManagedJob, parameters());
            }  else {
                job = jobCreator.createJob(null, scope(), name(), subject, isManagedJob, parameters());
            }

            //Register the brokers  else the detach functionality will not work
            for (NameListerPair nameListerPair : nameListerPairs) {
                job.getEventBroker().registerListener(nameListerPair.nameRegexp, nameListerPair.listener);
            }

            if (isManagedJob)  {
                jobManager.registerJob(job);
            }
            CommandRunnerImpl.this.doCommand(this, command, subject, job);
            job.complete(report(), outboundPayload());
            if (progressStatusChild != null) {
                progressStatusChild.complete();
            }
            CommandSupport.done(habitat, command, job, isNotify);
        }
    }

    /**
     * An InjectionResolver that uses an Object as the source of
     * the data to inject.
     */
    private static class DelegatedInjectionResolver
            extends InjectionResolver<Param> {

        private final CommandModel model;
        private final CommandParameters parameters;
        private final MultiMap<String, File> optionNameToUploadedFileMap;

        public DelegatedInjectionResolver(CommandModel model,
                CommandParameters parameters,
                final MultiMap<String, File> optionNameToUploadedFileMap) {
            super(Param.class);
            this.model = model;
            this.parameters = parameters;
            this.optionNameToUploadedFileMap = optionNameToUploadedFileMap;

        }

        @Override
        public boolean isOptional(AnnotatedElement element, Param annotation) {
            String name = CommandModel.getParamName(annotation, element);
            CommandModel.ParamModel param = model.getModelFor(name);
            return param.getParam().optional();
        }

        @Override
        public <V> V getValue(Object component, AnnotatedElement target, Type genericType, Class<V> type) {

            // look for the name in the list of parameters passed.
            if (target instanceof Field) {
                final Field targetField = (Field) target;
                try {
                    Field sourceField =
                            parameters.getClass().getField(targetField.getName());
                    AccessController.doPrivileged(new PrivilegedAction<Object>() {

                        @Override
                        public Object run() {
                            targetField.setAccessible(true);
                            return null;
                        }
                    });
                    Object paramValue = sourceField.get(parameters);

                    /*
                     * If this field is a File, then replace the param value
                     * (which is whatever the client supplied on the command) with
                     * the actual absolute path(s) of the uploaded and extracted
                     * file(s) if, in fact, the file(s) was (were) uploaded.
                     */

                    final List<String> paramFileValues =
                            MapInjectionResolver.getUploadedFileParamValues(
                            targetField.getName(),
                            targetField.getType(),
                            optionNameToUploadedFileMap);
                    if (!paramFileValues.isEmpty()) {
                        V fileValue = (V) MapInjectionResolver.convertListToObject(target, type, paramFileValues);
                        return fileValue;
                    }
                    /*
                    if (paramValue==null) {
                    return convertStringToObject(target, type,
                    param.defaultValue());
                    }
                     */
                    // XXX temp fix, to revisit
                    if (paramValue != null) {
                        checkAgainstAcceptableValues(target,
                                paramValue.toString());
                    }
                    return type.cast(paramValue);
                } catch (IllegalAccessException e) {
                } catch (NoSuchFieldException e) {
                }
            }
            return null;
        }

        private static void checkAgainstAcceptableValues(
                AnnotatedElement target, String paramValueStr) {
            Param param = target.getAnnotation(Param.class);
            String acceptable = param.acceptableValues();
            String paramName = CommandModel.getParamName(param, target);

            if (ok(acceptable) && ok(paramValueStr)) {
                String[] ss = acceptable.split(",");

                for (String s : ss) {
                    if (paramValueStr.equals(s.trim())) {
                        return;         // matched, value is good
                    }
                }

                // didn't match any, error
                throw new UnacceptableValueException(
                        adminStrings.getLocalString(
                        "adapter.command.unacceptableValue",
                        "Invalid parameter: {0}.  Its value is {1} "
                        + "but it isn''t one of these acceptable values: {2}",
                        paramName,
                        paramValueStr,
                        acceptable));
            }
        }
    }

    /**
     * Is the boolean valued parameter specified?
     * If so, and it has a value, is the value "true"?
     */
    private static boolean isSet(ParameterMap params, String name) {
        String val = params.getOne(name);
        if (val == null) {
            return false;
        }
        return val.length() == 0 || Boolean.valueOf(val).booleanValue();
    }

    /** Works as a key in ETag cache map
     */
    private static class NameCommandClassPair {
        private String name;
        private Class<? extends AdminCommand> clazz;
        private int hash; //immutable, we can cache it

        public NameCommandClassPair(String name, Class<? extends AdminCommand> clazz) {
            this.name = name;
            this.clazz = clazz;
            hash = 3;
            hash = 67 * hash + (this.name != null ? this.name.hashCode() : 0);
            hash = 67 * hash + (this.clazz != null ? this.clazz.hashCode() : 0);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final NameCommandClassPair other = (NameCommandClassPair) obj;
            if (this.clazz != other.clazz) {
                return false;
            }
            if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    /**
     * Encapsulates handling of files uploaded to the server in the payload
     * of the incoming HTTP request.
     * <p>
     * Extracts any such files from the payload into a temporary directory
     * under the domain's applications directory.  (Putting them there allows
     * the deployment processing to rename the uploaded archive to another location
     * under the applications directory, rather than having to copy them.)
     */
    private class UploadedFilesManager {

        private final ActionReport report;
        private final Logger logger;
        /**
         * maps option names as sent with each uploaded file to the corresponding
         * extracted files
         */
        private MultiMap<String, File> optionNameToFileMap;

        /*
         * PFM needs to be a field so it is not gc-ed before the
         * UploadedFilesManager is closed.
         */
        private PayloadFilesManager.Temp payloadFilesMgr = null;

        private UploadedFilesManager(final ActionReport report,
                final Logger logger,
                final Payload.Inbound inboundPayload) throws IOException, Exception {
            this.logger = logger;
            this.report = report;
            extractFiles(inboundPayload);
        }

        private MultiMap<String, File> optionNameToFileMap() {
            return optionNameToFileMap;
        }

        private void close() {
            if (payloadFilesMgr != null) {
                payloadFilesMgr.cleanup();
            }
        }

        private void extractFiles(final Payload.Inbound inboundPayload)
                throws Exception {
            if (inboundPayload == null) {
                return;
            }

            final File uniqueSubdirUnderApplications = chooseTempDirParent();
            payloadFilesMgr = new PayloadFilesManager.Temp(
                    uniqueSubdirUnderApplications,
                    report,
                    logger);

            /*
             * Extract the files into the temp directory.
             */
            final Map<File, Properties> payloadFiles =
                    payloadFilesMgr.processPartsExtended(inboundPayload);

            /*
             * Prepare the map of command options names to corresponding
             * uploaded files.
             */
            optionNameToFileMap = new MultiMap<String, File>();
            for (Map.Entry<File, Properties> e : payloadFiles.entrySet()) {
                final String optionName = e.getValue().getProperty("data-request-name");
                if (optionName != null) {
                    logger.finer("UploadedFilesManager: map " + optionName
                            + " to " + e.getKey());
                    optionNameToFileMap.add(optionName, e.getKey());
                }
            }
        }

        private File chooseTempDirParent() throws IOException {
            final File appRoot = new File(domain.getApplicationRoot());

            /*
             * Apparently during embedded runs the applications directory
             * might not be present already.  Create it if needed.
             */
            if (!appRoot.isDirectory()) {
                if (!appRoot.exists() && !appRoot.mkdirs()) {
                    throw new IOException(adminStrings.getLocalString("commandrunner.errCreDir",
                            "Could not create the directory {0}; no further information is available.",
                            appRoot.getAbsolutePath()));
                }
            }

            return appRoot;
        }
    }

    /**
     * Format the lock acquisition time.
     */
    private String formatSuspendDate(Date lockTime) {
        if (lockTime != null) {
            String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            return sdf.format(lockTime);
        } else {
            return adminStrings.getLocalString("lock.timeoutunavailable",
                    "<<Date is unavailable>>");
        }
    }
}
