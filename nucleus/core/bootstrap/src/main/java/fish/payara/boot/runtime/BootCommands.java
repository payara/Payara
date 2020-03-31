/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.glassfish.config.support.TranslatedConfigView;
import fish.payara.asadmin.CommandRunner;
import java.util.function.Consumer;

/**
 * Class to hold a list of Boot Commands for execution
 *
 * @author Steve Millidge
 */
public class BootCommands {

    /**
     * Command flag pattern include 3 groups to parse the command-line flag
     * [^\"']\\S+=[\"'].+?[\"'] e.g --description="results in error"
     * [^\"']\\S+ e.g --enabled=true, --enabled true
     * [\"'].+?[\"'] e.g --description "results in error"
     *
     */
    private static final Pattern COMMAND_FLAG_PATTERN = Pattern.compile("([^\"']\\S+=[\"'].+?[\"']|[^\"']\\S*|[\"'].+?[\"'])\\s*");
    private final List<BootCommand> commands;
    private final List<Consumer<CommandRunner>> commandFunctions;

    private static final Logger LOGGER = Logger.getLogger(BootCommands.class.getName());

    public BootCommands() {
        commands = new LinkedList<>();
        commandFunctions = new ArrayList<>();
    }
    
    public void add(BootCommand command) {
        commands.add(command);
    }
    
    /**
     * Adds a function that includes commands.
     * @param command A function that will use the CommandRunner to execute one
     * or more commands
     */
    public void addFunction(Consumer<CommandRunner> command) {
        commandFunctions.add(command);
    }

    public List<BootCommand> getCommands() {
        return commands;
    }

    public void parseCommandScript(File file) throws IOException {
        parseCommandScript(file.toURI().toURL());
    }

    public void parseCommandScript(URL scriptURL) throws IOException {
        try (InputStream scriptStream = scriptURL.openStream()) {
            Reader reader = new InputStreamReader(scriptStream);
            parseCommandScript(reader);
        } catch (IOException ex) {
            LOGGER.log(SEVERE, null, ex);
        }
    }

    void parseCommandScript(Reader reader) throws IOException {
        BufferedReader bufferReader = new BufferedReader(reader);
        String commandStr = bufferReader.readLine();
        while (commandStr != null) {
            commandStr = commandStr.trim();
            // # is a comment
            if (commandStr.length() > 0 && !commandStr.startsWith("#")) {
                commandStr = TranslatedConfigView.expandValue(commandStr);
                String[] command;
                List<String> elements = new ArrayList<>();
                Matcher flagMatcher = COMMAND_FLAG_PATTERN.matcher(commandStr);
                while (flagMatcher.find()) {
                    elements.add(flagMatcher.group(1));
                }
                command = elements.toArray(new String[elements.size()]);
                if (command.length > 1) {
                    commands.add(new BootCommand(command[0], Arrays.copyOfRange(command, 1, command.length)));
                } else if (command.length == 1) {
                    commands.add(new BootCommand(command[0]));
                }
            }
            commandStr = bufferReader.readLine();
        }
    }

    /**
     * Executes all stored commands.
     * This is equivalent to {@code executeCommands(CommandRunner, false).
     * @param runner CommandRunner to use
     * @return If all commands executed successfully
     * @see #executeCommands(fish.payara.asadmin.CommandRunner, boolean) 
     */
    public boolean executeCommands(CommandRunner runner) {
        return executeCommands(runner, false);
    }
    
    /**
     * Execute all stored commands
     * @param runner CommandRunner to use
     * @param stopOnFailure
     * @return If all commands executed successfully
     */
    public boolean executeCommands(CommandRunner runner, boolean stopOnFailure) {
        boolean result = true;
        for (BootCommand command : commands) {
            boolean commandResult = command.execute(runner);
            if (stopOnFailure && !commandResult) {
                return commandResult;
            }
            result = commandResult && result;
        }
        for (Consumer<CommandRunner> commandFunction : commandFunctions) {
            commandFunction.accept(runner);
        }
        return result;
    }

}
