/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.kernel.javaee;

import com.sun.enterprise.config.serverbeans.Server;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.api.naming.NamingObjectProxy;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;

import javax.naming.Context;
import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;


/**
 * Used to register MEJB for MEJB lazy initialization
 */
public class MEJBNamingObjectProxy implements NamingObjectProxy {

    private static final String NON_PORTABLE_MEJB_JNDI_NAME = "ejb/mgmt/MEJB";
    private static final String PORTABLE_MEJB_JNDI_NAME_SHORT = "java:global/mejb/MEJBBean";
    private static final String PORTABLE_MEJB_JNDI_NAME_LONG =
            "java:global/mejb/MEJBBean!org.glassfish.admin.mejb.MEJBHome";

    private static String[] jndiNames = new String[]
            {NON_PORTABLE_MEJB_JNDI_NAME,
                    PORTABLE_MEJB_JNDI_NAME_SHORT,
                    PORTABLE_MEJB_JNDI_NAME_LONG};

    private ServiceLocator habitat;

    private static final Logger _logger = LogDomains.getLogger(
            MEJBNamingObjectProxy.class, LogDomains.EJB_LOGGER);


    public MEJBNamingObjectProxy(ServiceLocator habitat) {
        this.habitat = habitat;
    }

    static String[] getJndiNames() {
        return jndiNames;
    }

    public Object create(Context ic) throws NamingException {

        Object mEJBHome = null;
        try {
            unpublishJndiNames();
            deployMEJB();
            mEJBHome = ic.lookup(NON_PORTABLE_MEJB_JNDI_NAME);
        } catch (NamingException ne) {
            throw ne;
        } catch (Exception e) {
            NamingException namingException =
                    new NamingException(e.getMessage());
            namingException.initCause(e);
            throw namingException;
        }
        return mEJBHome;
    }

    private void unpublishJndiNames() throws NamingException {
        GlassfishNamingManager gfNamingManager = habitat.getService(GlassfishNamingManager.class);
        for (String next : getJndiNames()) {
            gfNamingManager.unpublishObject(next);
        }
    }

    private void deployMEJB() throws IOException {
        _logger.info("Loading MEJB app on JNDI look up");
        ServerContext serverContext = habitat.getService(ServerContext.class);
        File mejbArchive = new File(serverContext.getInstallRoot(),
                "lib/install/applications/mejb.jar");
        DeployCommandParameters deployParams =
                new DeployCommandParameters(mejbArchive);
        String targetName = habitat.<Server>getService(Server.class, ServerEnvironment.DEFAULT_INSTANCE_NAME).getName();
        deployParams.target = targetName;
        deployParams.name = "mejb";
        ActionReport report = habitat.getService(ActionReport.class, "plain");
        Deployment deployment = habitat.getService(Deployment.class);
        ExtendedDeploymentContext dc = deployment.getBuilder(_logger, deployParams, report).source(mejbArchive).build();
        deployment.deploy(dc);

        if (report.getActionExitCode() != ActionReport.ExitCode.SUCCESS) {
            throw new RuntimeException("Failed to deploy MEJB app: " +
                    report.getFailureCause());
        }
    }
}
