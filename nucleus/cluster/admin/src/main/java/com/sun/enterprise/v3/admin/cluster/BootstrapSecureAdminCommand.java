/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018-2021] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import static org.glassfish.api.ActionReport.ExitCode.FAILURE;
import static org.glassfish.api.ActionReport.ExitCode.SUCCESS;
import static org.glassfish.api.admin.RestEndpoint.OpType.POST;

import java.io.File;
import java.io.IOException;

import jakarta.inject.Inject;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.Payload;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.PostConstruct;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.config.serverbeans.Domain;

/**
 * Bootstraps secure admin on a new instance by downloading the minimum files
 * required for the client to offer client authentication using a cert.
 * 
 * @author Tim Quinn
 */
@Service(name="_bootstrap-secure-admin")
@PerLookup
@ExecuteOn(value=RuntimeType.DAS)
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=POST, 
        path="_bootstrap-secure-admin", 
        description="_bootstrap-secure-admin")
})
public class BootstrapSecureAdminCommand implements AdminCommand, PostConstruct {

    private static final String DOWNLOADED_FILE_MIME_TYPE = "application/octet-stream";
    private static final String DOWNLOAD_DATA_REQUEST_NAME = "secure-admin";

    private File[] bootstrappedFiles = null;

    @Inject
    private ServerEnvironment serverEnvironment;

    @Override
    public void postConstruct() {
        bootstrappedFiles = new File[] { serverEnvironment.getJKS(), serverEnvironment.getTrustStore() };
    }

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Payload.Outbound outboundPayload = context.getOutboundPayload();
        File instanceRoot = serverEnvironment.getInstanceRoot();

        try {
            for (File bootstrappedFile : bootstrappedFiles) {
                outboundPayload.attachFile(
                    DOWNLOADED_FILE_MIME_TYPE, 
                    instanceRoot.toURI().relativize(bootstrappedFile.toURI()), 
                    DOWNLOAD_DATA_REQUEST_NAME, bootstrappedFile);
            }
            report.setActionExitCode(SUCCESS);
        } catch (IOException ex) {
            report.setActionExitCode(FAILURE);
            report.setFailureCause(ex);
        }
    }
}
