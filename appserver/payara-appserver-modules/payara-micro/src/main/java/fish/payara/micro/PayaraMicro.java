/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2015 C2B2 Consulting Limited. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.micro;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;



/**
 * Main class for Bootstrapping Payara Micro Edition
 * @author steve
 */
public class PayaraMicro {
    
    private static Logger logger = Logger.getLogger(PayaraMicro.class.getName());
    private String hzMulticastGroup;
    private int hzPort = Integer.MIN_VALUE;
    private int httpPort = Integer.MIN_VALUE;
    private int sslPort = Integer.MIN_VALUE;
    private String instanceName;
    private File deploymentRoot;
    private File alternateDomainXML;
    private List<File> deployments;
    private GlassFish gf;
    private boolean noCluster = false;
    
    public static void main(String args[]) throws GlassFishException{
        PayaraMicro main = new PayaraMicro(args);
        main.bootStrap();
    }
    
    public PayaraMicro() {
        addShutdownHook();        
    }
    
    public PayaraMicro(List<File> deployments) {
        this.deployments = deployments;
        addShutdownHook();  
    }

    public PayaraMicro(String args[]) {
        scanArgs(args);
        addShutdownHook();  
    }

    public String getClusterMulticastGroup() {
        return hzMulticastGroup;
    }

    public void setClusterMulticastGroup(String hzMulticastGroup) {
        this.hzMulticastGroup = hzMulticastGroup;
    }

    public int getClusterPort() {
        return hzPort;
    }

    public void setClusterPort(int hzPort) {
        this.hzPort = hzPort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public int getSslPort() {
        return sslPort;
    }

    public void setSslPort(int sslPort) {
        this.sslPort = sslPort;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public File getDeploymentDir() {
        return deploymentRoot;
    }

    public void setDeploymentDir(File deploymentRoot) {
        this.deploymentRoot = deploymentRoot;
    }

    public File getAlternateDomainXML() {
        return alternateDomainXML;
    }

    public void setAlternateDomainXML(File alternateDomainXML) {
        this.alternateDomainXML = alternateDomainXML;
    }

    public boolean isNoCluster() {
        return noCluster;
    }

    public void setNoCluster(boolean noCluster) {
        this.noCluster = noCluster;
    }

    private void scanArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--port".equals(arg)) {
                String httpPortS = args[i+1];
                try {
                httpPort = Integer.parseInt(httpPortS);
                    if (httpPort < 1 || httpPort > 65535) {
                        throw new NumberFormatException("Not a valid tcp port");
                    }
                }catch(NumberFormatException nfe) {
                    System.err.println(httpPortS + " is not a valid http port number and will be ignored");
                }
                i++;
            } else if ("--sslPort".equals(arg)) {
                String httpPortS = args[i+1];
                try {
                sslPort = Integer.parseInt(httpPortS);
                    if (sslPort < 1 || sslPort > 65535) {
                        throw new NumberFormatException("Not a valid tcp port");
                    }
                }catch(NumberFormatException nfe) {
                    System.err.println(httpPortS + " is not a valid ssl port number and will be ignored");
                }
                i++;                
            } else if ("--mcAddress".equals(arg)) {
                hzMulticastGroup = args[i+1];
                i++;
            } else if ("--mcPort".equals(arg)) {
                String httpPortS = args[i+1];
                try {
                hzPort = Integer.parseInt(httpPortS);
                    if (hzPort < 1 || hzPort > 65535) {
                        throw new NumberFormatException("Not a valid tcp port");
                    }
                }catch(NumberFormatException nfe) {
                    System.err.println(httpPortS + " is not a valid multicast port number and will be ignored");
                }
                i++;                   
            } else if ("--name".equals(arg)) {
                instanceName = args[i+1];
                i++;
            } else if (("--deploymentDir".equals(arg))) {
                deploymentRoot = new File(args[i+1]);
                if (!deploymentRoot.exists() || !deploymentRoot.isDirectory()) {
                    System.err.println(args[i+1] + " is not a valid deployment directory and will be ignored");
                    deploymentRoot = null;
                }
                i++;
            } else if ("--deploy".equals(arg)) {
                File deployment = new File(args[i+1]);
                if (!deployment.exists() || !deployment.isFile() || !deployment.canRead() || !deployment.getAbsolutePath().endsWith(".war")) {
                    System.err.println(deployment.getAbsolutePath() + " is not a valid deployment path and will be ignored");
                } else {
                    if (deployments == null) {
                        deployments = new LinkedList<>();
                    }
                    deployments.add(deployment);
                }
                i++;
            } else if ("--domainConfig".equals(arg)) {
                alternateDomainXML = new File(args[i]+1);
                if (!alternateDomainXML.exists() || !alternateDomainXML.isFile() || !alternateDomainXML.canRead() || !alternateDomainXML.getAbsolutePath().endsWith(".xml")) {
                    System.err.println(alternateDomainXML.getAbsolutePath() + " is not a valid path to an xml file and will be ignored");  
                    alternateDomainXML = null;
                }
                i++;
            } else if ("--noCluster".equals(arg)) {
                noCluster = true;
            } else if ("--help".equals(arg)) {
                System.err.println("Usage: --noCluster  Disables clustering\n"
                        + "--port sets the http port\n"
                        + "--sslPort sets the https port number\n"
                        + "--mcAddress sets the cluster multicast group\n"
                        + "--mcPort sets the cluster multicast port\n"
                        + "--name sets the instance name\n"
                        + "--deploymentDir if set to a valid directory all war files in this directory will be deployed\n"
                        + "--deploy specifies a war file to deploy\n"
                        + "--domainConfig overrides the complete server configuration with an alternative domain.xml file\n"
                        + "--help Shows this message and exits\n");
                System.exit(-1);
            }
        }
    }

    public void bootStrap() throws GlassFishException {
        BootstrapProperties bprops = new BootstrapProperties();
        GlassFishRuntime runtime = GlassFishRuntime.bootstrap();
        GlassFishProperties gfproperties = new GlassFishProperties();
        if (httpPort != Integer.MIN_VALUE) {
            gfproperties.setPort("http-listener", httpPort);            
        }
        
        if (sslPort != Integer.MIN_VALUE) {
            gfproperties.setPort("https-listener", sslPort);            
            
        }
        
        if (alternateDomainXML != null) {
            gfproperties.setConfigFileURI("file://" + alternateDomainXML.getAbsolutePath());            
        } else {
            if (noCluster) {
                gfproperties.setConfigFileURI(ClassLoader.getSystemResource("microdomain-nocluster.xml").toExternalForm());
                
            } else {
                gfproperties.setConfigFileURI(ClassLoader.getSystemResource("microdomain.xml").toExternalForm());
            }
        }
        gf = runtime.newGlassFish(gfproperties);
        gf.start(); 
        deployAll();
    }

    private void deployAll() throws GlassFishException {
        // deploy explicit wars first.
        int deploymentCount = 0;
        Deployer deployer = gf.getDeployer();
        if (deployments != null) {
            for (File war : deployments) {
                deployer.deploy(war, "--availabilityenabled=true");
                deploymentCount++;
            }            
        }
        
        // deploy from deployment director
        if (deploymentRoot != null) {
            for (File war : deploymentRoot.listFiles()) {
                if (war.isFile() && war.canRead() && war.getAbsolutePath().endsWith(".war")) {
                    deployer.deploy(war, "--availabilityenabled=true");
                    deploymentCount++;
                }
            }
        }
        logger.info("Deployed " + deploymentCount + " wars");
    }
    
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(
                "GlassFish Shutdown Hook") {
            public void run() {
                try {
                    if (gf != null) {
                        gf.stop();
                        gf.dispose();
                    }
                } catch (Exception ex) {
                }
            }
        });
    }
    
}
