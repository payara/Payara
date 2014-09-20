/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.generator.client;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.LocalStringManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.Payload;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoint.OpType;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

import javax.inject.Inject;
import org.glassfish.api.admin.AccessRequired;

/**
 *
 * @author jdlee
 */
// TODO: This command is not quite ready yet, so we disable it until
//@Service(name = "__generate-rest-client")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS,
    CommandTarget.STANDALONE_INSTANCE,
    CommandTarget.CLUSTER,
    CommandTarget.CONFIG,
    CommandTarget.CLUSTERED_INSTANCE})
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=OpType.GET,
        path="client",
        description="Generate REST client")
})
@AccessRequired(resource="domain/rest-client", action="read")
public class GenerateClientCommand implements AdminCommand {
    @Inject
    ServiceLocator habitat;

    @Param
    private String outputDir;

    @Param(shortName="lang", optional=true, defaultValue="java")
    private String languages;

    private final static LocalStringManager localStrings =
            new LocalStringManagerImpl(GenerateClientCommand.class);

    @Override
    public void execute(AdminCommandContext context) {
        List<ClientGenerator> generators = new ArrayList<ClientGenerator>();

        for (String lang : languages.split(",")) {
            ClientGenerator gen = null;
            if ("java".equalsIgnoreCase(lang)) {
                gen = new JavaClientGenerator(habitat);
            } else if ("python".equalsIgnoreCase(lang)) {
                gen = new PythonClientGenerator(habitat);
            }

            if (gen != null) {
                generators.add(gen);
                gen.generateClasses();
            }
        }

        Logger logger = context.getLogger();
        try {
            Payload.Outbound outboundPayload = context.getOutboundPayload();
            Properties props = new Properties();
            /*
             * file-xfer-root is used as a URI, so convert backslashes.
             */
            props.setProperty("file-xfer-root", outputDir.replace('\\', '/'));
            for (ClientGenerator gen : generators) {
                for (Map.Entry<String, URI> entry : gen.getArtifact().entrySet()) {
                    final URI artifact = entry.getValue();
                    outboundPayload.attachFile("application/octet-stream",
                            new URI(entry.getKey()), "files", props, new File(artifact));
                }
                List<String> messages = gen.getMessages();
                if (!messages.isEmpty()) {
                    ActionReport ar = context.getActionReport();
                    for (String msg : messages) {
                        ar.addSubActionsReport().appendMessage(msg);
                    }
                }
            }
        } catch (Exception e) {
            final String errorMsg = localStrings.getLocalString(
                    "download.errDownloading", "Error while downloading generated files");
            logger.log(Level.SEVERE, errorMsg, e);
            ActionReport report = context.getActionReport();

            report = report.addSubActionsReport();
            report.setActionExitCode(ExitCode.WARNING);
            report.setMessage(errorMsg);
            report.setFailureCause(e);
        }
    }
}
