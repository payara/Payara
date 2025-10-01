/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2025 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.deployment.admin;

import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Server;
import fish.payara.deployment.util.GAVConvertor;
import fish.payara.deployment.util.JavaArchiveUtils;
import fish.payara.deployment.util.URIUtils;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.common.util.admin.ParameterMapExtractor;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Andrew Pielage
 */
@Service(name="deploy-remote-archive")
@PerLookup
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DOMAIN, CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean = Applications.class, opType = RestEndpoint.OpType.POST, path = "deploy-remote-archive"),
    @RestEndpoint(configBean = Cluster.class, opType = RestEndpoint.OpType.POST, path = "deploy-remote-archive", params = {
        @RestParam(name = "target", value = "$parent")
    }),
    @RestEndpoint(configBean = Server.class, opType = RestEndpoint.OpType.POST, path = "deploy-remote-archive", params = {
        @RestParam(name = "target", value = "$parent")
    })
})
public class DeployRemoteArchiveCommand extends DeployCommandParameters implements AdminCommand {

    private final static Logger logger = Logger.getLogger(DeployRemoteArchiveCommand.class.getName());

    @Param(primary = true)
    private String path;

    @Param(name = "additionalRepositories", optional = true, alias = "additionalrepositories")
    private List<String> additionalRepositories;

    @Inject
    ServiceLocator serviceLocator;

    @Override
    public void execute(AdminCommandContext context) {
        CommandRunner commandRunner = serviceLocator.getService(CommandRunner.class);
        ActionReport actionReport = context.getActionReport();

        // Initialise to null so we can do a null check later
        File fileToDeploy = null;

        // Should treat uppercase letters as equivalent to lowercase
        // in schema names (section 3.1 of the RFC 2396)
        String lowerPath = path.toLowerCase();
        // Assume only Http or Https connections are direct URIs
        if (lowerPath.startsWith("http://") || lowerPath.startsWith("https://")) {
            try {
                URI pathURI = new URI(path);
                // Download the file to temp, and return a File object to pass to the deploy command
                fileToDeploy = URIUtils.convertToFile(pathURI);

                // Get file name from URI
                String fileName = new File(pathURI.getPath()).getName();

                // If a name hasn't been provided, get it from the file name
                if (name == null) {
                    name = JavaArchiveUtils.removeJavaArchiveExtension(fileName, true);
                }

                // If a context root hasn't been provided, get it from the file name
                if (contextroot == null) {
                    contextroot = "/" + JavaArchiveUtils.removeJavaArchiveExtension(fileName, true);
                }
            } catch (IOException | URISyntaxException ex) {
                logger.log(Level.SEVERE, ex.getMessage());
                actionReport.setMessage("Exception converting URI to File: " + path);
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }
        } else {
            try {
                // If the path String doesn't start with Http or Https, then assume it's a GAV coordinate
                logger.log(Level.FINE, "Path does not appear to be a URI, will attempt to read as GAV coordinate");

                // Get the URI for the given GAV coordinate
                Entry<String, URI> artefactEntry = GAVConvertor.getArtefactMapEntry(path, additionalRepositories);

                // Download the file to temp, and return a File object to pass to the deploy command
                fileToDeploy = URIUtils.convertToFile(artefactEntry.getValue());

                // If a name hasn't been provided, get it from the artefact URI
                if (name == null) {
                    String fileName = new File(artefactEntry.getValue().getPath()).getName();
                    name = JavaArchiveUtils.removeJavaArchiveExtension(fileName, true);
                }

                // If a context root hasn't been provided, get it from the artefact name
                if (contextroot == null) {
                    contextroot = "/" + artefactEntry.getKey();
                }
            } catch (IOException | URISyntaxException ex) {
                logger.log(Level.SEVERE, ex.getMessage());
                actionReport.setMessage("Exception converting GAV to File: " + path);
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }
        }

        // Only continue if we actually have a file to deploy
        if (fileToDeploy != null) {
            ActionReport subReport = actionReport.addSubActionsReport();
            CommandRunner.CommandInvocation commandInvocation =
                    commandRunner.getCommandInvocation("deploy", subReport, context.getSubject());
            ParameterMap parameters;
            final ParameterMapExtractor extractor = new ParameterMapExtractor(this);
            try {
                List<String> excludedParameters = new ArrayList<>();
                excludedParameters.add("path");
                parameters = extractor.extract(excludedParameters);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
            parameters.add("path", fileToDeploy.getAbsolutePath());

            commandInvocation.parameters(parameters);
            commandInvocation.execute();
        } else {
            actionReport.setMessage("Provided path does not appear to be a valid URI or GAV coordinate: " + path
                    + "\nSee the server log for more details");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }
}
