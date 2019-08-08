/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.boot.runtime;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.embeddable.CommandRunner;

/**
 * Class to hold a list of Boot Commands for execution
 *
 * @author Steve Millidge
 */
public class BootCommands {

    private final List<BootCommand> commands;

    private static final Logger LOGGER = Logger.getLogger(BootCommands.class.getName());

    public BootCommands() {
        commands = new LinkedList<>();
    }
    
    public void add(BootCommand command) {
        commands.add(command);
    }
    
    public void parseCommandScript(File file) throws IOException {
        parseCommandScript(file.toURI().toURL());
    }

    public void parseCommandScript(URL scriptURL) throws IOException {
        try (InputStream scriptStream = scriptURL.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(scriptStream));

            String commandStr = reader.readLine();
            while (commandStr != null) {
                commandStr = commandStr.trim();
                // # is a comment
                if (commandStr.length() > 0 && !commandStr.startsWith("#")) {
                    commandStr = TranslatedConfigView.expandValue(commandStr);
                    String command[] = commandStr.split(" ");
                    if (command.length > 1) {
                        commands.add(new BootCommand(command[0], Arrays.copyOfRange(command, 1, command.length)));
                    } else if (command.length == 1) {
                        commands.add(new BootCommand(command[0]));
                    }
                }
                commandStr = reader.readLine();
            }
        } catch (IOException ex) {
            LOGGER.log(SEVERE, null, ex);
        }
    }

    public boolean executeCommands(CommandRunner runner) {
        return executeCommands(runner, false);
    }
    
    public boolean executeCommands(CommandRunner runner, boolean stopOnFailure) {
        boolean result = true;
        for (BootCommand command : commands) {
            boolean commandResult = command.execute(runner);
            if (stopOnFailure && !commandResult) {
                return commandResult;
            }
            result = commandResult && result;
        }
        return result;
    }

}
