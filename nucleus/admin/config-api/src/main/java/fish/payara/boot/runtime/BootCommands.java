/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2022] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
import org.glassfish.embeddable.CommandRunner;

/**
 * Class to hold a list of Boot Commands for execution
 *
 * @author Steve Millidge
 */
public class BootCommands {

    /**
     * Command flag pattern include 3 groups to parse the command-line flag
     * Double and Single Quotes can be included in properties if they are escaped.
     * This is achieved by the following flag pattern: (?:(?!(?<!\\)["']).)*
     *
     * [^"']\S+=["'](?:(?!(?<!\\)["']).)*["'] e.g --description="results \"in\" error"
     * [^\"']\\S+ e.g --enabled=true, --enabled true
     * [^"']\S+=["'](?:(?!(?<!\\)["']).)*["'] e.g --description "results \"in\" error"
     *
     */
    private static final Pattern COMMAND_FLAG_PATTERN = Pattern.compile("([^\"']\\S+=[\"'](?:(?!(?<!\\\\)[\"']).)*[\"']|" +
                                                                        "[^\"']\\S*|" +
                                                                        "[\"'](?:(?!(?<!\\\\)\").)*[\"'])\\s*");
    private final List<BootCommand> commands;

    private static final Logger LOGGER = Logger.getLogger(BootCommands.class.getName());

    public BootCommands() {
        commands = new LinkedList<>();
    }
    
    public void add(BootCommand command) {
        commands.add(command);
    }

    public List<BootCommand> getCommands() {
        return commands;
    }

    /**
     * Parse the given pre-boot, post-boot, or post-deploy command file, optionally performing variable expansion.
     *
     * @param file The {@link File} to parse commands from
     * @param expandValues Whether variable expansion should be performed - cannot be done during pre-boot
     * @throws IOException
     */
    public void parseCommandScript(File file, boolean expandValues) throws IOException {
        parseCommandScript(file.toURI().toURL(), expandValues);
    }

    /**
     * Parse the given pre-boot, post-boot, or post-deploy command file, optionally performing variable expansion.
     *
     * @param scriptURL The {@link URL} to parse commands from
     * @param expandValues Whether variable expansion should be performed - cannot be done during pre-boot
     * @throws IOException
     */
    public void parseCommandScript(URL scriptURL, boolean expandValues) throws IOException {
        try (InputStream scriptStream = scriptURL.openStream()) {
            Reader reader = new InputStreamReader(scriptStream);
            parseCommandScript(reader, expandValues);
        } catch (IOException ex) {
            LOGGER.log(SEVERE, null, ex);
        }
    }

    /**
     * Parse the given pre-boot, post-boot, or post-deploy command file, optionally performing variable expansion.
     *
     * @param reader The {@link Reader} to parse commands from
     * @param expandValues Whether variable expansion should be performed - cannot be done during pre-boot
     * @throws IOException
     */
    void parseCommandScript(Reader reader, boolean expandValues) throws IOException {
        BufferedReader bufferReader = new BufferedReader(reader);
        String commandStr = bufferReader.readLine();
        while (commandStr != null) {
            commandStr = commandStr.trim();
            // # is a comment
            if (commandStr.length() > 0 && !commandStr.startsWith("#")) {
                // Variable expansion cannot be done during pre-boot since the required services won't have started yet
                if (expandValues) {
                    commandStr = TranslatedConfigView.expandValue(commandStr);
                }
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
