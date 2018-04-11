/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ant.embedded.tasks;

import org.apache.tools.ant.BuildException;

import java.util.ArrayList;
import java.util.List;


public class DeployTask extends TaskBase {

    String serverID = Constants.DEFAULT_SERVER_ID;
    String app = null; // a default value?
    List<String> deployParams = new ArrayList();

    public void setServerID(String serverID) {
        this.serverID = serverID;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public void setName(String name) {
        deployParams.add("--name=" + name);
    }

    public void setContextroot(String contextroot) {
        deployParams.add("--contextroot=" + contextroot);
    }

    public void setForce(boolean force) {
        deployParams.add("--force=" + force);
    }


    public void setPrecompilejsp(boolean precompilejsp) {
        deployParams.add("--precompilejsp=" + precompilejsp);
    }

    public void setVerify(boolean verify) {
        deployParams.add("--verify=" + verify);
    }

    public void setCreatetables(boolean createtables) {
        deployParams.add("--createtables=" + createtables);
    }

    public void setDropandcreatetables(boolean dropandcreatetables) {
        deployParams.add("--dropandcreatetables=" + dropandcreatetables);
    }

    public void setUniquetablenames(boolean uniquetablenames) {
        deployParams.add("--uniquetablenames=" + uniquetablenames);
    }

    public void setEnabled(boolean enabled) {
        deployParams.add("--enabled=" + enabled);
    }

    public void setAvailabilityenabled(boolean availabilityenabled) {
        deployParams.add("--availabilityenabled=" + availabilityenabled);
    }

    @Override
    public void setDescription(String description) {
        deployParams.add("--description=" + description);
    }

    public void setVirtualservers(String virtualservers) {
        deployParams.add("--virtualservers=" + virtualservers);
    }

    public void setRetrievestubs(String retrieve) {
        deployParams.add("--retrieve=" + retrieve);
    }

    public void setdbvendorname(String dbvendorname) {
        deployParams.add("--dbvendorname=" + dbvendorname);
    }

    public void setLibraries(String libraries) {
        deployParams.add("--libraries=" + libraries);
    }

    public void setDeploymentPlan(String deploymentplan) {
        deployParams.add("--deploymentplan=" + deploymentplan);
    }

    @Override
    public void execute() throws BuildException {
        try {
            Util.deploy(app, serverID, deployParams);
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }

}
