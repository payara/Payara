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

import com.sun.enterprise.admin.cli.Environment;
import com.sun.enterprise.admin.cli.ProgramOptions;
import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import java.util.TimerTask;
import java.util.Timer;
import java.util.logging.Logger;
import java.io.File;

import org.glassfish.api.admin.*;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import static com.sun.enterprise.admin.servermgmt.SLogger.*;
public class MonitorTask extends TimerTask {
    private String type = null;
    private String filter = null;
    private Timer timer = null;
    private String[] remoteArgs;
    private String exceptionMessage = null;
    private RemoteCLICommand cmd;
    private static final int NUM_ROWS = 25;
    private int counter = 0;
    private static final Logger logger = getLogger();
    private final static LocalStringsImpl strings =
            new LocalStringsImpl(MonitorTask.class);
    volatile Boolean allOK = null;

    public MonitorTask(final Timer timer, final String[] remoteArgs,
            ProgramOptions programOpts, Environment env,
            final String type, final String filter, final File fileName)
            throws CommandException, CommandValidationException {
        this.timer = timer;
        if ((type != null) && (type.length() > 0))
            this.type = type;
        if ((filter != null) && (filter.length() > 0))
            this.filter = filter;
        this.remoteArgs = remoteArgs;
        cmd = new RemoteCLICommand(remoteArgs[0], programOpts, env);
        displayHeader(type);

    }

    void displayHeader(String type) {
        // print title
        String title = "";
        if ("servlet".equals(type)) {
            title = String.format("%1$-10s %2$-10s %3$-10s",
                    "aslc", "mslc", "tslc");
        }
        else if ("httplistener".equals(type)) {
            title = String.format("%1$-4s %2$-4s %3$-6s %4$-4s",
                    "ec", "mt", "pt", "rc");
        }
        else if ("jvm".equals(type)) {
            title = String.format("%1$45s", MONITOR_TITLE);
            logger.info(title);
            // row title
            title = null;
            if (filter != null) {
                if (("heapmemory".equals(filter))
                        || ("nonheapmemory".equals(filter))) {
                    title = String.format("%1$-10s %2$-10s %3$-10s %4$-10s",
                            "init", "used", "committed", "max");
                }
            }
            if (title == null) {
                // default jvm stats
                title = String.format("%1$-35s %2$-40s",
                        MONITOR_UPTIME_TITLE, MONITOR_MEMORY_TITLE);
                logger.info(title);
                title = String.format(
                        "%1$-25s %2$-10s %3$-10s %4$-10s %5$-10s %6$-10s",
                        strings.get("monitor.jvm.current"), strings.get("monitor.jvm.min"),
                        strings.get("monitor.jvm.max"), strings.get("monitor.jvm.low"),
                        strings.get("monitor.jvm.high"), strings.get("monitor.jvm.count"));
            }
        }
        else if ("webmodule".equals(type)) {
            title = String.format(
                    "%1$-5s %2$-5s %3$-5s %4$-5s %5$-5s %6$-5s %7$-5s %8$-8s %9$-10s %10$-5s",
                    "asc", "ast", "rst", "st", "ajlc", "mjlc", "tjlc", "aslc", "mslc", "tslc");
        }
        logger.info(title);
    }

    void cancelMonitorTask() {
        timer.cancel();
    }

    public void run() {
        try {
            cmd.execute(remoteArgs);
            allOK = true;
            if (counter == NUM_ROWS) {
                displayHeader(type);
                counter = 0;
            }
            counter++;
        }
        catch (Exception e) {
            //logger.severe(
            //strings.get("monitorCommand.errorRemote", e.getMessage()));
            allOK = false;
            cancelMonitorTask();
            exceptionMessage = e.getMessage();
        }
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void displayDetails() {
        String details = "";
        if ("servlet".equals(type)) {
            details = strings.get("commands.monitor.servlet_detail");
        }
        else if ("httplistener".equals(type)) {
            details = strings.get("commands.monitor.httplistener_detail");
        }
        else if ("jvm".equals(type)) {
            //no details
        }
        else if ("webmodule".equals(type)) {
            details = strings.get("commands.monitor.webmodule_virtual_server_detail");
        }
        logger.info(details);
    }
    /*
    synchronized void writeToFile(final String text) {
    try {
    BufferedWriter out =
    new BufferedWriter(new FileWriter(fileName, true));
    out.append(text);
    out.newLine();
    out.close();
    } catch (IOException ioe) {
    final String unableToWriteFile =
    strings.getString("commands.monitor.unable_to_write_to_file",
    fileName.getName());
    logger.info(unableToWriteFile);
    //if (verbose) {
    //ioe.printStackTrace();
    //}
    }
    }
     */
}
