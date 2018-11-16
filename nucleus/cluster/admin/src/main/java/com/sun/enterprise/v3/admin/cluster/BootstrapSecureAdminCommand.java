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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.config.serverbeans.Domain;
import java.io.File;
import java.io.IOException;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.PostConstruct;

/**
 * Bootstraps secure admin on a new instance by downloading the minimum files
 * required for the client to offer client authentication using a cert.
 * 
 * @author Tim Quinn
 */
@Service(name="_bootstrap-secure-admin")
@PerLookup
@ExecuteOn(value={RuntimeType.DAS})
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.POST, 
        path="_bootstrap-secure-admin", 
        description="_bootstrap-secure-admin")
})
public class BootstrapSecureAdminCommand implements AdminCommand, PostConstruct {

    private static final String DOWNLOADED_FILE_MIME_TYPE = "application/octet-stream";
    private static final String DOWNLOAD_DATA_REQUEST_NAME = "secure-admin";

    private File[] bootstrappedFiles = null;

    @Inject
    private ServerEnvironment env;
    
    
    @Override
    public void postConstruct() {
        bootstrappedFiles = new File[] {
            env.getJKS(),
            env.getTrustStore()
                };
    }

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        final Payload.Outbound outboundPayload = context.getOutboundPayload();
        final File instanceRoot = env.getInstanceRoot();

        try {
            for (File f : bootstrappedFiles) {
                outboundPayload.attachFile(
                        DOWNLOADED_FILE_MIME_TYPE,
                        instanceRoot.toURI().relativize(f.toURI()),
                        DOWNLOAD_DATA_REQUEST_NAME,
                        f);
            }
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (IOException ex) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(ex);
        }
    }
}
