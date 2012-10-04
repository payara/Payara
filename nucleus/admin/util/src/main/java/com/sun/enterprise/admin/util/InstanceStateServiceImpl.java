/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.util;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

/**
 * Service that is called at startup and parses the instance state file.
 *
 * @author Vijay Ramachandran
 */
@Service
@RunLevel(value=StartupRunLevel.VAL, mode=RunLevel.RUNLEVEL_MODE_NON_VALIDATING)
public class InstanceStateServiceImpl implements InstanceStateService {

    @Inject
    private ServerEnvironment serverEnv;

    @Inject
    private Domain domain;

    @Inject
    private CommandThreadPool cmdPool;

    private InstanceStateFileProcessor stateProcessor;
    private HashMap<String, InstanceState> instanceStates;
    private final static int MAX_RECORDED_FAILED_COMMANDS = 10;
    private final static Logger logger = AdminLoggerInfo.getLogger();
    
    

    public InstanceStateServiceImpl() {}

    /*
     * Perform lazy-initialization for the object, since this InstanceStateService
     * is not needed if there are not any instances.
     */
    private void init() {
        if (instanceStates != null) {
            return;
        }
        instanceStates = new HashMap<String, InstanceState>();
        File stateFile = new File(serverEnv.getConfigDirPath().getAbsolutePath(),
                            ".instancestate");
        try {
            stateProcessor = new InstanceStateFileProcessor(instanceStates,
                        stateFile);
        } catch (IOException ioe) {
            logger.log(Level.FINE, AdminLoggerInfo.mISScannotread, stateFile);
            instanceStates = new HashMap<String, InstanceState>();
            // Even though instances may already exist, do not populate the
            // instancesStates array because it will be repopulated as it is
            // used. Populating it early causes problems during instance
            // creation.
            try {
                stateProcessor = InstanceStateFileProcessor.createNew(instanceStates, stateFile);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, AdminLoggerInfo.mISScannotcreate, 
                        new Object[] { stateFile, ex.getLocalizedMessage()} );
                stateProcessor = null;
            }
        }
    }

    @Override
    public synchronized void addServerToStateService(String instanceName) {
        init();
        instanceStates.put(instanceName, new InstanceState(InstanceState.StateType.NEVER_STARTED));
        try {
            stateProcessor.addNewServer(instanceName);
        } catch (Exception e) {
            logger.log(Level.SEVERE, AdminLoggerInfo.mISSaddstateerror, e.getLocalizedMessage());
        }
    }

    @Override
    public synchronized void addFailedCommandToInstance(String instance, String cmd, ParameterMap params) {
        init();
        String cmdDetails = cmd;
        String defArg = params.getOne("DEFAULT");
        if (defArg != null) {
            cmdDetails += " " + defArg;
        }

        try {
            InstanceState i = instanceStates.get(instance);
            if (i != null && i.getState() != InstanceState.StateType.NEVER_STARTED &&
                    i.getFailedCommands().size() < MAX_RECORDED_FAILED_COMMANDS) {
                i.addFailedCommands(cmdDetails);
                stateProcessor.addFailedCommand(instance, cmdDetails);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, AdminLoggerInfo.mISSaddcmderror, e.getLocalizedMessage());
        }
    }

    @Override
    public synchronized void removeFailedCommandsForInstance(String instance) {
        init();
        try {
            InstanceState i = instanceStates.get(instance);
            if(i != null) {
                i.removeFailedCommands();
                stateProcessor.removeFailedCommands(instance);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, AdminLoggerInfo.mISSremcmderror, e.getLocalizedMessage());
        }
    }

    @Override
    public synchronized InstanceState.StateType getState(String instanceName) {
        init();
        InstanceState s = instanceStates.get(instanceName);
        if (s == null) {
            return InstanceState.StateType.NEVER_STARTED;
        }
        return s.getState();
    }

    @Override
    public synchronized List<String> getFailedCommands(String instanceName) {
        init();
        InstanceState s = instanceStates.get(instanceName);
        if(s == null) {
            return new ArrayList<String>();
        }
        return s.getFailedCommands();
    }

    @Override
    public synchronized InstanceState.StateType setState(String name, InstanceState.StateType newState, boolean force) {
        init();
        boolean updateXML = false;
        InstanceState.StateType ret = newState;
        InstanceState is = instanceStates.get(name);
        InstanceState.StateType currState;
        if (is == null || (currState = is.getState()) == null) {
            instanceStates.put(name, new InstanceState(newState));
            updateXML = true;
            ret = newState;
        } else if (!force && currState == InstanceState.StateType.RESTART_REQUIRED) {
            // If current state is RESTART_REQUIRED, no updates to state is allowed because
            // only an instance restart can move this instance out of RESTART_REQD state
            updateXML = false;
            ret = currState;
        } else if (!force && currState == InstanceState.StateType.NEVER_STARTED &&
                    (newState == InstanceState.StateType.NOT_RUNNING ||
                     newState == InstanceState.StateType.RESTART_REQUIRED ||
                     newState == InstanceState.StateType.NO_RESPONSE)) {
            // invalid state change
            updateXML = false;
            ret = currState;
        } else if (!currState.equals(newState)) {
            instanceStates.get(name).setState(newState);
            updateXML = true;
            ret = newState;
        }

        try {
            if (updateXML) {
                stateProcessor.updateState(name, newState.getDescription());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, AdminLoggerInfo.mISSsetstateerror, e.getLocalizedMessage());
        }
        return ret;
    }

    @Override
    public synchronized void removeInstanceFromStateService(String name) {
        init();
        instanceStates.remove(name);
        try {
            stateProcessor.removeInstanceNode(name);
        } catch (Exception e) {
            logger.log(Level.SEVERE, AdminLoggerInfo.mISSremstateerror, e.getLocalizedMessage());
        }
    }

    /*
     * For now, this just submits the job directly to the pool.  In the future
     * it might be possible to avoid submitting the job
     */
    @Override
    public Future<InstanceCommandResult> submitJob(Server server, InstanceCommand ice, InstanceCommandResult r) {
        return cmdPool.submitJob(ice, r);
    }
}
