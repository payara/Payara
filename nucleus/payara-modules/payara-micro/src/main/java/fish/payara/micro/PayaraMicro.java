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

import static com.sun.enterprise.glassfish.bootstrap.StaticGlassFishRuntime.copy;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;

/**
 * Main class for Bootstrapping Payara Micro Edition
 *
 * @author steve
 */
public class PayaraMicro {

    private static Logger logger = Logger.getLogger(PayaraMicro.class.getName());
    private String hzMulticastGroup;
    private int hzPort = Integer.MIN_VALUE;
    private int hzStartPort = Integer.MIN_VALUE;
    private int httpPort = Integer.MIN_VALUE;
    private int sslPort = Integer.MIN_VALUE;
    private int maxHttpThreads = Integer.MIN_VALUE;
    private int minHttpThreads = Integer.MIN_VALUE;
    private String instanceName = UUID.randomUUID().toString();
    private File rootDir;
    private File deploymentRoot;
    private File alternateDomainXML;
    private List<File> deployments;
    private GlassFish gf;
    private boolean noCluster = false;
    private static PayaraMicro instance;

    public static void main(String args[]) throws BootstrapException {
        PayaraMicro main = getInstance();
        main.scanArgs(args);
        main.bootStrap();
    }

    public static PayaraMicro getInstance() {
        return getInstance(true);
    }

    public static PayaraMicro getInstance(boolean create) {
        if (instance == null) {
            instance = new PayaraMicro();
        }
        return instance;
    }

    private PayaraMicro() {
        addShutdownHook();
    }

    private PayaraMicro(String args[]) {
        scanArgs(args);
        addShutdownHook();
    }

    public String getClusterMulticastGroup() {
        return hzMulticastGroup;
    }

    public PayaraMicro setClusterMulticastGroup(String hzMulticastGroup) {
        this.hzMulticastGroup = hzMulticastGroup;
        return this;
    }

    public int getClusterPort() {
        return hzPort;
    }

    public PayaraMicro setClusterPort(int hzPort) {
        this.hzPort = hzPort;
        return this;
    }

    public int getClusterStartPort() {
        return hzStartPort;
    }

    public PayaraMicro setClusterStartPort(int hzStartPort) {
        this.hzStartPort = hzStartPort;
        return this;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public PayaraMicro setHttpPort(int httpPort) {
        this.httpPort = httpPort;
        return this;
    }

    public int getSslPort() {
        return sslPort;
    }

    public PayaraMicro setSslPort(int sslPort) {
        this.sslPort = sslPort;
        return this;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public PayaraMicro setInstanceName(String instanceName) {
        this.instanceName = instanceName;
        return this;
    }

    public File getDeploymentDir() {
        return deploymentRoot;
    }

    public PayaraMicro setDeploymentDir(File deploymentRoot) {
        this.deploymentRoot = deploymentRoot;
        return this;
    }

    public File getAlternateDomainXML() {
        return alternateDomainXML;
    }

    public PayaraMicro setAlternateDomainXML(File alternateDomainXML) {
        this.alternateDomainXML = alternateDomainXML;
        return this;
    }

    public PayaraMicro addDeployment(String pathToWar) {
        File file = new File(pathToWar);
        return addDeploymentFile(file);
    }

    public PayaraMicro addDeploymentFile(File file) {

        if (deployments == null) {
            deployments = new LinkedList<>();
        }
        deployments.add(file);
        return this;
    }

    public boolean isNoCluster() {
        return noCluster;
    }

    public PayaraMicro setNoCluster(boolean noCluster) {
        this.noCluster = noCluster;
        return this;
    }

    public int getMaxHttpThreads() {
        return maxHttpThreads;
    }

    public PayaraMicro setMaxHttpThreads(int maxHttpThreads) {
        this.maxHttpThreads = maxHttpThreads;
        return this;
    }

    public int getMinHttpThreads() {
        return minHttpThreads;
    }

    public PayaraMicro setMinHttpThreads(int minHttpThreads) {
        this.minHttpThreads = minHttpThreads;
        return this;
    }

    private void scanArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--port".equals(arg)) {
                String httpPortS = args[i + 1];
                try {
                    httpPort = Integer.parseInt(httpPortS);
                    if (httpPort < 1 || httpPort > 65535) {
                        throw new NumberFormatException("Not a valid tcp port");
                    }
                } catch (NumberFormatException nfe) {
                    System.err.println(httpPortS + " is not a valid http port number and will be ignored");
                    httpPort = Integer.MIN_VALUE;
                }
                i++;
            } else if ("--sslPort".equals(arg)) {
                String httpPortS = args[i + 1];
                try {
                    sslPort = Integer.parseInt(httpPortS);
                    if (sslPort < 1 || sslPort > 65535) {
                        throw new NumberFormatException("Not a valid tcp port");
                    }
                } catch (NumberFormatException nfe) {
                    System.err.println(httpPortS + " is not a valid ssl port number and will be ignored");
                    sslPort = Integer.MIN_VALUE;
                }
                i++;
            } else if ("--maxHttpThreads".equals(arg)) {
                String threads = args[i + 1];
                try {
                    maxHttpThreads = Integer.parseInt(threads);
                    if (maxHttpThreads < 2) {
                        throw new NumberFormatException("Maximum Threads must be 2 or greater");
                    }
                } catch (NumberFormatException nfe) {
                    System.err.println(threads + " is not a valid maximum threads number and will be ignored");
                    maxHttpThreads = Integer.MIN_VALUE;
                }
                i++;
            } else if ("--minHttpThreads".equals(arg)) {
                String threads = args[i + 1];
                try {
                    minHttpThreads = Integer.parseInt(threads);
                    if (minHttpThreads < 0) {
                        throw new NumberFormatException("Minimum Threads must be zero or greater");
                    }
                } catch (NumberFormatException nfe) {
                    System.err.println(threads + " is not a valid minimum threads number and will be ignored");
                    minHttpThreads = Integer.MIN_VALUE;
                }
                i++;
            } else if ("--mcAddress".equals(arg)) {
                hzMulticastGroup = args[i + 1];
                i++;
            } else if ("--mcPort".equals(arg)) {
                String httpPortS = args[i + 1];
                try {
                    hzPort = Integer.parseInt(httpPortS);
                    if (hzPort < 1 || hzPort > 65535) {
                        throw new NumberFormatException("Not a valid tcp port");
                    }
                } catch (NumberFormatException nfe) {
                    System.err.println(httpPortS + " is not a valid multicast port number and will be ignored");
                    hzPort = Integer.MIN_VALUE;
                }
                i++;
            } else if ("--startPort".equals(arg)) {
                String startPort = args[i + 1];
                try {
                    hzStartPort = Integer.parseInt(startPort);
                    if (hzStartPort < 1 || hzStartPort > 65535) {
                        throw new NumberFormatException("Not a valid tcp port");
                    }
                } catch (NumberFormatException nfe) {
                    System.err.println(startPort + " is not a valid port number and will be ignored");
                    hzStartPort = Integer.MIN_VALUE;
                }
                i++;
            } else if ("--name".equals(arg)) {
                instanceName = args[i + 1];
                i++;
            } else if (("--deploymentDir".equals(arg))) {
                deploymentRoot = new File(args[i + 1]);
                if (!deploymentRoot.exists() || !deploymentRoot.isDirectory()) {
                    System.err.println(args[i + 1] + " is not a valid deployment directory and will be ignored");
                    deploymentRoot = null;
                }
                i++;
            } else if (("--rootDir".equals(arg))) {
                rootDir = new File(args[i + 1]);
                if (!rootDir.exists() || !rootDir.isDirectory()) {
                    System.err.println(args[i + 1] + " is not a valid root directory and will be ignored");
                    rootDir = null;
                }
                i++;
            } else if ("--deploy".equals(arg)) {
                File deployment = new File(args[i + 1]);
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
                alternateDomainXML = new File(args[i] + 1);
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
                        + "--startPort sets the cluster start port number\n"
                        + "--name sets the instance name\n"
                        + "--rootDir Sets the root configuration directory and saves the configuration across restarts\n"
                        + "--deploymentDir if set to a valid directory all war files in this directory will be deployed\n"
                        + "--deploy specifies a war file to deploy\n"
                        + "--domainConfig overrides the complete server configuration with an alternative domain.xml file\n"
                        + "--minHttpThreads the minimum number of threads in the HTTP thread pool"
                        + "--maxHttpThreads the maximum number of threads in the HTTP thread pool"
                        + "--help Shows this message and exits\n");
                System.exit(-1);
            }
        }
    }

    public void bootStrap() throws BootstrapException {
        BootstrapProperties bprops = new BootstrapProperties();
        GlassFishRuntime runtime;
        try {
            runtime = GlassFishRuntime.bootstrap();
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

            if (rootDir != null) {
                gfproperties.setInstanceRoot(rootDir.getAbsolutePath());
                File configFile = new File(rootDir.getAbsolutePath() + File.separator + "config" + File.separator + "domain.xml");
                if (!configFile.exists()) {
                    installFiles(gfproperties);
                } else {
                    gfproperties.setConfigFileURI("file://" + rootDir.getAbsolutePath() + File.separator + "config" + File.separator + "domain.xml");
                }

            }

            if (this.hzPort != Integer.MIN_VALUE) {
                gfproperties.setProperty("embedded-glassfish-config.server.hazelcast-runtime-configuration.multicastPort", Integer.toString(hzPort));
            }

            if (this.hzMulticastGroup != null) {
                gfproperties.setProperty("embedded-glassfish-config.server.hazelcast-runtime-configuration.multicastGroup", hzMulticastGroup);
            }
            
            if (this.maxHttpThreads != Integer.MIN_VALUE) {
                gfproperties.setProperty("embedded-glassfish-config.server.thread-pools.thread-pool.http-thread-pool.max-thread-pool-size", Integer.toString(maxHttpThreads));
            } 
            
            if (this.minHttpThreads != Integer.MIN_VALUE) {
                gfproperties.setProperty("embedded-glassfish-config.server.thread-pools.thread-pool.http-thread-pool.min-thread-pool-size", Integer.toString(minHttpThreads));
            }   
            

            gf = runtime.newGlassFish(gfproperties);
            gf.start();
            deployAll();
        } catch (GlassFishException ex) {
            throw new BootstrapException(ex.getMessage(), ex);
        }
    }

    private void deployAll() throws GlassFishException {
        // deploy explicit wars first.
        int deploymentCount = 0;
        Deployer deployer = gf.getDeployer();
        if (deployments != null) {
            for (File war : deployments) {
                if (war.exists() && war.isFile() && war.canRead()) {
                    deployer.deploy(war, "--availabilityenabled=true");
                    deploymentCount++;
                } else {
                    logger.warning(war.getAbsolutePath() + " is not a valid deployment");
                }
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

    private void installFiles(GlassFishProperties gfproperties) {
        // make directories
        File configDir = new File(rootDir.getAbsolutePath(), "config");
        new File(rootDir.getAbsolutePath(), "docroot").mkdirs();
        configDir.mkdirs();
        String[] configFiles = new String[]{"config/keyfile",
            "config/server.policy",
            "config/cacerts.jks",
            "config/keystore.jks",
            "config/login.conf",
            "config/logging.properties",
            "config/admin-keyfile",
            "config/default-web.xml",
            "org/glassfish/embed/domain.xml"
        };

        /**
         * Copy all the config files from uber jar to the instanceConfigDir
         */
        ClassLoader cl = getClass().getClassLoader();
        for (String configFile : configFiles) {
            URL url = cl.getResource(configFile);
            if (url != null) {
                copy(url, new File(configDir.getAbsoluteFile(),
                        configFile.substring(configFile.lastIndexOf('/') + 1)), false);
            }
        }

        // copy branding file if available
        URL brandingUrl = cl.getResource("config/branding/glassfish-version.properties");
        if (brandingUrl != null) {
            copy(brandingUrl, new File(configDir.getAbsolutePath(), "branding/glassfish-version.properties"), false);
        }

        //Copy in the relevant domain.xml
        String configFileURI = gfproperties.getConfigFileURI();
        try {
            copy(URI.create(configFileURI).toURL(),
                    new File(configDir.getAbsolutePath(), "domain.xml"), true);
        } catch (MalformedURLException ex) {
            Logger.getLogger(PayaraMicro.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
