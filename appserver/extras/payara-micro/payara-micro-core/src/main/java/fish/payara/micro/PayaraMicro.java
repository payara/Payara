/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2015-2016 C2B2 Consulting Limited and/or its affiliates.
 All rights reserved.

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
import fish.payara.nucleus.hazelcast.HazelcastCore;
import fish.payara.nucleus.hazelcast.MulticastConfiguration;
import fish.payara.nucleus.phonehome.PhoneHomeCore;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import fish.payara.nucleus.healthcheck.HealthCheckService;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.JarURLConnection;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFish.Status;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import com.sun.appserv.server.util.Version;
import java.io.FileNotFoundException;

/**
 * Main class for Bootstrapping Payara Micro Edition This class is used from
 * applications to create a full JavaEE runtime environment and deploy war
 * files.
 *
 * This class is used to configure and bootstrap a Payara Micro Runtime.
 *
 * @author steve
 */
public class PayaraMicro {

    private static final Logger logger = Logger.getLogger("PayaraMicro");
    private static PayaraMicro instance;

    private String hzMulticastGroup;
    private int hzPort = Integer.MIN_VALUE;
    private int hzStartPort = Integer.MIN_VALUE;
    private String hzClusterName;
    private String hzClusterPassword;
    private int httpPort = Integer.MIN_VALUE;
    private int sslPort = Integer.MIN_VALUE;
    private int maxHttpThreads = Integer.MIN_VALUE;
    private int minHttpThreads = Integer.MIN_VALUE;
    private String instanceName = UUID.randomUUID().toString();
    private File rootDir;
    private File deploymentRoot;
    private File alternateDomainXML;
    private URI alternateHZConfigFile;
    private List<File> deployments;
    private GlassFish gf;
    private PayaraMicroRuntime runtime;
    private boolean noCluster = false;
    private boolean autoBindHttp = false;
    private boolean autoBindSsl = false;
    private boolean liteMember = false;
    private boolean generateLogo = false;
    private boolean logToFile = false;
    private int autoBindRange = 5;
    private String bootImage = "boot.txt";
    private String applicationDomainXml;
    private boolean enableHealthCheck = false;
    private boolean disablePhoneHome = false;
    private List<String> GAVs;
    private File uberJar;
    private Properties userSystemProperties;
    private Map<String, URL> deploymentURLsMap;
    private List<URL> repositoryURLs;
    private final String defaultMavenRepository = "https://repo.maven.apache.org/maven2/";
    private final short defaultHttpPort = 8080;
    private final short defaultHttpsPort = 8181;
    private String loggingPropertiesFileName = "logging.properties";
    private String loggingToFilePropertiesFileName = "loggingToFile.properties";
    private String userLogFile="payara-server%u.log";
    
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
     * --lite Sets this Payara Micro to not store Cluster Data<br/>
     * --enableHealthCheck enables/disables Health Check Service<br/>
     * --disablePhomeHome disables Phone Home Service<br/>
     * --logToFile outputs all the Log entries to a user defined file<br/>
     * --help Shows this message and exits\n
     * @throws BootstrapException If there is a problem booting the server
     */
    public static void main(String args[]) throws BootstrapException {
        PayaraMicro main = getInstance();
        main.scanArgs(args);

        if (main.getUberJar() != null) {
            main.packageUberJar();
        } else {
            main.bootStrap();
        }
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
     * Bootstraps the PayaraMicroRuntime with all defaults and no additional
     * configuration. Functionally equivalent to
     * PayaraMicro.getInstance().bootstrap();
     *
     * @return
     */
    public static PayaraMicroRuntime bootstrap() throws BootstrapException {
        return getInstance().bootStrap();
    }

    /**
     *
     * @param create If false the instance won't be created if it has not been
     * initialised
     * @return null if no instance exists and create is false. Otherwise returns
     * the singleton instance
     */
    public static PayaraMicro getInstance(boolean create) {
        if (instance == null && create) {
            instance = new PayaraMicro();
        }
        return instance;
    }

    /**
     * Gets the cluster group
     *
     * @return The Multicast Group that will beused for the Hazelcast clustering
     */
    public String getClusterMulticastGroup() {
        return hzMulticastGroup;
    }

    /**
     * Sets the cluster group used for Payara Micro clustering used for cluster
     * communications and discovery. Each Payara Micro cluster should have
     * different values for the MulticastGroup
     *
     * @param hzMulticastGroup String representation of the multicast group
     * @return
     */
    public PayaraMicro setClusterMulticastGroup(String hzMulticastGroup) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        this.hzMulticastGroup = hzMulticastGroup;
        return this;
    }

    /**
     * Sets the path to the logo file printed at boot. This can be on the
     * classpath of the server or an absolute URL
     *
     * @param filePath
     * @return
     */
    public PayaraMicro setLogoFile(String filePath) {
        bootImage = filePath;
        return this;
    }

    /**
     * Set whether the logo should be generated on boot
     *
     * @param generate
     * @return
     */
    public PayaraMicro setPrintLogo(boolean generate) {
        generateLogo = generate;
        return this;
    }
    
    /**
     * Set whether the Log entries can be sent to a file
     *
     * @param log
     * @return
     */
    public PayaraMicro setLogToFile(boolean log) {
        logToFile = log;
        return this;
    }

    /**
     * Set user defined file for the Log entries
     *
     * @param fileName
     */
    public void setUserLogFile(String fileName) {
        if (!fileName.endsWith("/")) {
            this.userLogFile = fileName;
        } else {
            this.userLogFile = fileName + userLogFile;
        }
        logToFile = true;
    }
    
    /**
     * Gets the cluster multicast port used for cluster communications
     *
     * @return The configured cluster port
     */
    public int getClusterPort() {
        return hzPort;
    }

    /**
     * Sets the multicast group used for Payara Micro clustering used for
     * cluster communication and discovery. Each Payara Micro cluster should
     * have different values for the cluster port
     *
     * @param hzPort The port number
     * @return
     */
    public PayaraMicro setClusterPort(int hzPort) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        this.hzPort = hzPort;
        return this;
    }

    /**
     * Gets the instance listen port number used by clustering. This number will
     * be incremented automatically if the port is unavailable due to another
     * instance running on the same host,
     *
     * @return The start port number
     */
    public int getClusterStartPort() {
        return hzStartPort;
    }

    /**
     * Sets the start port number for the Payara Micro to listen on for cluster
     * communications.
     *
     * @param hzStartPort Start port number
     * @return
     */
    public PayaraMicro setClusterStartPort(int hzStartPort) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        this.hzStartPort = hzStartPort;
        return this;
    }

    /**
     * The configured port Payara Micro will use for HTTP requests.
     *
     * @return The HTTP port
     */
    public int getHttpPort() {
        return httpPort;
    }

    /**
     * Sets the port used for HTTP requests
     *
     * @param httpPort The port number
     * @return
     */
    public PayaraMicro setHttpPort(int httpPort) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        this.httpPort = httpPort;
        return this;
    }

    /**
     * The configured port for HTTPS requests
     *
     * @return The HTTPS port
     */
    public int getSslPort() {
        return sslPort;
    }

    /**
     * The UberJar to create
     *
     * @return
     */
    public File getUberJar() {
        return uberJar;
    }

    /**
     * Sets the configured port for HTTPS requests. If this is not set HTTPS is
     * disabled
     *
     * @param sslPort The HTTPS port
     * @return
     */
    public PayaraMicro setSslPort(int sslPort) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        this.sslPort = sslPort;
        return this;
    }

    /**
     * Gets the logical name for this PayaraMicro Server within the server
     * cluster
     *
     * @return The configured instance name
     */
    public String getInstanceName() {
        return instanceName;
    }

    /**
     * Sets the logical instance name for this PayaraMicro server within the
     * server cluster If this is not set a UUID is generated
     *
     * @param instanceName The logical server name
     * @return
     */
    public PayaraMicro setInstanceName(String instanceName) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        this.instanceName = instanceName;
        return this;
    }

    /**
     * A directory which will be scanned for archives to deploy
     *
     * @return
     */
    public File getDeploymentDir() {
        return deploymentRoot;
    }

    /**
     * Sets a directory to scan for archives to deploy on boot. This directory
     * is not monitored while running for changes. Therefore archives in this
     * directory will NOT be redeployed during runtime.
     *
     * @param deploymentRoot File path to the directory
     * @return
     */
    public PayaraMicro setDeploymentDir(File deploymentRoot) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        this.deploymentRoot = deploymentRoot;
        return this;
    }

    /**
     * The path to an alternative domain.xml for PayaraMicro to use at boot
     *
     * @return The path to the domain.xml
     */
    public File getAlternateDomainXML() {
        return alternateDomainXML;
    }

    /**
     * Sets an application specific domain.xml file that is embedded on the
     * classpath of your application.
     *
     * @param domainXml This is a resource string for your domain.xml
     * @return
     */
    public PayaraMicro setApplicationDomainXML(String domainXml) {
        applicationDomainXml = domainXml;
        return this;
    }

    /**
     * Sets the path to a domain.xml file PayaraMicro should use to boot. If
     * this is not set PayaraMicro will use an appropriate domain.xml from
     * within its jar file
     *
     * @param alternateDomainXML
     * @return
     */
    public PayaraMicro setAlternateDomainXML(File alternateDomainXML) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        this.alternateDomainXML = alternateDomainXML;
        return this;
    }

    /**
     * Adds an archive to the list of archives to be deployed at boot. These
     * archives are not monitored for changes during running so are not
     * redeployed without restarting the server
     *
     * @param pathToWar File path to the deployment archive
     * @return
     */
    public PayaraMicro addDeployment(String pathToWar) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        File file = new File(pathToWar);
        return addDeploymentFile(file);
    }

    /**
     * Adds an archive to the list of archives to be deployed at boot. These
     * archives are not monitored for changes during running so are not
     * redeployed without restarting the server
     *
     * @param file File path to the deployment archive
     * @return
     */
    public PayaraMicro addDeploymentFile(File file) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        if (deployments == null) {
            deployments = new LinkedList<>();
        }
        deployments.add(file);
        return this;
    }

    /**
     * Adds a Maven GAV coordinate to the list of archives to be deployed at boot.
     *
     * @param GAV GAV coordinate
     * @return
     */
    public PayaraMicro addDeployFromGAV(String GAV) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        if (GAVs == null) {
            GAVs = new LinkedList<>();
        }
        GAVs.add(GAV);
        if (GAVs != null) {
            try {
                // Convert the provided GAV Strings into target URLs
                getGAVURLs();
            } catch (GlassFishException ex) {
                Logger.getLogger(PayaraMicro.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return this;
    }

    /**
     * Adds a Maven repository to the list of repositories to search for artifacts in 
     *
     * @param URLs URL to Maven repository
     * @return
     */
    public PayaraMicro addRepoUrl(String... URLs){
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        for (String url : URLs){
            try {
                if (!url.endsWith("/")) {
                    repositoryURLs.add(new URL(url + "/"));
                } else {
                    repositoryURLs.add(new URL(url));
                }
            } catch (MalformedURLException ex) {
                logger.log(Level.SEVERE, "{0} is not a valid URL and will be ignored", url);
            }
        }
        return this;
    }

    /**
     * Indicated whether clustering is enabled
     *
     * @return
     */
    public boolean isNoCluster() {
        return noCluster;
    }

    /**
     * Enables or disables clustering before bootstrap
     *
     * @param noCluster set to true to disable clustering
     * @return
     */
    public PayaraMicro setNoCluster(boolean noCluster) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        this.noCluster = noCluster;
        return this;
    }

    /**
     * Indicates whether this is a lite cluster member which means it stores no
     * cluster data although it participates fully in the cluster.
     *
     * @return
     */
    public boolean isLite() {
        return liteMember;
    }

    /**
     * Sets the lite status of this cluster member. If true the Payara Micro is
     * a lite cluster member which means it stores no cluster data.
     *
     * @param liteMember set to true to set as a lite cluster member with no
     * data storage
     * @return
     */
    public PayaraMicro setLite(boolean liteMember) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        this.liteMember = liteMember;
        return this;
    }

    /**
     * The maximum threads in the HTTP(S) threadpool processing HTTP(S)
     * requests. Setting this will determine how many concurrent HTTP requests
     * can be processed. The default value is 200. This value is shared by both
     * HTTP and HTTP(S) requests.
     *
     * @return
     */
    public int getMaxHttpThreads() {
        return maxHttpThreads;
    }

    /**
     * The maximum threads in the HTTP(S) threadpool processing HTTP(S)
     * requests. Setting this will determine how many concurrent HTTP requests
     * can be processed. The default value is 200
     *
     * @param maxHttpThreads Maximum threads in the HTTP(S) threadpool
     * @return
     */
    public PayaraMicro setMaxHttpThreads(int maxHttpThreads) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        this.maxHttpThreads = maxHttpThreads;
        return this;
    }

    /**
     * The minimum number of threads in the HTTP(S) threadpool Default value is
     * 10
     *
     * @return The minimum threads to be created in the threadpool
     */
    public int getMinHttpThreads() {
        return minHttpThreads;
    }

    /**
     * The minimum number of threads in the HTTP(S) threadpool Default value is
     * 10
     *
     * @param minHttpThreads
     * @return
     */
    public PayaraMicro setMinHttpThreads(int minHttpThreads) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        this.minHttpThreads = minHttpThreads;
        return this;
    }

    /**
     * The File path to a directory that PayaraMicro should use for storing its
     * configuration files
     *
     * @return
     */
    public File getRootDir() {
        return rootDir;
    }

    /**
     * Sets the File path to a directory PayaraMicro should use to install its
     * configuration files. If this is set the PayaraMicro configuration files
     * will be stored in the directory and persist across server restarts. If
     * this is not set the configuration files are created in a temporary
     * location and not persisted across server restarts.
     *
     * @param rootDir Path to a valid directory
     * @return Returns the PayaraMicro instance
     */
    public PayaraMicro setRootDir(File rootDir) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        this.rootDir = rootDir;
        return this;
    }

    /**
     * Indicates whether autobinding of the HTTP port is enabled
     *
     * @return
     */
    public boolean getHttpAutoBind() {
        return autoBindHttp;
    }

    /**
     * Enables or disables autobinding of the HTTP port
     *
     * @param httpAutoBind The true or false value to enable or disable HTTP
     * autobinding
     * @return
     */
    public PayaraMicro setHttpAutoBind(boolean httpAutoBind) {
        this.autoBindHttp = httpAutoBind;
        return this;
    }

    /**
     * Indicates whether autobinding of the HTTPS port is enabled
     *
     * @return
     */
    public boolean getSslAutoBind() {
        return autoBindSsl;
    }

    /**
     * Enables or disables autobinding of the HTTPS port
     *
     * @param sslAutoBind The true or false value to enable or disable HTTPS
     * autobinding
     * @return
     */
    public PayaraMicro setSslAutoBind(boolean sslAutoBind) {
        this.autoBindSsl = sslAutoBind;
        return this;
    }

    /**
     * Gets the maximum number of ports to check if free for autobinding
     * purposes
     *
     * @return The number of ports to check if free
     */
    public int getAutoBindRange() {
        return autoBindRange;
    }

    /**
     * Sets the maximum number of ports to check if free for autobinding
     * purposes
     *
     * @param autoBindRange The maximum number of ports to increment the port
     * value by
     * @return
     */
    public PayaraMicro setAutoBindRange(int autoBindRange) {
        this.autoBindRange = autoBindRange;
        return this;
    }

    /**
     * Gets the name of the Hazelcast cluster group.
     * Clusters with different names do not interact
     * @return The current Cluster Name
     */
    public String getHzClusterName() {
        return hzClusterName;
    }

    /**
     * Sets the name of the Hazelcast cluster group
     * @param hzClusterName The name of the hazelcast cluster
     * @return
     */
    public PayaraMicro setHzClusterName(String hzClusterName) {
        this.hzClusterName = hzClusterName;
        return this;
    }

    /**
     * Gets the password of the Hazelcast cluster group
     * @return
     */
    public String getHzClusterPassword() {
        return hzClusterPassword;
    }

    /**
     * Sets the Hazelcast cluster group password.
     * For two clusters to work together then the group name and password must be the same
     * @param hzClusterPassword The password to set
     * @return
     */
    public PayaraMicro setHzClusterPassword(String hzClusterPassword) {
        this.hzClusterPassword = hzClusterPassword;
        return this;
    }

    /**
     * Boots the Payara Micro Server. All parameters are checked at this point
     *
     * @return An instance of PayaraMicroRuntime that can be used to access the
     * running server
     * @throws BootstrapException
     */
    public PayaraMicroRuntime bootStrap() throws BootstrapException {

        long start = System.currentTimeMillis();
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, calling bootstrap now is meaningless");
        }

        // check hazelcast cluster overrides
        MulticastConfiguration mc = new MulticastConfiguration();
        mc.setMemberName(instanceName);
        if (hzPort > Integer.MIN_VALUE) {
            mc.setMulticastPort(hzPort);
        }

        if (hzStartPort > Integer.MIN_VALUE) {
            mc.setStartPort(hzStartPort);
        }

        if (hzMulticastGroup != null) {
            mc.setMulticastGroup(hzMulticastGroup);
        }

        if (alternateHZConfigFile != null) {
            mc.setAlternateConfiguration(alternateHZConfigFile);
        }
        mc.setLite(liteMember);

        if (hzClusterName != null) {
            mc.setClusterGroupName(hzClusterName);
        }

        if (hzClusterPassword != null) {
            mc.setClusterGroupPassword(hzClusterPassword);
        }

        HazelcastCore.setMulticastOverride(mc);

        setSystemProperties();
        BootstrapProperties bprops = new BootstrapProperties();
        GlassFishRuntime gfruntime;
        PortBinder portBinder = new PortBinder();

        try {
            gfruntime = GlassFishRuntime.bootstrap(bprops, Thread.currentThread().getContextClassLoader());
            GlassFishProperties gfproperties = new GlassFishProperties();

            if (httpPort != Integer.MIN_VALUE) {
                if (autoBindHttp == true) {
                    // Log warnings if overriding other options
                    logPortPrecedenceWarnings(false);

                    // Search for an available port from the specified port
                    try {
                        gfproperties.setPort("http-listener",
                                portBinder.findAvailablePort(httpPort,
                                        autoBindRange));
                    } catch (BindException ex) {
                        logger.log(Level.SEVERE, "No available port found in range: "
                                + httpPort + " - "
                                + (httpPort + autoBindRange), ex);

                        throw new GlassFishException("Could not bind HTTP port");
                    }
                } else {
                    // Log warnings if overriding other options
                    logPortPrecedenceWarnings(false);

                    // Set the port as normal
                    gfproperties.setPort("http-listener", httpPort);
                }
            } else if (autoBindHttp == true) {
                // Log warnings if overriding other options
                logPortPrecedenceWarnings(false);

                // Search for an available port from the default HTTP port
                try {
                    gfproperties.setPort("http-listener",
                            portBinder.findAvailablePort(defaultHttpPort,
                                    autoBindRange));
                } catch (BindException ex) {
                    logger.log(Level.SEVERE, "No available port found in range: "
                            + defaultHttpPort + " - "
                            + (defaultHttpPort + autoBindRange), ex);

                    throw new GlassFishException("Could not bind HTTP port");
                }
            }

            if (sslPort != Integer.MIN_VALUE) {
                if (autoBindSsl == true) {
                    // Log warnings if overriding other options
                    logPortPrecedenceWarnings(true);

                    // Search for an available port from the specified port
                    try {
                        gfproperties.setPort("https-listener",
                                portBinder.findAvailablePort(sslPort, autoBindRange));
                    } catch (BindException ex) {
                        logger.log(Level.SEVERE, "No available port found in range: "
                                + sslPort + " - " + (sslPort + autoBindRange), ex);

                        throw new GlassFishException("Could not bind SSL port");
                    }
                } else {
                    // Log warnings if overriding other options
                    logPortPrecedenceWarnings(true);

                    // Set the port as normal
                    gfproperties.setPort("https-listener", sslPort);
                }
            } else if (autoBindSsl == true) {
                // Log warnings if overriding other options
                logPortPrecedenceWarnings(true);

                // Search for an available port from the default HTTPS port
                try {
                    gfproperties.setPort("https-listener",
                            portBinder.findAvailablePort(defaultHttpsPort, autoBindRange));
                } catch (BindException ex) {
                    logger.log(Level.SEVERE, "No available port found in range: "
                            + defaultHttpsPort + " - " + (defaultHttpsPort + autoBindRange), ex);

                    throw new GlassFishException("Could not bind SSL port");
                }
            }           

            if (alternateDomainXML != null) {
                gfproperties.setConfigFileReadOnly(false);
                gfproperties.setConfigFileURI("file:///" + alternateDomainXML.getAbsolutePath().replace('\\', '/'));
            } else if (applicationDomainXml != null) {
                gfproperties.setConfigFileURI(Thread.currentThread().getContextClassLoader().getResource(applicationDomainXml).toExternalForm());
            } else if (noCluster) {
                gfproperties.setConfigFileURI(Thread.currentThread().getContextClassLoader().getResource("microdomain-nocluster.xml").toExternalForm());

            } else {
                gfproperties.setConfigFileURI(Thread.currentThread().getContextClassLoader().getResource("microdomain.xml").toExternalForm());
            }

            if (rootDir != null) {
                gfproperties.setInstanceRoot(rootDir.getAbsolutePath());
                File configFile = new File(rootDir.getAbsolutePath() + File.separator + "config" + File.separator + "domain.xml");
                if (!configFile.exists()) {
                    installFiles(gfproperties);
                } else {
                    if (alternateDomainXML ==null) {
                    String absolutePath = rootDir.getAbsolutePath();
                    absolutePath = absolutePath.replace('\\', '/');
                    gfproperties.setConfigFileURI("file:///" + absolutePath + "/config/domain.xml");
                    gfproperties.setConfigFileReadOnly(false);
                }
                }

            }

            if (this.maxHttpThreads != Integer.MIN_VALUE) {
                gfproperties.setProperty("embedded-glassfish-config.server.thread-pools.thread-pool.http-thread-pool.max-thread-pool-size", Integer.toString(maxHttpThreads));
            }

            if (this.minHttpThreads != Integer.MIN_VALUE) {
                gfproperties.setProperty("embedded-glassfish-config.server.thread-pools.thread-pool.http-thread-pool.min-thread-pool-size", Integer.toString(minHttpThreads));
            }

            gf = gfruntime.newGlassFish(gfproperties);

            // reset logger.
            // reset the Log Manager     
            String instanceRootStr = System.getProperty("com.sun.aas.instanceRoot");
            File configDir = new File(instanceRootStr, "config");
            File loggingToFileProperties = new File(configDir.getAbsolutePath(), loggingToFilePropertiesFileName);
            if (logToFile) {
                loggingPropertiesFileName = loggingToFilePropertiesFileName;
                Properties props = new Properties();
                String propsFilename = loggingToFileProperties.getAbsolutePath();
                FileInputStream configStream;
                try {
                    configStream = new FileInputStream(propsFilename);
                    props.load(configStream);
                    configStream.close();
                    props.setProperty("java.util.logging.FileHandler.pattern", userLogFile);
                    FileOutputStream output = new FileOutputStream(propsFilename);
                    props.store(output, "Payara Micro Logging Properties File");
                    output.close();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
            
            File loggingProperties = new File(configDir.getAbsolutePath(), loggingPropertiesFileName);
            if (loggingProperties.exists() && loggingProperties.canRead() && loggingProperties.isFile()) {
                if (System.getProperty("java.util.logging.config.file") == null) {
                    System.setProperty("java.util.logging.config.file", loggingProperties.getAbsolutePath());
                }
                try {
                    LogManager.getLogManager().readConfiguration();
                } catch (IOException | SecurityException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
            configureSecurity();
            gf.start();
            //this.runtime = new PayaraMicroRuntime(instanceName, gf);
            this.runtime = new PayaraMicroRuntime(instanceName, gf, gfruntime);
            deployAll();

            if (generateLogo) {
                generateLogo();
            }

            if (enableHealthCheck) {
                HealthCheckService healthCheckService = gf.getService(HealthCheckService.class);
                healthCheckService.setEnabled(enableHealthCheck);
            }

            if (disablePhoneHome) {
                logger.log(Level.INFO, "Phone Home Service Disabled");
            } else {
                gf.getService(PhoneHomeCore.class).start();
            }

            long end = System.currentTimeMillis();
            logger.info(Version.getFullVersion() + " ready in " + (end - start) + " (ms)");

            return runtime;
        } catch (GlassFishException ex) {
            throw new BootstrapException(ex.getMessage(), ex);
        }
    }

    /**
     * Get a handle on the running Payara instance to manipulate the server once
     * running
     *
     * @return
     * @throws IllegalStateException
     */
    public PayaraMicroRuntime getRuntime() throws IllegalStateException {
        if (!isRunning()) {
            throw new IllegalStateException("Payara Micro is not running");
        }
        return runtime;
    }

    /**
     * Stops and then shutsdown the Payara Micro Server
     *
     * @throws BootstrapException
     */
    public void shutdown() throws BootstrapException {
        if (!isRunning()) {
            throw new IllegalStateException("Payara Micro is not running");
        }
        runtime.shutdown();
        runtime = null;
    }

    private PayaraMicro() {
        try {
            repositoryURLs = new LinkedList<>();
            repositoryURLs.add(new URL(defaultMavenRepository));
            setArgumentsFromSystemProperties();
            addShutdownHook();
        } catch (MalformedURLException ex) {
            logger.log(Level.SEVERE, "{0} is not a valid default URL", defaultMavenRepository);
        }
    }

    private void scanArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (null != arg) {
                switch (arg) {
                    case "--port": {
                        String httpPortS = args[i + 1];
                        try {
                            httpPort = Integer.parseInt(httpPortS);
                            if (httpPort < 1 || httpPort > 65535) {
                                throw new NumberFormatException("Not a valid tcp port");
                            }
                        } catch (NumberFormatException nfe) {
                            logger.log(Level.SEVERE, "{0} is not a valid http port number", httpPortS);
                            throw new IllegalArgumentException();
                        }
                        i++;
                        break;
                    }
                    case "--sslPort": {
                        String httpPortS = args[i + 1];
                        try {
                            sslPort = Integer.parseInt(httpPortS);
                            if (sslPort < 1 || sslPort > 65535) {
                                throw new NumberFormatException("Not a valid tcp port");
                            }
                        } catch (NumberFormatException nfe) {
                            logger.log(Level.SEVERE, "{0} is not a valid ssl port number and will be ignored", httpPortS);
                            throw new IllegalArgumentException();
                        }
                        i++;
                        break;
                    }
                    case "--version": {
                        String deployments = System.getProperty("user.dir");
                        System.err.println("deployments " + deployments);
                        try {
                            Properties props = new Properties();
                            InputStream input = PayaraMicro.class.getResourceAsStream("/config/branding/glassfish-version.properties");
                            props.load(input);
                            StringBuilder output = new StringBuilder();
                            if (props.getProperty("product_name").isEmpty() == false){
                                output.append(props.getProperty("product_name")+" ");
                            }
                            if (props.getProperty("major_version").isEmpty() == false){
                                output.append(props.getProperty("major_version")+".");
                            }
                            if (props.getProperty("minor_version").isEmpty() == false){
                                output.append(props.getProperty("minor_version")+".");
                            }
                            if (props.getProperty("update_version").isEmpty() == false){
                                output.append(props.getProperty("update_version")+".");
                            }
                            if (props.getProperty("payara_version").isEmpty() == false){
                                output.append(props.getProperty("payara_version"));
                            }
                            if (props.getProperty("payara_update_version").isEmpty() == false){
                                output.append("." + props.getProperty("payara_update_version"));
                            }
                            if (props.getProperty("build_id").isEmpty() == false){
                                output.append(" Build Number " + props.getProperty("build_id"));
                            }

                            System.err.println(output.toString());
                        } catch (FileNotFoundException ex) {
                            Logger.getLogger(PayaraMicro.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException io){
                            Logger.getLogger(PayaraMicro.class.getName()).log(Level.SEVERE, null, io);
                        }
                        System.exit(1);
                        break;
                    }
                    case "--maxHttpThreads": {
                        String threads = args[i + 1];
                        try {
                            maxHttpThreads = Integer.parseInt(threads);
                            if (maxHttpThreads < 2) {
                                throw new NumberFormatException("Maximum Threads must be 2 or greater");
                            }
                        } catch (NumberFormatException nfe) {
                            logger.log(Level.SEVERE, "{0} is not a valid maximum threads number and will be ignored", threads);
                            throw new IllegalArgumentException();
                        }
                        i++;
                        break;
                    }
                    case "--minHttpThreads": {
                        String threads = args[i + 1];
                        try {
                            minHttpThreads = Integer.parseInt(threads);
                            if (minHttpThreads < 0) {
                                throw new NumberFormatException("Minimum Threads must be zero or greater");
                            }
                        } catch (NumberFormatException nfe) {
                            logger.log(Level.SEVERE, "{0} is not a valid minimum threads number and will be ignored", threads);
                            throw new IllegalArgumentException();
                        }
                        i++;
                        break;
                    }
                    case "--mcAddress":
                        hzMulticastGroup = args[i + 1];
                        i++;
                        break;
                case "--clusterName" :
                    hzClusterName = args[i+1];
                        i++;
                        break;
                case "--clusterPassword" :
                    hzClusterPassword = args[i+1];
                        i++;
                        break;
                    case "--mcPort": {
                        String httpPortS = args[i + 1];
                        try {
                            hzPort = Integer.parseInt(httpPortS);
                            if (hzPort < 1 || hzPort > 65535) {
                                throw new NumberFormatException("Not a valid tcp port");
                            }
                        } catch (NumberFormatException nfe) {
                            logger.log(Level.SEVERE, "{0} is not a valid multicast port number and will be ignored", httpPortS);
                            throw new IllegalArgumentException();
                        }
                        i++;
                        break;
                    }
                    case "--startPort":
                        String startPort = args[i + 1];
                        try {
                            hzStartPort = Integer.parseInt(startPort);
                            if (hzStartPort < 1 || hzStartPort > 65535) {
                                throw new NumberFormatException("Not a valid tcp port");
                            }
                        } catch (NumberFormatException nfe) {
                            logger.log(Level.SEVERE, "{0} is not a valid port number and will be ignored", startPort);
                            throw new IllegalArgumentException();
                        }
                        i++;
                        break;
                    case "--name":
                        instanceName = args[i + 1];
                        i++;
                        break;
                    case "--deploymentDir":
                        deploymentRoot = new File(args[i + 1]);
                        if (!deploymentRoot.exists() || !deploymentRoot.isDirectory()) {
                            logger.log(Level.SEVERE, "{0} is not a valid deployment directory and will be ignored", args[i + 1]);
                            throw new IllegalArgumentException();
                        }
                        i++;
                        break;
                    case "--rootDir":
                        rootDir = new File(args[i + 1]);
                        if (!rootDir.exists() || !rootDir.isDirectory()) {
                            logger.log(Level.SEVERE, "{0} is not a valid root directory and will be ignored", args[i + 1]);
                            throw new IllegalArgumentException();
                        }
                        i++;
                        break;
                    case "--deploy":
                        File deployment = new File(args[i + 1]);
                        if (!deployment.exists() || !deployment.canRead()) {
                            logger.log(Level.SEVERE, "{0} is not a valid deployment path and will be ignored", deployment.getAbsolutePath());
                        } else {
                            if (deployments == null) {
                                deployments = new LinkedList<>();
                            }
                            deployments.add(deployment);
                        }
                        i++;
                        break;
                    case "--domainConfig":
                        alternateDomainXML = new File(args[i + 1]);
                        if (!alternateDomainXML.exists() || !alternateDomainXML.isFile() || !alternateDomainXML.canRead() || !alternateDomainXML.getAbsolutePath().endsWith(".xml")) {
                            logger.log(Level.SEVERE, "{0} is not a valid path to an xml file and will be ignored", alternateDomainXML.getAbsolutePath());
                            throw new IllegalArgumentException();
                        }
                        i++;
                        break;
                    case "--noCluster":
                        noCluster = true;
                        break;
                    case "--lite":
                        liteMember = true;
                        break;
                    case "--hzConfigFile":
                        File testFile = new File(args[i + 1]);
                        if (!testFile.exists() || !testFile.isFile() || !testFile.canRead() || !testFile.getAbsolutePath().endsWith(".xml")) {
                            logger.log(Level.SEVERE, "{0} is not a valid path to an xml file and will be ignored", testFile.getAbsolutePath());
                            throw new IllegalArgumentException();
                        }
                        alternateHZConfigFile = testFile.toURI();
                        i++;
                        break;
                    case "--autoBindHttp":
                        autoBindHttp = true;
                        break;
                    case "--autoBindSsl":
                        autoBindSsl = true;
                        break;
                    case "--autoBindRange":
                        String autoBindRangeString = args[i + 1];
                        try {
                            autoBindRange = Integer.parseInt(autoBindRangeString);
                            if (autoBindRange < 1) {
                                throw new NumberFormatException("Not a valid auto bind range");
                            }
                        } catch (NumberFormatException nfe) {
                            logger.log(Level.SEVERE,
                                    "{0} is not a valid auto bind range number",
                                    autoBindRangeString);
                            throw new IllegalArgumentException();
                        }
                        i++;
                        break;
                    case "--enableHealthCheck":
                        String enableHealthCheckString = args[i + 1];
                        enableHealthCheck = Boolean.valueOf(enableHealthCheckString);
                        break;
                    case "--deployFromGAV":
                        if (GAVs == null) {
                            GAVs = new LinkedList<>();
                        }

                        GAVs.add(args[i + 1]);
                        i++;
                        break;
                    case "--additionalRepository":
                        try {
                            // If there isn't a trailing /, add one
                            if (!args[i + 1].endsWith("/")) {
                                repositoryURLs.add(new URL(args[i + 1] + "/"));
                            } else {
                                repositoryURLs.add(new URL(args[i + 1]));
                            }
                        } catch (MalformedURLException ex) {
                            logger.log(Level.SEVERE, "{0} is not a valid URL and will be ignored", args[i + 1]);
                        }

                        i++;
                        break;
                    case "--outputUberJar":
                        uberJar = new File(args[i + 1]);
                        i++;
                        break;
                    case "--systemProperties": {
                        File propertiesFile = new File(args[i + 1]);
                        userSystemProperties = new Properties();
                        try (FileReader reader = new FileReader(propertiesFile)) {
                            userSystemProperties.load(reader);
                            Enumeration<String> names = (Enumeration<String>) userSystemProperties.propertyNames();
                            while (names.hasMoreElements()) {
                                String name = names.nextElement();
                                System.setProperty(name, userSystemProperties.getProperty(name));
                            }
                        } catch (IOException e) {
                            logger.log(Level.SEVERE,
                                    "{0} is not a valid properties file",
                                    propertiesFile.getAbsolutePath());
                            throw new IllegalArgumentException(e);
                        }
                        if (!propertiesFile.isFile() && !propertiesFile.canRead()) {
                            logger.log(Level.SEVERE,
                                    "{0} is not a valid properties file",
                                    propertiesFile.getAbsolutePath());
                            throw new IllegalArgumentException();

                        }
                    }
                    break;
                    case "--disablePhoneHome":
                        disablePhoneHome = true;
                        break;
                    case "--help":
                        System.err.println("Usage:\n  --noCluster  Disables clustering\n"
                                + "  --port <http-port-number> sets the http port\n"
                                + "  --sslPort <ssl-port-number> sets the https port number\n"
                                + "  --mcAddress <muticast-address> sets the cluster multicast group\n"
                                + "  --mcPort <multicast-port-number> sets the cluster multicast port\n"
                                + "  --clusterName <cluster-name> sets the Cluster Group Name\n"
                                + "  --clusterPassword <cluster-password> sets the Cluster Group Password\n"
                                + "  --startPort <cluster-start-port-number> sets the cluster start port number\n"
                                + "  --name <instance-name> sets the instance name\n"
                                + "  --rootDir <directory-path> Sets the root configuration directory and saves the configuration across restarts\n"
                                + "  --deploymentDir <directory-path> if set to a valid directory all war files in this directory will be deployed\n"
                                + "  --deploy <file-path> specifies a war file to deploy\n"
                                + "  --domainConfig <file-path> overrides the complete server configuration with an alternative domain.xml file\n"
                                + "  --minHttpThreads <threads-number> the minimum number of threads in the HTTP thread pool\n"
                                + "  --maxHttpThreads <threads-number> the maximum number of threads in the HTTP thread pool\n"
                                + "  --hzConfigFile <file-path> the hazelcast-configuration file to use to override the in-built hazelcast cluster configuration\n"
                                + "  --autoBindHttp sets autobinding of the http port to a non-bound port\n"
                                + "  --autoBindSsl sets autobinding of the https port to a non-bound port\n"
                                + "  --autoBindRange <number-of-ports> sets the maximum number of ports to look at for port autobinding\n"
                                + "  --lite sets the micro container to lite mode which means it clusters with other Payara Micro instances but does not store any cluster data\n"
                                + "  --enableHealthCheck <boolean> enables/disables Health Check Service (disabled by default).\n"
                                + "  --logo reveal the #BadAssFish\n"
                                + "  --deployFromGAV <list-of-artefacts> specifies a comma separated groupId,artifactId,versionNumber of an artefact to deploy from a repository\n"
                                + "  --additionalRepository <repo-url> specifies an additional repository to search for deployable artefacts in\n"
                                + "  --outputUberJar <file-path> packages up an uber jar at the specified path based on the command line arguments and exits\n"
                                + "  --systemProperties <file-path> Reads system properties from a file\n"
                                + "  --disablePhoneHome Disables sending of usage tracking information\n"
                                + "  --version Displays the version information\n"
                                + "  --logToFile <file-path> outputs all the Log entries to a user defined file\n"
                                + "  --help Shows this message and exits\n");
                        System.exit(1);
                        break;
                    case "--logToFile":
                        setUserLogFile(args[i + 1]);
                        break;

                    case "--logo":
                        generateLogo = true;
                        break;
                }
            }
        }
    }

    private void deployAll() throws GlassFishException {
        // Deploy explicit wars first.
        int deploymentCount = 0;
        Deployer deployer = gf.getDeployer();
        if (deployments != null) {
            for (File war : deployments) {
                if (war.exists() && war.canRead()) {
                    deployer.deploy(war, "--availabilityenabled=true");
                    deploymentCount++;
                } else {
                    logger.log(Level.WARNING, "{0} is not a valid deployment", war.getAbsolutePath());
                }
            }
        }

        // Deploy from deployment directory
        if (deploymentRoot != null) {
            for (File war : deploymentRoot.listFiles()) {
                String warPath = war.getAbsolutePath();
                if (war.isFile() && war.canRead() && (warPath.endsWith(".war") || warPath.endsWith(".ear") || warPath.endsWith(".jar") || warPath.endsWith(".rar"))) {
                    deployer.deploy(war, "--availabilityenabled=true");
                    deploymentCount++;
                }
            }
        }

        // Deploy from URI only called if GAVs provided
        if (GAVs != null) {
            // Convert the provided GAV Strings into target URLs
            getGAVURLs();

            if (!deploymentURLsMap.isEmpty()) {
                for (Map.Entry<String, URL> deploymentMapEntry : deploymentURLsMap.entrySet()) {
                    try {
                        // Convert the URL to a URI for use with the deploy method
                        URI artefactURI = deploymentMapEntry.getValue().toURI();

                        deployer.deploy(artefactURI, "--availabilityenabled",
                                "true", "--contextroot",
                                deploymentMapEntry.getKey());

                        deploymentCount++;
                    } catch (URISyntaxException ex) {
                        logger.log(Level.WARNING, "{0} could not be converted to a URI,"
                                + " artefact will be skipped",
                                deploymentMapEntry.getValue().toString());
                    }
                }
            }
        }

        // search META-INF/deploy for deployments
        // if there is a deployment called ROOT deploy to the root context /
        URL url = this.getClass().getClassLoader().getResource("META-INF/deploy");
        if (url != null) {
            String entryName = "";
            try {
                HashSet<String> entriesToDeploy = new HashSet<>();
                JarURLConnection urlcon = (JarURLConnection) url.openConnection();
                JarFile jFile = urlcon.getJarFile();
                Enumeration<JarEntry> entries = jFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    entryName = entry.getName();
                    if (!entry.isDirectory() && !entry.getName().endsWith(".properties") && !entry.getName().endsWith(".xml") && entry.getName().startsWith("META-INF/deploy")) {
                        entriesToDeploy.add(entry.getName());
                    }
                }

                for (String entry : entriesToDeploy) {
                    File file = new File(entry);
                    String contextRoot = file.getName();
                    if (contextRoot.endsWith(".ear") || contextRoot.endsWith(".war") || contextRoot.endsWith(".jar") || contextRoot.endsWith(".rar")) {
                        contextRoot = contextRoot.substring(0, contextRoot.length() - 4);
                    }

                    if (contextRoot.equals("ROOT")) {
                        contextRoot = "/";
                    }

                    deployer.deploy(this.getClass().getClassLoader().getResourceAsStream(entry), "--availabilityenabled",
                            "true", "--contextroot",
                            contextRoot, "--name", file.getName());
                    deploymentCount++;
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Could not deploy jar entry {0}",
                        entryName);
            }
        } else {
            logger.info("No META-INF/deploy directory");
        }

        logger.log(Level.INFO, "Deployed {0} archives", deploymentCount);
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(
                "GlassFish Shutdown Hook") {
            @Override
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
            "config/loggingToFile.properties",       
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
            ClassLoader loader = getClass().getClassLoader();
            embeddedBootProperties.load(loader.getResourceAsStream("payara-boot.properties"));
            for (Object key : embeddedBootProperties.keySet()) {
                String keyStr = (String) key;
                if (System.getProperty(keyStr) == null) {
                    System.setProperty(keyStr, embeddedBootProperties.getProperty(keyStr));
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(PayaraMicro.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Determines whether the server is running i.e. bootstrap has been called
     *
     * @return true of the server is running
     */
    boolean isRunning() {
        try {
            return (gf != null && gf.getStatus() == Status.STARTED);
        } catch (GlassFishException ex) {
            return false;
        }
    }

    void generateLogo() {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(bootImage);) {
            byte[] buffer = new byte[1024];
            for (int length; (length = is.read(buffer)) != -1;) {

                System.err.write(buffer, 0, length);
                System.err.flush();
            }
        } catch (IOException | NullPointerException ex) {
            Logger.getLogger(PayaraMicro.class.getName()).log(Level.WARNING, "Problems displaying Boot Image", ex);
        }
    }

    /**
     * Converts the GAVs provided to a URLs, and stores them in the
     * deploymentURLsMap.
     */
    private void getGAVURLs() throws GlassFishException {
        GAVConvertor gavConvertor = new GAVConvertor();

        for (String gav : GAVs) {
            Map.Entry<String, URL> artefactMapEntry
                    = gavConvertor.getArtefactMapEntry(gav, repositoryURLs);

            if (deploymentURLsMap == null) {
                deploymentURLsMap = new LinkedHashMap<>();
            }

            deploymentURLsMap.put(artefactMapEntry.getKey(),
                    artefactMapEntry.getValue());
        }
    }

    /**
     * Logs warnings if ports are being overridden
     * @param HTTPS True if checking if HTTPS ports are being overridden
     */
    private void logPortPrecedenceWarnings(boolean HTTPS) {
        if (HTTPS == true) {
            if (alternateDomainXML != null) {
                if (sslPort != Integer.MIN_VALUE) {
                    if (autoBindSsl == true) {
                        logger.log(Level.INFO, "Overriding HTTPS port value set"
                                + " in {0} and auto-binding against " + sslPort,
                                alternateDomainXML.getAbsolutePath());
                    } else {
                        logger.log(Level.INFO, "Overriding HTTPS port value set"
                                + " in {0} with " + sslPort,
                                alternateDomainXML.getAbsolutePath());
                    }
                } else if (autoBindSsl == true) {
                    logger.log(Level.INFO, "Overriding HTTPS port value set"
                            + " in {0} and auto-binding against "
                            + defaultHttpsPort,
                            alternateDomainXML.getAbsolutePath());
                } else {
                    logger.log(Level.INFO, "Overriding HTTPS port value set"
                            + " in {0} with " + defaultHttpsPort,
                            alternateDomainXML.getAbsolutePath());
                }
            }

            if (rootDir != null) {
                File configFile = new File(rootDir.getAbsolutePath()
                        + File.separator + "config" + File.separator
                        + "domain.xml");
                if (configFile.exists()) {
                    if (sslPort != Integer.MIN_VALUE) {
                        if (autoBindSsl == true) {
                            logger.log(Level.INFO, "Overriding HTTPS port value"
                                    + " set in {0} and auto-binding against "
                                    + sslPort, configFile.getAbsolutePath());
                        } else {
                            logger.log(Level.INFO, "Overriding HTTPS port value"
                                    + " set in {0} with " + sslPort,
                                    configFile.getAbsolutePath());
                        }
                    } else if (autoBindSsl == true) {
                        logger.log(Level.INFO, "Overriding HTTPS port value"
                                + " set in {0} and auto-binding against "
                                + defaultHttpsPort,
                                configFile.getAbsolutePath());
                    } else {
                        logger.log(Level.INFO, "Overriding HTTPS port value"
                                + " set in {0} with default value of "
                                + defaultHttpsPort,
                                configFile.getAbsolutePath());
                    }
                }
            }
        } else {
            if (alternateDomainXML != null) {
                if (httpPort != Integer.MIN_VALUE) {
                    if (autoBindHttp == true) {
                        logger.log(Level.INFO, "Overriding HTTP port value set "
                                + "in {0} and auto-binding against " + httpPort,
                                alternateDomainXML.getAbsolutePath());
                    } else {
                        logger.log(Level.INFO, "Overriding HTTP port value set "
                                + "in {0} with " + httpPort,
                                alternateDomainXML.getAbsolutePath());
                    }
                } else if (autoBindHttp == true) {
                    logger.log(Level.INFO, "Overriding HTTP port value set "
                            + "in {0} and auto-binding against "
                            + defaultHttpPort,
                            alternateDomainXML.getAbsolutePath());
                } else {
                    logger.log(Level.INFO, "Overriding HTTP port value set "
                            + "in {0} with default value of "
                            + defaultHttpPort,
                            alternateDomainXML.getAbsolutePath());
                }
            }

            if (rootDir != null) {
                File configFile = new File(rootDir.getAbsolutePath()
                        + File.separator + "config" + File.separator
                        + "domain.xml");
                if (configFile.exists()) {
                    if (httpPort != Integer.MIN_VALUE) {
                        if (autoBindHttp == true) {
                            logger.log(Level.INFO, "Overriding HTTP port value "
                                    + "set in {0} and auto-binding against "
                                    + httpPort, configFile.getAbsolutePath());
                        } else {
                            logger.log(Level.INFO, "Overriding HTTP port value "
                                    + "set in {0} with " + httpPort,
                                    configFile.getAbsolutePath());
                        }
                    } else if (autoBindHttp == true) {
                        logger.log(Level.INFO, "Overriding HTTP port value "
                                + "set in {0} and auto-binding against "
                                + defaultHttpPort,
                                configFile.getAbsolutePath());
                    } else {
                        logger.log(Level.INFO, "Overriding HTTP port value "
                                + "set in {0} with default value of "
                                + defaultHttpPort,
                                configFile.getAbsolutePath());
                    }
                }
            }
        }
    }

    private void setArgumentsFromSystemProperties() {

        // load all from the resource
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("META-INF/deploy/payaramicro.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                for (Map.Entry<?, ?> entry : props.entrySet()) {
                    System.setProperty((String) entry.getKey(), (String) entry.getValue());
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "", ex);
        }

        // Set the domain.xml
        String alternateDomainXMLStr = System.getProperty("payaramicro.domainConfig");
        if (alternateDomainXMLStr != null && !alternateDomainXMLStr.isEmpty()) {
            applicationDomainXml = alternateDomainXMLStr;
        }

        // Set the hazelcast config file
        String alternateHZConfigFileStr = System.getProperty("payaramicro.hzConfigFile");
        if (alternateHZConfigFileStr != null && !alternateHZConfigFileStr.isEmpty()) {
            try {
                alternateHZConfigFile = Thread.currentThread().getContextClassLoader().getResource(alternateHZConfigFileStr).toURI();
            } catch (URISyntaxException ex) {
                logger.log(Level.WARNING, "payaramicro.hzConfigFile has invalid URI syntax and will be ignored", ex);
                alternateHZConfigFile = null;
            }
        }

        autoBindHttp = Boolean.getBoolean("payaramicro.autoBindHttp");
        autoBindRange = Integer.getInteger("payaramicro.autoBindRange", 5);
        autoBindSsl = Boolean.getBoolean("payaramicro.autoBindSsl");
        generateLogo = Boolean.getBoolean("payaramicro.logo");
        logToFile = Boolean.getBoolean("payaramicro.logToFile");
        enableHealthCheck = Boolean.getBoolean("payaramicro.enableHealthCheck");
        httpPort = Integer.getInteger("payaramicro.port", Integer.MIN_VALUE);
        hzMulticastGroup = System.getProperty("payaramicro.mcAddress");
        hzPort = Integer.getInteger("payaramicro.mcPort", Integer.MIN_VALUE);
        hzStartPort = Integer.getInteger("payaramicro.startPort", Integer.MIN_VALUE);
        hzClusterName = System.getProperty("payaramicro.clusterName");
        hzClusterPassword = System.getProperty("payaramicro.clusterPassword");
        liteMember = Boolean.getBoolean("payaramicro.lite");
        maxHttpThreads = Integer.getInteger("payaramicro.maxHttpThreads", Integer.MIN_VALUE);
        minHttpThreads = Integer.getInteger("payaramicro.minHttpThreads", Integer.MIN_VALUE);
        noCluster = Boolean.getBoolean("payaramicro.noCluster");
        disablePhoneHome = Boolean.getBoolean("payaramicro.disablePhoneHome");

        // Set the rootDir file
        String rootDirFileStr = System.getProperty("payaramicro.rootDir");
        if (rootDirFileStr != null && !rootDirFileStr.isEmpty()) {
            rootDir = new File(rootDirFileStr);
        }

        String name = System.getProperty("payaramicro.name");
        if (name != null && !name.isEmpty()) {
            instanceName = name;
        }
    }

    private void packageUberJar() {
        long start = System.currentTimeMillis();
        logger.info("Building Uber Jar... " + uberJar);
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(uberJar));) {
            // get the current payara micro jar
            URL url = this.getClass().getClassLoader().getResource("payara-boot.properties");
            JarURLConnection urlcon = (JarURLConnection) url.openConnection();

            // copy all entries from the existing jar file
            JarFile jFile = urlcon.getJarFile();
            Enumeration<JarEntry> entries = jFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                InputStream is = jFile.getInputStream(entry);
                jos.putNextEntry(new JarEntry(entry.getName()));
                byte[] buffer = new byte[4096];
                int bytesRead = 0;
                while ((bytesRead = is.read(buffer)) != -1) {
                    jos.write(buffer, 0, bytesRead);
                }
                is.close();
                jos.flush();
                jos.closeEntry();
            }

            // create the directory entry
            JarEntry deploymentDir = new JarEntry("META-INF/deploy/");
            jos.putNextEntry(deploymentDir);
            jos.flush();
            jos.closeEntry();
            if (deployments != null) {
                for (File deployment : deployments) {
                    JarEntry deploymentEntry = new JarEntry("META-INF/deploy/" + deployment.getName());
                    jos.putNextEntry(deploymentEntry);
                    try (FileInputStream fis = new FileInputStream(deployment)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead = 0;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            jos.write(buffer, 0, bytesRead);
                        }
                        jos.flush();
                        jos.closeEntry();
                    } catch (IOException ioe) {
                        logger.log(Level.WARNING, "Error adding deployment " + deployment.getAbsolutePath() + " to the Uber Jar Skipping...", ioe);
                    }
                }
            }

            if (deploymentRoot != null) {
                for (File deployment : deploymentRoot.listFiles()) {
                    if (deployment.isFile()) {
                        JarEntry deploymentEntry = new JarEntry("META-INF/deploy/" + deployment.getName());
                        jos.putNextEntry(deploymentEntry);
                        try (FileInputStream fis = new FileInputStream(deployment)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead = 0;
                            while ((bytesRead = fis.read(buffer)) != -1) {
                                jos.write(buffer, 0, bytesRead);
                            }
                            jos.flush();
                            jos.closeEntry();
                        } catch (IOException ioe) {
                            logger.log(Level.WARNING, "Error adding deployment " + deployment.getAbsolutePath() + " to the Uber Jar Skipping...", ioe);
                        }
                    }
                }
            }

            if (GAVs != null) {
                try {
                    // Convert the provided GAV Strings into target URLs
                    getGAVURLs();
                    for (Map.Entry<String, URL> deploymentMapEntry : deploymentURLsMap.entrySet()) {
                        URL deployment = deploymentMapEntry.getValue();
                        String name = deploymentMapEntry.getKey();
                        try (InputStream is = deployment.openStream()) {
                            JarEntry deploymentEntry = new JarEntry("META-INF/deploy/" + name);
                            jos.putNextEntry(deploymentEntry);
                            byte[] buffer = new byte[4096];
                            int bytesRead = 0;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                jos.write(buffer, 0, bytesRead);
                            }
                            jos.flush();
                            jos.closeEntry();
                        } catch (IOException ioe) {
                            logger.log(Level.WARNING, "Error adding deployment " + name + " to the Uber Jar Skipping...", ioe);
                        }
                    }
                } catch (GlassFishException ex) {
                    logger.log(Level.SEVERE, "Unable to process maven deployment units", ex);
                }
            }

            // write the system properties file
            JarEntry je = new JarEntry("META-INF/deploy/payaramicro.properties");
            jos.putNextEntry(je);
            Properties props = new Properties();
            if (hzMulticastGroup != null) {
                props.setProperty("payaramicro.mcAddress", hzMulticastGroup);
            }

            if (hzPort != Integer.MIN_VALUE) {
                props.setProperty("payaramicro.mcPort", Integer.toString(hzPort));
            }

            if (hzStartPort != Integer.MIN_VALUE) {
                props.setProperty("payaramicro.startPort", Integer.toString(hzStartPort));
            }

            props.setProperty("payaramicro.name", instanceName);

            if (rootDir != null) {
                props.setProperty("payaramicro.rootDir", rootDir.getAbsolutePath());
            }

            if (alternateDomainXML != null) {
                props.setProperty("payaramicro.domainConfig", "META-INF/deploy/domain.xml");
            }

            if (minHttpThreads != Integer.MIN_VALUE) {
                props.setProperty("payaramicro.minHttpThreads", Integer.toString(minHttpThreads));
            }

            if (maxHttpThreads != Integer.MIN_VALUE) {
                props.setProperty("payaramicro.maxHttpThreads", Integer.toString(maxHttpThreads));
            }

            if (alternateHZConfigFile != null) {
                props.setProperty("payaramicro.hzConfigFile", "META-INF/deploy/hzconfig.xml");
            }

            if (hzClusterName != null) {
                props.setProperty("payaramicro.clusterName", hzClusterName);
            }

            if (hzClusterPassword != null) {
                props.setProperty("payaramicro.clusterPassword", hzClusterPassword);
            }

            props.setProperty("payaramicro.autoBindHttp", Boolean.toString(autoBindHttp));
            props.setProperty("payaramicro.autoBindSsl", Boolean.toString(autoBindSsl));
            props.setProperty("payaramicro.autoBindRange", Integer.toString(autoBindRange));
            props.setProperty("payaramicro.lite", Boolean.toString(liteMember));
            props.setProperty("payaramicro.enableHealthCheck", Boolean.toString(enableHealthCheck));
            props.setProperty("payaramicro.logo", Boolean.toString(generateLogo));
            props.setProperty("payaramicro.logToFile", Boolean.toString(logToFile));
            props.setProperty("payaramicro.noCluster", Boolean.toString(noCluster));
            props.setProperty("payaramicro.disablePhoneHome", Boolean.toString(disablePhoneHome));

            if (httpPort != Integer.MIN_VALUE) {
                props.setProperty("payaramicro.port", Integer.toString(httpPort));
            }

            if (sslPort != Integer.MIN_VALUE) {
                props.setProperty("payaramicro.sslPort", Integer.toString(sslPort));
            }

            // write all user defined system properties
            if (userSystemProperties != null) {
                Enumeration<String> names = (Enumeration<String>) userSystemProperties.propertyNames();
                while (names.hasMoreElements()) {
                    String name = names.nextElement();
                    props.setProperty(name, userSystemProperties.getProperty(name));
                }
            }

            props.store(jos, "");
            jos.flush();
            jos.closeEntry();

            // add the alternate domain.xml file if present
            if (alternateDomainXML != null && alternateDomainXML.isFile() && alternateDomainXML.canRead()) {
                try (InputStream is = new FileInputStream(alternateDomainXML)) {
                    JarEntry domainXml = new JarEntry("META-INF/deploy/domain.xml");
                    jos.putNextEntry(domainXml);
                    byte[] buffer = new byte[4096];
                    int bytesRead = 0;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        jos.write(buffer, 0, bytesRead);
                    }
                    jos.flush();
                    jos.closeEntry();
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, "Error adding alternative domain.xml to the Uber Jar Skipping...", ioe);
                }
            }

            // add the alternate hazelcast config to the uberJar
            if (alternateHZConfigFile != null) {
                try (InputStream is = alternateHZConfigFile.toURL().openStream()) {
                    JarEntry domainXml = new JarEntry("META-INF/deploy/hzconfig.xml");
                    jos.putNextEntry(domainXml);
                    byte[] buffer = new byte[4096];
                    int bytesRead = 0;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        jos.write(buffer, 0, bytesRead);
                    }
                    jos.flush();
                    jos.closeEntry();
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, "Error adding alternative hzconfig.xml to the Uber Jar Skipping...", ioe);
                }
            }

            logger.info("Built Uber Jar " + uberJar + " in " + (System.currentTimeMillis() - start) + " (ms)");

        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error creating Uber Jar " + uberJar.getAbsolutePath(), ex);
        }

    }

    private void configureSecurity() {
        String instanceRootStr = System.getProperty("com.sun.aas.instanceRoot");
        File configDir = new File(instanceRootStr, "config");

        // Set security properties PAYARA-803
        if (System.getProperty("java.security.auth.login.config") == null) {
                System.setProperty("java.security.auth.login.config", new File(configDir.getAbsolutePath(),"login.conf").getAbsolutePath());
        }

        if (System.getProperty("java.security.policy") == null) {
                System.setProperty("java.security.policy", new File(configDir.getAbsolutePath(),"server.policy").getAbsolutePath());
        }

        // check keystore
        if (System.getProperty("javax.net.ssl.keyStore") == null) {
            System.setProperty("javax.net.ssl.keyStore",new File(configDir.getAbsolutePath(),"keystore.jks").getAbsolutePath());
        }

        // check truststore
        if (System.getProperty("javax.net.ssl.trustStore") == null) {
            System.setProperty("javax.net.ssl.trustStore",new File(configDir.getAbsolutePath(),"cacerts.jks").getAbsolutePath());
        }
    }
    
}
