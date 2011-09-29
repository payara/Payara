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

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.SystemProperty;
import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.net.NetUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import org.glassfish.common.util.admin.AuthTokenManager;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.paas.lbplugin.logger.LBPluginLogger;
import org.glassfish.paas.lbplugin.util.LBServiceConfiguration;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.glassfish.virtualization.util.VirtualizationType;
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
    private static final String ASSOCIATE_SERVERS_SCRIPT_NAME = "/associateServer.sh";
    private static final String CONFIGURE_SERVER_SCRIPT_NAME = "/configureServer.sh";
    private static final String APACHECTL_SCRIPT_NAME = "/bin/apachectl";

    private static final String DEFAULT_APACHE_INSTALL_DIR_WINDOWS = "c:\\glassfish\\lb\\install";
    private static final String DEFAULT_SCRIPTS_DIR_WINDOWS = "c:\\glassfish\\lb\\scripts";
    private static final String ASSOCIATE_SERVERS_SCRIPT_NAME_WINDOWS = "\\associateServer.bat";
    private static final String CONFIGURE_SERVER_SCRIPT_NAME_WINDOWS = "\\configureServer.bat";
    private static final String APACHECTL_SCRIPT_NAME_WINDOWS = "\\bin\\httpd.exe";

    private String apacheInstallDir;
    private String apachectl;
    private String scriptsDir;
    private String associateServerScript;
    private String configureServerScript;

    private static final String AJP_LISTENER_NAME = "ajp-listener-1";
    private static final String AJP_LISTENER_PORT = "AJP_LISTENER_PORT";
    private static final int DEFAULT_AJP_LISTENER_PORT = 28009;
    public static final String VENDOR_NAME = "apache";
    private String virtualizationType;
    private HttpdThread httpdThread;

    public ApacheLBProvisioner() {
    }
    
    @Override
    public void initialize() {
        if(useWindowsConfig()){
            setInstallDir(DEFAULT_APACHE_INSTALL_DIR_WINDOWS);
            setScriptsDir(DEFAULT_SCRIPTS_DIR_WINDOWS);
        } else {
            setInstallDir(DEFAULT_APACHE_INSTALL_DIR);
            setScriptsDir(DEFAULT_SCRIPTS_DIR);
        }
    }

    @Override
    public void startLB(VirtualMachine virtualMachine) throws Exception{
        if(useWindowsConfig()){
            startHttpdProcess();
        } else {
            String output = virtualMachine.executeOn(new String[]{apachectl, "start"});
            LBPluginLogger.getLogger().log(Level.INFO,"Start apache command output : " + output);
        }
    }

    @Override
    public void stopLB(VirtualMachine virtualMachine)  throws Exception {
        if(useWindowsConfig()){
            stopHttpdProcess();
        } else {
            String output = virtualMachine.executeOn(new String[]{apachectl, "stop"});
            LBPluginLogger.getLogger().log(Level.INFO,"Stop apache command output : " + output);
        }
    }

    @Override
    public void configureLB(VirtualMachine virtualMachine, LBServiceConfiguration configuration) throws Exception{
        String[] command = null;
        if (configuration.isSslEnabled()) {
            command = new String[]{configureServerScript,
                        configuration.getHttpPort(), configuration.getHttpsPort()};
        } else {
            command = new String[]{configureServerScript,
                        configuration.getHttpPort()};
        }
        String output = virtualMachine.executeOn(command);
        LBPluginLogger.getLogger().log(Level.INFO,"Output of configure apache server command : " + output);
        
    }

    @Override
    public void associateApplicationServerWithLB(VirtualMachine virtualMachine,
            String serviceName, CommandRunner commandRunner, String clusterName,
            Habitat habitat, String glassfishHome, boolean isReconfig) throws Exception{
        if(!isReconfig){
            createApacheConfig(clusterName, commandRunner, habitat, serviceName);
        } else {
            if(isNativeMode()){
                createAjpListenerPerInstance(clusterName, commandRunner, habitat);
            }
        }
        reconfigureApache(habitat, virtualMachine, serviceName, glassfishHome);
    }

    private void reconfigureApache(Habitat habitat, VirtualMachine virtualMachine,
            String serviceName, String glassfishHome) throws IOException, ComponentException, InterruptedException {
        AuthTokenManager tokenMgr = habitat.getComponent(AuthTokenManager.class);
        String output = virtualMachine.executeOn(new String[]{associateServerScript, serviceName + "-lb-config", tokenMgr.createToken(30L * 60L * 1000L), glassfishHome});
        LBPluginLogger.getLogger().log(Level.INFO, "Output of associate apache servers command : " + output);
        //Doing hard reconfig  on windows till a solution is found for graceful reconfig
        if(useWindowsConfig()){
            restartHttpdProcess();
        }
    }

    private void createApacheConfig(String clusterName,
            CommandRunner commandRunner, Habitat habitat, String serviceName)
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

        params.clear();
        params.add("--target");
        params.add(clusterName);
        params.add(AJP_LISTENER_PORT + "=" + DEFAULT_AJP_LISTENER_PORT);
        result = commandRunner.run("create-system-properties", (String[]) params.toArray(new String[params.size()]));
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO, "create-system-properties failed");
            throw new RuntimeException("Creation of system property " + AJP_LISTENER_PORT + " failed.");
        }
        LBPluginLogger.getLogger().log(Level.INFO, "create-system-properties succeeded");

        if(isNativeMode()){
            createAjpListenerPerInstance(clusterName, commandRunner, habitat);
        }

        params.clear();
        params.add("--target");
        params.add(clusterName);
        params.add("--listenerport");
        params.add("${" + AJP_LISTENER_PORT + "}");
        params.add("--listeneraddress");
        params.add("0.0.0.0");
        params.add("--default-virtual-server");
        params.add("server");
        params.add(AJP_LISTENER_NAME);
        result = commandRunner.run("create-http-listener", (String[]) params.toArray(new String[params.size()]));
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO, "create-http-listener failed");
            throw new RuntimeException("Creation of " + AJP_LISTENER_NAME + " failed.");
        }
        LBPluginLogger.getLogger().log(Level.INFO, "create-http-listener succeeded");

        params.clear();
        params.add("configs.config." + clusterName + "-config.network-config.protocols.protocol." + AJP_LISTENER_NAME + ".http.jk-enabled=true");
        result = commandRunner.run("set", (String[]) params.toArray(new String[params.size()]));
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO, "jk-enabled failed");
            throw new RuntimeException("jk-enabled for " + AJP_LISTENER_NAME + " failed.");
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
    public void dissociateApplicationServerWithLB(VirtualMachine virtualMachine,
            String serviceName, CommandRunner commandRunner, String clusterName,
            Habitat habitat) {
        ArrayList params = new ArrayList();
        params.add(clusterName);
        CommandResult result = commandRunner.run("disable-http-lb-server", (String[]) params.toArray(new String[params.size()]));
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO, "disable-http-lb-server failed");
            throw new RuntimeException("disable-http-lb-server failed.");
        }
        LBPluginLogger.getLogger().log(Level.INFO, "disable-http-lb-server succeeded");

        params.clear();
        params.add("--config");
        params.add(serviceName + "-lb-config");
        params.add(clusterName);
        result = commandRunner.run("delete-http-lb-ref", (String[]) params.toArray(new String[params.size()]));
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO, "delete-http-lb-ref failed");
            throw new RuntimeException("delete-http-lb-ref failed.");
        }
        LBPluginLogger.getLogger().log(Level.INFO, "delete-http-lb-ref succeeded");

        params.clear();
        params.add(serviceName + "-lb-config");
        result = commandRunner.run("delete-http-lb-config", (String[]) params.toArray(new String[params.size()]));
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO, "delete-http-lb-config failed");
            throw new RuntimeException("delete-http-lb-config failed.");
        }
        LBPluginLogger.getLogger().log(Level.INFO, "delete-http-lb-config succeeded");
    }

    @Override
    public boolean handles(String vendorName) {
        return vendorName.equalsIgnoreCase(VENDOR_NAME);
    }

    @Override
    public void setInstallDir(String installDir) {
        apacheInstallDir =  installDir;
        if(useWindowsConfig()){
            apachectl = apacheInstallDir + APACHECTL_SCRIPT_NAME_WINDOWS;
        } else {
            apachectl = apacheInstallDir + APACHECTL_SCRIPT_NAME;
        }
    }

    @Override
    public void setScriptsDir(String scriptsDir) {
        this.scriptsDir = scriptsDir;
        if(useWindowsConfig()){
            associateServerScript = this.scriptsDir + ASSOCIATE_SERVERS_SCRIPT_NAME_WINDOWS;
            configureServerScript = this.scriptsDir + CONFIGURE_SERVER_SCRIPT_NAME_WINDOWS;
        } else {
            associateServerScript = this.scriptsDir + ASSOCIATE_SERVERS_SCRIPT_NAME;
            configureServerScript = this.scriptsDir + CONFIGURE_SERVER_SCRIPT_NAME;
        }
    }

    @Override
    public void setVirtualizationType(String value) {
        virtualizationType = value;
    }

    private boolean useWindowsConfig(){
        return OS.isWindows() && isNativeMode();
    }

    private boolean isNativeMode(){
        return virtualizationType.equals(VirtualizationType.Type.Native.name());
    }

    private void startHttpdProcess() {
        LBPluginLogger.getLogger().log(Level.INFO, "Starting httpd process ...");
        httpdThread = new HttpdThread();
        httpdThread.start();
        LBPluginLogger.getLogger().log(Level.INFO, "Started httpd process");
    }

    private void stopHttpdProcess() {
        if(httpdThread != null){
            LBPluginLogger.getLogger().log(Level.INFO, "Stopping httpd process ...");
            httpdThread.stopProcess();
            LBPluginLogger.getLogger().log(Level.INFO, "Stopped httpd process");
            httpdThread = null;
        }
    }
    
    private void restartHttpdProcess() {
        stopHttpdProcess();
        startHttpdProcess();
    }

    private void createAjpListenerPerInstance(String clusterName, CommandRunner commandRunner, Habitat habitat) {
        Cluster cluster = habitat.getComponent(Cluster.class, clusterName);
        if(cluster == null){
            throw new RuntimeException("Unable to get cluster server beans with name " + clusterName);
        }
        //Assuming no other command is trying to add a port
        //So get highest current AJP port and then sequential increase from
        //there to get a free port
        //logic will fail if multiple simultaneous call occurs to
        //NetUtils.getNextFreePort(..)
        int ajpPort = DEFAULT_AJP_LISTENER_PORT;
        for(Server server : cluster.getInstances()){
            SystemProperty property = server.getSystemProperty(AJP_LISTENER_PORT);
            if(property != null){
                int port = Integer.parseInt(property.getValue());
                if(port > ajpPort){
                    ajpPort = port;
                }
            }
        }
        for(Server server : cluster.getInstances()){
            SystemProperty property = server.getSystemProperty(AJP_LISTENER_PORT);
            if(property == null){
                //passing null assuming it is all localhost
                ajpPort = NetUtils.getNextFreePort(null, ajpPort);
                String[] params = {"--target", server.getName(),
                    AJP_LISTENER_PORT + "=" + ajpPort};
                CommandResult result = commandRunner.run("create-system-properties",
                        params);
                if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
                    LBPluginLogger.getLogger().log(Level.INFO, "create-system-properties failed for instance " + server.getName());
                    throw new RuntimeException("Creation of system property "
                            + AJP_LISTENER_PORT + " for instance " + server.getName() + " failed.");
                }
                LBPluginLogger.getLogger().log(Level.INFO, "create-system-properties succeeded for instance " + server.getName());
            }
        }
    }

    class HttpdThread extends Thread{

        Process process;
        boolean killProcess = false;

        HttpdThread() {
        }

        @Override
        public void run() {
            try {
                process = Runtime.getRuntime().exec(apachectl);
                process.waitFor();
            } catch (Exception ex) {
                process = null;
                LBPluginLogger.getLogger().log((killProcess? Level.FINE : Level.SEVERE),
                        "Httpd process exited ...", ex);
            }
        }

        private void stopProcess() {
            try {
                killProcess = true;
                Runtime.getRuntime().exec("taskkill /F /T /IM httpd.exe");
                //Sleep for 2 seconds to 
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    //ignore
                }
            } catch (Exception ioe) {
                LBPluginLogger.getLogger().log(Level.SEVERE,
                        "Httpd process could not be killed ...", ioe);
            }
        }
        
    }
}
