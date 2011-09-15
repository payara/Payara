/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.lbplugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import org.glassfish.common.util.admin.AuthTokenManager;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.paas.lbplugin.logger.LBPluginLogger;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.ComponentException;
import org.jvnet.hk2.component.Habitat;

/**
 *
 * @author kshitiz
 */
@Service
public class ApacheLBProvisioner implements LBProvisioner{

    private static final String DEFAULT_APACHE_INSTALL_DIR = "/u01/glassfish/lb/install";
    private static final String DEFAULT_SCRIPTS_DIR = "/u01/glassfish/lb/scripts";
    private static final String ASSOCIATE_SERVERS_SCRIPT_NAME = "associateServer.sh";
    private static final String CONFIGURE_SERVER_SCRIPT_NAME = "configureServer.sh";

    private String apacheInstallDir;
    private String apachectl;
    private String scriptsDir;
    private String associateServerScript;
    private String configureServerScript;

    private static final String LISTENER_NAME = "ajp-listener-1";
    private static final String HTTP_LISTENER_NAME = "http-listener-1";
    private static final String LISTENER_PORT = "28009";//"\\$\\{AJP_LISTENER_PORT\\}"
    public static final String VENDOR_NAME = "apache";

    public ApacheLBProvisioner() {
        setInstallDir(DEFAULT_APACHE_INSTALL_DIR);
        setScriptsDir(DEFAULT_SCRIPTS_DIR);
    }

    @Override
    public void startLB(VirtualMachine virtualMachine) throws Exception{
        String output = virtualMachine.executeOn(new String[]{apachectl, "start"});
        LBPluginLogger.getLogger().log(Level.INFO,"Start apache command output : " + output);
    }

    @Override
    public void stopLB(VirtualMachine virtualMachine)  throws Exception {
        String output = virtualMachine.executeOn(new String[]{apachectl, "stop"});
        LBPluginLogger.getLogger().log(Level.INFO,"Stop apache command output : " + output);
    }

    @Override
    public void configureLB(VirtualMachine virtualMachine) throws Exception{
        String output = virtualMachine.executeOn(new String[]{configureServerScript});
        LBPluginLogger.getLogger().log(Level.INFO,"Output of configure apache server command : " + output);
        
    }

    @Override
    public void associateApplicationServerWithLB(VirtualMachine virtualMachine,
            String serviceName, CommandRunner commandRunner, String clusterName,
            Habitat habitat, String glassfishHome, boolean isReconfig) throws Exception{
        if(!isReconfig){
            createApacheConfig(clusterName, commandRunner, serviceName);
        }
        reconfigureApache(habitat, virtualMachine, serviceName, glassfishHome);
    }

    private void reconfigureApache(Habitat habitat, VirtualMachine virtualMachine,
            String serviceName, String glassfishHome) throws IOException, ComponentException, InterruptedException {
        AuthTokenManager tokenMgr = habitat.getComponent(AuthTokenManager.class);
        String output = virtualMachine.executeOn(new String[]{associateServerScript, serviceName + "-lb-config", tokenMgr.createToken(30L * 60L * 1000L), glassfishHome});
        LBPluginLogger.getLogger().log(Level.INFO, "Output of associate apache servers command : " + output);
    }

    private void createApacheConfig(String clusterName,
            CommandRunner commandRunner, String serviceName)
            throws RuntimeException {
        ArrayList params = new ArrayList();
        CommandResult result;
        params.add("--target");
        params.add(clusterName);
        params.add("\"-DjvmRoute=\\${com.sun.aas.instanceName}\"");
        result = commandRunner.run("create-jvm-options", (String[]) params.toArray(new String[params.size()]));
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO, "create-jvm-options failed");
            throw new RuntimeException("Creation of jvm option \"-DjvmRoute=\\${com.sun.aas.instanceName}\" failed");
        }
        /*
        params.clear();
        params.add("--target");
        params.add(clusterName);
        params.add("--listenerport");
        params.add(LISTENER_PORT);
        params.add("--listeneraddress");
        params.add("0.0.0.0");
        params.add("--default-virtual-server");
        params.add("server");
        params.add(LISTENER_NAME);
        result = commandRunner.run("create-http-listener", (String[]) params.toArray(new String[params.size()]));
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO, "create-http-listener failed");
            throw new RuntimeException("Creation of " + LISTENER_NAME + " failed.");
        }
        LBPluginLogger.getLogger().log(Level.INFO, "create-http-listener succeeded");
         */
        params.clear();
        params.add("configs.config." + clusterName + "-config.network-config.protocols.protocol." + HTTP_LISTENER_NAME + ".http.jk-enabled=true");
        result = commandRunner.run("set", (String[]) params.toArray(new String[params.size()]));
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO, "jk-enabled failed");
            throw new RuntimeException("jk-enabled for " + LISTENER_NAME + " failed.");
        }
        LBPluginLogger.getLogger().log(Level.INFO, "jk-enabled succeeded");
        params.clear();
        params.add("--target");
        params.add(clusterName);
        params.add(serviceName + "-lb-config");
        result = commandRunner.run("create-http-lb-config", (String[]) params.toArray(new String[params.size()]));
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO, "create-http-lb-config failed");
            throw new RuntimeException("create-http-lb-config failed.");
        }
        LBPluginLogger.getLogger().log(Level.INFO, "create-http-lb-config succeeded");
    }

    @Override
    public boolean handles(String vendorName) {
        return vendorName.equalsIgnoreCase(VENDOR_NAME);
    }

    @Override
    public final void setInstallDir(String installDir) {
        apacheInstallDir =  installDir;
        apachectl = apacheInstallDir + "/bin/apachectl";
    }

    @Override
    public final void setScriptsDir(String scriptsDir) {
        this.scriptsDir = scriptsDir;
        associateServerScript = this.scriptsDir + "/" + ASSOCIATE_SERVERS_SCRIPT_NAME;
        configureServerScript = this.scriptsDir + "/" + CONFIGURE_SERVER_SCRIPT_NAME;
    }

}
