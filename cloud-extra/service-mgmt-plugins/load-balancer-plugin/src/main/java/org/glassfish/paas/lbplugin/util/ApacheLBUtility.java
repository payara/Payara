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
/**
 * 
 * @author Shyamant Hegde
 * 
 */
package org.glassfish.paas.lbplugin.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.glassfish.paas.lbplugin.logger.LBPluginLogger;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.glassfish.paas.lbplugin.Constants;

public class ApacheLBUtility {

    private String apacheInstallDir;
    private String apacheConfigDir;
    private String apacheCtl;
    private boolean isWindows;
    private String fileSep;
    private static String GLASSFISH_VHOSTS_CONF = "glassfish-vhosts.conf";
    private static String DEFAULT_PING_INTERVAL = "60";
    private static String DEFAULT_PING_TIMEOUT = "10";

    public ApacheLBUtility(String apacheInstallDir, String apacheConfigDir,
	    String apacheCtl, boolean isWindows) {
	this.apacheInstallDir = apacheInstallDir;
	this.apacheConfigDir = apacheConfigDir;
	this.apacheCtl = apacheCtl;
	this.isWindows = isWindows;
	if (this.isWindows) {
	    fileSep = "\\";
	} else {
	    fileSep = "/";
	}
    }

    /* Associate server */
    public void associateServer(VirtualMachine virtualMachine, Properties appDomainConfigProperties)
	    throws IOException, InterruptedException {
	String httpPort = appDomainConfigProperties.getProperty("http-port");
	String httpsPort = appDomainConfigProperties.getProperty("https-port");
	List<String> contextRoots = extractContextRootMap();

	if (!contextRoots.isEmpty() && appDomainConfigProperties.getProperty(
                "app."+contextRoots.get(0).substring(1,contextRoots.get(0)
                .indexOf("="))+".domain-name")!=null) {
	    
	    createVirtualServerEntryPerAppDomain(httpPort, contextRoots,appDomainConfigProperties,true);
            if (appDomainConfigProperties.getProperty("SSL").equalsIgnoreCase(
		    "true")) {
		disableVhostsInSSLConf(virtualMachine);
		createVirtualServerEntryPerAppDomain(httpsPort, contextRoots,appDomainConfigProperties,false);
	    }
	} else if (appDomainConfigProperties.getProperty("LB.domain-name") != null) {
            createVirtualServerEntryForLBDomain(
                    appDomainConfigProperties.getProperty("LB.domain-name"),
                    httpPort, contextRoots);
	    if (appDomainConfigProperties.getProperty("SSL").equalsIgnoreCase(
		    "true")) {
		disableVhostsInSSLConf(virtualMachine);
		createVirtualServerEntryForLBDomain(
			appDomainConfigProperties.getProperty("LB.domain-name"),
			httpsPort, contextRoots);
	    }
	} else {
	    storeContextRootMappings(contextRoots);
	}
	
	setHealthMonitoringAttributesforClusters(contextRoots,appDomainConfigProperties);
	
	File tempFile = new File(System.getProperty("user.dir")
		+ File.separator + GLASSFISH_VHOSTS_CONF);
	virtualMachine.upload(tempFile, new File(apacheConfigDir));
        deleteTempFile(tempFile);
	LBPluginLogger.getLogger().log(Level.INFO,
		"ASSOCIATION COMMAND COMPLETED SUCCESSFULLY");

    }

    /* Configure LB with SSL */
    public void configureLBWithSSL(String domainName, String httpsPort,
	    String httpPort, VirtualMachine virtualMachine) throws IOException,
	    InterruptedException {

	setHttpPort(virtualMachine, httpPort);
	setHttpsPort(virtualMachine, httpsPort, domainName);
	createCertificate(virtualMachine, domainName);
	enableSSL(virtualMachine);
	LBPluginLogger.getLogger().log(Level.INFO,
		"CONFIGURATION COMMAND COMPLETED SUCCESSFULLY");
    }

    /* Configure LB wihtout SSL */
    public void configureLBWithoutSSL(String domainName, String httpPort,
	    VirtualMachine virtualMachine) throws IOException,
	    InterruptedException {

	setHttpPort(virtualMachine, httpPort);
	LBPluginLogger.getLogger().log(Level.INFO,
		"CONFIGURATION COMMAND COMPLETED SUCCESSFULLY");
    }

    /* Create certificate */
    public void createCertificate(VirtualMachine virtualMachine,
	    String domainName) throws IOException, InterruptedException {
	createKey(virtualMachine,domainName);
	createNewKey(virtualMachine,domainName);
	createServerCert(virtualMachine, domainName);

    }

    /* Create Key */
    private void createKey(VirtualMachine virtualMachine,String domainName) throws IOException,
	    InterruptedException {
	    String command = apacheInstallDir + fileSep + "bin" + fileSep
		+ "openssl " + "genrsa -des3 -passout pass:glassfish -out "
		+ apacheConfigDir + fileSep +(domainName != null ? domainName + "-server.key":"server.key");
	String[] commands = command.split(" ");
	String output = virtualMachine.executeOn(commands);
	LBPluginLogger.getLogger().log(Level.FINE,
		"The output of key creation is " + output);
	renameKeyFile(virtualMachine,domainName);
    }
    
    /* Rename key File */
    private void renameKeyFile(VirtualMachine virtualMachine,String domainName) throws IOException, InterruptedException {
	//Use mv until we have delete and rename util from IMS.
	String command = "mv "+apacheConfigDir + fileSep +(domainName!=null?domainName
		+ "-server.key ": "server.key ")+ apacheConfigDir + fileSep +(domainName!=null?domainName
		+ "-server.key.org": "server.key.org");
	String [] commands = command.split(" ");
	String output = virtualMachine.executeOn(commands);
	LBPluginLogger.getLogger().log(Level.FINE,
		"The output of moving key is " + output);
    }
    
    /* Create new Key */
    private void createNewKey(VirtualMachine virtualMachine,String domainName)
	    throws IOException, InterruptedException {
	String command = apacheInstallDir + fileSep + "bin" + fileSep
		+ "openssl" + " rsa -in " + apacheConfigDir + fileSep
		+ (domainName!=null?domainName + "-server.key.org":"server.key.org") + " -passin pass:glassfish -out "
		+ apacheConfigDir + fileSep + (domainName!=null?domainName + "-server.key":"server.key");
	String[] commands = command.split(" ");
	String output = virtualMachine.executeOn(commands);
	LBPluginLogger.getLogger().log(Level.FINE,
		"The output of new key creation is " + output);
	removeOriginalKey(virtualMachine,domainName);

    }
    
    /* Remove original key */
    private void removeOriginalKey(VirtualMachine virtualMachine,
	    String domainName) throws IOException, InterruptedException {
	String rmCommand = "rm -rf "
		+ apacheConfigDir
		+ fileSep
		+ (domainName != null ? domainName + "-server.key.org"
			: "server.key.org");
	String[] commands = rmCommand.split(" ");
	String output = virtualMachine.executeOn(commands);
	LBPluginLogger.getLogger().log(Level.FINE,
		"The output of deleting original key is " + output);
    }

    /*Remove certificates before disassociation*/
    public void removeCertificates(String domainName,VirtualMachine virtualMachine) throws IOException, InterruptedException{
	String rmKeyCommand = "rm -rf "+apacheConfigDir + fileSep +domainName+"-server.key";
	String rmCertCommand = "rm -rf "+apacheConfigDir + fileSep +domainName+"-server.crt";
	String[] keyCommands = rmKeyCommand.split(" ");
	String keyOutput = virtualMachine.executeOn(keyCommands);
	LBPluginLogger.getLogger().log(Level.FINEST,
		"The output of deletion of  key command is " + keyOutput);
	
	String[] certCommands = rmCertCommand.split(" ");
	String certOutput = virtualMachine.executeOn(certCommands);
	LBPluginLogger.getLogger().log(Level.FINEST,
		"The output of deletion of  Certificate command is " + certOutput);
    }

    /* Restart apache gracefully */
    public void restartApacheGracefully(VirtualMachine virtualMachine)
	    throws IOException, InterruptedException {
	String command = apacheCtl + " graceful";
	String[] commands = command.split(" ");
	String output = virtualMachine.executeOn(commands);
	LBPluginLogger.getLogger().log(Level.FINE,
		"Restart output is " + output);

    }

    /* Create server Certificate */
    private void createServerCert(VirtualMachine virtualMachine,
	    String domainName) throws IOException, InterruptedException {

	String certHostName;
	if (domainName != null) {
	    certHostName = domainName;
	} else {
	    certHostName = virtualMachine
		    .executeOn(new String[] { "hostname" }).substring(
			    0,
			    virtualMachine.executeOn(
				    new String[] { "hostname" }).length() - 1);
	}

	String command = "umask 77 ; echo \"" + "\n" + "\n" + "\n" + "\n"
		+ "\n" + certHostName + "\n\" | " + apacheInstallDir + fileSep
		+ "bin" + fileSep + "openssl req -new" + " -key "
		+ apacheConfigDir + fileSep
		+ (domainName!=null?domainName+"-server.key":"server.key")+" -x509 -days 365 -out " + apacheConfigDir
		+ fileSep +(domainName!=null?domainName+"-server.crt >/dev/null 2>&1":"server.crt >/dev/null 2>&1");
	byte[] certCommand = command.getBytes();
	File certScript = new File("certscript.sh");
	certScript.createNewFile();
	FileOutputStream certOut = new FileOutputStream(certScript);
        try {
            certOut.write(certCommand);
        } finally {
            try {
                if(certOut != null){
                    certOut.close();
                }
            } catch (IOException ex) {
                //ignore
            }
        }
	virtualMachine.upload(new File("certscript.sh"), new File(
		apacheConfigDir));
	String permCommand = "chmod a+x " + apacheConfigDir + fileSep
		+ "certscript.sh";
	String[] permCommands = permCommand.split(" ");
	String permOutput = virtualMachine.executeOn(permCommands);
	LBPluginLogger.getLogger().log(Level.FINE,
		"The output of chmod command is " + permOutput);
	String output = virtualMachine.executeOn(new String[] { apacheConfigDir
		+ fileSep + "certscript.sh" });
        deleteTempFile(certScript);
	LBPluginLogger.getLogger().log(Level.FINE,
		"The output of server creation is " + output);
    }
    
    /* Enable SSL */
    private void enableSSL(VirtualMachine virtualMachine) throws IOException,
	    InterruptedException {

	String curDir = System.getProperty("user.dir");
	virtualMachine.download(new File(apacheConfigDir + fileSep
		+ "httpd.conf"), new File(curDir));
	fileContentReplacer(curDir + File.separator + "httpd.conf",
		"#Include conf/extra/httpd-ssl.conf",
		"Include conf/extra/httpd-ssl.conf");
	File tempFile = new File(curDir + File.separator + "httpd.conf");
	virtualMachine.upload(tempFile, new File(apacheConfigDir));
        deleteTempFile(tempFile);

    }

    /* Set http-port in httpd.conf */
    private void setHttpPort(VirtualMachine virtualMachine, String httpPort)
	    throws IOException, InterruptedException {

	String curDir = System.getProperty("user.dir");

	virtualMachine.download(new File(apacheConfigDir + fileSep
		+ "httpd.conf"), new File(curDir));
	fileContentReplacer(curDir + File.separator + "httpd.conf",
		"<HTTP_PORT>", httpPort);
	File tempFile = new File(curDir + File.separator + "httpd.conf");
	virtualMachine.upload(tempFile, new File(apacheConfigDir));
        deleteTempFile(tempFile);
	
    }

    /* Set https-port in httpd-ssl.conf */
    private void setHttpsPort(VirtualMachine virtualMachine, String httpsPort,
	    String domainName) throws IOException, InterruptedException {

	String curDir = System.getProperty("user.dir");
	String hostName = virtualMachine.executeOn(new String[] { "hostname" })
		.substring(
			0,
			virtualMachine.executeOn(new String[] { "hostname" })
				.length() - 1);

	virtualMachine.download(new File(apacheConfigDir + fileSep + "extra"
		+ fileSep + "httpd-ssl.conf"), new File(curDir));
	fileContentReplacer(curDir + File.separator + "httpd-ssl.conf",
		"<HTTPS_PORT>", httpsPort);
	fileContentReplacer(curDir + File.separator + "httpd-ssl.conf",
		"ServerName www.example.com:443", "ServerName " + hostName
			+ ":" + httpsPort);
	if (domainName == null) {
	    fileContentReplacer(curDir + fileSep + "httpd-ssl.conf",
		    "#Include conf/glassfish-vhosts.conf",
		    "Include conf/glassfish-vhosts.conf");
	}

	File tempFile = new File(curDir + File.separator + "httpd-ssl.conf");
	virtualMachine.upload(tempFile, new File(apacheConfigDir + fileSep
		+ "extra"));
        deleteTempFile(tempFile);

    }

    /* Extract context root from worker.properties file */
    private List<String> extractContextRootMap() throws IOException {
	String fileLine;
	File workerProperties = new File(System.getProperty("user.dir")
		+ fileSep + "worker.properties");
	FileInputStream fis = new FileInputStream(workerProperties);
	Pattern p = Pattern.compile("#CONTEXT_ROOT_MAPPING.(.*)");
	List<String> contextRoots = new ArrayList<String>();

	// Might add Buffered Input Stream to increase speed,depends on how good
	// this operation is without it.
	DataInputStream dis = new DataInputStream(fis);
	BufferedReader brd = new BufferedReader(new InputStreamReader(dis));
        try {
            while ((fileLine = brd.readLine()) != null) {
                if (fileLine.contains("CONTEXT_ROOT_MAPPING")) {
                    Matcher m = p.matcher(fileLine);
                    if (m.find()) {
                        contextRoots.add(m.group(1));
                    }
                } else if (!fileLine.contains("#")) {
                    break;
                }

            }
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                    //ignore
                }
            }
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException ex) {
                    //ignore
                }
            }
            if (brd != null) {
                try {
                    brd.close();
                } catch (IOException ex) {
                    //ignore
                }
            }
        }
	return contextRoots;

    }
    

    /* Set Health Monitoring Configurations */
    private void setHealthMonitoringAttributesforClusters(
            List<String> clusters, Properties appConfigProperties)
            throws IOException {
        String appName = null;
        String interval = null;
        String timeout = null;
        Properties workerProperties = readPropertiesFile(System.getProperty("user.dir")
                + File.separator + "worker.properties");
        String cluster = null;

        for (int i = 0; i < clusters.size(); i++) {
            appName = clusters.get(i).substring(1, clusters.get(i).indexOf("="));
            cluster = clusters.get(i).substring(
                    clusters.get(i).indexOf("=") + 1, clusters.get(i).length());
            interval = appConfigProperties.getProperty(appName + "." + Constants.HEALTH_CHECK_INTERVAL_PROP_NAME);
            if (interval == null) {
                interval = appConfigProperties.getProperty(Constants.HEALTH_CHECK_INTERVAL_PROP_NAME);
            }
            timeout = appConfigProperties.getProperty(appName + "." + Constants.HEALTH_CHECK_TIMEOUT_PROP_NAME);
            if (timeout == null) {
                timeout = appConfigProperties.getProperty(Constants.HEALTH_CHECK_TIMEOUT_PROP_NAME);
            }

            if (interval == null && timeout == null) {
                continue;
            }
            
            workerProperties.setProperty(
                    "worker." + cluster + ".ping_mode", "I");
            workerProperties.setProperty(
                    "worker." + cluster
                    + ".connection_ping_interval",
                    (interval != null ? interval : DEFAULT_PING_INTERVAL));

            workerProperties.setProperty("worker." + cluster
                    + ".ping_timeout",
                    (timeout != null ? timeout : DEFAULT_PING_TIMEOUT));

        }

        FileOutputStream fout = new FileOutputStream(new File(
                System.getProperty("user.dir") + File.separator
                + "worker.properties"));
        try {
            workerProperties.store(fout, "worker.properties");
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException ex) {
                    //ignore
                }
            }
        }

    }

    /* Create VS entry per app */
    private void createVirtualServerEntryPerAppDomain(String httpPort, List<String> contextRoots,Properties appDomainConfigProps,boolean skipCertEntry)
	    throws IOException {
	String contextRoot = null;
	String appName = null;
	String domainName = null;
	FileWriter vhostsConf = null;
	BufferedWriter out = null;
	
	try {
	    vhostsConf = new FileWriter(System.getProperty("user.dir")

	    + fileSep + GLASSFISH_VHOSTS_CONF, true);
	    out = new BufferedWriter(vhostsConf);

	    out.write("NameVirtualHost *:" + httpPort);
	    out.newLine();
	    for (int i = 0; i < contextRoots.size(); i++) {
		contextRoot = contextRoots.get(i).replace("=", " ");
		appName = contextRoots.get(i).substring(1,
			contextRoots.get(i).indexOf("="));
		domainName = appDomainConfigProps.getProperty("app." + appName
			+ ".domain-name");
		out.newLine();
		out.write("<VirtualHost *:" + httpPort + " >");
		out.newLine();
		out.write("\tServerName " + domainName);
		out.newLine();
		out.write("\tRewriteEngine on");
		out.newLine();
		out.write("\tRewriteRule ^(.*)$ /" + appName + "$1 [PT]");
		out.newLine();
		if (!skipCertEntry) {
		    out.write("\tSSLEngine on");
		    out.newLine();
		    out.write("\tSSLCertificateFile " + apacheConfigDir
			    + fileSep + domainName + "-server.crt");
		    out.newLine();
		    out.write("\tSSLCertificateKeyFile " + apacheConfigDir
			    + fileSep + domainName + "-server.key");
		    out.newLine();
		}
		out.write("\tJkMount /* "
			+ contextRoot.substring(contextRoot.indexOf(" ") + 1));
		out.newLine();
		out.write("</VirtualHost>");
		out.newLine();
		out.newLine();
	    }
	} finally {
	    if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    //ignore
                }
	    }
	    if (vhostsConf != null) {
                try {
                    vhostsConf.close();
                } catch (IOException ex) {
                    //ignore
                }
	    }
	}

    }

    /* Create VS entry for LB */
    private void createVirtualServerEntryForLBDomain(String domainName,
	    String httpPort, List<String> contextRoots) throws IOException {
	String contextRoot = null;
	FileWriter vhostsConf = null;
	BufferedWriter out = null;

	try {
	    vhostsConf = new FileWriter(System.getProperty("user.dir")
		    + fileSep + GLASSFISH_VHOSTS_CONF);
	    out = new BufferedWriter(vhostsConf);

	    out.write("NameVirtualHost *:" + httpPort);
	    out.newLine();
	    out.newLine();
	    out.write("<VirtualHost *:" + httpPort + " >");
	    out.newLine();
	    out.write("\tServerName " + domainName);
	    out.newLine();
	    for (int i = 0; i < contextRoots.size(); i++) {
		contextRoot = contextRoots.get(i).replace("=", " ");
		out.write("\tJkMount " + contextRoot);
		out.newLine();
		out.write("\tJkMount " + contextRoot.replace(" ", "/* "));
		out.newLine();
	    }
	    out.write("</VirtualHost>");
	    out.newLine();
	} finally {
	    if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    //ignore
                }
	    }

	}

    }

    /* Comment include statement for vhosts */
    private void disableVhostsInSSLConf(VirtualMachine virtualMachine)
	    throws IOException, InterruptedException {

	String curDir = System.getProperty("user.dir");
	virtualMachine.download(new File(apacheConfigDir + fileSep + "extra"
		+ fileSep + "httpd-ssl.conf"), new File(curDir));
	fileContentReplacer(curDir + File.separator + "httpd-ssl.conf",
		"Include conf/glassfish-vhosts.conf",
		"#Include conf/glassfish-vhosts.conf");
	virtualMachine.upload(new File(curDir + File.separator
		+ "httpd-ssl.conf"), new File(apacheConfigDir + fileSep
		+ "extra"));
        deleteTempFile(new File(curDir + File.separator + "httpd-ssl.conf"));
    }

    /* Store the mappings to glassfish-vhosts.conf */
    private void storeContextRootMappings(List<String> contextRoots)
	    throws IOException {
	String contextRoot = null;
	FileWriter vhostsConf = null;
	BufferedWriter out = null;
	try {
	    vhostsConf = new FileWriter(System.getProperty("user.dir")
		    + fileSep + GLASSFISH_VHOSTS_CONF);
	    out = new BufferedWriter(vhostsConf);
	    for (int i = 0; i < contextRoots.size(); i++) {
		contextRoot = contextRoots.get(i).replace("=", " ");
		;
		out.write("\tJkMount " + contextRoot);
		out.newLine();
		out.write("\tJkMount " + contextRoot.replace(" ", "/* "));
		out.newLine();
	    }
	} finally {
	    if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    //ignore
                }
	    }
	    if (vhostsConf != null) {
                try {
                    vhostsConf.close();
                } catch (IOException ex) {
                    //ignore
                }
	    }
	}

    }

    /* Load the properties file */
    public Properties readPropertiesFile(String propFile) {
	Properties prop = new Properties();
	FileInputStream is = null;
	try {
	    is = new FileInputStream(propFile);
	    prop.load(is);
	    return prop;
	} catch (Exception e) {
	    LBPluginLogger.getLogger().log(Level.FINE,
		    "Failed to read from " + propFile + " file.");

	} finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    //ignore
                }
            }
	}
	return prop;
    }

    /* Content replacer */
    private void fileContentReplacer(String fileName, String oldText,
	    String newText) throws IOException {
	String fileContent = null;
	String line = null;
	FileReader in = null;
	BufferedReader brd = null;
	BufferedWriter bwd = null;
	FileWriter out = null;
	File newFile = new File(fileName);
	try {
	    in = new FileReader(newFile);

	    brd = new BufferedReader(in);

	    StringBuilder contents = new StringBuilder();

	    while ((line = brd.readLine()) != null) {
		contents.append(line);
		contents.append(System.getProperty("line.separator"));
	    }
	    fileContent = contents.toString();
	    fileContent = fileContent.replaceAll(oldText, newText);
	    out = new FileWriter(fileName);
	    bwd = new BufferedWriter(out);
	    bwd.write(fileContent);
	} finally {
	    if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    //ignore
                }
	    }
	    if (brd != null) {
                try {
                    brd.close();
                } catch (IOException ex) {
                    //ignore
                }
	    }
	    if (bwd != null) {
                try {
                    bwd.close();
                } catch (IOException ex) {
                    //ignore
                }
	    }
	    if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    //ignore
                }
	    }

	}

    }
    
    public void deleteTempFile(File tempFile) {
        boolean isClean = tempFile.delete();
        if (isClean) {
            LBPluginLogger.getLogger()
                    .log(Level.FINEST, "Temp File cleaned up");
        } else {
            LBPluginLogger.getLogger().log(Level.FINEST,
                    "Temp File clean failed");
        }
    }

}
