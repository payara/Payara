/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.module.common_impl.Tokenizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.Inhabitant;
import org.jvnet.hk2.component.Inhabitants;
import org.jvnet.hk2.component.Singleton;

/**
 * Simple admin command to list all existing commands.
 *
 * @author Jerome Dochez
 * 
 */
@Service(name="list-commands")
@Scoped(Singleton.class)        // no per-execution state
@CommandLock(CommandLock.LockType.NONE)
public class ListCommandsCommand implements AdminCommand {

    private static final String DEBUG_PAIR = "mode=debug";
    @Inject
    Habitat habitat;


    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {

        context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);
        ActionReport report = context.getActionReport();
        report.setMessage("List of Commands");
        report.getTopMessagePart().setChildrenType("Command");
        for (String name : sortedAdminCommands()) {
            ActionReport.MessagePart part = report.getTopMessagePart().addChild();
            part.setMessage(name);
        }
    }
    
    private List<String> sortedAdminCommands() {
        List<String> names = new ArrayList<String>();
        for (Inhabitant<?> command : habitat.getInhabitantsByContract(AdminCommand.class.getName())) {
            for (String name : Inhabitants.getNamesFor(command, AdminCommand.class.getName())) {
                //see 6161 -- I thought we should ensure that a command found in habitat should
                //return a valid Command Object, but it was decided that we don't need to instantiate
                //each object satisfying AdminCommand contract to get a list of all commands
                if (debugCommand(command)) { //it's a debug command, add only if debug is set
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
    
    private static boolean debugCommand(Inhabitant command) {
        return !Inhabitants.getNamesFor(command, "mode").isEmpty();
    }
    
    private static boolean metadataContains(String md, String nev) {
        boolean contains = false;
        Tokenizer st = new Tokenizer(md, ","); //TODO
        for (String pair : st) {
            if (pair.trim().equals(nev)) {
                contains = true;
                break;
            }
        }
        return ( contains );
    }
    
    private static boolean debugSet() { //TODO take into a/c debug-enabled?
        String s = System.getenv("AS_DEBUG");
        return ( Boolean.valueOf(s) );
    }
}
