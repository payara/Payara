/*
 *   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *   Copyright (c) [2024] Payara Foundation and/or its affiliates.
 *   All rights reserved.
 *
 *   The contents of this file are subject to the terms of either the GNU
 *   General Public License Version 2 only ("GPL") or the Common Development
 *   and Distribution License("CDDL") (collectively, the "License").  You
 *   may not use this file except in compliance with the License.  You can
 *   obtain a copy of the License at
 *   https://github.com/payara/Payara/blob/master/LICENSE.txt
 *   See the License for the specific
 *   language governing permissions and limitations under the License.
 *
 *   When distributing the software, include this License Header Notice in each
 *   file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *   GPL Classpath Exception:
 *   The Payara Foundation designates this particular file as subject to the
 *   "Classpath" exception as provided by the Payara Foundation in the GPL
 *   Version 2 section of the License file that accompanied this code.
 *
 *   Modifications:
 *   If applicable, add the following below the License Header, with the fields
 *   enclosed by brackets [] replaced by your own identifying information:
 *   "Portions Copyright [year] [name of copyright owner]"
 *
 *   Contributor(s):
 *   If you wish your version of this file to be governed by only the CDDL or
 *   only the GPL Version 2, indicate your decision by adding "[Contributor]
 *   elects to include this software in this distribution under the [CDDL or GPL
 *   Version 2] license."  If you don't indicate a single choice of license, a
 *   recipient has the option to distribute your version of this file under
 *   either the CDDL, the GPL Version 2 or to extend the choice of license to
 *   its licensees as provided above.  However, if you add GPL Version 2 code
 *   and therefore, elected the GPL Version 2 license, then the option applies
 *   only if the new code is made subject to such option by the copyright
 *   holder.
 */

package com.sun.enterprise.v3.admin.commands;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.JavaConfig;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.universal.glassfish.TokenResolver;
import com.sun.enterprise.universal.process.ProcessUtils;
import com.sun.enterprise.util.StringUtils;
import fish.payara.enterprise.config.serverbeans.DeploymentGroup;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service(name = "generate-heap-dump")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("generate.heap.dump")
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.DEPLOYMENT_GROUP})
@ExecuteOn(value = {RuntimeType.INSTANCE}, ifNeverStarted = FailurePolicy.Error)
@RestEndpoints({
        @RestEndpoint(configBean = Cluster.class,
                opType = RestEndpoint.OpType.GET,
                path = "generate-heap-dump",
                description = "Generate Heap Dump",
                params = {
                        @RestParam(name = "target", value = "$parent")
                }),
        @RestEndpoint(configBean = Server.class,
                opType = RestEndpoint.OpType.GET,
                path = "generate-heap-dump",
                description = "Generate Heap Dump",
                params = {
                        @RestParam(name = "target", value = "$parent")
                }),
        @RestEndpoint(configBean = DeploymentGroup.class,
                opType = RestEndpoint.OpType.GET,
                path = "generate-heap-dump",
                description = "Generate Heap Dump",
                params = {
                        @RestParam(name = "target", value = "$parent")
                }),
        @RestEndpoint(configBean = JavaConfig.class,
                opType = RestEndpoint.OpType.GET,
                path = "generate-heap-dump",
                description = "Generate Heap Dump",
                params = {
                        @RestParam(name = "target", value = "$grandparent")
                })
})
@AccessRequired(resource = "domain/jvm", action = "read")
public class GenerateHeapDumpCommand implements AdminCommand {

    Logger LOGGER = Logger.getLogger(GenerateHeapDumpCommand.class.getName());

    @Param(name = "target", optional = true)
    String target;

    @Param(name = "outputDir")
    String outputDir;

    @Param(name = "outputFileName", optional = true)
    String outputFileName;

    public void execute(AdminCommandContext ctx) {
        ActionReport report = ctx.getActionReport();

        if (!StringUtils.ok(outputDir)) {
            report.setMessage("Invalid Output Directory");
            report.setActionExitCode(ExitCode.FAILURE);
            return;
        }

        StringBuilderNewLineAppender stringBuilderNewLineAppender = new StringBuilderNewLineAppender(new StringBuilder());
        if (!StringUtils.ok(outputFileName)) {
            String prefix = StringUtils.ok(target) ? target : "server";
            outputFileName = prefix + "-heap-dump-" + DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ssX").withZone(ZoneOffset.UTC).format(Instant.now());
        }
        try {

            Map<String, String> systemPropsMap = new HashMap<String, String>((Map) (System.getProperties()));
            TokenResolver resolver = new TokenResolver(systemPropsMap);
            String resolvedOutput = resolver.resolve(outputDir);
            if (!resolvedOutput.endsWith("/")) {
                resolvedOutput = resolvedOutput + "/";
            }
            stringBuilderNewLineAppender.append("Resolved the following output directory: " + resolvedOutput);

            Path outputDirPath = Paths.get(resolvedOutput);
            if (!Files.exists(outputDirPath)) {
                report.setMessage("Output directory does not exist!");
                report.setActionExitCode(ExitCode.FAILURE);
                return;
            }


            int pid = ProcessUtils.getPid();
            LOGGER.log(Level.FINE, "Process ID is " + pid);

            Runtime runtime = Runtime.getRuntime();
            boolean error = false;
            int suffix = 0;
            while (true) {
                String finalFileName = outputFileName + (suffix == 0 ? "" : ("(" + suffix + ")"));
                String command = String.format("jcmd %s GC.heap_dump %s%s.hprof", pid, resolvedOutput, finalFileName);
                LOGGER.log(Level.FINE, "Executing the following command: " + command);
                Process process = runtime.exec(command);
                stringBuilderNewLineAppender.append("Generating Heap Dump");
                BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                process.waitFor();
                boolean success = true;
                while ((line = input.readLine()) != null) {
                    if (line.contains("File exists")) {
                        suffix++;
                        stringBuilderNewLineAppender.append("File already exists, incrementing file name");
                        success = false;
                    }

                    if (line.contains("No such file or directory")) {
                        error = true;
                        stringBuilderNewLineAppender.append("Directory does not exist! " + resolvedOutput);
                        break;
                    }
                    LOGGER.log(Level.FINE, line);
                }

                if (success) {
                    stringBuilderNewLineAppender.append("File name is " + finalFileName);
                    break;
                }

            }

            if (error) {
                stringBuilderNewLineAppender.append("Heap Dump could not be created!");
                report.setActionExitCode(ExitCode.FAILURE);
            } else {
                stringBuilderNewLineAppender.append("Heap Dump has been created");
                report.setActionExitCode(ExitCode.SUCCESS);
            }
            report.setMessage(stringBuilderNewLineAppender.toString());
        } catch (IOException | InterruptedException e) {
            report.setMessage("Could not generate the heap dump");
            report.setActionExitCode(ExitCode.FAILURE);
        }

    }
}
