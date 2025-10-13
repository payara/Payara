/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2025] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

import com.sun.enterprise.util.JDK;
import com.sun.enterprise.util.PropertyPlaceholderHelper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.sun.appserv.server.util.Version;
import com.sun.common.util.logging.SortedLoggingProperties;
import com.sun.enterprise.glassfish.bootstrap.Constants;
import com.sun.enterprise.glassfish.bootstrap.GlassFishImpl;
import com.sun.enterprise.server.logging.ODLLogFormatter;

import fish.payara.deployment.util.JavaArchiveUtils;
import fish.payara.deployment.util.URIUtils;
import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFish.Status;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;

import fish.payara.appserver.rest.endpoints.config.admin.ListRestEndpointsCommand;
import fish.payara.boot.runtime.BootCommand;
import fish.payara.boot.runtime.BootCommands;
import fish.payara.deployment.util.GAVConvertor;
import fish.payara.micro.BootstrapException;
import fish.payara.micro.PayaraMicroRuntime;
import fish.payara.micro.boot.AdminCommandRunner;
import fish.payara.micro.boot.PayaraMicroBoot;
import fish.payara.micro.boot.PayaraMicroLauncher;
import fish.payara.micro.boot.loader.OpenURLClassLoader;
import fish.payara.micro.cmd.options.RUNTIME_OPTION;
import fish.payara.micro.cmd.options.RuntimeOptions;
import fish.payara.micro.cmd.options.ValidationException;
import fish.payara.micro.data.InstanceDescriptor;
import fish.payara.nucleus.executorservice.PayaraFileWatcher;

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

    private static final Logger LOGGER = Logger.getLogger("PayaraMicro");

    private static final String BOOT_PROPS_FILE = "/MICRO-INF/payara-boot.properties";
    private static final String USER_PROPS_FILE = "MICRO-INF/deploy/payaramicro.properties";
    private static final String CONTEXT_PROPS_FILE = "MICRO-INF/deploy/contexts.properties";

    private static PayaraMicroImpl instance;

    private String hzMulticastGroup;
    private int hzPort = Integer.MIN_VALUE;
    private int hzStartPort = Integer.MIN_VALUE;
    private String hzClusterName;
    private int httpPort = Integer.MIN_VALUE;
    private int sslPort = Integer.MIN_VALUE;
    private int maxHttpThreads = Integer.MIN_VALUE;
    private int minHttpThreads = Integer.MIN_VALUE;
    private String instanceName;
    private String contextRoot;
    private String globalContextRoot;
    private File rootDir;
    private File deploymentRoot;
    private File alternateDomainXML;
    private File alternateHZConfigFile;
    private List<Map.Entry<RUNTIME_OPTION, String>> deploymentOptions;
    private Map<String, URI> deployments;
    private Properties contextRoots;
    private List<File> libraries;
    private GlassFish gf;
    private PayaraMicroRuntimeImpl runtime;
    private boolean noCluster = false;
    private boolean noHazelcast = false;
    private boolean hostAware = true;
    private boolean autoBindHttp = false;
    private boolean autoBindSsl = false;
    private boolean liteMember = false;
    private boolean generateLogo = false;
    private boolean logToFile = false;
    private boolean enableAccessLog = false;
    private boolean enableAccessLogFormat = false;
    private boolean logPropertiesFile = false;
    private boolean enableDynamicLogging;
    private String userLogPropertiesFile = "";
    private int autoBindRange = 50;
    private String bootImage = "MICRO-INF/domain/boot.txt";
    private String applicationDomainXml;
    private boolean enableHealthCheck = false;
    private boolean disablePhoneHome = false;
    private File uberJar;
    private boolean outputLauncher;
    private File copyDirectory;
    private Properties userSystemProperties;
    private final List<String> repositoryURIs;
    private final short defaultHttpPort = 8080;
    private final short defaultHttpsPort = 8181;
    private final BootCommands preBootCommands;
    private final BootCommands postBootCommands;
    private final BootCommands postDeployCommands;
    private Consumer<AdminCommandRunner> preBootHandler;
    private Consumer<AdminCommandRunner> postBootHandler;
    private String userLogFile = "payara-server%u.log";
    private String userAccessLogDirectory = "";
    private String accessLogFormat = "%client.name% %auth-user-name% %datetime% %request% %status% %response.length%";
    private int accessLogInterval = 300;
    private String accessLogSuffix = "yyyy-MM-dd";
    private String accessLogPrefix;
    private boolean enableRequestTracing = false;
    private String requestTracingThresholdUnit = "SECONDS";
    private long requestTracingThresholdValue = 30;
    private boolean enableRequestTracingAdaptiveSampling = false;
    private int requestTracingAdaptiveSamplingTargetCount = 12;
    private int requestTracingAdaptiveSamplingTimeValue = 1;
    private String requestTracingAdaptiveSamplingTimeUnit = "MINUTES";
    private String instanceGroup;
    private String preBootFileName;
    private String postBootFileName;
    private String postDeployFileName;
    private RuntimeDirectory runtimeDir = null;
    private String clustermode;
    private String interfaces;
    private String secretsDir;
    private String sslCert;
    private boolean showServletMappings;
    private boolean sniEnabled = false;
    private String publicAddress = "";
    private int initialJoinWait = 1;
    private boolean warmup;
    private boolean hotDeploy;

    /**
     * Runs a Payara Micro server used via java -jar payara-micro.jar
     *
     * @param args Command line arguments for PayaraMicro Usage: --help to see
     * all the options
     * <br/>
     * --help Shows this message and exits\n
     * @throws BootstrapException If there is a problem booting the server
     */
    public static void main(String[] args) throws Exception {
        create(args);
    }

    public static PayaraMicroBoot create(String[] args) throws Exception {
        // configure boot system properties
        setBootProperties();
        PayaraMicroImpl main = getInstance();
        main.scanArgs(args);
        if (main.getUberJar() != null) {
            main.packageUberJar();
        } else if (main.outputLauncher) {
            main.createLauncher();
        } else {
            main.bootStrap();
            if (main.warmup) {
                main.shutdown();
            }
        }
        return main;
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
            PayaraMicroLauncher.registerLaunchedInstance(instance);
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
        checkNotRunning();
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
     * @return
     */
    @Override
    public PayaraMicroBoot setAccessLogDir(String filePath) {
        this.userAccessLogDirectory = filePath;
        enableAccessLog = true;
        return this;
    }

    /**
     * Set user defined formatting for the access log
     *
     * @param format
     * @return
     */
    @Override
    public PayaraMicroBoot setAccessLogFormat(String format) {
        this.accessLogFormat = format;
        this.enableAccessLogFormat = true;
        return this;
    }

    /**
     * Set user defined interval for the access log
     *
     * @param interval
     * @return
     */
    public PayaraMicroBoot setAccessLogInterval(int interval) {
        this.accessLogInterval = interval;
        return this;
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
        checkNotRunning();
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
        checkNotRunning();
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
        checkNotRunning();
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
        checkNotRunning();
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
     *
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
        checkNotRunning();
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
        checkNotRunning();

        validateRuntimeOption(RUNTIME_OPTION.deploydir, deploymentRoot.getPath());

        if (this.deploymentRoot == null) {
            if (deploymentOptions == null) {
                deploymentOptions = new LinkedList<>();
            }
            // Map entry value are unused because we use deploymentRoot property
            deploymentOptions.add(new AbstractMap.SimpleImmutableEntry<>(RUNTIME_OPTION.deploydir, null));
        } else {
            LOGGER.warning("Multiple deploy dirs only last one will be apply");
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
        checkNotRunning();
        this.alternateDomainXML = alternateDomainXML;
        return this;
    }

    private void checkNotRunning() {
        if (isRunning()) {
            throw new IllegalStateException("Payara Micro is already running, setting attributes has no effect");
        }
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
        checkNotRunning();

        validateRuntimeOption(RUNTIME_OPTION.deploy, pathToWar);

        if (deploymentOptions == null) {
            deploymentOptions = new LinkedList<>();
        }
        deploymentOptions.add(new AbstractMap.SimpleImmutableEntry<>(RUNTIME_OPTION.deploy, pathToWar));
        return this;
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
        checkNotRunning();
        return addDeployment(file.getPath());
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
        checkNotRunning();

        validateRuntimeOption(RUNTIME_OPTION.deployfromgav, GAV);

        if (deploymentOptions == null) {
            deploymentOptions = new LinkedList<>();
        }
        deploymentOptions.add(new AbstractMap.SimpleImmutableEntry<>(RUNTIME_OPTION.deployfromgav, GAV));
        return this;
    }

    private void validateRuntimeOption(RUNTIME_OPTION option, String optionValue) {
        try {
            option.validate(optionValue);
        } catch (ValidationException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            System.exit(-1);
        }
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
        checkNotRunning();
        repositoryURIs.addAll(Arrays.asList(URLs));
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
     * Indicated whether distributed data grid is enabled
     *
     * @return
     */
    @Override
    public boolean isNoHazelcast() {
        return noHazelcast;
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
        checkNotRunning();
        this.noCluster = noCluster;
        return this;
    }

    /**
     * Enables or disables clustering before bootstrap
     *
     * @param noHazelcast set to true to disable clustering
     * @return
     */
    @Override
    public PayaraMicroImpl setNoHazelcast(boolean noHazelcast) {
        checkNotRunning();
        this.noHazelcast = noHazelcast;
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
        checkNotRunning();
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
        checkNotRunning();
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
        checkNotRunning();
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
        checkNotRunning();
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
        final String loggingProperty = System.getProperty("java.util.logging.config.file");
        resetLogging(loggingProperty);
        // If it's been enabled, watch the log file for changes
        if (enableDynamicLogging) {
            PayaraFileWatcher.watch(new File(loggingProperty).toPath(), () -> {
                LOGGER.info("Logging file modified, resetting logging");
                resetLogging(loggingProperty);
            });
        }
        //Check a supported JDK version is being used
        if (!JDK.isRunningLTSJDK()) {
            LOGGER.warning("You are running the product on an unsupported JDK version and might see unexpected results or exceptions.");
        }
        runtimeDir.processDirectoryInformation();

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
                parsePreBootCommandFiles();
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
            configureRequestTracingService();
            configureSecrets();

            // Add additional libraries
            addLibraries();

            // boot the server
            preBootCommands.executeCommands(gf.getCommandRunner());
            callHandler(preBootHandler);
            gf.start();

            // Parse and execute post boot commands
            try {
                parsePostBootCommandFiles();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Unable to load command file", ex);
            }
            postBootCommands.executeCommands(gf.getCommandRunner());
            callHandler(postBootHandler);
            this.runtime = new PayaraMicroRuntimeImpl(gf, gfruntime);

            // deploy all applications and then initialize them
            deployAll();
            // These steps are separated in case any steps need to be done in between
            gf.getCommandRunner().run("initialize-all-applications");

            // Parse and execute post deploy commands
            try {
                parsePostDeployCommandFiles();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Unable to load command file", ex);
            }
            postDeployCommands.executeCommands(gf.getCommandRunner());

            long end = System.currentTimeMillis();
            dumpFinalStatus(end - start);
            return runtime;
        } catch (Exception ex) {
            try {
                if (gf != null) {
                    gf.dispose();
                }
            } catch (GlassFishException ex1) {
                LOGGER.log(Level.SEVERE, null, ex1);
            }
            throw new BootstrapException(ex.getMessage(), ex);
        }
    }

    private void callHandler(Consumer<AdminCommandRunner> handler) throws GlassFishException {
        CommandRunner runner = gf.getCommandRunner();
        if (handler != null) {
            handler.accept((cmd, args) -> new CommandResultAdapter(runner.run(cmd, args)));
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
        repositoryURIs = new LinkedList<>();
        preBootCommands = new BootCommands();
        postBootCommands = new BootCommands();
        postDeployCommands = new BootCommands();
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

        for (Map.Entry<RUNTIME_OPTION, String> optionEntry : options.getOptions()) {
            RUNTIME_OPTION option = optionEntry.getKey();
            String value = optionEntry.getValue();
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
                case hostaware: {
                    hostAware = true;
                    break;
                }
                case nohostaware: {
                    hostAware = false;
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
                    if (deploymentRoot == null) {
                        if (deploymentOptions == null) {
                            deploymentOptions = new LinkedList<>();
                        }
                        deploymentOptions.add(new AbstractMap.SimpleImmutableEntry<>(option, null));
                    } else {
                        LOGGER.warning("Multiple --deploydir arguments only the last one will apply");
                    }
                    deploymentRoot = new File(value);
                    break;
                case rootdir:
                    rootDir = new File(value);
                    break;
                case addlibs:
                case addjars:
                    List<File> files = UberJarCreator.parseFileList(value, File.pathSeparator);
                    if (!files.isEmpty()) {
                        if (libraries == null) {
                            libraries = new LinkedList<>();
                        }
                        libraries.addAll(files);
                    }
                    break;
                case deploy:
                case deployfromgav:
                    if (deploymentOptions == null) {
                        deploymentOptions = new LinkedList<>();
                    }
                    deploymentOptions.add(new AbstractMap.SimpleImmutableEntry<>(option, value));
                    break;
                case domainconfig:
                    alternateDomainXML = new File(value);
                    break;
                case nocluster:
                    noCluster = true;
                    break;
                case nohazelcast:
                    noHazelcast = true;
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
                    enableHealthCheck = Boolean.parseBoolean(value);
                    break;
                case additionalrepository:
                    repositoryURIs.add(value);
                    break;
                case outputuberjar:
                    uberJar = new File(value);
                    break;
                case copytouberjar:
                    copyDirectory = new File(value);
                    break;
                case outputlauncher:
                    outputLauncher = true;
                    break;
                case warmup:
                    warmup = true;
                    break;
                case hotdeploy:
                    hotDeploy = true;
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
                                requestTracingThresholdValue = parseArgument(requestTracing[0],
                                        "request tracing threshold value", Long::parseLong);
                                // If there is a second entry, and it's a String
                                if (requestTracing.length == 2 && requestTracing[1].matches("\\D+")) {
                                    requestTracingThresholdUnit = parseTimeUnit(requestTracing[1],
                                            "request tracing threshold unit").name();
                                }
                            } // If the first entry is a String
                            else if (requestTracing[0].matches("\\D+")) {
                                requestTracingThresholdUnit = parseTimeUnit(requestTracing[0],
                                        "request tracing threshold unit").name();
                            }
                        } else {
                            throw new IllegalArgumentException();
                        }
                    }
                    break;
                case requesttracingthresholdunit:
                    requestTracingThresholdUnit = parseTimeUnit(value, "value for --requestTracingThresholdUnit").name();
                    break;
                case requesttracingthresholdvalue:
                    requestTracingThresholdValue = parseArgument(value, "value for --requestTracingThresholdValue",
                            Long::parseLong);
                    break;
                case enablerequesttracingadaptivesampling:
                    enableRequestTracingAdaptiveSampling = true;
                    break;
                case requesttracingadaptivesamplingtargetcount:
                    enableRequestTracingAdaptiveSampling = true;
                    requestTracingAdaptiveSamplingTargetCount = parseArgument(value,
                            "value for --requestTracingAdaptiveSamplingTargetCount", Integer::parseInt);
                    break;
                case requesttracingadaptivesamplingtimevalue:
                    enableRequestTracingAdaptiveSampling = true;
                    requestTracingAdaptiveSamplingTimeValue = parseArgument(value,
                            "value for --requestTracingAdaptiveSamplingTimeValue", Integer::parseInt);
                    break;
                case requesttracingadaptivesamplingtimeunit:
                    enableRequestTracingAdaptiveSampling = true;
                    requestTracingAdaptiveSamplingTimeUnit = parseTimeUnit(value,
                            "value for --requestTracingAdaptiveSamplingTimeUnit").name();
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
                case enabledynamiclogging:
                    enableDynamicLogging = true;
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
                case clustermode:
                    clustermode = value;
                    break;
                case interfaces:
                    interfaces = value;
                    break;
                case secretsdir:
                    secretsDir = value;
                    break;
                case showservletmappings:
                    showServletMappings = true;
                    break;
                case enablesni:
                    sniEnabled = true;
                    break;
                case hzpublicaddress:
                    publicAddress = value;
                    break;
                case shutdowngrace:
                    System.setProperty(GlassFishImpl.PAYARA_SHUTDOWNGRACE_PROPERTY, value);
                    break;
                case hzinitialjoinwait:
                    initialJoinWait = Integer.parseInt(value);
                    break;
                case contextroot:
                    if (contextRoot != null) {
                        LOGGER.warning("Multiple --contextroot arguments only the last one will apply");
                    }
                    contextRoot = value;
                    if (isRoot(contextRoot)) {
                        contextRoot = "/";
                    }
                    break;
                case globalcontextroot:
                    if (globalContextRoot != null) {
                        LOGGER.warning("Multiple --globalContextRoot arguments only the last one will apply");
                    }
                    globalContextRoot = value;
                    break;
                case accessloginterval:
                    accessLogInterval = Integer.parseInt(value);
                    break;
                case accesslogsuffix:
                    accessLogSuffix = value;
                    break;
                case accesslogprefix:
                    accessLogPrefix = value;
                    break;
                default:
                    break;
            }
        }
    }

    private boolean isRoot(String context) {
        return "ROOT".equals(context);
    }

    private void configureRequestTracingService() {
        if (enableRequestTracing) {

            preBootCommands.add(new BootCommand("set",
                    "configs.config.server-config.request-tracing-service-configuration.enabled=true"));

            preBootCommands.add(new BootCommand("set",
                    "configs.config.server-config.request-tracing-service-configuration.threshold-unit="
                            + requestTracingThresholdUnit));

            preBootCommands.add(new BootCommand("set",
                    "configs.config.server-config.request-tracing-service-configuration.threshold-value" + "="
                            + Long.toString(requestTracingThresholdValue)));

            if (enableRequestTracingAdaptiveSampling) {
                preBootCommands.add(new BootCommand("set",
                        "configs.config.server-config.request-tracing-service-configuration.adaptive-sampling-enabled="
                                + Boolean.toString(enableRequestTracingAdaptiveSampling)));

                preBootCommands.add(new BootCommand("set",
                        "configs.config.server-config.request-tracing-service-configuration.adaptive-sampling-target-count="
                                + Integer.toString(requestTracingAdaptiveSamplingTargetCount)));

                preBootCommands.add(new BootCommand("set",
                        "configs.config.server-config.request-tracing-service-configuration.adaptive-sampling-time-value="
                                + Integer.toString(requestTracingAdaptiveSamplingTimeValue)));

                preBootCommands.add(new BootCommand("set",
                        "configs.config.server-config.request-tracing-service-configuration.adaptive-sampling-time-unit="
                                + requestTracingAdaptiveSamplingTimeUnit));
            }
        }
    }

    /**
     * Process the user system properties in precedence 1st loads the properties
     * from the uber jar location then loads each command line system properties
     * file which will override uber jar properties
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
                    userSystemProperties.setProperty((String) entry.getKey(), (String) entry.getValue());
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

    private void processDeploymentOptions() throws GlassFishException {
        if (deploymentOptions == null) {
            return;
        }

        if (deployments == null) {
            deployments = new LinkedHashMap<>();
        }

        boolean contextRootAvailable = contextRoot != null;
        for (Map.Entry<RUNTIME_OPTION, String> deploymentOption : deploymentOptions) {
            RUNTIME_OPTION option = deploymentOption.getKey();
            String value = deploymentOption.getValue();
            if (option == RUNTIME_OPTION.deploy) {
                String fileName = value;

                String deploymentContext = null;
                if (fileName.contains(File.pathSeparator)) {
                    fileName = fileName.substring(0, fileName.indexOf(File.pathSeparator));
                    deploymentContext = value.substring(value.indexOf(File.pathSeparator) + 1);
                }

                File deployment = new File(fileName);

                deployments.put(deployment.getName(), deployment.toURI());

                if (JavaArchiveUtils.hasContextRoot(deployment)) {
                    if (deploymentContext != null) {
                        addDeploymentContext(deployment.getName(), deploymentContext);
                    } else {
                        contextRootAvailable = false;
                    }
                }
            } else if (option == RUNTIME_OPTION.deploydir || option == RUNTIME_OPTION.deploymentdir) {
                // Get all files in the directory, and sort them by file type
                File[] deploymentEntries = deploymentRoot.listFiles(file -> JavaArchiveUtils.hasJavaArchiveExtension(file.getName(), false));
                Arrays.sort(deploymentEntries, new DeploymentComparator());

                for (File deploymentEntry : deploymentEntries) {
                    if (deploymentEntry.isFile() && deploymentEntry.canRead()) {
                        deployments.put(deploymentEntry.getName(), deploymentEntry.toURI());
                        if (JavaArchiveUtils.hasContextRoot(deploymentEntry)) {
                            contextRootAvailable = false;
                        }
                    }
                }
            } else if (option == RUNTIME_OPTION.deployfromgav) {
                Map.Entry<String, URI> gavEntry = getGAVURI(value);
                URI artifactURI = gavEntry.getValue();

                String artifactName = new File(artifactURI.getPath()).getName();

                deployments.put(artifactName, artifactURI);

                if (JavaArchiveUtils.hasWebArchiveExtension(artifactName)) {
                    String deploymentContext = gavEntry.getKey();
                    if (contextRootAvailable) {
                        deploymentContext = contextRoot;
                        contextRoot = null; // use only once
                        contextRootAvailable = false;
                    }
                    addDeploymentContext(artifactName, deploymentContext);
                }
            }
        }
    }

    private void addDeploymentContext(String fileName, String deploymentContext) {
        if (contextRoots == null) {
            contextRoots = new Properties();
        }
        if (isRoot(deploymentContext)) {
            deploymentContext = "/";
        }
        contextRoots.put(fileName, deploymentContext);
    }

    private void deployAll() throws GlassFishException {
        // Deploy from within the jar first.
        int deploymentCount = 0;
        Deployer deployer = gf.getDeployer();

        // load context roots from uber jar
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(CONTEXT_PROPS_FILE)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);

                for (Map.Entry<?, ?> entry : props.entrySet()) {
                    if (contextRoots == null) {
                        contextRoots = new Properties();
                    }
                    contextRoots.setProperty((String) entry.getKey(), (String) entry.getValue());
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }

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
                    File deployment = new File(entry);
                    String deploymentName = JavaArchiveUtils.removeJavaArchiveExtension(deployment.getName(), false);

                    List<String> deploymentParams = new ArrayList<>();
                    deploymentParams.add("--availabilityenabled=true");
                    deploymentParams.add("--force=true");
                    deploymentParams.add("--loadOnly=true");
                    deploymentParams.add("--name=" + deploymentName);
                    if (hotDeploy) {
                        deploymentParams.add("--hotDeploy=true");
                    }
                    if (JavaArchiveUtils.hasWebArchiveExtension(deployment.getName())) {
                        String deploymentContext;
                        if (isRoot(deploymentName)) {
                            deploymentContext = "/";
                        } else if (contextRoots != null && contextRoots.containsKey(deployment.getName())) {
                            deploymentContext = contextRoots.getProperty(deployment.getName());
                        } else {
                            deploymentContext = deploymentName;
                        }
                        if (isRoot(deploymentContext)) {
                            deploymentContext = "/";
                        }
                        deploymentParams.add("--contextroot=" + deploymentContext);
                    }
                    addGlobalContextRootProperty(deploymentParams);
                    deployer.deploy(this.getClass().getClassLoader().getResourceAsStream(entry), deploymentParams.toArray(new String[0]));

                    deploymentCount++;
                }
            } catch (IOException ioe) {
                LOGGER.log(Level.WARNING, "Could not deploy jar entry {0}", entryName);
            }
        } else {
            LOGGER.info("No META-INF/deploy directory");
        }

        processDeploymentOptions();

        if (deployments != null) {
            for (Map.Entry<String, URI> deploymentEntry : deployments.entrySet()) {
                String fileName = deploymentEntry.getKey();
                URI deploymentURI = deploymentEntry.getValue();

                List<String> deploymentParams = new ArrayList<>();
                deploymentParams.add("--availabilityenabled=true");
                deploymentParams.add("--force=true");
                deploymentParams.add("--loadOnly=true");
                if (hotDeploy) {
                    deploymentParams.add("--hotDeploy=true");
                }
                String deploymentContext = null;
                if (URIUtils.hasFileScheme(deploymentURI)) {
                    File deployment = new File(deploymentURI);
                    if (JavaArchiveUtils.hasContextRoot(deployment)) {
                        String deploymentName = deployment.isFile() ? JavaArchiveUtils.removeJavaArchiveExtension(fileName, false) : fileName;
                        if (isRoot(deploymentName)) {
                            deploymentContext = "/";
                        } else if (contextRoots != null && contextRoots.containsKey(fileName)) {
                            deploymentContext = contextRoots.getProperty(fileName);
                        } else if (contextRoot != null) {
                            deploymentContext = contextRoot;
                            contextRoot = null; // use only once
                        } else if (deployment.isDirectory()) {
                            deploymentContext = fileName;
                        }
                    }
                } else {
                    deploymentParams.add("--name=" + JavaArchiveUtils.removeJavaArchiveExtension(fileName, false));
                    if (JavaArchiveUtils.hasWebArchiveExtension(fileName)) {
                        if (contextRoot != null) {
                            deploymentContext = contextRoot;
                            contextRoot = null;
                        } else {
                            deploymentContext = contextRoots.getProperty(fileName);
                        }
                    }
                }

                if (deploymentContext != null) {
                    if (isRoot(deploymentContext)) {
                        deploymentContext = "/";
                    }
                    deploymentParams.add("--contextroot=" + deploymentContext);
                }
                addGlobalContextRootProperty(deploymentParams);
                deployer.deploy(deploymentURI, deploymentParams.toArray(new String[0]));

                deploymentCount++;
            }
        }
        LOGGER.log(Level.INFO, "Deployed {0} archive(s)", deploymentCount);
    }

    private void addGlobalContextRootProperty(List<String> deploymentParams) {
        if (globalContextRoot != null && !globalContextRoot.isBlank()) {
            deploymentParams.add("--properties=" + "globalContextRoot="+globalContextRoot);
        }
    }

    private ConsoleHandler configureLogger(Logger logger) {
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        logger.addHandler(consoleHandler);
        logger.setLevel(Level.ALL);
        return consoleHandler;
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(
                "Payara Micro Shutdown Hook") {
            @Override
            public void run() {
                try {
                    ConsoleHandler handler = configureLogger(LOGGER);
                    if (gf != null) {
                        gf.stop();
                        gf.dispose();
                        LOGGER.log(Level.INFO, "Payara Micro STOPPED.");
                        handler.flush();
                    }
                } catch (GlassFishException ex) {
                } catch (IllegalStateException ex) {
                    // Just log at a fine level and move on
                    LOGGER.log(Level.FINE, "Already shut down");
                }
            }
        });
    }

    /**
     * Reset the logging properties from the given file.
     * @param loggingProperty the location of the file to read from.
     */
    private void resetLogging(String loggingProperty) {
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
            try (InputStream is = new FileInputStream(runtimeDir.getLoggingProperties())) {
                LogManager.getLogManager().readConfiguration(replaceEnvProperties(is));

                // go through all root handlers and set formatters based on properties
                Logger rootLogger = LogManager.getLogManager().getLogger("");

                for (Handler handler : rootLogger.getHandlers()) {
                    String formatter = LogManager.getLogManager().getProperty(handler.getClass().getCanonicalName() + ".formatter");
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
                    new SortedLoggingProperties(currentProps).store(os, "Generated Logging properties file from Payara Micro log to file option");
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Unable to load the logging properties from the runtime directory", ex);
                }
            }
            System.setProperty("java.util.logging.config.file", runtimeDir.getLoggingProperties().getAbsolutePath());
            try (InputStream is = new FileInputStream(runtimeDir.getLoggingProperties())) {
                LogManager.getLogManager().readConfiguration(replaceEnvProperties(is));

                // reset the formatters on the two handlers
                //Logger rootLogger = Logger.getLogger("");
                String formatter = LogManager.getLogManager().getProperty("java.util.logging.ConsoleHandler.formatter");
                Formatter formatterClass = new ODLLogFormatter(null);
                try {
                    formatterClass = (Formatter) Class.forName(formatter).newInstance();
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                    LOGGER.log(Level.SEVERE, "Specified Formatter class could not be loaded " + formatter, ex);
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

    /**
     * Method to replace Env properties with corresponding value from System properties
     * @param is InputStream from File with properties
     * @return an instance of ByteArrayInputStream with the preprocessed properties
     * @throws IOException
     */
    private ByteArrayInputStream replaceEnvProperties(InputStream is) throws IOException {
        //preprocessing the properties read from the custom properties file
        Properties configuration = new Properties();
        configuration.load(is);
        //set the System.getenv() to be used for the replacement process. The desired property to be mapped
        //should need to be available on the System.getenv() Map
        configuration = new PropertyPlaceholderHelper(System.getenv(),
                PropertyPlaceholderHelper.ENV_REGEX).replacePropertiesPlaceholder(configuration);
        StringWriter writer = new StringWriter();
        configuration.store(new PrintWriter(writer), null);
        //here is added the new inputStream with the preprocessed properties solving the replacement issues
        return new ByteArrayInputStream(writer.getBuffer().toString().getBytes());
    }

    /**
     * Helper method to parse the pre-boot command file.
     *
     * @throws IOException
     */
    private void parsePreBootCommandFiles() throws IOException {
        parseCommandFiles("MICRO-INF/pre-boot-commands.txt", preBootFileName, preBootCommands, false);
    }

    /**
     * Helper method to parse the post-boot command file.
     *
     * @throws IOException
     */
    private void parsePostBootCommandFiles() throws IOException {
        parseCommandFiles("MICRO-INF/post-boot-commands.txt", postBootFileName, postBootCommands, true);
    }

    /**
     * Helper method to parse the post-deploy command file.
     *
     * @throws IOException
     */
    private void parsePostDeployCommandFiles() throws IOException {
        parseCommandFiles("MICRO-INF/post-deploy-commands.txt", postDeployFileName, postDeployCommands, true);
    }

    /**
     * Parse the pre-boot, post-boot, or post-deploy command files - both the embedded one in MICRO-INF and the file
     * provided by the user.
     *
     * @param resourcePath The path of the embedded pre-boot, post-boot, or post-deploy command file.
     * @param fileName The path of the pre-boot, post-boot, or post-deploy command file provided by the user.
     * @param bootCommands The {@link BootCommands} object to add the parsed commands to.
     * @param expandValues Whether variable expansion should be attempted - cannot be done during pre-boot.
     * @throws IOException
     */
    private void parseCommandFiles(String resourcePath, String fileName, BootCommands bootCommands,
            boolean expandValues) throws IOException {
        // Load the embedded file
        URL scriptURL = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        if (scriptURL != null) {
            bootCommands.parseCommandScript(scriptURL, expandValues);
        }

        if (fileName != null) {
            bootCommands.parseCommandScript(new File(fileName), expandValues);
        }
    }

    private void configureAccessLogging() {
        if (enableAccessLog) {
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.http-service.access-logging-enabled=true"));
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.http-service.virtual-server.server.access-log=" + userAccessLogDirectory));
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.http-service.virtual-server.server.access-logging-enabled=true"));
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.http-service.access-log.write-interval-seconds=" + accessLogInterval));
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.http-service.access-log.rotation-suffix=" + accessLogSuffix));
            if(accessLogPrefix != null && !accessLogPrefix.trim().isEmpty()) {
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.http-service.virtual-server.server.property.accessLogPrefix=" + accessLogPrefix));
            }
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

        if (sniEnabled) {
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.network-config.protocols.protocol.https-listener.ssl.sni-enabled=true"));
        }
        if (sslCert != null) {
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.network-config.protocols.protocol.https-listener.ssl.cert-nickname=" + sslCert));
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
        // set join wait based on the command flag
        if (System.getProperty("hazelcast.wait.seconds.before.join") == null) {
            System.setProperty("hazelcast.wait.seconds.before.join", Integer.toString(initialJoinWait));
        }

        if (noCluster) {
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.hazelcast-config-specific-configuration.clustering-enabled=false"));
        } else if (noHazelcast) {
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.hazelcast-config-specific-configuration.enabled=false"));
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.ejb-container.ejb-timer-service.ejb-timer-service=Dummy"));
        } else {
            if (hzPort > Integer.MIN_VALUE) {
                preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.multicast-port=" + hzPort));
            }

            if (hzStartPort > Integer.MIN_VALUE) {
                preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.start-port=" + hzStartPort));
            }

            if (hzMulticastGroup != null) {
                preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.multicast-group=" + hzMulticastGroup));
            }

            if (alternateHZConfigFile != null) {
                preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.hazelcast-configuration-file=" + alternateHZConfigFile.getPath()));
            }
            preBootCommands.add(new BootCommand("set", "configs.config.server-config.hazelcast-config-specific-configuration.lite=" + liteMember));

            if (hzClusterName != null) {
                preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.cluster-group-name=" + hzClusterName));
            }

            if (instanceName != null) {
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.hazelcast-config-specific-configuration.member-name=" + instanceName));
                preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.generate-names=false"));
            }

            if (instanceGroup != null) {
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.hazelcast-config-specific-configuration.member-group=" + instanceGroup));
            }

            if (publicAddress != null && !publicAddress.isEmpty()) {
                preBootCommands.add(new BootCommand("set", "configs.config.server-config.hazelcast-config-specific-configuration.public-address=" + publicAddress));
            }
            preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.host-aware-partitioning=" + hostAware));

            if (clustermode != null) {
                if (clustermode.startsWith("tcpip:")) {
                    String tcpipmembers = clustermode.substring(6);
                    preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.tcpip-members=" + tcpipmembers));
                    preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.discovery-mode=tcpip"));
                } else if (clustermode.startsWith("multicast:")) {
                    String hostPort[] = clustermode.split(":");
                    if (hostPort.length == 3) {
                        preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.multicast-group=" + hostPort[1]));
                        preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.multicast-port=" + hostPort[2]));
                        preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.discovery-mode=multicast"));
                    }
                } else if (clustermode.startsWith("domain:")) {
                    String hostPort[] = clustermode.split(":");
                    if (hostPort.length == 3) {
                        preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.das-public-address=" + hostPort[1]));
                        preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.das-port=" + hostPort[2]));
                        preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.discovery-mode=domain"));
                    }
                } else if (clustermode.startsWith("kubernetes")) {
                    preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.discovery-mode=kubernetes"));
                    if (clustermode.length() > 11) {
                        String[] kubernetesInfo = clustermode.substring(11).split(",");
                        if (kubernetesInfo.length == 2) {
                            preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.kubernetes-namespace=" + kubernetesInfo[0]));
                            preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.kubernetes-service-name=" + kubernetesInfo[1]));
                        }
                    }
                } else if (clustermode.startsWith("dns:")) {
                    String dnsmembers = clustermode.substring(4);
                    preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.dns-members=" + dnsmembers));
                    preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.discovery-mode=dns"));
                }
            } else {
                preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.discovery-mode=multicast"));
            }

            if (interfaces != null) {
                preBootCommands.add(new BootCommand("set", "hazelcast-runtime-configuration.interface=" + interfaces));
            }
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

    private Map.Entry<String, URI> getGAVURI(String gav) throws GlassFishException {
        try {
            Map.Entry<String, URI> artefactMapEntry = GAVConvertor.getArtefactMapEntry(gav, repositoryURIs);
            return new AbstractMap.SimpleImmutableEntry<>(artefactMapEntry.getKey(), artefactMapEntry.getValue());
        } catch (URISyntaxException ex) {
            throw new GlassFishException(ex.getMessage());
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

        String userLogPropertiesFileStr = getProperty("payaramicro.logPropertiesFile");
        if (userLogPropertiesFileStr  != null && !userLogPropertiesFileStr.trim().isEmpty()) {
             setLogPropertiesFile(new File(userLogPropertiesFileStr));
        }

        autoBindHttp = getBooleanProperty("payaramicro.autoBindHttp");
        autoBindRange = getIntegerProperty("payaramicro.autoBindRange", 5);
        initialJoinWait = getIntegerProperty("payaramicro.initialJoinWait", 1);
        autoBindSsl = getBooleanProperty("payaramicro.autoBindSsl");
        generateLogo = getBooleanProperty("payaramicro.logo");
        logToFile = getBooleanProperty("payaramicro.logToFile");
        userLogFile = getProperty("payaramicro.userLogFile");
        enableAccessLog = getBooleanProperty("payaramicro.enableAccessLog");
        enableAccessLogFormat = getBooleanProperty("payaramicro.enableAccessLogFormat");
        enableDynamicLogging = getBooleanProperty("payaramicro.enableDynamicLogging");
        enableHealthCheck = getBooleanProperty("payaramicro.enableHealthCheck");
        httpPort = getIntegerProperty("payaramicro.port", Integer.MIN_VALUE);
        sslPort = getIntegerProperty("payaramicro.sslPort", Integer.MIN_VALUE);
        sslCert = getProperty("payaramicro.sslCert");
        sniEnabled = getBooleanProperty("payaramicro.sniEnabled");
        hzMulticastGroup = getProperty("payaramicro.mcAddress");
        hzPort = getIntegerProperty("payaramicro.mcPort", Integer.MIN_VALUE);
        hostAware = getBooleanProperty("payaramicro.hostAware", "true");
        hzStartPort = getIntegerProperty("payaramicro.startPort", Integer.MIN_VALUE);
        hzClusterName = getProperty("payaramicro.clusterName");
        liteMember = getBooleanProperty("payaramicro.lite");
        maxHttpThreads = getIntegerProperty("payaramicro.maxHttpThreads", Integer.MIN_VALUE);
        minHttpThreads = getIntegerProperty("payaramicro.minHttpThreads", Integer.MIN_VALUE);
        noCluster = getBooleanProperty("payaramicro.noCluster");
        noHazelcast = getBooleanProperty("payaramicro.noHazelcast");
        disablePhoneHome = getBooleanProperty("payaramicro.disablePhoneHome");
        enableRequestTracing = getBooleanProperty("payaramicro.enableRequestTracing");
        requestTracingThresholdUnit = getProperty("payaramicro.requestTracingThresholdUnit", "SECONDS");
        requestTracingThresholdValue = getLongProperty("payaramicro.requestTracingThresholdValue", 30L);
        enableRequestTracingAdaptiveSampling = getBooleanProperty("payaramicro.enableRequestTracingAdaptiveSampling");
        requestTracingAdaptiveSamplingTargetCount = getIntegerProperty("payaramicro.requestTracingAdaptiveSamplingTargetCount", requestTracingAdaptiveSamplingTargetCount);
        requestTracingAdaptiveSamplingTimeValue = getIntegerProperty("payaramicro.requestTracingAdaptiveSamplingTimeValue", requestTracingAdaptiveSamplingTimeValue);
        requestTracingAdaptiveSamplingTimeUnit = getProperty("payaramicro.requestTracingAdaptiveSamplingTimeUnit", requestTracingAdaptiveSamplingTimeUnit).toUpperCase();
        clustermode = getProperty("payaramicro.clusterMode");
        interfaces = getProperty("payaramicro.interfaces");
        secretsDir = getProperty("payaramicro.secretsDir");
        showServletMappings = getBooleanProperty("payaramicro.showServletMappings", "false");
        publicAddress = getProperty("payaramicro.publicAddress");
        contextRoot = getProperty("payaramicro.contextRoot");
        globalContextRoot = getProperty("payaramicro.globalContextRoot");

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

    private void packageUberJar() throws GlassFishException {
        processDeploymentOptions();

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
            for (Map.Entry<String, URI> deployment : deployments.entrySet()) {
                creator.addDeployment(deployment.getKey(), deployment.getValue());
            }
        }

        if (libraries != null) {
            for (File lib : libraries) {
                creator.addLibraryJar(lib);
            }
        }

        if (copyDirectory != null) {
            creator.setDirectoryToCopy(copyDirectory);
        }

        if (contextRoots != null) {
            creator.setContextRoots(contextRoots);
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

        if (clustermode != null) {
            props.setProperty("payaramicro.clusterMode", clustermode);
        }

        if (interfaces != null) {
            props.setProperty("payaramicro.interfaces", interfaces);
        }

        if (secretsDir != null) {
            props.setProperty("payaramicro.secretsDir", secretsDir);
        }

        if (sslCert != null) {
            props.setProperty("payaramicro.sslCert", sslCert);
        }

        if (contextRoot != null) {
            props.setProperty("payaramicro.contextRoot", contextRoot);
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
        props.setProperty("payaramicro.enableDynamicLogging", Boolean.toString(enableDynamicLogging));
        props.setProperty("payaramicro.noCluster", Boolean.toString(noCluster));
        props.setProperty("payaramicro.noHazelcast", Boolean.toString(noHazelcast));
        props.setProperty("payaramicro.hostAware", Boolean.toString(hostAware));
        props.setProperty("payaramicro.disablePhoneHome", Boolean.toString(disablePhoneHome));
        props.setProperty("payaramicro.showServletMappings", Boolean.toString(showServletMappings));
        props.setProperty("payaramicro.sniEnabled", Boolean.toString(sniEnabled));
        props.setProperty("payaramicro.initialJoinWait", Integer.toString(initialJoinWait));

        if (publicAddress != null) {
            props.setProperty("payaramicro.publicAddress", publicAddress);
        }

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

        if (enableRequestTracingAdaptiveSampling) {
            props.setProperty("payaramicro.enableRequestTracingAdaptiveSampling", Boolean.toString(enableRequestTracingAdaptiveSampling));
        }

        if (requestTracingAdaptiveSamplingTargetCount != 12) {
            props.setProperty("payaramicro.requestTracingAdaptiveSamplingTargetCount", Integer.toString(requestTracingAdaptiveSamplingTargetCount));
        }

        if (requestTracingAdaptiveSamplingTimeValue != 1) {
            props.setProperty("payaramicro.requestTracingAdaptiveSamplingTimeValue", Integer.toString(requestTracingAdaptiveSamplingTimeValue));
        }

        if (!requestTracingAdaptiveSamplingTimeUnit.equals("MINUTES")) {
            props.setProperty("payaramicro.requestTracingAdaptiveSamplingTimeUnit", requestTracingAdaptiveSamplingTimeUnit);
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


    private void createLauncher() throws BootstrapException {
        try {
            if (rootDir == null) {
                LOGGER.severe("--rootdir is required for creating a launcher");
                System.exit(-1);
            }
            unPackRuntime();
            addLibraries();
            LauncherCreator creator = new LauncherCreator(rootDir, ((URLClassLoader)getClass().getClassLoader()));
            creator.buildLauncher();
        } catch (RuntimeException | URISyntaxException | IOException e) {
            throw new BootstrapException("Unable to create launcher", e);
        }
    }

    private static String unifyTimeUnit(String option) {
        switch (option.toLowerCase()) {
            case "nanosecond":
            case "ns":
                return "NANOSECONDS";
            case "microsecond":
            case "us":
            case "µs":
                return "MICROSECONDS";
            case "millisecond":
            case "ms":
                return "MILLISECONDS";
            case "second":
            case "s":
                return "SECONDS";
            case "m":
            case "minute":
            case "min":
            case "mins":
                return "MINUTES";
            case "hour":
            case "h":
                return "HOURS";
            case "day":
            case "d":
                return "DAYS";
            default:
                return option;
        }
    }

    private static TimeUnit parseTimeUnit(String value, String errorText) {
        return parseArgument(value, errorText, val -> TimeUnit.valueOf(unifyTimeUnit(val).toUpperCase()));
    }

    private static <T> T parseArgument(String value, String errorText, Function<String, T> parser) {
        try {
            return parser.apply(value);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "{0} is not a valid " + errorText, value);
            throw e;
        }
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

        // Print instance descriptor
        InstanceDescriptor id = getRuntime().getLocalDescriptor();
        LOGGER.log(Level.INFO, id.toJsonString(showServletMappings));

        // Get Payara Micro endpoints
        StringBuilder sb = new StringBuilder();
        sb.append("\nPayara Micro URLs:\n");
        List<URL> urls = id.getApplicationURLS();
        for (URL url : urls) {
            sb.append(url.toString()).append('\n');
        }

        // Count through applications and add their REST endpoints
        try {
            ListRestEndpointsCommand cmd = gf.getService(ListRestEndpointsCommand.class);
            id.getDeployedApplications().forEach(app -> {
                Map<String, Set<String>> endpoints = null;
                try {
                    endpoints = cmd.getEndpointMap(app.getName());
                } catch (IllegalArgumentException ex) {
                    // The application has no endpoints
                    endpoints = null;
                }
                if (endpoints != null) {
                    sb.append("\n'").append(app.getName()).append("' REST Endpoints:\n");
                    endpoints.forEach((path, methods) -> {
                        methods.forEach(method -> {
                            sb.append(method).append("\t").append(path).append("\n");
                        });
                    });
                }
            });
        } catch (GlassFishException ex) {
            // Really shouldn't happen, the command catches it's own errors most of the time
            LOGGER.log(Level.SEVERE, "Failed to get REST endpoints for application", ex);
        }
        sb.append("\n");

        // Print out all endpoints
        LOGGER.log(Level.INFO, sb.toString());

        // Print the logo if it's enabled
        if (generateLogo) {
            generateLogo();
        }

        // Print final ready message
        LOGGER.log(Level.INFO, "{0} ready in {1} (ms)", new Object[]{Version.getFullVersion(), bootTime});
    }

    private static String getProperty(String value) {
        String result;
        result = System.getProperty(value);
        if (result == null) {
            result = System.getenv(value.replace('.', '_'));
        }
        return result;
    }

    private static String getProperty(String value, String defaultValue) {
        String result = getProperty(value);
        if (result == null) {
            result = defaultValue;
        }
        return result;
    }

    private static boolean getBooleanProperty(String value) {
        String property;
        property = System.getProperty(value);
        if (property == null) {
            property = System.getenv(value.replace('.', '_'));
        }
        return "true".equals(property);
    }

    private static boolean getBooleanProperty(String value, String defaultValue) {
        String property;
        property = System.getProperty(value);
        if (property == null) {
            property = System.getenv(value.replace('.', '_'));
            if (property == null) {
                property = defaultValue;
            }
        }
        return "true".equals(property);
    }

    private static int getIntegerProperty(String value, int defaultValue) {
        String property;
        property = System.getProperty(value);
        if (property == null) {
            property = System.getenv(value.replace('.', '_'));
        }
        if (property == null) {
            return defaultValue;
        }
        return Integer.decode(property);
    }

    private static long getLongProperty(String value, long defaultValue) {
        String property;
        property = System.getProperty(value);
        if (property == null) {
            property = System.getenv(value.replace('.', '_'));
        }
        if (property == null) {
            return defaultValue;
        }
        return Long.decode(property);
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
                LOGGER.log(Level.SEVERE, null, ex);
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
                LOGGER.log(Level.SEVERE, null, ex);
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

    @Override
    public PayaraMicroBoot setPreBootHandler(Consumer<AdminCommandRunner> handler) {
        checkNotRunning();
        preBootHandler = handler;
        return this;
    }

    @Override
    public PayaraMicroBoot setPostBootHandler(Consumer<AdminCommandRunner> handler) {
        checkNotRunning();
        postBootHandler = handler;
        return this;
    }
}
