/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.hk2.component.InjectionResolver;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.common.util.admin.MapInjectionResolver;
import org.jvnet.hk2.component.*;
import org.glassfish.common.util.admin.CommandModelImpl;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * An executor that executes Supplemental commands means for current command
 *
 * @author Vijay Ramachandran
 */
@Service(name="SupplementalCommandExecutorImpl")
public class SupplementalCommandExecutorImpl implements SupplementalCommandExecutor {

    @Inject
    private Domain domain;

    @Inject
    private ExecutorService threadExecutor;

    @Inject
    private Habitat habitat;

    @Inject
    private ServerEnvironment serverEnv;

    private final Logger logger = LogDomains.getLogger(SupplementalCommandExecutorImpl.class,
                                        LogDomains.ADMIN_LOGGER);

    private static final LocalStringManagerImpl strings =
                        new LocalStringManagerImpl(SupplementalCommandExecutor.class);

    private Map<String, List<Inhabitant<? extends Supplemental>>> supplementalCommandsMap = null;

    public ActionReport.ExitCode execute(String commandName, Supplemental.Timing time,
                             AdminCommandContext context, ParameterMap parameters, Map<String, File> optionFileMap) {
        //TODO : Use the executor service to parallelize this
        ActionReport.ExitCode finalResult = ActionReport.ExitCode.SUCCESS;
        if(!getSupplementalCommandsList().isEmpty() && getSupplementalCommandsList().containsKey(commandName)) {
            List<Inhabitant<? extends Supplemental>> supplementalList = getSupplementalCommandsList().get(commandName);
            for(Inhabitant<? extends Supplemental> inh : supplementalList) {
                AdminCommand cmdObject = (AdminCommand) inh.get();
                Supplemental ann = cmdObject.getClass().getAnnotation(Supplemental.class);
                SupplementalCommand aCmd = new SupplementalCommand(cmdObject, ann.on(), ann.ifFailure());
                if( (serverEnv.isDas() && aCmd.whereToRun().contains(RuntimeType.DAS)) ||
                    (serverEnv.isInstance() && aCmd.whereToRun().contains(RuntimeType.INSTANCE)) ) {
                    if( (time.equals(Supplemental.Timing.Before) && aCmd.toBeExecutedBefore()) ||
                        (time.equals(Supplemental.Timing.After) && aCmd.toBeExecutedAfter()) ) {
                        ActionReport.ExitCode result = FailurePolicy.applyFailurePolicy(aCmd.onFailure(),
                                inject(aCmd, getInjector(aCmd.command, parameters, optionFileMap),
                                        context.getActionReport()));
                        if(!result.equals(ActionReport.ExitCode.SUCCESS)) {
                            if(finalResult.equals(ActionReport.ExitCode.SUCCESS))
                                finalResult = result;
                            continue;
                        }
                        logger.fine(strings.getLocalString("dynamicreconfiguration.diagnostics.supplementalexec",
                                "Executing supplemental command " + aCmd.getClass().getCanonicalName()));
                        aCmd.execute(context);
                        if(context.getActionReport().hasFailures()) {
                            result = FailurePolicy.applyFailurePolicy(aCmd.onFailure(), ActionReport.ExitCode.FAILURE);
                        } else if(context.getActionReport().hasWarnings()) {
                            result = FailurePolicy.applyFailurePolicy(aCmd.onFailure(), ActionReport.ExitCode.WARNING);
                        }
                        if(!result.equals(ActionReport.ExitCode.SUCCESS)) {
                            if(finalResult.equals(ActionReport.ExitCode.SUCCESS))
                                finalResult = result;
                        }
                    }
                }
            }
        }
        return finalResult;
    }

    /**
     * Get list of all supplemental commands, map it to various commands and cache htis list
     */
    private Map<String, List<Inhabitant<? extends Supplemental>>> getSupplementalCommandsList() {
        if(supplementalCommandsMap == null) {
            synchronized(this) {
                if(supplementalCommandsMap == null) {
                    supplementalCommandsMap = new ConcurrentHashMap<String, List<Inhabitant<? extends Supplemental>>>();
                    Collection<Inhabitant<? extends Supplemental>> supplementals = habitat.getInhabitants(Supplemental.class);
                    if(!supplementals.isEmpty()) {
                        Iterator<Inhabitant<? extends Supplemental>> iter = supplementals.iterator();
                        while(iter.hasNext()) {
                            Inhabitant<? extends Supplemental> inh = iter.next();
                            String commandName = inh.metadata().getOne("target");
                            if(supplementalCommandsMap.containsKey(commandName)) {
                                supplementalCommandsMap.get(commandName).add(inh);
                            } else {
                                ArrayList<Inhabitant<? extends Supplemental>> inhList =
                                        new ArrayList<Inhabitant<? extends Supplemental>>();
                                inhList.add(inh);
                                supplementalCommandsMap.put(commandName, inhList);
                            }
                        }
                    }
                }
            }
        }
        return supplementalCommandsMap;
    }

    private InjectionResolver<Param> getInjector(AdminCommand command, ParameterMap parameters, Map<String, File> map) {
        CommandModel model;
        try {
            CommandModelProvider c = CommandModelProvider.class.cast(command);
            model = c.getModel();
        } catch (ClassCastException e) {
            model = new CommandModelImpl(command.getClass());
        }
        return new MapInjectionResolver(model, parameters, map);
    }

    private ActionReport.ExitCode inject(SupplementalCommand cmd,InjectionResolver<Param> injector, ActionReport subActionReport) {

        ActionReport.ExitCode result = ActionReport.ExitCode.SUCCESS;
        try {
            new InjectionManager().inject(cmd.command, injector);
        } catch (Exception e) {
            result = ActionReport.ExitCode.FAILURE;
            subActionReport.setActionExitCode(result);
            subActionReport.setMessage(e.getMessage());
            subActionReport.setFailureCause(e);
        }
        return result;
    }

    private class SupplementalCommand {
        private AdminCommand command;
        private Supplemental.Timing timing;
        private FailurePolicy failurePolicy;
        private List<RuntimeType> whereToRun = new ArrayList<RuntimeType>();

        public SupplementalCommand(AdminCommand cmd, Supplemental.Timing time, FailurePolicy onFail) {
            command = cmd;
            timing = time;
            failurePolicy = onFail;
            org.glassfish.api.admin.ExecuteOn ann =
                    cmd.getClass().getAnnotation(org.glassfish.api.admin.ExecuteOn.class);
            if( ann == null) {
                whereToRun.add(RuntimeType.DAS);
                whereToRun.add(RuntimeType.INSTANCE);
            } else {
                if(ann.value().length == 0) {
                    whereToRun.add(RuntimeType.DAS);
                    whereToRun.add(RuntimeType.INSTANCE);
                } else {
                    for(RuntimeType t : ann.value()) {
                        whereToRun.add(t);
                    }
                }
            }
        }

        public void execute(AdminCommandContext ctxt) {
            command.execute(ctxt);
        }

        public boolean toBeExecutedBefore() {
            return timing.equals(Supplemental.Timing.Before);
        }

        public boolean toBeExecutedAfter() {
            return timing.equals(Supplemental.Timing.After);
        }

        public FailurePolicy onFailure() {
            return failurePolicy;
        }

        public List<RuntimeType> whereToRun() {
            return whereToRun;
        }
    }
}
