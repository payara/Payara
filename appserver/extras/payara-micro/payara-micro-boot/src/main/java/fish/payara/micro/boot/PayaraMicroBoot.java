/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.micro.boot;

import fish.payara.micro.BootstrapException;
import fish.payara.micro.PayaraMicroRuntime;
import java.io.File;

/**
 *
 * @author steve
 */
public interface PayaraMicroBoot {

    /**
     * Adds a Maven GAV coordinate to the list of archives to be deployed at
     * boot.
     *
     * @param GAV GAV coordinate
     * @return
     */
    PayaraMicroBoot addDeployFromGAV(String GAV);

    /**
     * Adds an archive to the list of archives to be deployed at boot. These
     * archives are not monitored for changes during running so are not
     * redeployed without restarting the server
     *
     * @param pathToWar File path to the deployment archive
     * @return
     */
    PayaraMicroBoot addDeployment(String pathToWar);

    /**
     * Adds an archive to the list of archives to be deployed at boot. These
     * archives are not monitored for changes during running so are not
     * redeployed without restarting the server
     *
     * @param file File path to the deployment archive
     * @return
     */
    PayaraMicroBoot addDeploymentFile(File file);

    /**
     * Adds a Maven repository to the list of repositories to search for
     * artifacts in
     *
     * @param URLs URL to Maven repository
     * @return
     */
    PayaraMicroBoot addRepoUrl(String... URLs);

    /**
     * Boots the Payara Micro Server. All parameters are checked at this point
     *
     * @return An instance of PayaraMicroRuntime that can be used to access the
     * running server
     * @throws BootstrapException
     */
    PayaraMicroRuntime bootStrap() throws BootstrapException;

    /**
     * The path to an alternative domain.xml for PayaraMicro to use at boot
     *
     * @return The path to the domain.xml
     */
    File getAlternateDomainXML();

    /**
     * Gets the maximum number of ports to check if free for autobinding
     * purposes
     *
     * @return The number of ports to check if free
     */
    int getAutoBindRange();

    /**
     * Gets the cluster group
     *
     * @return The Multicast Group that will beused for the Hazelcast clustering
     */
    String getClusterMulticastGroup();

    /**
     * Gets the cluster multicast port used for cluster communications
     *
     * @return The configured cluster port
     */
    int getClusterPort();

    /**
     * Gets the instance listen port number used by clustering. This number will
     * be incremented automatically if the port is unavailable due to another
     * instance running on the same host,
     *
     * @return The start port number
     */
    int getClusterStartPort();

    /**
     * A directory which will be scanned for archives to deploy
     *
     * @return
     */
    File getDeploymentDir();

    /**
     * Indicates whether autobinding of the HTTP port is enabled
     *
     * @return
     */
    boolean getHttpAutoBind();

    /**
     * The configured port Payara Micro will use for HTTP requests.
     *
     * @return The HTTP port
     */
    int getHttpPort();

    /**
     * Gets the name of the Hazelcast cluster group. Clusters with different
     * names do not interact
     *
     * @return The current Cluster Name
     */
    String getHzClusterName();

    /**
     * Gets the password of the Hazelcast cluster group
     *
     * @return
     */
    String getHzClusterPassword();

    /**
     * Gets the logical name for this PayaraMicro Server within the server
     * cluster
     *
     * @return The configured instance name
     */
    String getInstanceName();

    /**
     * The maximum threads in the HTTP(S) threadpool processing HTTP(S)
     * requests. Setting this will determine how many concurrent HTTP requests
     * can be processed. The default value is 200. This value is shared by both
     * HTTP and HTTP(S) requests.
     *
     * @return
     */
    int getMaxHttpThreads();

    /**
     * The minimum number of threads in the HTTP(S) threadpool Default value is
     * 10
     *
     * @return The minimum threads to be created in the threadpool
     */
    int getMinHttpThreads();

    /**
     * The File path to a directory that PayaraMicro should use for storing its
     * configuration files
     *
     * @return
     */
    File getRootDir();

    /**
     * Get a handle on the running Payara instance to manipulate the server once
     * running
     *
     * @return
     * @throws IllegalStateException
     */
    PayaraMicroRuntime getRuntime() throws IllegalStateException;

    /**
     * Indicates whether autobinding of the HTTPS port is enabled
     *
     * @return
     */
    boolean getSslAutoBind();

    /**
     * The configured port for HTTPS requests
     *
     * @return The HTTPS port
     */
    int getSslPort();

    /**
     * The UberJar to create
     *
     * @return
     */
    File getUberJar();

    /**
     * Indicates whether this is a lite cluster member which means it stores no
     * cluster data although it participates fully in the cluster.
     *
     * @return
     */
    boolean isLite();

    /**
     * Indicated whether clustering is enabled
     *
     * @return
     */
    boolean isNoCluster();

    /**
     * Set user defined file directory for the access log
     *
     * @param filePath
     */
    void setAccessLogDir(String filePath);

    /**
     * Set user defined formatting for the access log
     *
     * @param format
     */
    void setAccessLogFormat(String format);

    /**
     * Sets the path to a domain.xml file PayaraMicro should use to boot. If
     * this is not set PayaraMicro will use an appropriate domain.xml from
     * within its jar file
     *
     * @param alternateDomainXML
     * @return
     */
    PayaraMicroBoot setAlternateDomainXML(File alternateDomainXML);

    /**
     * Sets an application specific domain.xml file that is embedded on the
     * classpath of your application.
     *
     * @param domainXml This is a resource string for your domain.xml
     * @return
     */
    PayaraMicroBoot setApplicationDomainXML(String domainXml);

    /**
     * Sets the maximum number of ports to check if free for autobinding
     * purposes
     *
     * @param autoBindRange The maximum number of ports to increment the port
     * value by
     * @return
     */
    PayaraMicroBoot setAutoBindRange(int autoBindRange);

    /**
     * Sets the cluster group used for Payara Micro clustering used for cluster
     * communications and discovery. Each Payara Micro cluster should have
     * different values for the MulticastGroup
     *
     * @param hzMulticastGroup String representation of the multicast group
     * @return
     */
    PayaraMicroBoot setClusterMulticastGroup(String hzMulticastGroup);

    /**
     * Sets the multicast group used for Payara Micro clustering used for
     * cluster communication and discovery. Each Payara Micro cluster should
     * have different values for the cluster port
     *
     * @param hzPort The port number
     * @return
     */
    PayaraMicroBoot setClusterPort(int hzPort);

    /**
     * Sets the start port number for the Payara Micro to listen on for cluster
     * communications.
     *
     * @param hzStartPort Start port number
     * @return
     */
    PayaraMicroBoot setClusterStartPort(int hzStartPort);

    /**
     * Sets a directory to scan for archives to deploy on boot. This directory
     * is not monitored while running for changes. Therefore archives in this
     * directory will NOT be redeployed during runtime.
     *
     * @param deploymentRoot File path to the directory
     * @return
     */
    PayaraMicroBoot setDeploymentDir(File deploymentRoot);

    /**
     * Enables or disables autobinding of the HTTP port
     *
     * @param httpAutoBind The true or false value to enable or disable HTTP
     * autobinding
     * @return
     */
    PayaraMicroBoot setHttpAutoBind(boolean httpAutoBind);

    /**
     * Sets the port used for HTTP requests
     *
     * @param httpPort The port number
     * @return
     */
    PayaraMicroBoot setHttpPort(int httpPort);

    /**
     * Sets the name of the Hazelcast cluster group
     *
     * @param hzClusterName The name of the hazelcast cluster
     * @return
     */
    PayaraMicroBoot setHzClusterName(String hzClusterName);

    /**
     * Sets the Hazelcast cluster group password. For two clusters to work
     * together then the group name and password must be the same
     *
     * @param hzClusterPassword The password to set
     * @return
     */
    PayaraMicroBoot setHzClusterPassword(String hzClusterPassword);

    /**
     * Sets the logical instance name for this PayaraMicro server within the
     * server cluster If this is not set a UUID is generated
     *
     * @param instanceName The logical server name
     * @return
     */
    PayaraMicroBoot setInstanceName(String instanceName);

    /**
     * Sets the lite status of this cluster member. If true the Payara Micro is
     * a lite cluster member which means it stores no cluster data.
     *
     * @param liteMember set to true to set as a lite cluster member with no
     * data storage
     * @return
     */
    PayaraMicroBoot setLite(boolean liteMember);

    /**
     * Set user defined properties file for logging
     *
     * @param fileName
     * @return
     */
    PayaraMicroBoot setLogPropertiesFile(File fileName);

    /**
     * Sets the path to the logo file printed at boot. This can be on the
     * classpath of the server or an absolute URL
     *
     * @param filePath
     * @return
     */
    PayaraMicroBoot setLogoFile(String filePath);

    /**
     * The maximum threads in the HTTP(S) threadpool processing HTTP(S)
     * requests. Setting this will determine how many concurrent HTTP requests
     * can be processed. The default value is 200
     *
     * @param maxHttpThreads Maximum threads in the HTTP(S) threadpool
     * @return
     */
    PayaraMicroBoot setMaxHttpThreads(int maxHttpThreads);

    /**
     * The minimum number of threads in the HTTP(S) threadpool Default value is
     * 10
     *
     * @param minHttpThreads
     * @return
     */
    PayaraMicroBoot setMinHttpThreads(int minHttpThreads);

    /**
     * Enables or disables clustering before bootstrap
     *
     * @param noCluster set to true to disable clustering
     * @return
     */
    PayaraMicroBoot setNoCluster(boolean noCluster);

    /**
     * Set whether the logo should be generated on boot
     *
     * @param generate
     * @return
     */
    PayaraMicroBoot setPrintLogo(boolean generate);

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
    PayaraMicroBoot setRootDir(File rootDir);

    /**
     * Enables or disables autobinding of the HTTPS port
     *
     * @param sslAutoBind The true or false value to enable or disable HTTPS
     * autobinding
     * @return
     */
    PayaraMicroBoot setSslAutoBind(boolean sslAutoBind);

    /**
     * Sets the configured port for HTTPS requests. If this is not set HTTPS is
     * disabled
     *
     * @param sslPort The HTTPS port
     * @return
     */
    PayaraMicroBoot setSslPort(int sslPort);

    /**
     * Set user defined file for the Log entries
     *
     * @param fileName
     * @return
     */
    PayaraMicroBoot setUserLogFile(String fileName);
    
    /**
     * Sets the instance group name
     *
     * @param instanceGroup The instance group name
     * @return
     */
    PayaraMicroBoot setInstanceGroup(String groupName);
    
    /**
     * Gets the name of the instance group
     *
     * @return The name of the instance group
     */
    String getInstanceGroup();

    /**
     * Stops and then shutsdown the Payara Micro Server
     *
     * @throws BootstrapException
     */
    void shutdown() throws BootstrapException;
    
}
