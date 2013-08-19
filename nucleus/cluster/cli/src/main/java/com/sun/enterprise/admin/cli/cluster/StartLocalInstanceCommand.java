/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.cli.cluster;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import com.sun.enterprise.admin.launcher.GFLauncher;
import com.sun.enterprise.admin.launcher.GFLauncherException;
import com.sun.enterprise.admin.launcher.GFLauncherFactory;
import com.sun.enterprise.admin.launcher.GFLauncherInfo;
import com.sun.enterprise.universal.xml.MiniXmlParserException;



import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.*;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.PerLookup;

import com.sun.enterprise.admin.cli.*;
import com.sun.enterprise.util.ObjectAnalyzer;
import static com.sun.enterprise.admin.cli.CLIConstants.*;
import com.sun.enterprise.admin.servermgmt.cli.StartServerCommand;
import com.sun.enterprise.admin.servermgmt.cli.StartServerHelper;

/**
 * Start a local server instance.
 */
@Service(name = "start-local-instance")
@ExecuteOn(RuntimeType.DAS)
@PerLookup
public class StartLocalInstanceCommand extends SynchronizeInstanceCommand
                                        implements StartServerCommand {
    @Param(optional = true, shortName = "v", defaultValue = "false")
    private boolean verbose;

    @Param(optional = true, shortName = "w", defaultValue = "false")
    private boolean watchdog;

    @Param(optional = true, shortName = "d", defaultValue = "false")
    private boolean debug;

    @Param(name = "dry-run", shortName = "n", optional = true,
            defaultValue = "false")
    private boolean dry_run;

    // handled by superclass
    //@Param(name = "instance_name", primary = true, optional = false)
    //private String instanceName0;

    private StartServerHelper helper;

    @Override
    public List<String> getLauncherArgs() {
        return getLauncher().getCommandLine();
    }

    @Override
    public RuntimeType getType() {
         return RuntimeType.INSTANCE;
    }

    @Override
    protected boolean mkdirs(File f) {
        // we definitely do NOT want dirs created for this instance if
        // they don't exist!
        return false;
    }


    @Override
    protected void validate() throws CommandException {
        super.validate();

        File dir = getServerDirs().getServerDir();

        if(!dir.isDirectory())
            throw new CommandException(Strings.get("Instance.noSuchInstance"));
    }

    /**
     */
    @Override
    protected int executeCommand() throws CommandException {

        if (logger.isLoggable(Level.FINER))
            logger.finer(toString());

        if (sync.equals("none")) {
            logger.info(Strings.get("Instance.nosync"));
        } else {
            if (!synchronizeInstance()) {
                File domainXml =
                    new File(new File(instanceDir, "config"), "domain.xml");
                if (!domainXml.exists()) {
                    logger.info(Strings.get("Instance.nodomainxml"));
                    return ERROR;
                }
                logger.info(Strings.get("Instance.syncFailed"));
            }
        }

        try {
                 // createLauncher needs to go before the helper is created!!
            createLauncher();
            final String mpv = getMasterPassword();

            helper = new StartServerHelper(
                        logger,
                        programOpts.isTerse(),
                        getServerDirs(),
                        launcher,
                        mpv,
                        debug);

            if(!helper.prepareForLaunch())
                return ERROR;

            if(dry_run) {
                if (logger.isLoggable(Level.FINE))
                    logger.fine(Strings.get("dry_run_msg"));
                List<String> cmd = getLauncher().getCommandLine();
                StringBuilder sb = new StringBuilder();
                for (String s : cmd) {
                    sb.append(s);
                    sb.append('\n');
                }
                logger.info(sb.toString());
                return SUCCESS;
            }

            getLauncher().launch();

            if (verbose || watchdog) { // we can potentially loop forever here...
                while (true) {
                    int returnValue = getLauncher().getExitValue();

                    switch (returnValue) {
                        case RESTART_NORMAL:
                            logger.info(Strings.get("restart"));
                            break;
                        case RESTART_DEBUG_ON:
                            logger.info(Strings.get("restartChangeDebug", "on"));
                            getInfo().setDebug(true);
                            break;
                        case RESTART_DEBUG_OFF:
                            logger.info(Strings.get("restartChangeDebug", "off"));
                            getInfo().setDebug(false);
                            break;
                        default:
                            return returnValue;
                    }

                    if (env.debug())
                        System.setProperty(CLIConstants.WALL_CLOCK_START_PROP,
                                            "" + System.currentTimeMillis());
                    getLauncher().relaunch();
                }

            } else {
                helper.waitForServer();
                helper.report();
                return SUCCESS;
            }
        } catch (GFLauncherException gfle) {
            throw new CommandException(gfle.getMessage());
        } catch (MiniXmlParserException me) {
            throw new CommandException(me);
        }
    }

    /**
     * Create a launcher for the instance specified by arguments to
     * this command.  The launcher is for a server of the specified type.
     * Sets the launcher and info fields.
     */
    @Override
    public void createLauncher()
                        throws GFLauncherException, MiniXmlParserException {
            setLauncher(GFLauncherFactory.getInstance(getType()));
            setInfo(getLauncher().getInfo());
            getInfo().setInstanceName(instanceName);
            getInfo().setInstanceRootDir(instanceDir);
            getInfo().setVerbose(verbose);
            getInfo().setWatchdog(watchdog);
            getInfo().setDebug(debug);
            getInfo().setRespawnInfo(programOpts.getClassName(),
                            programOpts.getClassPath(),
                            respawnArgs());

            getLauncher().setup();
    }

    /**
     * Return the asadmin command line arguments necessary to
     * start this server instance.
     */
    private String[] respawnArgs() {
        List<String> args = new ArrayList<String>(15);
        args.addAll(Arrays.asList(programOpts.getProgramArguments()));

        // now the start-local-instance specific arguments
        args.add(getName());    // the command name
        args.add("--verbose=" + String.valueOf(verbose));
        args.add("--watchdog=" + String.valueOf(watchdog));
        args.add("--debug=" + String.valueOf(debug));

        // IT 14015
        // We now REQUIRE all restarted instance to do a sync.
        // just stick with the default...
        //args.add("--nosync=" + String.valueOf(nosync));

        if (ok(nodeDir)) {
            args.add("--nodedir");
            args.add(nodeDir);
        }
        if (ok(node)) {
            args.add("--node");
            args.add(node);
        }
        if (ok(instanceName))
            args.add(instanceName);     // the operand

        if (logger.isLoggable(Level.FINER))
            logger.finer("Respawn args: " + args.toString());
        String[] a = new String[args.size()];
        args.toArray(a);
        return a;
    }

    private GFLauncher getLauncher() {
        if(launcher == null)
            throw new RuntimeException(Strings.get("internal.error", "GFLauncher was not initialized"));

        return launcher;
    }
    private void setLauncher(GFLauncher gfl) {
        launcher = gfl;
    }

    private GFLauncherInfo getInfo() {
        if(info == null)
            throw new RuntimeException(Strings.get("internal.error", "GFLauncherInfo was not initialized"));

            return info;
    }

    private void setInfo(GFLauncherInfo inf) {
            info = inf;
    }

    public String toString() {
        return ObjectAnalyzer.toStringWithSuper(this);
    }

    private GFLauncherInfo info;
    private GFLauncher launcher;

}
