/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
import java.io.*;
import java.util.*;
import org.jvnet.hk2.annotations.*;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.PerLookup;

import com.sun.enterprise.universal.i18n.LocalStringsImpl;

/**
 * A local Monitor Command (this will call the remote 'monitor' command).
 * The reason for having to implement this as local is to interpret the options
 * --interval and --filename(TBD) options.
 *
 * @author Prashanth
 * @author Bill Shannon
 */
@Service(name = "monitor")
@PerLookup
public class MonitorCommand extends CLICommand {
    @Param(optional = true, defaultValue = "30")
    private int interval = 30;	// default 30 seconds
    @Param
    private String type;
    @Param(optional = true)
    private String filter;
    @Param(optional = true)
    private File fileName;
    @Param(primary = true, optional = true)
    private String target;	// XXX - not currently used
    private static final LocalStringsImpl strings =
            new LocalStringsImpl(MonitorCommand.class);

    @Override
    protected int executeCommand()
            throws CommandException, CommandValidationException {
        // Based on interval, loop the subject to print the output
        Timer timer = new Timer();
        try {
            MonitorTask monitorTask = new MonitorTask(timer, getRemoteArgs(),
                    programOpts, env, type, filter, fileName);
            timer.scheduleAtFixedRate(monitorTask, 0, (long) interval * 1000);
            boolean done = false;
            final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

            while (!done) {
                String str = "";

                if (monitorTask.allOK == null)
                    str = ""; // not ready yet
                else if (monitorTask.allOK == false)
                    str = "Q";
                else if (System.in.available() > 0)
                    str = in.readLine();

                if (str == null || str.equals("q") || str.equals("Q")) {
                    timer.cancel();
                    done = true;
                    String exceptionMessage = monitorTask.getExceptionMessage();
                    if (exceptionMessage != null) {
                        throw new CommandException(exceptionMessage);
                    }
                }
                else if (str.equals("h") || str.equals("H")) {
                    monitorTask.displayDetails();
                }
            }
            try {
                Thread.sleep(500);
            }
            catch(Exception e) {
                // ignore
            }
        }
        catch (Exception e) {
            timer.cancel();
            throw new CommandException(
                    strings.get("monitorCommand.errorRemote", e.getMessage()));
        }
        return 0;
    }

    private String[] getRemoteArgs() {
        List<String> list = new ArrayList<String>(5);
        list.add("monitor");

        if (ok(type)) {
            list.add("--type");
            list.add(type);
        }
        if (ok(filter)) {
            list.add("--filter");
            list.add(filter);
        }
        return list.toArray(new String[list.size()]);
    }
}
