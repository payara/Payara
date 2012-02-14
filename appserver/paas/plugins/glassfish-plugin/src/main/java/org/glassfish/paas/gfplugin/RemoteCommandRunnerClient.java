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

package org.glassfish.paas.gfplugin;

import com.sun.enterprise.util.OS;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.virtualization.spi.VirtualMachine;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Until the CommandRunnerClient supports connecting to remote DAS having
 * secure-admin enabled, this class can be used to execute the commands
 * directly in the remote machine via SSH.
 * <p/>
 * In general, this can also be used to execute local commands in the remote DAS
 * eg., start-database, start-domain are local commands.
 *
 * @author Bhavanishankar S
 */

public class RemoteCommandRunnerClient implements CommandRunner {

    private VirtualMachine vm;
    private String adminPort;

    private static final MessageFormat ASADMIN_COMMAND = new MessageFormat(
            "{0}" + File.separator + "lib" + File.separator + "nadmin" +
                    (OS.isWindows() ? ".bat" : "")); // {0} must be install root.

    public RemoteCommandRunnerClient(Properties glassfishProperties) {
        vm = (VirtualMachine) glassfishProperties.get("vm");
        this.adminPort = glassfishProperties.getProperty("port");
    }

    /**
     * {@inheritDoc}
     */
    public CommandResult run(String command, String... args) {
        List<String> argsAsList = new ArrayList<String>();
        Collections.addAll(argsAsList, args);

        List<String> allArgs = new ArrayList<String>();

        String[] installDir = {vm.getProperty(VirtualMachine.PropertyName.INSTALL_DIR) +
                File.separator + "glassfish"};
        allArgs.add(ASADMIN_COMMAND.format(installDir).toString());

        // Separate out the program options
        allArgs.add("--host=" + vm.getAddress().getHostAddress());

        allArgs.add("--port=" + adminPort);

        String userArg = removeArg(argsAsList, "--user");
        if (userArg != null) {
            allArgs.add(userArg);
        }

        String interactiveArg = removeArg(argsAsList, "--interactive");
        if (interactiveArg != null) {
            allArgs.add(interactiveArg);
        }

        // parse --passwordfile program option
        String passfile = removeArg(argsAsList, "--passwordfile");
        if (passfile != null) { // transfer the password file to the vm
            File passwdFile = new File(passfile.substring(passfile.indexOf('=') + 1));
            try {
                vm.executeOn(new String[]{"mkdir", "-p", "/tmp/passwd"});
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            vm.upload(passwdFile, new File("/tmp/passwd"));
            allArgs.add("--passwordfile=/tmp/passwd/" + passwdFile.getName());
        }

        // append the command and other arguments.
        allArgs.add(command);
        allArgs.addAll(argsAsList);

        try {
            String output = vm.executeOn(allArgs.toArray(new String[]{}));
            System.out.println("\nRan command :-> " +
                    allArgs.toString().replace(',', ' ') + "\n\noutput :-> " + output);
            if (output.indexOf("successfully") != -1) {
                return new RemoteCommandResult(0, output, null);
            } else {
                return new RemoteCommandResult(1, output, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new RemoteCommandResult(1, e.getMessage(), e);
        }
    }

    // remove the specified argument and return it
    private String removeArg(List<String> args, String argName) {
        List<Integer> removeIndexes = new ArrayList<Integer>();
        String value = null;
        for (String arg : args) {
            if (arg.startsWith(argName)) {
                int index = args.indexOf(arg);
                if (arg.indexOf("=") == -1) { // two consecutive strings form one argument.
                    removeIndexes.add(index);
                    removeIndexes.add(index + 1);
                    value = args.get(index) + "=" + args.get(index + 1);
                } else { // string represents complete argument with the value.
                    removeIndexes.add(index);
                    value = arg.trim();
                }
            }
        }
        for (int index : removeIndexes) {
            args.remove(index);
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public void setTerse(boolean terse) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
