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

package com.sun.enterprise.v3.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

/**
 * Simple admin command to list all existing commands.
 *
 * @author Jerome Dochez
 * 
 */
@Service(name="list-commands")
@Singleton        // no per-execution state
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-commands", 
        description="list-commands")
})
@AccessRequired(resource="domain", action="read")
public class ListCommandsCommand implements AdminCommand {
    private static final String MODE = "mode";
    private static final String DEBUG = "debug";

    @Inject
    ServiceLocator habitat;
    

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    @Override
    public void execute(AdminCommandContext context) {

        context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);
        ActionReport report = context.getActionReport();
        report.getTopMessagePart().setChildrenType("Command");
        for (String name : sortedAdminCommands()) {
            ActionReport.MessagePart part = report.getTopMessagePart().addChild();
            part.setMessage(name);
        }
    }
    
    protected String getScope() {
        return null;
    }
    
    private List<String> sortedAdminCommands() {
        String scope = getScope();
        List<String> names = new ArrayList<String>();
        for (ServiceHandle<?> command : habitat.getAllServiceHandles(AdminCommand.class)) {
            String name = command.getActiveDescriptor().getName() ;
                //see 6161 -- I thought we should ensure that a command found in habitat should
                //return a valid Command Object, but it was decided that we don't need to instantiate
                //each object satisfying AdminCommand contract to get a list of all commands
                
                // limit list to commands for current scope
            if (name != null) {
                int ci = name.indexOf("/");
                if (ci != -1) {
                    String cmdScope = name.substring(0, ci + 1);
                    if (scope == null || !cmdScope.equals(scope)) continue;
                    name = name.substring(ci + 1);
                } else {
                    if (scope != null) continue;
                   }

               if (debugCommand(command) ) { //it's a debug command, add only if debug is set
                    if (debugSet())
                        names.add(name);
                } else { //always add non-debug commands     \
                    names.add(name);
                }
            }
        }
        Collections.sort(names);
        return (names);

    }
    
    private static boolean debugCommand(ServiceHandle<?> command) {
        ActiveDescriptor<?> ad = command.getActiveDescriptor();
        Map<String, List<String>> metadata = ad.getMetadata();
        
        List<String> modes = metadata.get(MODE);
        if (modes == null) return false;
        
        for (String mode : modes) {
            if (DEBUG.equals(mode)) return true;
        }
        
        return false;
    }
    
    private static boolean debugSet() { //TODO take into a/c debug-enabled?
        String s = System.getenv("AS_DEBUG");
        return ( Boolean.valueOf(s) );
    }
}
