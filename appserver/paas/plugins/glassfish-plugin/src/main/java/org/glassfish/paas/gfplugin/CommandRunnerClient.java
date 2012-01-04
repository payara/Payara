/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.gfplugin;

import com.sun.enterprise.admin.cli.CLICommand;
import com.sun.enterprise.admin.cli.Environment;
import com.sun.enterprise.admin.cli.ProgramOptions;
import com.sun.enterprise.admin.cli.remote.RemoteCommand;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.jvnet.hk2.component.Habitat;

import java.io.ByteArrayOutputStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * @author bhavanishankar@java.net
 */
public class CommandRunnerClient implements CommandRunner {

    // CLICommand writes the output to this logger, so capture it into output bytearraystream.
    protected static final Logger logger =
            Logger.getLogger(CLICommand.class.getPackage().getName());

    private Habitat habitat;
    ByteArrayOutputStream output = new ByteArrayOutputStream(); // command output

    public CommandRunnerClient(Habitat habitat) {
        Logger.getLogger("").getHandlers()[0].setLevel(Level.OFF);
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                try {
                    output.write(record.getMessage().getBytes());
                    output.write("\n".getBytes());
                } catch (Exception ex) {
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        };
        logger.addHandler(handler);
        this.habitat = habitat;
    }

    public CommandResult run(String s, String... strings) {
        CommandResult result = null;
        if (s != null) {
            System.out.print("\nPROV/ASSOC/DEPL: CommandRunnerClient running command [" + s + " ");
            if (strings != null) {
                for (String arg : strings) {
                    System.out.print(arg + " ");
                }
            }
            System.out.println("]\n");
        }

        CLICommand cliCommand = getCommand(s);
        int size = strings != null ? strings.length + 1 : 1;
        String[] args = new String[size];
        args[0] = s;
        if (size > 1) {
            System.arraycopy(strings, 0, args, 1, strings.length);
        }
        Throwable t = null;
        try {
            cliCommand.execute(args);
        } catch (Exception ex) {
            t = ex;
        }
        try {
            return buildCommandResult(new String(output.toByteArray()), t);
        } finally {
            output.reset();
        }
    }

    private CLICommand getCommand(String command) {
        CLICommand cliCommand = habitat.getComponent(CLICommand.class, command);
        // since we need to capture the output, we can not use the above, hence re-initialize.
        if (cliCommand == null) {
            try {
                cliCommand = new RemoteCommand(command,
                        habitat.getComponent(ProgramOptions.class),
                        habitat.getComponent(Environment.class));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return cliCommand;
    }


    private CommandResult buildCommandResult(String outputStr, Throwable failureCause) {
        if (outputStr.trim().length() == 0 && failureCause != null) {
            outputStr = failureCause.getMessage();
        }
        return new RemoteCommandResult(failureCause == null ? 0 : 1, outputStr, failureCause);
    }

    public void setTerse(boolean b) {
    }

}
