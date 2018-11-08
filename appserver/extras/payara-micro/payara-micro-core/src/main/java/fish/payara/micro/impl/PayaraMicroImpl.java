/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.micro.impl;

import fish.payara.deployment.util.GAVConvertor;
import fish.payara.micro.BootstrapException;
import fish.payara.micro.boot.runtime.BootCommand;
import fish.payara.micro.cmd.options.RuntimeOptions;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.JarURLConnection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFish.Status;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import com.sun.appserv.server.util.Version;
import com.sun.enterprise.glassfish.bootstrap.Constants;
import com.sun.enterprise.server.logging.ODLLogFormatter;
import fish.payara.micro.PayaraMicroRuntime;
import fish.payara.micro.boot.PayaraMicroBoot;
import fish.payara.micro.boot.loader.OpenURLClassLoader;
import fish.payara.micro.boot.runtime.BootCommands;
import fish.payara.micro.cmd.options.RUNTIME_OPTION;
import fish.payara.micro.cmd.options.ValidationException;
import fish.payara.micro.data.ApplicationDescriptor;
import fish.payara.micro.data.InstanceDescriptor;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import org.glassfish.embeddable.CommandResult;

/**
 * Main class for Bootstrapping Payara Micro Edition This class is used from
 * applications to create a full JavaEE runtime environment and deploy war
 * files.
 *
 * This class is used to configure and bootstrap a Payara Micro Runtime.
 *
 * @author steve
 */
public class PayaraMicroImpl implements PayaraMicroBoot {

    private static final String BOOT_PROPS_FILE = "/MICRO-INF/payara-boot.properties";
    private static final String USER_PROPS_FILE = "MICRO-INF/deploy/payaramicro.properties";
    private static final Logger LOGGER = Logger.getLogger("PayaraMicro");
    private static PayaraMicroImpl instance;

    private String hzMulticastGroup;
    private int hzPort = Integer.MIN_VALUE;
    private int hzStartPort = Integer.MIN_VALUE;
    private String hzClusterName;
    private String hzClusterPassword;
    private int httpPort = Integer.MIN_VALUE;
    private int sslPort = Integer.MIN_VALUE;
    private int maxHttpThreads = Integer.MIN_VALUE;
    private int minHttpThreads = Integer.MIN_VALUE;
    private String instanceName;
    private File rootDir;
    private File deploymentRoot;
    private File alternateDomainXML;
    private File alternateHZConfigFile;
    private List<File> deployments;
    private List<File> libraries;
    private GlassFish gf;
    private PayaraMicroRuntimeImpl runtime;
    private boolean noCluster = false;
    private boolean hostAware = false;
    private boolean autoBindHttp = false;
    private boolean autoBindSsl = false;
    private boolean liteMember = false;
    private boolean generateLogo = false;
    private boolean logToFile = false;
    private boolean enableAccessLog = false;
    private boolean enableAccessLogFormat = false;
    private boolean logPropertiesFile = false;
    private String userLogPropertiesFile = "";
    private int autoBindRange = 50;
    private String bootImage = "MICRO-INF/domain/boot.txt";
    private String applicationDomainXml;
    private boolean enableHealthCheck = false;
    private boolean disablePhoneHome = false;
    private List<String> GAVs;
    private File uberJar;
    private File copyDirectory;
    private Properties userSystemProperties;
    private Map<String, URL> deploymentURLsMap;
    private List<String> repositoryURLs;
    private final String defaultMavenRepository = "https://repo.maven.apache.org/maven2/";
    private final short defaultHttpPort = 8080;
    private final short defaultHttpsPort = 8181;
    private BootCommands preBootCommands;
    private BootCommands postBootCommands;
    private BootCommands postDeployCommands;
    private String userLogFile = "payara-server%u.log";
    private String userAccessLogDirectory = "";
    private String accessLogFormat = "%client.name% %auth-user-name% %datetime% %request% %status% %response.length%";
    private boolean enableRequestTracing = false;
    private String requestTracingThresholdUnit = "SECONDS";
    private long requestTracingThresholdValue = 30;
    private String instanceGroup;
    private String preBootFileName;
    private String postBootFileName;
    private String postDeployFileName;
    private RuntimeDirectory runtimeDir = null;
    private String secretsDir;
    private String sslCert;
    private boolean sniEnabled = false;

    /**
     * Runs a Payara Micro server used via java -jar payara-micro.jar
     *
     * @param args Command line arguments for PayaraMicro Usage: --help to see
     * all the options
     * <br/>
     * --help Shows this message and exits\n
     * @throws BootstrapException If there is a problem booting the server
     */
    public static void main(String args[]) throws Exception {

        // configure boot system properties
        setBootProperties();
        PayaraMicroImpl main = getInstance();
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
    public static PayaraMicroImpl getInstance() {
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
    public static PayaraMicroImpl getInstance(boolean create) {
        if (instance == null && create) {
            instance = new PayaraMicroImpl();
        }
        return instance;
    }

    /**
     * Gets the cluster group
     *
     * @return The Multicast Group that will beused for the Hazelcast clustering
     */
    @Override
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
    @Override
    public PayaraMicroImpl setClusterMulticastGroup(String hzMulticastGroup) {
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
    @Override
    public PayaraMicroImpl setLogoFile(String filePath) {
        bootImage = filePath;
        return this;
    }

    /**
     * Set whether the logo should be generated on boot
     *
     * @param generate
     * @return
     */
    @Override
    public PayaraMicroImpl setPrintLogo(boolean generate) {
        generateLogo = generate;
        return this;
    }

    /**
     * Set user defined file for the Log entries
     *
     * @param fileName
     * @return
     */
    @Override
    public PayaraMicroImpl setUserLogFile(String fileName) {
        File file = new File(fileName);
        if (file.isDirectory()) {
            if (!file.exists() || !file.canWrite()) {
                LOGGER.log(Level.SEVERE, "{0} is not a valid directory for storing logs as it must exist and be writable", file.getAbsolutePath());
                throw new IllegalArgumentException();
            }
            this.userLogFile = file.getAbsolutePath() + File.separator + userLogFile;
        } else {
            userLogFile = fileName;
        }
        logToFile = true;
        return this;
    }

    /**
     * Set user defined properties file for logging
     *
     * @param fileName
     * @return
     */
    @Override
    public PayaraMicroImpl setLogPropertiesFile(File fileName) {
        System.setProperty("java.util.logging.config.file", fileName.getAbsolutePath());
        logPropertiesFile = true;
        userLogPropertiesFile = fileName.getAbsolutePath();
        return this;
    }

    /**
     * Set user defined file directory for the access log
     *
     * @param filePath
     */
    @Override
    public void setAccessLogDir(String filePath) {
        this.userAccessLogDirectory = filePath;
        enableAccessLog = true;
    }

    /**
     * Set user defined formatting for the access log
     *
     * @param format
     */
    @Override
    public void setAccessLogFormat(String format) {
        this.accessLogFormat = format;
        this.enableAccessLogFormat = true;
    }

    /**
     * Gets the cluster multicast port used for cluster communications
     *
     * @return The configured cluster port
     */
    @Override
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
    @Override
    public PayaraMicroImpl setClusterPort(int hzPort) {
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
    @Override
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
    @Override
    public PayaraMicroImpl setClusterStartPort(int hzStartPort) {
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
    @Override
    public int getHttpPort() {
        return httpPort;
    }

    /**
     * Sets the port used for HTTP requests
     *
     * @param httpPort The port number
     * @return
     */
    @Override
    public PayaraMicroImpl setHttpPort(int httpPort) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        this.httpPort = httpPort;
        return this;
    }

    /**
     * The UberJar to create
     *
     * @return
     */
    @Override
    public File getUberJar() {
        return uberJar;
    }
    
    /**
     * The configured port for HTTPS requests
     *
     * @return The HTTPS port
     */
    @Override
    public int getSslPort() {
        return sslPort;
    }

    /**
     * Sets the configured port for HTTPS requests. If this is not set HTTPS is
     * disabled
     *
     * @param sslPort The HTTPS port
     * @return
     */
    @Override
    public PayaraMicroImpl setSslPort(int sslPort) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        this.sslPort = sslPort;
        return this;
    }
    
    @Override
    public PayaraMicroImpl setSniEnabled(boolean value) {
        sniEnabled = value;
        return this;
    }

    /**
     * Set the certificate alias in the keystore to use for the server cert
     * @param alias name of the certificate in the keystore
     * @return 
     */
    @Override
    public PayaraMicroImpl setSslCert(String alias) {
        sslCert = alias;
        return this;
    }
    
    @Override
    public String getSslCert() {
        return sslCert;
    }
    
    /**
     * Gets the logical name for this PayaraMicro Server within the server
     * cluster
     *
     * @return The configured instance name
     */
    @Override
    public String getInstanceName() {
        return instanceName;
    }

    /**
     * Sets the logical instance name for this PayaraMicro server within the
     * server cluster If this is not set a name is generated
     *
     * @param instanceName The logical server name
     * @return
     */
    @Override
    public PayaraMicroImpl setInstanceName(String instanceName) {
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
    @Override
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
    @Override
    public PayaraMicroImpl setDeploymentDir(File deploymentRoot) {
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
    @Override
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
    @Override
    public PayaraMicroImpl setApplicationDomainXML(String domainXml) {
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
    @Override
    public PayaraMicroImpl setAlternateDomainXML(File alternateDomainXML) {
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
    @Override
    public PayaraMicroImpl addDeployment(String pathToWar) {
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
    @Override
    public PayaraMicroImpl addDeploymentFile(File file) {
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
     * Adds a Maven GAV coordinate to the list of archives to be deployed at
     * boot.
     *
     * @param GAV GAV coordinate
     * @return
     */
    @Override
    public PayaraMicroImpl addDeployFromGAV(String GAV) {
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
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        return this;
    }

    /**
     * Adds a Maven repository to the list of repositories to search for
     * artifacts in
     *
     * @param URLs URL to Maven repository
     * @return
     */
    @Override
    public PayaraMicroImpl addRepoUrl(String... URLs) {
        //if (runtime != null) {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
        repositoryURLs.addAll(Arrays.asList(URLs));
        return this;
    }

    /**
     * Indicated whether clustering is enabled
     *
     * @return
     */
    @Override
    public boolean isNoCluster() {
        return noCluster;
    }

    /**
     * Enables or disables clustering before bootstrap
     *
     * @param noCluster set to true to disable clustering
     * @return
     */
    @Override
    public PayaraMicroImpl setNoCluster(boolean noCluster) {
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
    @Override
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
    @Override
    public PayaraMicroImpl setLite(boolean liteMember) {
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
    @Override
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
    @Override
    public PayaraMicroImpl setMaxHttpThreads(int maxHttpThreads) {
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
    @Override
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
    @Override
    public PayaraMicroImpl setMinHttpThreads(int minHttpThreads) {
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
    @Override
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
    @Override
    public PayaraMicroImpl setRootDir(File rootDir) {
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
    @Override
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
    @Override
    public PayaraMicroImpl setHttpAutoBind(boolean httpAutoBind) {
        this.autoBindHttp = httpAutoBind;
        return this;
    }

    /**
     * Indicates whether autobinding of the HTTPS port is enabled
     *
     * @return
     */
    @Override
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
    @Override
    public PayaraMicroImpl setSslAutoBind(boolean sslAutoBind) {
        this.autoBindSsl = sslAutoBind;
        return this;
    }

    /**
     * Gets the maximum number of ports to check if free for autobinding
     * purposes
     *
     * @return The number of ports to check if free
     */
    @Override
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
    @Override
    public PayaraMicroImpl setAutoBindRange(int autoBindRange) {
        this.autoBindRange = autoBindRange;
        return this;
    }

    /**
     * Gets the name of the Hazelcast cluster group. Clusters with different
     * names do not interact
     *
     * @return The current Cluster Name
     */
    @Override
    public String getHzClusterName() {
        return hzClusterName;
    }

    /**
     * Sets the name of the Hazelcast cluster group
     *
     * @param hzClusterName The name of the hazelcast cluster
     * @return
     */
    @Override
    public PayaraMicroImpl setHzClusterName(String hzClusterName) {
        this.hzClusterName = hzClusterName;
        return this;
    }

    /**
     * Gets the password of the Hazelcast cluster group
     *
     * @return
     */
    @Override
    public String getHzClusterPassword() {
        return hzClusterPassword;
    }

    /**
     * Sets the Hazelcast cluster group password. For two clusters to work
     * together then the group name and password must be the same
     *
     * @param hzClusterPassword The password to set
     * @return
     */
    @Override
    public PayaraMicroImpl setHzClusterPassword(String hzClusterPassword) {
        this.hzClusterPassword = hzClusterPassword;
        return this;
    }

    /**
     * Gets the name of the instance group
     *
     * @return The name of the instance group
     */
    @Override
    public String getInstanceGroup() {
        return instanceGroup;
    }

    /**
     * Sets the instance group name
     *
     * @param instanceGroup The instance group name
     * @return
     */
    @Override
    public PayaraMicroImpl setInstanceGroup(String instanceGroup) {
        this.instanceGroup = instanceGroup;
        return this;
    }

    /**
     * Boots the Payara Micro Server. All parameters are checked at this point
     *
     * @return An instance of PayaraMicroRuntime that can be used to access the
     * running server
     * @throws BootstrapException
     */
    @Override
    public PayaraMicroRuntime bootStrap() throws BootstrapException {
        // First check whether we are already running
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, calling bootstrap now is meaningless");
        }

        long start = System.currentTimeMillis();

        // Build the runtime directory
        try {
            unPackRuntime();
        } catch (IOException | URISyntaxException ex) {
            throw new BootstrapException("Problem unpacking the Runtime", ex);
        }
        resetLogging();

        // build the runtime
        BootstrapProperties bprops = new BootstrapProperties();
        bprops.setInstallRoot(runtimeDir.getDirectory().getAbsolutePath());
        bprops.setProperty(Constants.PLATFORM_PROPERTY_KEY, Constants.Platform.PayaraMicro.toString());
        GlassFishRuntime gfruntime;
        try {
            gfruntime = GlassFishRuntime.bootstrap(bprops, Thread.currentThread().getContextClassLoader());
            GlassFishProperties gfproperties = new GlassFishProperties();
            gfproperties.setProperty("-type", "MICRO");
            gfproperties.setInstanceRoot(runtimeDir.getDirectory().getAbsolutePath());
            gfproperties.setConfigFileReadOnly(false);
            gfproperties.setConfigFileURI(runtimeDir.getDomainXML().toURI().toString());

            try {
                configureCommandFiles();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Unable to load command file", ex);
            }

            gf = gfruntime.newGlassFish(gfproperties);

            configurePorts();
            configureThreads();
            configureAccessLogging();
            configureHazelcast();
            configurePhoneHome();
            configureNotificationService();
            configureHealthCheck();
            configureSecrets();

            // Add additional libraries
            addLibraries();

            // boot the server
            preBootCommands.executeCommands(gf.getCommandRunner());
            gf.start();
            postBootCommands.executeCommands(gf.getCommandRunner());
            this.runtime = new PayaraMicroRuntimeImpl(gf, gfruntime);

            // do deployments
            deployAll();

            postBootActions();
            postDeployCommands.executeCommands(gf.getCommandRunner());

            long end = System.currentTimeMillis();
            dumpFinalStatus(end - start);
            return runtime;
        } catch (Exception ex) {
            try {
                gf.dispose();
            } catch (GlassFishException ex1) {
                Logger.getLogger(PayaraMicroImpl.class.getName()).log(Level.SEVERE, null, ex1);
            }
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
    @Override
    public PayaraMicroRuntimeImpl getRuntime() throws IllegalStateException {
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
    @Override
    public void shutdown() throws BootstrapException {
        if (!isRunning()) {

            throw new IllegalStateException("Payara Micro is not running");
        }
        runtime.shutdown();
        runtime = null;
    }

    private PayaraMicroImpl() {
        // Initialise a random instance name
        repositoryURLs = new LinkedList<>();
        preBootCommands = new BootCommands();
        postBootCommands = new BootCommands();
        postDeployCommands = new BootCommands();
        repositoryURLs.add(defaultMavenRepository);
        addShutdownHook();
    }

    private void scanArgs(String[] args) {

        RuntimeOptions options = null;
        try {
            options = new RuntimeOptions(args);
        } catch (ValidationException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            System.exit(-1);
        }

        processUserProperties(options);
        setArgumentsFromSystemProperties();

        for (RUNTIME_OPTION option : options.getOptions()) {
            List<String> values = options.getOption(option);
            for (String value : values) {
                switch (option) {
                    case port: {
                        httpPort = Integer.parseInt(value);
                        break;
                    }
                    case sslport: {
                        sslPort = Integer.parseInt(value);
                        break;
                    }
                    case sslcert: {
                        sslCert = value;
                        break;
                    }
                    case version: {
                        printVersion();
                        System.exit(1);
                        break;
                    }
                    case maxhttpthreads: {
                        maxHttpThreads = Integer.parseInt(value);
                        break;
                    }
                    case minhttpthreads: {
                        minHttpThreads = Integer.parseInt(value);
                        break;
                    }
                    case mcaddress:
                        hzMulticastGroup = value;
                        break;
                    case clustername:
                        hzClusterName = value;
                        break;
                    case clusterpassword:
                        hzClusterPassword = value;
                        break;
                    case hostaware: {
                        hostAware = true;
                        break;
                    }
                    case mcport: {
                        hzPort = Integer.parseInt(value);
                        break;
                    }
                    case startport:
                        hzStartPort = Integer.parseInt(value);
                        break;
                    case name:
                        instanceName = value;
                        break;
                    case instancegroup:
                    case group:
                        instanceGroup = value;
                        break;
                    case deploymentdir:
                    case deploydir:
                        deploymentRoot = new File(value);
                        break;
                    case rootdir:
                        rootDir = new File(value);
                        break;
                    case addlibs:
                    case addjars:
                        List<File> files = UberJarCreator.parseFileList(value, File.pathSeparator);
                        if(!files.isEmpty()) {
                            if (libraries == null) {
                                libraries = new LinkedList<>();
                            }
                            libraries.addAll(files);
                        }
                        break;
                    case deploy:
                        File deployment = new File(value);
                        if (deployments == null) {
                            deployments = new LinkedList<>();
                        }
                        deployments.add(deployment);
                        break;
                    case domainconfig:
                        alternateDomainXML = new File(value);
                        break;
                    case nocluster:
                        noCluster = true;
                        break;
                    case lite:
                        liteMember = true;
                        break;
                    case hzconfigfile:
                        alternateHZConfigFile = new File(value);
                        break;
                    case autobindhttp:
                        autoBindHttp = true;
                        break;
                    case autobindssl:
                        autoBindSsl = true;
                        break;
                    case autobindrange:
                        autoBindRange = Integer.parseInt(value);
                        break;
                    case enablehealthcheck:
                        enableHealthCheck = Boolean.valueOf(value);
                        break;
                    case deployfromgav:
                        if (GAVs == null) {
                            GAVs = new LinkedList<>();
                        }
                        GAVs.add(value);
                        break;
                    case additionalrepository:
                         repositoryURLs.add(value);
                        break;
                    case outputuberjar:
                        uberJar = new File(value);
                        break;
                    case copytouberjar:
                        copyDirectory = new File(value);
                        break;
                    case disablephonehome:
                        disablePhoneHome = true;
                        break;
                    case enablerequesttracing:
                        enableRequestTracing = true;
                        // Check if a value has actually been given
                        // Split strings from numbers
                        if (value != null) {
                            String[] requestTracing = value.split("(?<=\\d)(?=\\D)|(?=\\d)(?<=\\D)");
                            // If valid, there should be no more than 2 entries
                            if (requestTracing.length <= 2) {
                                // If the first entry is a number
                                if (requestTracing[0].matches("\\d+")) {
                                    try {
                                        requestTracingThresholdValue = Long.parseLong(requestTracing[0]);
                                    } catch (NumberFormatException e) {
                                        LOGGER.log(Level.WARNING, "{0} is not a valid request tracing "
                                                + "threshold value", requestTracing[0]);
                                        throw e;
                                    }
                                    // If there is a second entry, and it's a String
                                    if (requestTracing.length == 2 && requestTracing[1].matches("\\D+")) {
                                        String parsedUnit = parseRequestTracingUnit(requestTracing[1]);
                                        try {
                                            TimeUnit.valueOf(parsedUnit.toUpperCase());
                                            requestTracingThresholdUnit = parsedUnit.toUpperCase();
                                        } catch (IllegalArgumentException e) {
                                            LOGGER.log(Level.WARNING, "{0} is not a valid request "
                                                    + "tracing threshold unit", requestTracing[1]);
                                            throw e;
                                        }
                                    } // If there is a second entry, and it's not a String
                                    else if (requestTracing.length == 2 && !requestTracing[1].matches("\\D+")) {
                                        throw new IllegalArgumentException();
                                    }
                                } // If the first entry is a String
                                else if (requestTracing[0].matches("\\D+")) {
                                    String parsedUnit = parseRequestTracingUnit(requestTracing[0]);
                                    try {
                                        TimeUnit.valueOf(parsedUnit.toUpperCase());
                                        requestTracingThresholdUnit = parsedUnit.toUpperCase();
                                    } catch (IllegalArgumentException e) {
                                        LOGGER.log(Level.WARNING, "{0} is not a valid request "
                                                + "tracing threshold unit", requestTracing[0]);
                                        throw e;
                                    }
                                    // There shouldn't be a second entry
                                    if (requestTracing.length == 2) {
                                        throw new IllegalArgumentException();
                                    }
                                }
                            } else {
                                throw new IllegalArgumentException();
                            }
                        }
                        break;
                    case requesttracingthresholdunit:
                        try {
                            String parsedUnit = parseRequestTracingUnit(value);
                            TimeUnit.valueOf(parsedUnit.toUpperCase());
                            requestTracingThresholdUnit = parsedUnit.toUpperCase();
                        } catch (IllegalArgumentException e) {
                            LOGGER.log(Level.WARNING, "{0} is not a valid value for --requestTracingThresholdUnit",
                                    value);
                            throw e;
                        }
                        break;
                    case requesttracingthresholdvalue:
                        try {
                            requestTracingThresholdValue = Long.parseLong(value);
                        } catch (NumberFormatException e) {
                            LOGGER.log(Level.WARNING, "{0} is not a valid value for --requestTracingThresholdValue",
                                    value);
                            throw e;
                        }
                        break;
                    case help:
                        RuntimeOptions.printHelp();
                        System.exit(1);
                        break;
                    case logtofile:
                        setUserLogFile(value);
                        break;
                    case accesslog:
                        File file = new File(value);
                        setAccessLogDir(file.getAbsolutePath());
                        break;
                    case accesslogformat:
                        setAccessLogFormat(value);
                        break;
                    case logproperties:
                        setLogPropertiesFile(new File(value));
                        break;
                    case logo:
                        generateLogo = true;
                        break;
                    case postbootcommandfile:
                        postBootFileName = value;
                        break;
                    case prebootcommandfile:
                        preBootFileName = value;
                        break;
                    case postdeploycommandfile:
                        postDeployFileName = value;
                        break;
                    case secretsdir:
                        secretsDir = value;
                        break;
                    case enablesni:
                        sniEnabled = true;
                        break;
                    default:
                        break;
                }
            }
        }

    }

    /**
     * Process the user system properties in precedence
     * 1st loads the properties from the uber jar location
     * then loads each command line system properties file which will override
     * uber jar properties
     *
     * @param options
     * @throws IllegalArgumentException
     */
    private void processUserProperties(RuntimeOptions options) throws IllegalArgumentException {
        userSystemProperties = new Properties();
        // load all from the uber jar first
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(USER_PROPS_FILE)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                for (Map.Entry<?, ?> entry : props.entrySet()) {
                    userSystemProperties.setProperty((String)entry.getKey(), (String)entry.getValue());
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }

        // process each command line system properties option
        List<String> propertiesoption = options.getOption(RUNTIME_OPTION.systemproperties);
        if (propertiesoption != null && !propertiesoption.isEmpty()) {
            // process the system properties
            for (String string : propertiesoption) {
                File propertiesFile = new File(string);
                Properties tempProperties = new Properties();
                try (FileReader reader = new FileReader(propertiesFile)) {
                    tempProperties.load(reader);
                    Enumeration<String> names = (Enumeration<String>) tempProperties.propertyNames();
                    while (names.hasMoreElements()) {
                        String name = names.nextElement();
                        userSystemProperties.setProperty(name, tempProperties.getProperty(name));
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE,
                            "{0} is not a valid properties file",
                            propertiesFile.getAbsolutePath());
                    throw new IllegalArgumentException(e);
                }
            }
        }

        // now set them
        for (String stringPropertyName : userSystemProperties.stringPropertyNames()) {
            System.setProperty(stringPropertyName, userSystemProperties.getProperty(stringPropertyName));
        }
    }

    private void deployAll() throws GlassFishException {

        // Deploy from within the jar first.
        int deploymentCount = 0;
        Deployer deployer = gf.getDeployer();

        // search and deploy from MICRO-INF/deploy directory.
        // if there is a deployment called ROOT deploy to the root context /
        URL url = this.getClass().getClassLoader().getResource("MICRO-INF/deploy");
        if (url != null) {
            String entryName = "";
            try {
                List<String> microInfEntries = new LinkedList<>();
                JarURLConnection urlcon = (JarURLConnection) url.openConnection();
                JarFile jFile = urlcon.getJarFile();
                Enumeration<JarEntry> entries = jFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    entryName = entry.getName();
                    if (!entry.isDirectory() && !entryName.endsWith(".properties") && !entryName.endsWith(".xml") && !entryName.endsWith(".gitkeep") && entryName.startsWith("MICRO-INF/deploy")) {
                        microInfEntries.add(entryName);
                    }
                }

                for (String entry : microInfEntries) {
                    File file = new File(entry);
                    String contextRoot = file.getName();
                    String name = contextRoot.substring(0, contextRoot.length() - 4);
                    if (contextRoot.endsWith(".ear") || contextRoot.endsWith(".war") || contextRoot.endsWith(".jar") || contextRoot.endsWith(".rar")) {
                        contextRoot = name;
                    }

                    if (contextRoot.equals("ROOT")) {
                        contextRoot = "/";
                    }

                    deployer.deploy(this.getClass().getClassLoader().getResourceAsStream(entry), "--availabilityenabled",
                            "true", "--contextroot",
                            contextRoot, "--name", name, "--force", "true");
                    deploymentCount++;
                }
            } catch (IOException ioe) {
                LOGGER.log(Level.WARNING, "Could not deploy jar entry {0}",
                        entryName);
            }
        } else {
            LOGGER.info("No META-INF/deploy directory");
        }

        // Deploy command line provided files
        if (deployments != null) {
            for (File war : deployments) {
                if (war.exists() && war.canRead()) {
                    if (war.getName().startsWith("ROOT.")) {
                        deployer.deploy(war, "--availabilityenabled=true", "--force=true", "--contextroot=/");
                    } else {
                        deployer.deploy(war, "--availabilityenabled=true", "--force=true");
                    }
                    deploymentCount++;
                } else {
                    LOGGER.log(Level.WARNING, "{0} is not a valid deployment", war.getAbsolutePath());
                }
            }
        }

        // Deploy from deployment directory
        if (deploymentRoot != null) {
            
            // Get all files in the directory, and sort them by file type
            List<File> deploymentDirEntries = Arrays.asList(deploymentRoot.listFiles());
            Collections.sort(deploymentDirEntries, new DeploymentComparator());
            
            for (File entry : deploymentDirEntries) {
                String entryPath = entry.getAbsolutePath();
                if (entry.isFile() && entry.canRead() && (entryPath.endsWith(".war") || entryPath.endsWith(".ear") || entryPath.endsWith(".jar") || entryPath.endsWith(".rar"))) {
                    if (entry.getName().startsWith("ROOT.")) {
                        deployer.deploy(entry, "--availabilityenabled=true", "--force=true", "--contextroot=/");
                    } else {
                        deployer.deploy(entry, "--availabilityenabled=true", "--force=true");
                    }
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
                                deploymentMapEntry.getKey(), "--force=true");

                        deploymentCount++;
                    } catch (URISyntaxException ex) {
                        LOGGER.log(Level.WARNING, "{0} could not be converted to a URI,"
                                + " artefact will be skipped",
                                deploymentMapEntry.getValue().toString());
                    }
                }
            }
        }

        LOGGER.log(Level.INFO, "Deployed {0} archive(s)", deploymentCount);
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(
                "GlassFish Shutdown Hook") {
            @Override
            public void run() {
                try {
                    if (gf != null) {
                        gf.dispose();
                    }
                } catch (GlassFishException ex) {
                } catch (IllegalStateException ex) {
                    // Just log at a fine level and move on
                    LOGGER.log(Level.FINE, "Already shut down");
                }
            }
        });
    }

    private void postBootActions() throws GlassFishException {
        if (enableRequestTracing) {
            RequestTracingService requestTracing = gf.getService(RequestTracingService.class);
            requestTracing.getExecutionOptions().setEnabled(true);

            if (!requestTracingThresholdUnit.equals("SECONDS")) {
                requestTracing.getExecutionOptions().setThresholdUnit(TimeUnit.valueOf(requestTracingThresholdUnit));
            }

            if (requestTracingThresholdValue != 30) {
                requestTracing.getExecutionOptions().setThresholdValue(requestTracingThresholdValue);
            }
        }
    }

    private void resetLogging() {

        String loggingProperty = System.getProperty("java.util.logging.config.file");
        if (loggingProperty != null) {
            // we need to copy into the unpacked domain the specified logging.properties file
            File file = new File(loggingProperty);
            if (file.canRead()) {
                try {
                    runtimeDir.setLoggingProperties(file);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Could not copy over logging properties file", ex);
                }
            }

            if (logToFile) {
                LOGGER.log(Level.WARNING, "logToFile command line option ignored as a logging.properties file has been provided");
            }

            System.setProperty("java.util.logging.config.file", runtimeDir.getLoggingProperties().getAbsolutePath());
            try (InputStream is = new FileInputStream(runtimeDir.getLoggingProperties())){
                LogManager.getLogManager().readConfiguration(is);
                
                // go through all root handlers and set formatters based on properties
                Logger rootLogger = LogManager.getLogManager().getLogger("");
                for (Handler handler : rootLogger.getHandlers()) {
                    String formatter = LogManager.getLogManager().getProperty(handler.getClass().getCanonicalName()+".formatter");
                    if (formatter != null) {
                        handler.setFormatter((Formatter) Class.forName(formatter).newInstance());
                    }
                }
                
            } catch (SecurityException | IOException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                LOGGER.log(Level.SEVERE, "Unable to reset the log manager", ex);
            } 
        } else {  // system property was not set on the command line using the command option or via -D
            // we are likely using our default properties file so see if we need to rewrite it
            if (logToFile) {
                // we need to reset our logging properties to use the file handler as well
                // read the default properties and then add the file handler properties
                Properties currentProps = new Properties();
                try (InputStream is = new FileInputStream(runtimeDir.getLoggingProperties())) {
                    currentProps.load(is);

                    // add file handler properties
                    currentProps.setProperty("java.util.logging.FileHandler.pattern", userLogFile);
                    currentProps.setProperty("handlers", "java.util.logging.FileHandler, java.util.logging.ConsoleHandler");
                    currentProps.setProperty("java.util.logging.FileHandler.limit", "1024000");
                    currentProps.setProperty("java.util.logging.FileHandler.count", "10");
                    currentProps.setProperty("java.util.logging.FileHandler.level", "INFO");
                    currentProps.setProperty("java.util.logging.FileHandler.formatter", "java.util.logging.SimpleFormatter");
                    currentProps.setProperty("java.util.logging.FileHandler.append", "true");

                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Unable to load the logging properties from the runtime directory", ex);
                }

                // now write them back
                try (OutputStream os = new FileOutputStream(runtimeDir.getLoggingProperties())) {
                    currentProps.store(os, "Generated Logging properties file from Payara Micro log to file option");
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Unable to load the logging properties from the runtime directory", ex);
                }
            }
            System.setProperty("java.util.logging.config.file", runtimeDir.getLoggingProperties().getAbsolutePath());
            try (InputStream is = new FileInputStream(runtimeDir.getLoggingProperties())){
                LogManager.getLogManager().readConfiguration(is);

                // reset the formatters on the two handlers
                //Logger rootLogger = Logger.getLogger("");
                String formatter = LogManager.getLogManager().getProperty("java.util.logging.ConsoleHandler.formatter");
                Formatter formatterClass = new ODLLogFormatter();
                try {
                    formatterClass = (Formatter) Class.forName(formatter).newInstance();
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                    Logger.getLogger(PayaraMicroImpl.class.getName()).log(Level.SEVERE, "Specified Formatter class could not be loaded " + formatter, ex);
                }
                Logger rootLogger = Logger.getLogger("");
                for (Handler handler : rootLogger.getHandlers()) {
                    handler.setFormatter(formatterClass);
                }
            } catch (SecurityException | IOException ex) {
                LOGGER.log(Level.SEVERE, "Unable to reset the log manager", ex);
            }
        }

    }

    private void configureCommandFiles() throws IOException {
        // load the embedded files
        URL scriptURL = Thread.currentThread().getContextClassLoader().getResource("MICRO-INF/pre-boot-commands.txt");
        if (scriptURL != null) {
            preBootCommands.parseCommandScript(scriptURL);
        }

        if (preBootFileName != null) {
            preBootCommands.parseCommandScript(new File(preBootFileName));
        }

        scriptURL = Thread.currentThread().getContextClassLoader().getResource("MICRO-INF/post-boot-commands.txt");
        if (scriptURL != null) {
            postBootCommands.parseCommandScript(scriptURL);
        }

        if (postBootFileName != null) {
            postBootCommands.parseCommandScript(new File(postBootFileName));
        }

        scriptURL = Thread.currentThread().getContextClassLoader().getResource("MICRO-INF/post-deploy-commands.txt");
        if (scriptURL != null) {
            postDeployCommands.parseCommandScript(scriptURL);
        }

        if (postDeployFileName != null) {
            postDeployCommands.parseCommandScript(new File(postDeployFileName));
        }
    }

    private void configureAccessLogging() {
        if (enableAccessLog) {
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.http-service.access-logging-enabled=true"));
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.http-service.virtual-server.server.access-log=" + userAccessLogDirectory));
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.http-service.virtual-server.server.access-logging-enabled=true"));
            if (enableAccessLogFormat) {
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.http-service.access-log.format=" + accessLogFormat));
            }
        }
    }

    private void configureThreads() {
        if (this.maxHttpThreads != Integer.MIN_VALUE) {
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.thread-pools.thread-pool.http-thread-pool.max-thread-pool-size=" + maxHttpThreads));
        }

        if (this.minHttpThreads != Integer.MIN_VALUE) {
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.thread-pools.thread-pool.http-thread-pool.min-thread-pool-size=" + minHttpThreads));
        }
    }

    private void configurePorts() throws GlassFishException {
        // build the glassfish properties

        if (httpPort != Integer.MIN_VALUE) {
            if (autoBindHttp == true) {
                // Log warnings if overriding other options
                logPortPrecedenceWarnings(false);

                // Configure the port range from the specified port
                int minPort = httpPort;
                int maxPort = minPort + autoBindRange;
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.network-config.network-listeners.network-listener.http-listener.port-range=" + Integer.toString(minPort) + "," + Integer.toString(maxPort)));
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.network-config.network-listeners.network-listener.http-listener.enabled=true"));
            } else {
                // Log warnings if overriding other options
                logPortPrecedenceWarnings(false);
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.network-config.network-listeners.network-listener.http-listener.port=" + httpPort));
            }
        } else if (autoBindHttp == true) {
            // Log warnings if overriding other options
            logPortPrecedenceWarnings(false);

            // Configure the port range from the default HTTP port
            int minPort = defaultHttpPort;
            int maxPort = minPort + autoBindRange;
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.network-config.network-listeners.network-listener.http-listener.port-range=" + Integer.toString(minPort) + "," + Integer.toString(maxPort)));
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.network-config.network-listeners.network-listener.http-listener.enabled=true"));
        }
        if (sslPort != Integer.MIN_VALUE) {
            if (autoBindSsl == true) {
                // Log warnings if overriding other options
                logPortPrecedenceWarnings(true);

                // Configure the port range from the specified port
                int minPort = sslPort;
                int maxPort = minPort + autoBindRange;
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.network-config.network-listeners.network-listener.https-listener.port-range=" + Integer.toString(minPort) + "," + Integer.toString(maxPort)));
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.network-config.network-listeners.network-listener.https-listener.enabled=true"));
            } else {
                // Log warnings if overriding other options
                logPortPrecedenceWarnings(true);

                // Configure the port range from the default port
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.network-config.network-listeners.network-listener.https-listener.port=" + sslPort));
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.network-config.network-listeners.network-listener.https-listener.enabled=true"));
            }
        } else if (autoBindSsl == true) {
            // Log warnings if overriding other options
            logPortPrecedenceWarnings(true);

            // Configure the port range from the default HTTPS port
            int minPort = defaultHttpsPort;
            int maxPort = minPort + autoBindRange;
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.network-config.network-listeners.network-listener.https-listener.port-range=" + Integer.toString(minPort) + "," + Integer.toString(maxPort)));
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.network-config.network-listeners.network-listener.https-listener.enabled=true"));
        }
        
        if (sslCert != null) {
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.network-config.protocols.protocol.https-listener.ssl.cert-nickname=" + sslCert));            
        }
        if (sniEnabled) {
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.network-config.protocols.protocol.https-listener.ssl.sni-enabled=true"));
        }
    }

    private void configurePhoneHome() {
        if (disablePhoneHome == true) {
            LOGGER.info("Disabled Phone Home");
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.phone-home-runtime-configuration.enabled=false"));
        }
    }

    private void configureHazelcast() {
        // check hazelcast cluster overrides
        if (noCluster) {
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.hazelcast-runtime-configuration.enabled=false"));
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.ejb-container.ejb-timer-service.ejb-timer-service=Dummy"));
        } else {

            if (hzPort > Integer.MIN_VALUE) {
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.hazelcast-runtime-configuration.multicast-port=" + hzPort));
            }

            if (hzStartPort > Integer.MIN_VALUE) {
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.hazelcast-runtime-configuration.start-port=" + hzStartPort));
            }

            if (hzMulticastGroup != null) {
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.hazelcast-runtime-configuration.multicast-group=" + hzMulticastGroup));
            }

            if (alternateHZConfigFile != null) {
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.hazelcast-runtime-configuration.hazelcast-configuration-file=" + alternateHZConfigFile.getName()));
            }
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.hazelcast-runtime-configuration.lite=" + liteMember));

            if (hzClusterName != null) {
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.hazelcast-runtime-configuration.cluster-group-name=" + hzClusterName));
            }

            if (hzClusterPassword != null) {
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.hazelcast-runtime-configuration.cluster-group-password=" + hzClusterPassword));
            }

            if (instanceName != null) {
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.hazelcast-runtime-configuration.member-name=" + instanceName));
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.hazelcast-runtime-configuration.generate-names=false"));
            }

            if (instanceGroup != null) {
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.hazelcast-runtime-configuration.member-group=" + instanceGroup));
            }
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.hazelcast-runtime-configuration.host-aware-partitioning=" + hostAware));
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
            LOGGER.log(Level.WARNING, "Problems displaying Boot Image", ex);
        }
    }

    /**
     * Converts the GAVs provided to a URLs, and stores them in the
     * deploymentURLsMap.
     */
    private void getGAVURLs() throws GlassFishException {
        GAVConvertor gavConvertor = new GAVConvertor();
        
        

        for (String gav : GAVs) {
            try {
                Map.Entry<String, URL> artefactMapEntry = gavConvertor.getArtefactMapEntry(gav, repositoryURLs);

                if (deploymentURLsMap == null) {
                    deploymentURLsMap = new LinkedHashMap<>();
                }

                String contextRoot = artefactMapEntry.getKey();
                if ("ROOT".equals(contextRoot)) {
                    contextRoot = "/";
                }

                deploymentURLsMap.put(contextRoot, artefactMapEntry.getValue());
            } catch (MalformedURLException ex) {
                throw new GlassFishException(ex.getMessage());
            }
        }
    }

    /**
     * Logs warnings if ports are being overridden
     *
     * @param HTTPS True if checking if HTTPS ports are being overridden
     */
    private void logPortPrecedenceWarnings(boolean HTTPS) {
        if (HTTPS == true) {
            if (alternateDomainXML != null) {
                if (sslPort != Integer.MIN_VALUE) {
                    if (autoBindSsl == true) {
                        LOGGER.log(Level.INFO, "Overriding HTTPS port value set"
                                + " in {0} and auto-binding against " + sslPort,
                                alternateDomainXML.getAbsolutePath());
                    } else {
                        LOGGER.log(Level.INFO, "Overriding HTTPS port value set"
                                + " in {0} with " + sslPort,
                                alternateDomainXML.getAbsolutePath());
                    }
                } else if (autoBindSsl == true) {
                    LOGGER.log(Level.INFO, "Overriding HTTPS port value set"
                            + " in {0} and auto-binding against "
                            + defaultHttpsPort,
                            alternateDomainXML.getAbsolutePath());
                } else {
                    LOGGER.log(Level.INFO, "Overriding HTTPS port value set"
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
                            LOGGER.log(Level.INFO, "Overriding HTTPS port value"
                                    + " set in {0} and auto-binding against "
                                    + sslPort, configFile.getAbsolutePath());
                        } else {
                            LOGGER.log(Level.INFO, "Overriding HTTPS port value"
                                    + " set in {0} with " + sslPort,
                                    configFile.getAbsolutePath());
                        }
                    } else if (autoBindSsl == true) {
                        LOGGER.log(Level.INFO, "Overriding HTTPS port value"
                                + " set in {0} and auto-binding against "
                                + defaultHttpsPort,
                                configFile.getAbsolutePath());
                    } else {
                        LOGGER.log(Level.INFO, "Overriding HTTPS port value"
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
                        LOGGER.log(Level.INFO, "Overriding HTTP port value set "
                                + "in {0} and auto-binding against " + httpPort,
                                alternateDomainXML.getAbsolutePath());
                    } else {
                        LOGGER.log(Level.INFO, "Overriding HTTP port value set "
                                + "in {0} with " + httpPort,
                                alternateDomainXML.getAbsolutePath());
                    }
                } else if (autoBindHttp == true) {
                    LOGGER.log(Level.INFO, "Overriding HTTP port value set "
                            + "in {0} and auto-binding against "
                            + defaultHttpPort,
                            alternateDomainXML.getAbsolutePath());
                } else {
                    LOGGER.log(Level.INFO, "Overriding HTTP port value set "
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
                            LOGGER.log(Level.INFO, "Overriding HTTP port value "
                                    + "set in {0} and auto-binding against "
                                    + httpPort, configFile.getAbsolutePath());
                        } else {
                            LOGGER.log(Level.INFO, "Overriding HTTP port value "
                                    + "set in {0} with " + httpPort,
                                    configFile.getAbsolutePath());
                        }
                    } else if (autoBindHttp == true) {
                        LOGGER.log(Level.INFO, "Overriding HTTP port value "
                                + "set in {0} and auto-binding against "
                                + defaultHttpPort,
                                configFile.getAbsolutePath());
                    } else {
                        LOGGER.log(Level.INFO, "Overriding HTTP port value "
                                + "set in {0} with default value of "
                                + defaultHttpPort,
                                configFile.getAbsolutePath());
                    }
                }
            }
        }
    }

    private void setArgumentsFromSystemProperties() {

        // Set the domain.xml
        String alternateDomainXMLStr = getProperty("payaramicro.domainConfig");
        if (alternateDomainXMLStr != null && !alternateDomainXMLStr.isEmpty()) {
            applicationDomainXml = alternateDomainXMLStr;
        }

        // Set the hazelcast config file
        String alternateHZConfigFileStr = getProperty("payaramicro.hzConfigFile");
        if (alternateHZConfigFileStr != null && !alternateHZConfigFileStr.isEmpty()) {
            alternateHZConfigFile = new File(alternateHZConfigFileStr);
        }

        autoBindHttp = getBooleanProperty("payaramicro.autoBindHttp");
        autoBindRange = getIntegerProperty("payaramicro.autoBindRange", 5);
        autoBindSsl = getBooleanProperty("payaramicro.autoBindSsl");
        generateLogo = getBooleanProperty("payaramicro.logo");
        logToFile = getBooleanProperty("payaramicro.logToFile");
        userLogFile = getProperty("payaramicro.userLogFile");
        enableAccessLog = getBooleanProperty("payaramicro.enableAccessLog");
        enableAccessLogFormat = getBooleanProperty("payaramicro.logPropertiesFile");
        enableHealthCheck = getBooleanProperty("payaramicro.enableHealthCheck");
        httpPort = getIntegerProperty("payaramicro.port", Integer.MIN_VALUE);
        sslPort = getIntegerProperty("payaramicro.sslPort", Integer.MIN_VALUE);
        sslCert = getProperty("payaramicro.sslCert");
        sniEnabled = getBooleanProperty("payaramicro.sniEnabled");
        hzMulticastGroup = getProperty("payaramicro.mcAddress");
        hzPort = getIntegerProperty("payaramicro.mcPort", Integer.MIN_VALUE);
        hostAware = getBooleanProperty("payaramicro.hostAware");
        hzStartPort = getIntegerProperty("payaramicro.startPort", Integer.MIN_VALUE);
        hzClusterName = getProperty("payaramicro.clusterName");
        hzClusterPassword = getProperty("payaramicro.clusterPassword");
        liteMember = getBooleanProperty("payaramicro.lite");
        maxHttpThreads = getIntegerProperty("payaramicro.maxHttpThreads", Integer.MIN_VALUE);
        minHttpThreads = getIntegerProperty("payaramicro.minHttpThreads", Integer.MIN_VALUE);
        noCluster = getBooleanProperty("payaramicro.noCluster");
        disablePhoneHome = getBooleanProperty("payaramicro.disablePhoneHome");
        enableRequestTracing = getBooleanProperty("payaramicro.enableRequestTracing");
        requestTracingThresholdUnit = getProperty("payaramicro.requestTracingThresholdUnit", "SECONDS");
        requestTracingThresholdValue = getLongProperty("payaramicro.requestTracingThresholdValue", 30L);
        secretsDir = getProperty("payaramicro.secretsDir");

        // Set the rootDir file
        String rootDirFileStr = getProperty("payaramicro.rootDir");
        if (rootDirFileStr != null && !rootDirFileStr.isEmpty()) {
            rootDir = new File(rootDirFileStr);
        }

        String name = getProperty("payaramicro.name");
        if (name != null && !name.isEmpty()) {
            instanceName = name;
        }

        String instanceGroupName = getProperty("payaramicro.instanceGroup");
        if (instanceGroupName != null && !instanceGroupName.isEmpty()) {
            instanceGroup = instanceGroupName;
        }
    }

    private void packageUberJar() {

        UberJarCreator creator = new UberJarCreator(uberJar);
        if (rootDir != null) {
            creator.setDomainDir(rootDir);
        }

        if (postBootFileName != null) {
            creator.setPostBootCommands(new File(postBootFileName));
        }

        if (preBootFileName != null) {
            creator.setPreBootCommands(new File(preBootFileName));
        }

        if (postDeployFileName != null) {
            creator.setPostDeployCommands(new File(postDeployFileName));
        }

        if (logPropertiesFile) {
            creator.setLoggingPropertiesFile(new File(userLogPropertiesFile));
        }

        if (deployments != null) {
            for (File deployment : deployments) {
                creator.addDeployment(deployment);
            }
        }

        if (libraries != null){
            for (File lib : libraries){
                creator.addLibraryJar(lib);
            }
        }

        if (deploymentRoot != null) {
            creator.setDeploymentDir(deploymentRoot);
        }

        if (copyDirectory != null) {
            creator.setDirectoryToCopy(copyDirectory);
        }

        if (GAVs != null) {
            try {
                // Convert the provided GAV Strings into target URLs
                getGAVURLs();
                for (Map.Entry<String, URL> deploymentMapEntry : deploymentURLsMap.entrySet()) {
                    URL deployment = deploymentMapEntry.getValue();
                    String name = deploymentMapEntry.getKey();
                    creator.addDeployment(name, deployment);
                }
            } catch (GlassFishException ex) {
                LOGGER.log(Level.SEVERE, "Unable to process maven deployment units", ex);
            }
        }

        // write the system properties file
        Properties props = new Properties();
        if (hzMulticastGroup != null) {
            props.setProperty("payaramicro.mcAddress", hzMulticastGroup);
        }

        if (hzPort != Integer.MIN_VALUE) {
            props.setProperty("payaramicro.mcPort", Integer.toString(hzPort));
        }
        if (instanceName != null) {
            props.setProperty("payaramicro.name", instanceName);
        }

        if (instanceGroup != null) {
            props.setProperty("payaramicro.instanceGroup", instanceGroup);
        }

        if (hzStartPort != Integer.MIN_VALUE) {
            props.setProperty("payaramicro.startPort", Integer.toString(hzStartPort));
        }

        if (rootDir != null) {
            props.setProperty("payaramicro.rootDir", rootDir.getAbsolutePath());
        }

        if (alternateDomainXML != null) {
            props.setProperty("payaramicro.domainConfig", "MICRO-INF/domain/domain.xml");
        }

        if (minHttpThreads != Integer.MIN_VALUE) {
            props.setProperty("payaramicro.minHttpThreads", Integer.toString(minHttpThreads));
        }

        if (maxHttpThreads != Integer.MIN_VALUE) {
            props.setProperty("payaramicro.maxHttpThreads", Integer.toString(maxHttpThreads));
        }

        if (alternateHZConfigFile != null) {
            props.setProperty("payaramicro.hzConfigFile", "MICRO-INF/domain/hzconfig.xml");
        }

        if (hzClusterName != null) {
            props.setProperty("payaramicro.clusterName", hzClusterName);
        }

        if (hzClusterPassword != null) {
            props.setProperty("payaramicro.clusterPassword", hzClusterPassword);
        }
        
        if (secretsDir != null) {
            props.setProperty("payaramicro.secretsDir", secretsDir);
        }
        
        if (sslCert != null) {
            props.setProperty("payaramicro.sslCert", sslCert);
        }

        props.setProperty("payaramicro.autoBindHttp", Boolean.toString(autoBindHttp));
        props.setProperty("payaramicro.autoBindSsl", Boolean.toString(autoBindSsl));
        props.setProperty("payaramicro.autoBindRange", Integer.toString(autoBindRange));
        props.setProperty("payaramicro.lite", Boolean.toString(liteMember));
        props.setProperty("payaramicro.enableHealthCheck", Boolean.toString(enableHealthCheck));
        props.setProperty("payaramicro.logo", Boolean.toString(generateLogo));
        props.setProperty("payaramicro.logToFile", Boolean.toString(logToFile));
        props.setProperty("payaramicro.enableAccessLog", Boolean.toString(enableAccessLog));
        props.setProperty("payaramicro.enableAccessLogFormat", Boolean.toString(enableAccessLogFormat));
        props.setProperty("payaramicro.logPropertiesFile", Boolean.toString(logPropertiesFile));
        props.setProperty("payaramicro.noCluster", Boolean.toString(noCluster));
        props.setProperty("payaramicro.hostAware", Boolean.toString(hostAware));
        props.setProperty("payaramicro.disablePhoneHome", Boolean.toString(disablePhoneHome));
        props.setProperty("payaramicro.sniEnabled", Boolean.toString(sniEnabled));

        if (userLogFile != null) {
            props.setProperty("payaramicro.userLogFile", userLogFile);
        }

        if (httpPort != Integer.MIN_VALUE) {
            props.setProperty("payaramicro.port", Integer.toString(httpPort));
        }

        if (sslPort != Integer.MIN_VALUE) {
            props.setProperty("payaramicro.sslPort", Integer.toString(sslPort));
        }

        if (enableRequestTracing) {
            props.setProperty("payaramicro.enableRequestTracing", Boolean.toString(enableRequestTracing));
        }

        if (!requestTracingThresholdUnit.equals("SECONDS")) {
            props.setProperty("payaramicro.requestTracingThresholdUnit", requestTracingThresholdUnit);
        }

        if (requestTracingThresholdValue != 30) {
            props.setProperty("payaramicro.requestTracingThresholdValue", Long.toString(requestTracingThresholdValue));
        }

        // write all user defined system properties
        if (userSystemProperties != null) {
            Enumeration<String> names = (Enumeration<String>) userSystemProperties.propertyNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                props.setProperty(name, userSystemProperties.getProperty(name));
            }
        }
        creator.addBootProperties(props);

        // add the alternate domain.xml file if present
        if (alternateDomainXML != null && alternateDomainXML.isFile() && alternateDomainXML.canRead()) {
            creator.setDomainXML(alternateDomainXML);
        }

        // add the alternate hazelcast config to the uberJar
        if (alternateHZConfigFile != null) {
            creator.setAlternateHZConfigFile(alternateHZConfigFile);
        }
        creator.buildUberJar();
    }

    private String parseRequestTracingUnit(String option) {
        String returnValue = option;

        switch (option.toLowerCase()) {
            case "nanosecond":
            case "ns":
                returnValue = "NANOSECONDS";
                break;
            case "microsecond":
            case "us":
            case "µs":
                returnValue = "MICROSECONDS";
                break;
            case "millisecond":
            case "ms":
                returnValue = "MILLISECONDS";
                break;
            case "second":
            case "s":
                returnValue = "SECONDS";
                break;
            case "m":
            case "minute":
            case "min":
            case "mins":
                returnValue = "MINUTES";
                break;
            case "hour":
            case "h":
                returnValue = "HOURS";
                break;
            case "day":
            case "d":
                returnValue = "DAYS";
                break;
        }

        return returnValue;
    }

    private void printVersion() {
        try {
            Properties props = new Properties();
            InputStream input = PayaraMicroImpl.class
                    .getResourceAsStream("/MICRO-INF/domain/branding/glassfish-version.properties");
            props.load(input);
            StringBuilder output = new StringBuilder();
            if (props.getProperty("product_name").isEmpty() == false) {
                output.append(props.getProperty("product_name")).append(" ");
            }
            if (props.getProperty("major_version").isEmpty() == false) {
                output.append(props.getProperty("major_version")).append(".");
            }
            if (props.getProperty("minor_version").isEmpty() == false) {
                output.append(props.getProperty("minor_version")).append(".");
            }
            if (props.getProperty("update_version").isEmpty() == false) {
                output.append(props.getProperty("update_version")).append(".");
            }
            if (props.getProperty("payara_version").isEmpty() == false) {
                output.append(props.getProperty("payara_version"));
            }
            if (props.getProperty("payara_update_version").isEmpty() == false) {
                output.append(".").append(props.getProperty("payara_update_version"));
            }
            if (props.getProperty("build_id").isEmpty() == false) {
                output.append(" Build Number ").append(props.getProperty("build_id"));
            }

            System.err.println(output.toString());

        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, null, ex);

        } catch (IOException io) {
            LOGGER.log(Level.SEVERE, null, io);
        }
    }

    private void unPackRuntime() throws IOException, URISyntaxException {

        if (rootDir != null) {
            runtimeDir = new RuntimeDirectory(rootDir);
        } else {
            runtimeDir = new RuntimeDirectory();
        }

        if (alternateDomainXML != null) {
            runtimeDir.setDomainXML(alternateDomainXML);
        } else if (applicationDomainXml != null) {
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(applicationDomainXml)) {
                runtimeDir.setDomainXML(is);
            }
        }

        if (alternateHZConfigFile != null) {
            runtimeDir.setHZConfigFile(alternateHZConfigFile);
        }
    }

    private static void setBootProperties() {
        Properties bootProperties = new Properties();

        // First Read from embedded boot preoprties
        try (InputStream is = PayaraMicroImpl.class
                .getResourceAsStream(BOOT_PROPS_FILE)) {
            if (is != null) {
                bootProperties.load(is);
                for (String key : bootProperties.stringPropertyNames()) {
                    // do not override an existing system property
                    if (System.getProperty(key) == null) {
                        System.setProperty(key, bootProperties.getProperty(key));
                    }
                }
            }
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING, "Could not load the boot system properties from " + BOOT_PROPS_FILE, ioe);
        }
    }
    
    private void configureNotificationService() {
        if (enableHealthCheck || enableRequestTracing) {
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.notification-service-configuration.enabled=true"));
        }
    }

    private void configureHealthCheck() {
        if (enableHealthCheck) {
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.health-check-service-configuration.enabled=true"));
        }
    }

    private void dumpFinalStatus(long bootTime) {
        InstanceDescriptor id = getRuntime().getLocalDescriptor();
        LOGGER.log(Level.INFO, id.toString());
        StringBuilder sb = new StringBuilder();
        sb.append("\nPayara Micro URLs\n");
        List<URL> urls = id.getApplicationURLS();
        for (URL url : urls) {
            sb.append(url.toString()).append('\n');
        }
        // Count through applications and print out their REST endpoints
        for (ApplicationDescriptor app : id.getDeployedApplications()) {
            sb.append("\n").append("'" + app.getName()).append("' REST Endpoints\n");
            try {
                CommandResult result = gf.getCommandRunner().run("list-rest-endpoints", app.getName());
                sb.append(result.getOutput().replaceAll("PlainTextActionReporter(SUCCESS|FAILURE)", ""));
            } catch (GlassFishException ex) {
                // Really shouldn't happen, the command catches it's own errors most of the time
                Logger.getLogger(PayaraMicroImpl.class.getName()).log(Level.SEVERE, "Failed to get REST endpoints for application", ex);
            }
            sb.append("\n\n");
        }
        LOGGER.log(Level.INFO, sb.toString());
        if (generateLogo) {
            generateLogo();
        }
        LOGGER.log(Level.INFO, "{0} ready in {1} (ms)", new Object[]{Version.getFullVersion(), bootTime});
    }

    private String getProperty(String value) {
        String result;
        result = System.getProperty(value);
        if (result == null) {
            result = System.getenv(value.replace('.', '_'));
        }
        return result;
    }

    private String getProperty(String value, String defaultValue) {
        String result = getProperty(value);
        if (result == null) {
            result = defaultValue;
        }
        return result;
    }

    private Boolean getBooleanProperty(String value) {
        String property;
        property = System.getProperty(value);
        if (property == null) {
            property = System.getenv(value.replace('.', '_'));
        }
        return "true".equals(property);
    }

    private Integer getIntegerProperty(String value, Integer defaultValue) {
        String property;
        property = System.getProperty(value);
        if (property == null) {
            property = System.getenv(value.replace('.', '_'));
        }
        if (property == null) {
            return defaultValue;
        } else {
            return Integer.decode(property);
        }
    }

    private Long getLongProperty(String value, Long defaultValue) {
        String property;
        property = System.getProperty(value);
        if (property == null) {
            property = System.getenv(value.replace('.', '_'));
        }
        if (property == null) {
            return defaultValue;
        } else {
            return Long.decode(property);
        }
    }

    /**
     * Adds libraries to the classlader
     */
    private void addLibraries() {
        if (libraries != null) {
            try {
                for (File lib : libraries) {
                    addLibrary(lib);
                }
            } catch (SecurityException | IllegalArgumentException ex) {
                Logger.getLogger(PayaraMicroImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public PayaraMicroImpl addLibrary(File lib) {
        OpenURLClassLoader loader = (OpenURLClassLoader) this.getClass().getClassLoader();
        if (lib.exists() && lib.canRead() && lib.getName().endsWith(".jar")) {
            try {
                loader.addURL(lib.toURI().toURL());
                LOGGER.log(Level.INFO, "Added " + lib.getAbsolutePath() + " to classpath");
            } catch (MalformedURLException ex) {
                Logger.getLogger(PayaraMicroImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            LOGGER.log(Level.SEVERE, "Unable to read jar " + lib.getName());
        }
        return this;
    }

    private void configureSecrets() {
        if (secretsDir != null) {
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.microprofile-config.secret-dir=" + secretsDir));            
        }
    }

}
