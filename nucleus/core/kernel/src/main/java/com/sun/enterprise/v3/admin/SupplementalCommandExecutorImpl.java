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

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.hk2.component.InjectionResolver;
import com.sun.logging.LogDomains;
import java.util.Collection;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.Progress;
import org.glassfish.common.util.admin.MapInjectionResolver;
import org.jvnet.hk2.component.*;
import org.glassfish.common.util.admin.CommandModelImpl;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.admin.Supplemental.Timing;
import org.glassfish.internal.api.ServerContext;

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

    @Inject
    private ServerContext sc;

    private static final Logger logger = LogDomains.getLogger(SupplementalCommandExecutorImpl.class,
                                        LogDomains.ADMIN_LOGGER);

    private static final LocalStringManagerImpl strings =
                        new LocalStringManagerImpl(SupplementalCommandExecutor.class);

    private Map<String, List<Inhabitant<? extends Supplemental>>> supplementalCommandsMap = null;
    
    public Collection<SupplementalCommand> listSuplementalCommands(String commandName) {
        List<Inhabitant<? extends Supplemental>> supplementalList = getSupplementalCommandsList().get(commandName);
        if (supplementalList == null) {
            return Collections.EMPTY_LIST;
        }
        Collection<SupplementalCommand> result = new ArrayList<SupplementalCommand>(supplementalList.size());
        for (Inhabitant<? extends Supplemental> inh : supplementalList) {
            AdminCommand cmdObject = (AdminCommand) inh.get();
            SupplementalCommand aCmd = new SupplementalCommandImpl(cmdObject);
            if( (serverEnv.isDas() && aCmd.whereToRun().contains(RuntimeType.DAS)) ||
                (serverEnv.isInstance() && aCmd.whereToRun().contains(RuntimeType.INSTANCE)) ) {
                result.add(aCmd);
            }
        }
        return result;
    }

    @Override
    public ActionReport.ExitCode execute(Collection<SupplementalCommand> suplementals, Supplemental.Timing time,
                             AdminCommandContext context, ParameterMap parameters, 
                             MultiMap<String, File> optionFileMap) {
        //TODO : Use the executor service to parallelize this
        ActionReport.ExitCode finalResult = ActionReport.ExitCode.SUCCESS;
        if (suplementals == null) {
            return finalResult;
        }
        for (SupplementalCommand aCmd : suplementals) {
            if ((time.equals(Supplemental.Timing.Before) && aCmd.toBeExecutedBefore()) ||
                (time.equals(Supplemental.Timing.After) && aCmd.toBeExecutedAfter())   ||
                (time.equals(Supplemental.Timing.AfterReplication) && aCmd.toBeExecutedAfterReplication())) {
                ActionReport.ExitCode result = FailurePolicy.applyFailurePolicy(aCmd.onFailure(),
                        inject(aCmd, getInjector(aCmd.getCommand(), parameters, optionFileMap),
                                context.getActionReport()));
                if(!result.equals(ActionReport.ExitCode.SUCCESS)) {
                    if(finalResult.equals(ActionReport.ExitCode.SUCCESS))
                        finalResult = result;
                    continue;
                }
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(strings.getLocalString("dynamicreconfiguration.diagnostics.supplementalexec",
                            "Executing supplemental command " + aCmd.getClass().getCanonicalName()));
                }
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
        return finalResult;
    }

    /**
     * Get list of all supplemental commands, map it to various commands and cache this list
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

    private InjectionResolver<Param> getInjector(AdminCommand command, ParameterMap parameters, MultiMap<String, File> map) {
        CommandModel model;
        try {
            CommandModelProvider c = CommandModelProvider.class.cast(command);
            model = c.getModel();
        } catch (ClassCastException e) {
            model = new CommandModelImpl(command.getClass());
        }
        return new MapInjectionResolver(model, parameters, map);
    }

    private ActionReport.ExitCode inject(SupplementalCommand cmd, 
            InjectionResolver<Param> injector, ActionReport subActionReport) {
        ActionReport.ExitCode result = ActionReport.ExitCode.SUCCESS;
        try {
            new InjectionManager().inject(cmd.getCommand(), injector);
        } catch (Exception e) {
            result = ActionReport.ExitCode.FAILURE;
            subActionReport.setActionExitCode(result);
            subActionReport.setMessage(e.getMessage());
            subActionReport.setFailureCause(e);
        }
        return result;
    }

    public class SupplementalCommandImpl implements SupplementalCommand  {
        
        private AdminCommand command;
        private Supplemental.Timing timing;
        private FailurePolicy failurePolicy;
        private List<RuntimeType> whereToRun = new ArrayList<RuntimeType>(2);
        private ProgressStatus progressStatus;
        private Progress progressAnnotation;

        private SupplementalCommandImpl(AdminCommand cmd) {
            command = cmd;
            Supplemental supAnn = cmd.getClass().getAnnotation(Supplemental.class);
            timing = supAnn.on(); 
            failurePolicy = supAnn.ifFailure();
            ExecuteOn onAnn = cmd.getClass().getAnnotation(ExecuteOn.class);
            progressAnnotation = cmd.getClass().getAnnotation(Progress.class);
            if (onAnn == null) {
                whereToRun.add(RuntimeType.DAS);
                whereToRun.add(RuntimeType.INSTANCE);
            } else {
                if(onAnn.value().length == 0) {
                    whereToRun.add(RuntimeType.DAS);
                    whereToRun.add(RuntimeType.INSTANCE);
                } else {
                    whereToRun.addAll(Arrays.asList(onAnn.value()));
                }
            }
        }

        @Override
        public void execute(AdminCommandContext ctxt) {
                Thread thread = Thread.currentThread();
                ClassLoader origCL = thread.getContextClassLoader();
                ClassLoader ccl = sc.getCommonClassLoader();
                if (progressStatus != null) {
                    ctxt = new AdminCommandContextForInstance(ctxt, progressStatus);
                }
                if (origCL != ccl) {
                    try {
                        thread.setContextClassLoader(ccl);
                        command.execute(ctxt);
                    } finally {
                        thread.setContextClassLoader(origCL);
                    }
                } else {
                    command.execute(ctxt);
                }
        }
        
        @Override
        public AdminCommand getCommand() {
            return this.command;
        }

        @Override
        public boolean toBeExecutedBefore() {
            return timing.equals(Supplemental.Timing.Before);
        }

        @Override
        public boolean toBeExecutedAfter() {
            return timing.equals(Supplemental.Timing.After);
        }

        @Override
        public boolean toBeExecutedAfterReplication() {
            return timing.equals(Supplemental.Timing.AfterReplication);
        }
        
        @Override
        public FailurePolicy onFailure() {
            return failurePolicy;
        }

        @Override
        public List<RuntimeType> whereToRun() {
            return whereToRun;
        }

        @Override
        public ProgressStatus getProgressStatus() {
            return progressStatus;
        }

        @Override
        public void setProgressStatus(ProgressStatus progressStatus) {
            this.progressStatus = progressStatus;
        }

        @Override
        public Progress getProgressAnnotation() {
            return progressAnnotation;
        }
        
    }
    
}
