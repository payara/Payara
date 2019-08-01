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

import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.common.util.admin.CommandModelImpl;
import org.glassfish.common.util.admin.MapInjectionResolver;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.kernel.KernelLoggerInfo;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.InjectionManager;
import org.jvnet.hk2.config.InjectionResolver;

/**
 * An executor that executes Supplemental commands means for current command
 *
 * @author Vijay Ramachandran
 */
@Service(name="SupplementalCommandExecutorImpl")
public class SupplementalCommandExecutorImpl implements SupplementalCommandExecutor {

    @Inject
    private ServiceLocator habitat;

    @Inject
    private ServerEnvironment serverEnv;

    @Inject
    private ServerContext sc;
    
    private static final Logger logger = KernelLoggerInfo.getLogger();

    private static final LocalStringManagerImpl strings =
                        new LocalStringManagerImpl(SupplementalCommandExecutor.class);

    private Map<String, List<ServiceHandle<?>>> supplementalCommandsMap = null;
    
    public Collection<SupplementalCommand> listSupplementalCommands(String commandName) {
        List<ServiceHandle<?>> supplementalList = getSupplementalCommandsList().get(commandName);
        if (supplementalList == null) {
            return Collections.emptyList();
        }
        
        Collection<SupplementalCommand> result = new ArrayList<SupplementalCommand>(supplementalList.size());
        for (ServiceHandle<?> handle : supplementalList) {
            AdminCommand cmdObject = (AdminCommand) handle.getService();
            SupplementalCommand aCmd = new SupplementalCommandImpl(cmdObject);
            if( (serverEnv.isDas() && aCmd.whereToRun().contains(RuntimeType.DAS)) ||
                (serverEnv.isInstance() && aCmd.whereToRun().contains(RuntimeType.INSTANCE)) ) {
                result.add(aCmd);
            }
        }
        return result;
    }

    @Override
    public ActionReport.ExitCode execute(Collection<SupplementalCommand> supplementals, Supplemental.Timing time,
                             AdminCommandContext context, ParameterMap parameters, 
                             MultiMap<String, File> optionFileMap) {
        //TODO : Use the executor service to parallelize this
        ActionReport.ExitCode finalResult = ActionReport.ExitCode.SUCCESS;
        if (supplementals == null) {
            return finalResult;
        }
        for (SupplementalCommand aCmd : supplementals) {
            if ((time.equals(Supplemental.Timing.Before) && aCmd.toBeExecutedBefore()) ||
                (time.equals(Supplemental.Timing.After) && aCmd.toBeExecutedAfter())   ||
                (time.equals(Supplemental.Timing.AfterReplication) && aCmd.toBeExecutedAfterReplication())) {
                ActionReport.ExitCode result = FailurePolicy.applyFailurePolicy(aCmd.onFailure(),
                        inject(aCmd, getInjector(aCmd.getCommand(), parameters, optionFileMap, context),
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
    
    private static String getOne(String key, Map<String, List<String>> metadata) {
    	if (key == null || metadata == null) return null;
    	List<String> found = metadata.get(key);
    	if (found == null) return null;
    	
    	if (found.isEmpty()) return null;
    	
    	return found.get(0);
    }

    /**
     * Get list of all supplemental commands, map it to various commands and cache this list
     */
    private synchronized Map<String, List<ServiceHandle<?>>> getSupplementalCommandsList() {
        
        if (supplementalCommandsMap != null) return supplementalCommandsMap;

        supplementalCommandsMap = new ConcurrentHashMap<String, List<ServiceHandle<?>>>();
        List<ServiceHandle<Supplemental>> supplementals = habitat.getAllServiceHandles(Supplemental.class);
        for (ServiceHandle<Supplemental> handle : supplementals) {
            ActiveDescriptor<Supplemental> inh = handle.getActiveDescriptor();
            String commandName = getOne("target", inh.getMetadata());
            if(supplementalCommandsMap.containsKey(commandName)) {
                supplementalCommandsMap.get(commandName).add(handle);
            } else {
                ArrayList<ServiceHandle<?>> inhList =
                        new ArrayList<ServiceHandle<?>>();
                inhList.add(handle);
                supplementalCommandsMap.put(commandName, inhList);
            }
        }
        return supplementalCommandsMap; 
    }

    private InjectionResolver<Param> getInjector(AdminCommand command, ParameterMap parameters, MultiMap<String, File> map, AdminCommandContext context) {
        CommandModel model = command instanceof CommandModelProvider ? 
	    ((CommandModelProvider)command).getModel() :
	    new CommandModelImpl(command.getClass());
        MapInjectionResolver injector = new MapInjectionResolver(model, parameters, map);
        injector.setContext(context);
        return injector;
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
                        if (command instanceof AdminCommandSecurity.Preauthorization) {
                            ((AdminCommandSecurity.Preauthorization) command).preAuthorization(ctxt);
                        }
                        command.execute(ctxt);
                    } finally {
                        thread.setContextClassLoader(origCL);
                    }
                } else {
                    if (command instanceof AdminCommandSecurity.Preauthorization) {
                        ((AdminCommandSecurity.Preauthorization) command).preAuthorization(ctxt);
                    }
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
