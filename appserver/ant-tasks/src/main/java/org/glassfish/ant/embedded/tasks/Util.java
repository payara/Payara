/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Util {

    private final static Map<String, GlassFish> gfMap =
            new HashMap<String, GlassFish>();

    private static GlassFishRuntime glassfishRuntime;

    public static synchronized GlassFish startGlassFish(String serverID, String installRoot,
                                                        String instanceRoot, String configFileURI,
                                                        boolean configFileReadOnly, int httpPort)
            throws GlassFishException {
        GlassFish glassfish = gfMap.get(serverID);
        if (glassfish != null) {
            return glassfish;
        }
        if (glassfishRuntime == null) {
            BootstrapProperties bootstrapProperties = new BootstrapProperties();
            if (installRoot != null) {
                bootstrapProperties.setInstallRoot(installRoot);
            }
            glassfishRuntime = GlassFishRuntime.bootstrap(bootstrapProperties);
        }

        GlassFishProperties glassfishProperties = new GlassFishProperties();
        if (instanceRoot != null) {
            glassfishProperties.setInstanceRoot(instanceRoot);
        }
        if (configFileURI != null) {
            glassfishProperties.setConfigFileURI(configFileURI);
            glassfishProperties.setConfigFileReadOnly(configFileReadOnly);
        }

        if (instanceRoot==null && configFileURI==null) {
            // only set port if embedded domain.xml is used
            if (httpPort != -1) {
                glassfishProperties.setPort("http-listener", httpPort);
            }
        }

        glassfish = glassfishRuntime.newGlassFish(glassfishProperties);
        glassfish.start();

        gfMap.put(serverID, glassfish);

        System.out.println("Started GlassFish [" + serverID + "]");

        return glassfish;
    }

    public static void deploy(String app, String serverId, List<String> deployParams)
            throws Exception {
        GlassFish glassfish = gfMap.get(serverId);
        if (glassfish == null) {
            throw new Exception("Embedded GlassFish [" + serverId + "] not running");
        }
        if (app == null) {
            throw new Exception("Application can not be null");
        }
        Deployer deployer = glassfish.getDeployer();
        final int len = deployParams.size();
        if (len > 0) {
            deployer.deploy(new File(app).toURI(), deployParams.toArray(new String[len]));
            System.out.println("Deployed [" + app + "] with parameters " + deployParams);
        } else {
            deployer.deploy(new File(app).toURI());
            System.out.println("Deployed [" + app + "]");
        }
    }

    public static void undeploy(String appName, String serverId) throws Exception {
        GlassFish glassfish = gfMap.get(serverId);
        if (glassfish == null) {
            throw new Exception("Embedded GlassFish [" + serverId + "] not running");
        }
        if (appName == null) {
            throw new Exception("Application name can not be null");
        }
        Deployer deployer = glassfish.getDeployer();
        deployer.undeploy(appName);
        System.out.println("Undeployed [" + appName + "]");
    }

    public static void runCommand(String commandLine, String serverId) throws Exception {
        GlassFish glassfish = gfMap.get(serverId);
        if (glassfish == null) {
            throw new Exception("Embedded GlassFish [" + serverId + "] not running");
        }
        if (commandLine == null) {
            throw new Exception("Command can not be null");
        }
        String[] split = commandLine.split(" ");
        String command = split[0].trim();
        String[] commandParams = null;
        if (split.length > 1) {
            commandParams = new String[split.length - 1];
            for (int i = 1; i < split.length; i++) {
                commandParams[i - 1] = split[i].trim();
            }
        }
        CommandRunner cr = glassfish.getCommandRunner();
        CommandResult result = commandParams == null ?
                cr.run(command) : cr.run(command, commandParams);
        System.out.println("Executed command [" + commandLine +
                "]. Output : \n" + result.getOutput());
    }

    public static synchronized void disposeGlassFish(String serverID)
            throws GlassFishException {
        GlassFish glassfish = gfMap.remove(serverID);
        if (glassfish != null) {
            glassfish.dispose();
            System.out.println("Stopped GlassFish [" + serverID + "]");
        }
    }

}
