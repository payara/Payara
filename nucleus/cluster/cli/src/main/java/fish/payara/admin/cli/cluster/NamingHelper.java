/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.admin.cli.cluster;

import com.sun.enterprise.admin.cli.Environment;
import com.sun.enterprise.admin.cli.ProgramOptions;
import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import org.glassfish.api.admin.CommandException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class for naming instances from the CLI since CLI commands don't have a
 * {@link org.glassfish.api.ExecutionContext} to use with the {@link org.glassfish.api.admin.CommandRunner}
 *
 * @author Andrew Pielage
 */
public class NamingHelper {

    public static List<String> getAllNamesInUse(ProgramOptions programOptions, Environment environment) {
        List<String> namesInUse = new ArrayList<>();

        namesInUse.addAll(getNames(programOptions, environment, "servers"));
        namesInUse.addAll(getNames(programOptions, environment, "configs"));
        namesInUse.addAll(getNames(programOptions, environment, "nodes"));
        namesInUse.addAll(getNames(programOptions, environment, "clusters"));
        namesInUse.addAll(getNames(programOptions, environment, "deployment-groups"));

        return namesInUse;
    }

    /**
     * Helper method that calls the "get" command to retrieve the names of each child item (e.g. each server instance).
     * We specifically use the get command due to its consistent output - calling the individual "list-x" commands is
     * adding a fragile dependency on the text output of those commands.
     * @param programOptions The asadmin options from the invoking CLI command
     * @param environment The server environment from the invoking CLI command
     * @param dottedNameRoot The root of dotted name configs to get e.g. servers, nodes, deployment-groups etc.
     * @return A list of the names of each "item" under the given dotted name root
     */
    private static List<String> getNames(ProgramOptions programOptions, Environment environment, String dottedNameRoot) {
        List<String> names = new ArrayList<>();
        try {
            RemoteCLICommand rc = new RemoteCLICommand("get", programOptions, environment);
            String returnOutput = rc.executeAndReturnOutput("get", dottedNameRoot + ".*.name");
            if (returnOutput != null && !returnOutput.isEmpty()) {
                List<String> splitReturnOutput = Arrays.asList(returnOutput.split("\n"));
                splitReturnOutput.forEach(dottedConfig -> {
                    if (!dottedConfig.contains("system-property")) {
                        // Strip everything before "=" and the trailing "\r"
                        String name = dottedConfig.substring(dottedConfig.indexOf("=") + 1).replace("\r", "");
                        if (!names.contains(name)) {
                            names.add(name);
                        }
                    }
                });
            }
        } catch (CommandException ce) {
            if (!ce.getMessage().equals("remote failure: Dotted name path " + dottedNameRoot + ".*.name not found.")) {
                Logger.getLogger(NamingHelper.class.getName()).log(Level.WARNING, "Error executing command", ce);
            }
        }

        return names;
    }
}
