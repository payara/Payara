/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.paas.lbplugin.logger.LBPluginLogger;
import org.glassfish.paas.lbplugin.util.LBServiceConfiguration;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.glassfish.virtualization.util.VirtualizationType;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.ComponentException;
import org.jvnet.hk2.component.Habitat;
import org.glassfish.paas.lbplugin.util.ApacheLBUtility;

/**
 *
 * @author kshitiz
 */
@Service
public class ApacheLBProvisioner implements LBProvisioner{

    @Inject
    ServerEnvironment serverEnvironment;

    private static final String DEFAULT_APACHE_INSTALL_DIR = "/u01/glassfish/lb/install";
    private static final String APACHECTL_SCRIPT_NAME = "/bin/apachectl";
    private static final String LB_CONFIG_DIRECTORY = "load-balancer";
    private static final String CONFIGURATION_FILE_NAME = "configuration.properties";


    private static final String DEFAULT_APACHE_INSTALL_DIR_WINDOWS = "c:\\glassfish\\lb\\install";
    private static final String APACHECTL_SCRIPT_NAME_WINDOWS = "\\bin\\httpd.exe";

    private String apacheInstallDir;
    private String apachectl;
    private String apacheConfDir;
    
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
	} else {
	    setInstallDir(DEFAULT_APACHE_INSTALL_DIR);
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
    public void configureLB(String serviceName,VirtualMachine virtualMachine, String domainName, LBServiceConfiguration configuration) throws Exception{
           
	createLbDirectory();
	if(configuration.isSslEnabled()){
            getApacheLBUtility().configureLBWithSSL(domainName,configuration.getHttpsPort(),configuration.getHttpPort(),virtualMachine);
        }else{
            getApacheLBUtility().configureLBWithoutSSL(domainName,configuration.getHttpPort(),virtualMachine);
        }
	createAppDomainConfigProperties(serviceName,configuration,domainName);
        LBPluginLogger.getLogger().log(Level.INFO,"LB configuration done successfuly"); 
    }

    @Override
    public void associateApplicationServerWithLB(String appName, VirtualMachine virtualMachine,
            String serviceName, String domainName, CommandRunner commandRunner, String clusterName,
            Habitat habitat, String glassfishHome, boolean isFirst, boolean isReconfig,Properties healthProps) throws Exception{
        if(!isReconfig){
            createApacheConfig(clusterName, commandRunner, habitat, serviceName, isFirst);
	} else {
            if(isNativeMode()){
                createAjpListenerPerInstance(clusterName, commandRunner, habitat);
	    }
	}
	      
	Properties appDomainConfigProps = getApacheLBUtility()
		.readPropertiesFile(
			serverEnvironment.getConfigDirPath() + File.separator
				+ LB_CONFIG_DIRECTORY + File.separator
				+ serviceName + "_" + CONFIGURATION_FILE_NAME);
        if (appName != null && domainName != null) {

            appDomainConfigProps.setProperty("app." + appName + ".domain-name",
    		domainName);
        } else if (appName != null) {
            appDomainConfigProps.remove("app." + appName + ".domain-name");
        }
        if (healthProps != null) {
            setAppHealthConfig(appName, serviceName, healthProps, appDomainConfigProps);
        }
        createNewWorkerPropertiesFile(commandRunner, serviceName);
        if (appDomainConfigProps.getProperty("app." + appName + ".domain-name") != null) {
            getApacheLBUtility().createCertificate(virtualMachine,
                    appDomainConfigProps.getProperty("app." + appName
                    + ".domain-name"));
        }
	reconfigureApache(virtualMachine, serviceName, glassfishHome, appName,
		domainName, appDomainConfigProps);
	File tempworkerFile = new File(System.getProperty("user.dir")
		+ File.separator + "worker.properties");
	virtualMachine.upload(tempworkerFile, new File(apacheConfDir));
	persistPropertiesFile(serviceName,appDomainConfigProps);

	getApacheLBUtility().deleteTempFile(tempworkerFile);

	getApacheLBUtility().restartApacheGracefully(virtualMachine);
    }
    
    private void reconfigureApache(VirtualMachine virtualMachine,
            String serviceName, String glassfishHome, String appName,
            String domainName, Properties appDomainConfigProps)
            throws IOException, ComponentException, InterruptedException {
        
	getApacheLBUtility().associateServer(virtualMachine,appDomainConfigProps);
        //Doing hard reconfig  on windows till a solution is found for graceful reconfig
        if(useWindowsConfig()){
            restartHttpdProcess();
        }
    }

    private void createApacheConfig(String clusterName,
            CommandRunner commandRunner, Habitat habitat, String serviceName, boolean isFirst)
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

        if (isFirst) {
            params.clear();
            params.add(serviceName + "-lb-config");
            result = commandRunner.run("create-http-lb-config", (String[]) params.toArray(new String[params.size()]));
            if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
                LBPluginLogger.getLogger().log(Level.INFO, "create-http-lb-config failed");
                throw new RuntimeException("create-http-lb-config failed.");
            }
            LBPluginLogger.getLogger().log(Level.INFO, "create-http-lb-config succeeded");
        }
        
        params.clear();
        params.add("--config");
        params.add(serviceName + "-lb-config");
        params.add(clusterName);
        result = commandRunner.run("create-http-lb-ref", (String[]) params.toArray(new String[params.size()]));
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO, "create-http-lb-ref failed");
            throw new RuntimeException("create-http-lb-ref failed.");
        }
        LBPluginLogger.getLogger().log(Level.INFO, "create-http-lb-ref succeeded");

    }
    
    /* Create worker.properties file in apache config directory */
    private void createNewWorkerPropertiesFile(CommandRunner commandRunner,
	    String serviceName) throws Exception {

	ArrayList params = new ArrayList();
	CommandResult result = null;
	params.clear();
	params.add("--config");
	params.add(serviceName + "-lb-config");
	params.add("--type");
	params.add("apache");
	params.add("--retrievefile=false");
	params.add(System.getProperty("user.dir")+File.separator+ "worker.properties");
	result = commandRunner.run("export-http-lb-config",
		(String[]) params.toArray(new String[params.size()]));
	if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
	    LBPluginLogger.getLogger().log(Level.INFO,
		    "export-http-lb-config failed");
	    throw new Exception("export-http-lb-config failed.");
	}
	LBPluginLogger.getLogger().log(Level.INFO,
		"export-http-lb-config succeeded");

    }
    
    /* Set App Specific Health Configuration */
    private void setAppHealthConfig(String appName, String serviceName,
	    Properties healthProps, Properties appConfigProps)
	    throws IOException {
        
	String healthInterval = healthProps
		.getProperty(Constants.HEALTH_CHECK_INTERVAL_PROP_NAME);
	String healthTimeOut = healthProps
		.getProperty(Constants.HEALTH_CHECK_TIMEOUT_PROP_NAME);
	if ( healthInterval !=null) {
	    appConfigProps
		    .setProperty(appName + "."
			    + Constants.HEALTH_CHECK_INTERVAL_PROP_NAME,
			    healthInterval);

	}
	if (healthTimeOut != null) {
	    appConfigProps.setProperty(appName + "."
		    + Constants.HEALTH_CHECK_TIMEOUT_PROP_NAME, healthTimeOut);
	}
	persistPropertiesFile(serviceName, appConfigProps);

    }
    
    /* Create a Config properties file to store http ports and domain name */
    private void createAppDomainConfigProperties(String serviceName,LBServiceConfiguration configuration, String domainName)
	    throws IOException {

	Properties configProps = new Properties();
	configProps.setProperty("http-port", configuration.getHttpPort());
	configProps.setProperty("https-port", configuration.getHttpsPort());
	if (configuration.isSslEnabled()) {
	    configProps.setProperty("SSL", "true");
	} else {
	    configProps.setProperty("SSL", "false");
	}
	if (domainName != null) {
	    configProps.setProperty("LB.domain-name", domainName);
	}
	if (configuration.getHealthInterval() != null) {
	    configProps.setProperty(Constants.HEALTH_CHECK_INTERVAL_PROP_NAME,
		    configuration.getHealthInterval());
	}
	if (configuration.getHealthTimeout() != null) {
	    configProps.setProperty(Constants.HEALTH_CHECK_TIMEOUT_PROP_NAME,
		    configuration.getHealthTimeout());
	}
	persistPropertiesFile(serviceName, configProps);

    }
    
    /* Store the Application Domain Config Properties File */
    private void persistPropertiesFile(String serviceName,Properties propFile) throws IOException {
	File appDomainConfigFile = new File(serverEnvironment.getConfigDirPath()
		+ File.separator +LB_CONFIG_DIRECTORY + File.separator + serviceName + "_" + CONFIGURATION_FILE_NAME);

	FileOutputStream fout = new FileOutputStream(appDomainConfigFile);
	try{
	    propFile.store(fout, "Application Domain Names");
	}finally{
            if(fout != null){
                try {
                    fout.close(); 
                }catch (IOException ex){
                    //ignore
                }
            }
	}
	
    }
    
    /*Create LB directory to store config properties file*/
    public void createLbDirectory() {
	File lbDirectory = new File(serverEnvironment.getConfigDirPath()
		+ File.separator + LB_CONFIG_DIRECTORY);
	if (!lbDirectory.exists()) {
	    boolean lbDirectoryCreated = new File(
		    serverEnvironment.getConfigDirPath() + File.separator
			    + LB_CONFIG_DIRECTORY).mkdir();
	    if (!lbDirectoryCreated) {
		String msg = "load-balancer directory does not exist. Cannot create load-balancer";
		throw new RuntimeException(msg);
	    }
	}
    }

          
    @Override
    public void dissociateApplicationServerWithLB(String appName, VirtualMachine virtualMachine,
            String serviceName, CommandRunner commandRunner, String clusterName,
            Habitat habitat, String glassfishHome, boolean isLast) throws Exception {
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
       
	Properties configProps = getApacheLBUtility().readPropertiesFile(
		serverEnvironment.getConfigDirPath() + File.separator
			+ LB_CONFIG_DIRECTORY + File.separator + serviceName
			+ "_" + CONFIGURATION_FILE_NAME);
               
	createNewWorkerPropertiesFile(commandRunner, serviceName);
	getApacheLBUtility().removeCertificates(configProps.getProperty("app." +
                appName + ".domain-name"), virtualMachine);
	configProps.remove("app." + appName + ".domain-name");
	configProps.remove(appName +"."+Constants.HEALTH_CHECK_INTERVAL_PROP_NAME);
	configProps.remove(appName +"."+ Constants.HEALTH_CHECK_TIMEOUT_PROP_NAME);
	reconfigureApache(virtualMachine, serviceName, glassfishHome, appName,
		null, configProps);
	persistPropertiesFile(serviceName,configProps);
	File tempWorkFile = new File(System.getProperty("user.dir")
		+ File.separator + "worker.properties");
	virtualMachine.upload(tempWorkFile, new File(apacheConfDir));
	getApacheLBUtility().restartApacheGracefully(virtualMachine);
	getApacheLBUtility().deleteTempFile(tempWorkFile);
        if (isLast) {
            params.clear();
            params.add(serviceName + "-lb-config");
            result = commandRunner.run("delete-http-lb-config", (String[]) params.toArray(new String[params.size()]));
            if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
                LBPluginLogger.getLogger().log(Level.INFO, "delete-http-lb-config failed");
                throw new RuntimeException("delete-http-lb-config failed.");
            }
            LBPluginLogger.getLogger().log(Level.INFO, "delete-http-lb-config succeeded");
        }
        
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
            apacheConfDir = apacheInstallDir + "\\conf";
        } else {
            apachectl = apacheInstallDir + APACHECTL_SCRIPT_NAME;
            apacheConfDir = apacheInstallDir + "/conf";
        }
    }

    @Override
    public void setScriptsDir(String scriptsDir) {
        //No operation
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

    private ApacheLBUtility getApacheLBUtility() {
        return new ApacheLBUtility(apacheInstallDir, apacheConfDir, apachectl, useWindowsConfig());
    }

    class HttpdThread extends Thread{

        Process process;
        boolean killProcess = false;
        private ApacheLoggerThread inputStreamLoggerThread;
        private ApacheLoggerThread errorStreamLoggerThread;

        HttpdThread() {
        }

        @Override
        public void run() {
            try {
                process = Runtime.getRuntime().exec(apachectl);
                //create thread to read process input and error stream and
                //log it
                inputStreamLoggerThread = new ApacheLoggerThread(process, false);
                errorStreamLoggerThread = new ApacheLoggerThread(process, true);
                inputStreamLoggerThread.start();
                errorStreamLoggerThread.start();
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
                if(errorStreamLoggerThread != null){
                    errorStreamLoggerThread.stopThread();
                }
                if(inputStreamLoggerThread != null){
                    inputStreamLoggerThread.stopThread();
                }
                errorStreamLoggerThread = null;
                inputStreamLoggerThread = null;
                process = null;
            } catch (Exception ioe) {
                LBPluginLogger.getLogger().log(Level.SEVERE,
                        "Httpd process could not be killed ...", ioe);
            }
        }
        
    }

    class ApacheLoggerThread extends Thread{

        private InputStream inputStream;
        private boolean isError;
        private boolean stopThread;

        public ApacheLoggerThread(Process process, boolean isError) {
            this.isError = isError;
            if(isError){
                inputStream = process.getErrorStream();
            } else {
                inputStream = process.getInputStream();
            }
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream));
                String line = null;
                while (!stopThread && (line = reader.readLine()) != null) {
                    LBPluginLogger.getLogger().log(Level.FINE, "APACHE "
                            + (isError ? "ERROR" : "") + "LOG : " + line);
                }
            } catch (IOException ex) {
                LBPluginLogger.getLogger().log(Level.SEVERE,
                        "Exception while collecting apache " +
                        (isError ? "error" : "") + " logs", ex);
            } finally {
                inputStream = null;
            }
        }

        private void stopThread() {
            if(inputStream != null){
                stopThread = true;
                interrupt();
            }
        }

    }


}
