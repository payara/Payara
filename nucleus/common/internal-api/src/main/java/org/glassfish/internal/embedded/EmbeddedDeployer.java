/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.internal.embedded;

import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.*;
import org.jvnet.hk2.annotations.Contract;

import java.io.File;

/**
 * Service to deploy applications to the embedded server.
 *
 * @author Jerome Dochez
 */
@Contract
public interface EmbeddedDeployer {


    // todo : is this still used ?

    /**
     * Returns the location of the applications directory, where deployed applications
     * are saved.
     *
     * @return the deployed application directory.
     */
    public File getApplicationsDir();

    /**
     * Returns the location of the auto-deploy directory.
     *
     * @return the auto-deploy directory
     *
     */
    public File getAutoDeployDir();

    /**
     * Enables or disables the auto-deployment feature
     *
     * @param flag set to true to enable, false to disable
     */
    public void setAutoDeploy(boolean flag);

    /**
     * Deploys a file or directory to the servers passing the deployment command parameters
     * Starts the server if it is not started yet.
     *
     * @param archive archive or directory of the application
     * @param params deployment command parameters
     * @return the deployed application name
     */
    public String deploy(File archive, DeployCommandParameters params);

    /**
     * Deploys an archive abstraction to the servers passing the deployment command parameters
     *
     * @param archive archive or directory of the application
     * @param params deployment command parameters
     * @return the deployed application name
     */
    public String deploy(ReadableArchive archive, DeployCommandParameters params);


    // todo : add redeploy ?

    /**
     * Undeploys a previously deployed application
     *
     * @param name name returned by {@link EmbeddedDeployer#deploy(File, org.glassfish.api.deployment.DeployCommandParameters}
     * @param params the undeployment parameters, can be null for default values
     */
    public void undeploy(String name, UndeployCommandParameters params);

    /**
     * Undeploys all deployed applications.
     */
    public void undeployAll();
}
