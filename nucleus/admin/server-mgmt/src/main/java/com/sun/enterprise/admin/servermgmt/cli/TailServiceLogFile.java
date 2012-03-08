/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.servermgmt.cli;

import com.sun.enterprise.admin.cli.CLICommand;
import com.sun.enterprise.admin.cli.remote.RemoteCommand;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PerLookup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: naman
 * Date: 24/2/12
 * Time: 11:38 AM
 * To change this template use File | Settings | File Templates.
 */
@org.jvnet.hk2.annotations.Service(name = "tail-service-log-file")
@Scoped(PerLookup.class)
public class TailServiceLogFile extends CLICommand {

    private String commandName = "_hidden-tail-service-log-file";

    @Param(name = "filepointer", optional = true, defaultValue = "0")
    private String filepointer;

    @Param(name = "servicename", optional = false)
    private String serviceName;

    @Param(name = "logtype", optional = false)
    private String logtype;

    @Override
    protected int executeCommand() throws CommandException {

        while (true) {
            RemoteCommand cmd = new RemoteCommand(commandName, programOpts, env);
            Map<String, String> attr = cmd.executeAndReturnAttributes(getParams());

            String fileData = attr.get("filedata_value");
            String filePointer = attr.get("filepointer_value");

            if (fileData != null && fileData.trim().length() > 0) {
                System.out.println(fileData);
            }
            this.filepointer = filePointer;
        }
    }

private String[] getParams() {
        List<String> ss = new ArrayList<String>();

        ss.add(commandName);
        ss.add("--filepointer");
        ss.add(filepointer);
        ss.add("--serviceName");
        ss.add(serviceName);
        ss.add("--logtype");
        ss.add(logtype);
        return ss.toArray(new String[ss.size()]);
    }
}
