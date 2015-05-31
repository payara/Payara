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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;

/**
 * Main class for Bootstrapping Payara Micro Edition This class is used from
 * applications to creare a full JavaEE runtime environment and deploywar files.
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
    private transient GlassFish gf;
    private boolean noCluster = false;
    private static PayaraMicro instance;

    /**
     * Runs a Payara Micro server used via java -jar payara-micro.jar
     *
     * @param args Command line arguments for PayaraMicro Usage: --noCluster
     * Disables clustering<br/>
     * --port sets the http port<br/>
     * --sslPort sets the https port number<br/>
     * --mcAddress sets the cluster multicast group<br/>
     * --mcPort sets the cluster multicast port<br/>
     * --startPort sets the cluster start port number<br/>
     * --name sets the instance name<br/>
     * --rootDir Sets the root configuration directory and saves the
     * configuration across restarts<br/>
     * --deploymentDir if set to a valid directory all war files in this
     * directory will be deployed<br/>
     * --deploy specifies a war file to deploy<br/>
     * --domainConfig overrides the complete server configuration with an
     * alternative domain.xml file<br/>
     * --minHttpThreads the minimum number of threads in the HTTP thread
     * pool<br/>
     * --maxHttpThreads the maximum number of threads in the HTTP thread
     * pool<br/>
     * --help Shows this message and exits\n
     * @throws BootstrapException If there is a problem booting the server
     */
    public static void main(String args[]) throws BootstrapException {
        PayaraMicro main = getInstance();
        main.scanArgs(args);
        main.bootStrap();
    }

    /**
     * Obtains the static singleton instance of the Payara Micro Server. If it
     * does not exist it will be create.
     *
     * @return The singleton instance
     */
    public static PayaraMicro getInstance() {
        return getInstance(true);
    }

    /**
     *
     * @param create If false the instance won't be created if it has not been
     * initialised
     * @return null if no instance exists and create is false. Otherwise returns
     * the singleton instance
     */
    public static PayaraMicro getInstance(boolean create) {
        if (instance == null) {
            instance = new PayaraMicro();
        }
        return instance;
    }

    /**
     * Gets the cluster group
     * @return The Multicast Group that will beused for the Hazelcast clustering 
     */
    public String getClusterMulticastGroup() {
        return hzMulticastGroup;
    }

    /**
     * Sets the cluster group used for Payara Micro clustering
     * used for cluster communications and discovery. Each Payara Micro cluster should
     * have different values for the MulticastGroup
     * @param hzMulticastGroup String representation of the multicast group 
     * @return 
     */
    public PayaraMicro setClusterMulticastGroup(String hzMulticastGroup) {
        this.hzMulticastGroup = hzMulticastGroup;
        return this;
    }

    /**
     * Gets the cluster multicast  port used for cluster communications
     * @return The configured cluster port
     */
    public int getClusterPort() {
        return hzPort;
    }

    /**
     * Sets the multicast group used for Payara Micro clustering used for cluster
     * communication and discovery. Each Payara Micro cluster should have different
     * values for the cluster port
     * @param hzPort The port number
     * @return 
     */
    public PayaraMicro setClusterPort(int hzPort) {
        this.hzPort = hzPort;
        return this;
    }

    /**
     * Gets the instance listen port number used by clustering. 
     * This number will be incremented automatically
     * if the port is unavailable due to another instance running on the same host,
     * @return The start port number
     */
    public int getClusterStartPort() {
        return hzStartPort;
    }

    /**
     * Sets the start port number for the Payara Micro to listen on for cluster
     * communications.
     * @param hzStartPort Start port number
     * @return 
     */
    public PayaraMicro setClusterStartPort(int hzStartPort) {
        this.hzStartPort = hzStartPort;
        return this;
    }

    /**
     * The configured port Payara Micro will use for HTTP requests.
     * @return The HTTP port
     */
    public int getHttpPort() {
        return httpPort;
    }

    /**
     * Sets the port used for HTTP requests
     * @param httpPort The port number
     * @return 
     */
    public PayaraMicro setHttpPort(int httpPort) {
        this.httpPort = httpPort;
        return this;
    }

    /**
     * The configured port for HTTPS requests
     * @return  The HTTPS port
     */
    public int getSslPort() {
        return sslPort;
    }

    /**
     * Sets the configured port for HTTPS requests.
     * If this is not set HTTPS is disabled
     * @param sslPort The HTTPS port
     * @return 
     */
    public PayaraMicro setSslPort(int sslPort) {
        this.sslPort = sslPort;
        return this;
    }

    /**
     * Gets the logical name for this PayaraMicro Server within the server cluster
     * @return The configured instance name
     */
    public String getInstanceName() {
        return instanceName;
    }

    /**
     * Sets the logical instance name for this PayaraMicro server within the server cluster
     * If this is not set a UUID is generated
     * @param instanceName The logical server name
     * @return 
     */
    public PayaraMicro setInstanceName(String instanceName) {
        this.instanceName = instanceName;
        return this;
    }

    /**
     * A directory which will be scanned for archives to deploy
     * @return 
     */
    public File getDeploymentDir() {
        return deploymentRoot;
    }

    /**
     * Sets a directory to scan for archives to deploy on boot. This directory is not monitored 
     * while running for changes. Therefore archives in this directory will NOT be redeployed during runtime.
     * @param deploymentRoot File path to the directory
     * @return 
     */
    public PayaraMicro setDeploymentDir(File deploymentRoot) {
        this.deploymentRoot = deploymentRoot;
        return this;
    }

    /**
     * The path to an alternative domain.xml for PayaraMicro to use at boot
     * @return The path to the domain.xml
     */
    public File getAlternateDomainXML() {
        return alternateDomainXML;
    }

    /**
     * Sets the path to a domain.xml file PayaraMicro should use to boot. If this is not
     * set PayaraMicro will use an appropriate domain.xml from within its jar file
     * @param alternateDomainXML
     * @return 
     */
    public PayaraMicro setAlternateDomainXML(File alternateDomainXML) {
        this.alternateDomainXML = alternateDomainXML;
        return this;
    }

    /**
     * Adds an archive to the list of archives to be deployed at boot. These archives are not 
     * monitored for changes during running so are not redeployed without restarting the server
     * @param pathToWar File path to the deployment archive
     * @return 
     */
    public PayaraMicro addDeployment(String pathToWar) {
        File file = new File(pathToWar);
        return addDeploymentFile(file);
    }

    /**
     * Adds an archive to the list of archives to be deployed at boot. These archives are not 
     * monitored for changes during running so are not redeployed without restarting the server
     * @param file File path to the deployment archive
     * @return 
     */    
    public PayaraMicro addDeploymentFile(File file) {

        if (deployments == null) {
            deployments = new LinkedList<>();
        }
        deployments.add(file);
        return this;
    }

    /**
     * Indicated whether clustering is enabled
     * @return 
     */
    public boolean isNoCluster() {
        return noCluster;
    }

    /**
     * Enables or disables clustering before bootstrap
     * @param noCluster set to true to disable clustering
     * @return 
     */
    public PayaraMicro setNoCluster(boolean noCluster) {
        this.noCluster = noCluster;
        return this;
    }

    /**
     * The maximum threads in the HTTP(S) threadpool processing HTTP(S) requests.
     * Setting this will determine how many concurrent HTTP requests can be processed.
     * The default value is 200.
     * This value is shared by both HTTP and HTTP(S) requests.
     * @return 
     */
    public int getMaxHttpThreads() {
        return maxHttpThreads;
    }

    /**
     * The maximum threads in the HTTP(S) threadpool processing HTTP(S) requests.
     * Setting this will determine how many concurrent HTTP requests can be processed.
     * The default value is 200
     * @param maxHttpThreads Maximum threads in the HTTP(S) threadpool
     * @return 
     */
    public PayaraMicro setMaxHttpThreads(int maxHttpThreads) {
        this.maxHttpThreads = maxHttpThreads;
        return this;
    }

    /**
     * The minimum number of threads in the HTTP(S) threadpool
     * Default value is 10
     * @return The minimum threads to be created in the threadpool
     */
    public int getMinHttpThreads() {
        return minHttpThreads;
    }

    /**
     * The minimum number of threads in the HTTP(S) threadpool
     * Default value is 10
     * @param minHttpThreads
     * @return 
     */
    public PayaraMicro setMinHttpThreads(int minHttpThreads) {
        this.minHttpThreads = minHttpThreads;
        return this;
    }

    /**
     * The File path to a directory that PayaraMicro should use for storing its
     * configuration files
     * @return 
     */
    public File getRootDir() {
        return rootDir;
    }

    /**
     * Sets the File path to a directory PayaraMicro should use to install its
     * configuration files. If this is set the PayaraMicro configuration files will be
     * stored in the directory and persist across server restarts. If this is not set the
     * configuration files are created in a temporary location and not persisted across server restarts.
     * @param rootDir Path to a valid directory
     */
    public PayaraMicro setRootDir(File rootDir) {
        this.rootDir = rootDir;
        return this;
    }
    

    /**
     * Boots the Payara Micro Server. All parameters are checked at this point
     * @throws BootstrapException 
     */
    public void bootStrap() throws BootstrapException {
        setSystemProperties();
        BootstrapProperties bprops = new BootstrapProperties();
        GlassFishRuntime runtime;
        try {
            runtime = GlassFishRuntime.bootstrap(bprops,Thread.currentThread().getContextClassLoader());
            GlassFishProperties gfproperties = new GlassFishProperties();

            if (httpPort != Integer.MIN_VALUE) {
                gfproperties.setPort("http-listener", httpPort);
            }

            if (sslPort != Integer.MIN_VALUE) {
                gfproperties.setPort("https-listener", sslPort);

            }

            if (alternateDomainXML != null) {
                gfproperties.setConfigFileReadOnly(false);
                gfproperties.setConfigFileURI("file://" + alternateDomainXML.getAbsolutePath());
            } else {
                if (noCluster) {
                    gfproperties.setConfigFileURI(Thread.currentThread().getContextClassLoader().getResource("microdomain-nocluster.xml").toExternalForm());

                } else {
                    gfproperties.setConfigFileURI(Thread.currentThread().getContextClassLoader().getResource("microdomain.xml").toExternalForm());
                }
            }

            if (rootDir != null) {
                gfproperties.setInstanceRoot(rootDir.getAbsolutePath());
                File configFile = new File(rootDir.getAbsolutePath() + File.separator + "config" + File.separator + "domain.xml");
                if (!configFile.exists()) {
                    installFiles(gfproperties);
                } else {
                    gfproperties.setConfigFileURI("file://" + rootDir.getAbsolutePath() + File.separator + "config" + File.separator + "domain.xml");
                    gfproperties.setConfigFileReadOnly(false);
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
            
            // reset logger.
        
            // reset the Log Manager
            File configDir = new File(System.getProperty("com.sun.aas.instanceRoot"),"config");
            File loggingProperties = new File(configDir.getAbsolutePath(), "logging.properties");
            if (loggingProperties.exists() && loggingProperties.canRead() && loggingProperties.isFile()) {
                System.setProperty("java.util.logging.config.file", loggingProperties.getAbsolutePath());
                try {
                    LogManager.getLogManager().readConfiguration();
                } catch (IOException | SecurityException ex) {
                    Logger.getLogger(PayaraMicro.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            gf.start();
            deployAll();
        } catch (GlassFishException ex) {
            throw new BootstrapException(ex.getMessage(), ex);
        } 
    }
    
    /**
     * Stops and then shutsdown the Payara Micro Server
     * @throws BootstrapException 
     */
    public void shutdown() throws BootstrapException {
        try {
            this.gf.dispose();
        } catch (GlassFishException ex) {
            throw new BootstrapException("Unable to stop Payara Micro", ex);
        }
    }

    private PayaraMicro() {
        addShutdownHook();
    }

    private PayaraMicro(String args[]) {
        scanArgs(args);
        addShutdownHook();
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
                alternateDomainXML = new File(args[i + 1]);
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
                        + "--minHttpThreads the minimum number of threads in the HTTP thread pool\n"
                        + "--maxHttpThreads the maximum number of threads in the HTTP thread pool\n"
                        + "--help Shows this message and exits\n");
                System.exit(-1);
            }
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

    private void setSystemProperties() {
        try {
            Properties embeddedBootProperties = new Properties();
            embeddedBootProperties.load(ClassLoader.getSystemResourceAsStream("payara-boot.properties"));
            for (Object key : embeddedBootProperties.keySet()) {
                String keyStr = (String)key;
                if (System.getProperty(keyStr) == null) {
                    System.setProperty(keyStr, embeddedBootProperties.getProperty(keyStr));
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(PayaraMicro.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
