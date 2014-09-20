/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.connectors.jms.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.connectors.jms.config.JmsService;
import com.sun.enterprise.connectors.jms.inflow.MdbContainerProps;
import com.sun.enterprise.connectors.jms.system.MQAddressList;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.ConnectorConfigProperty;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.zip.ZipFile;
import com.sun.enterprise.util.zip.ZipFileException;
import org.glassfish.ejb.config.MdbContainer;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.RelativePathResolver;
import org.glassfish.internal.api.ServerContext;
import org.jvnet.hk2.config.types.Property;

/**
 *
 * @author
 */
public class JmsRaUtil {

    private final static String MQ_RAR = "imqjmsra.rar";

    private final String SYSTEM_APP_DIR =
        "lib" + File.separator + "install" + File.separator + "applications";

    private final String MQ_RAR_MANIFEST =
        ConnectorConstants.DEFAULT_JMS_ADAPTER + File.separator + "META-INF"
        + File.separator + "MANIFEST.MF";

    // Manifest version tag.
    private final static String MANIFEST_TAG = "Implementation-Version";

    private static final String propName_reconnect_delay_in_seconds =
        "reconnect-delay-in-seconds";
    private static final String propName_reconnect_max_retries =
        "reconnect-max-retries";
    private static final String propName_reconnect_enabled =
        "reconnect-enabled";
    private static final int DEFAULT_RECONNECT_DELAY = 60;
    private static final int DEFAULT_RECONNECT_RETRIES = 60;

    private static final String propName_cmt_max_runtime_exceptions
        = "cmt-max-runtime-exceptions";
    private static final int DEFAULT_CMT_MAX_RUNTIME_EXCEPTIONS = 1;

    private static final String ENABLE_AUTO_CLUSTERING = "com.sun.enterprise.connectors.system.enableAutoClustering";

    private int cmtMaxRuntimeExceptions = DEFAULT_CMT_MAX_RUNTIME_EXCEPTIONS;

    private int reconnectDelayInSeconds = DEFAULT_RECONNECT_DELAY;
    private int reconnectMaxRetries = DEFAULT_RECONNECT_RETRIES;
    private boolean reconnectEnabled = false;

    JmsService js = null;
    MQAddressList list = null;

    private static final Logger _mdblogger = Logger.getLogger("javax.enterprise.system.container.ejb.mdb");
    private static final Logger _rarlogger = Logger.getLogger("javax.enterprise.resource.resourceadapter");

    public JmsRaUtil() throws ConnectorRuntimeException {
        this(null);
    }

    public JmsRaUtil(JmsService js) throws ConnectorRuntimeException {
        try {
            if (js != null) {
            this.js = js;
            } else {
                  this.js = (JmsService) Globals.get(JmsService.class);
            }
            list = new MQAddressList(this.js);
//            if (isClustered() && ! this.js.getType().equals(
//                ActiveJmsResourceAdapter.REMOTE)) {
//                list.setupForLocalCluster();
//            } else {
//                list.setup();
//            }
        } catch(Exception ce) {
            throw handleException(ce);
        }
    }

    public void setupAddressList() throws ConnectorRuntimeException{
      try {
    list.setup();
    } catch (Exception e) {
        throw handleException(e);
    }
    }

    public String getUrl() {
    try {
            return list.toString();
    } catch (Exception e) {
        return null;
    }
    }

    public static boolean isClustered(List clusters, String instanceName) {
              return (enableClustering() && isServerClustered(clusters,
                instanceName));
     }
      /**
     * Return true if the given server instance is part of a cluster.
     */
    public static boolean isServerClustered(List clusters, String instanceName)
    {
        return (getClusterForServer(clusters, instanceName) != null);
    }
    public static Cluster getClusterForServer(List clusters, String instanceName){
        //Return the server only if it is part of a cluster (i.e. only if a cluster
        //has a reference to it).
        for (int i = 0; i < clusters.size(); i++) {
            final List servers = ((Cluster)clusters.get(i)).getInstances();
            for (int j = 0; j < servers.size(); j++) {
                if (((Server)servers.get(j)).getName().equals(instanceName)) {
                    // check to see if the server exists as a sanity check.
                    // NOTE: we are not checking for duplicate server instances here.
                    return (Cluster) clusters.get(i);
                }
            }
        }
        return null;
    }
     private static boolean enableClustering() {
     try {
        /* This flag disables the auto clustering functionality
           * No uMQ clusters will  be created with AS cluster if
           * this flag is set to false. Default is true.
           */
        String enablecluster = System.getProperty(ENABLE_AUTO_CLUSTERING);
        _rarlogger.log(Level.FINE,"Sun MQ Auto cluster system property" + enablecluster);
                  if ((enablecluster != null) &&
            (enablecluster.trim().equals("false"))){
        _rarlogger.log(Level.FINE,"Disabling Sun MQ Auto Clustering");
                    return false;
              }
     }catch (Exception e) {
        ;
     }
    _rarlogger.log(Level.FINE,"Enabling Sun MQ Auto Clustering");
    return true;
      }

    public String getJMSServiceType(){
     return this.js.getType();
    }

    public MQAddressList getUrlList() {
        return list;
    }

    public boolean getReconnectEnabled() {
        return Boolean.parseBoolean(js.getReconnectEnabled());
    }

    public String getReconnectInterval() {
        return js.getReconnectIntervalInSeconds();
    }

    public String getReconnectAttempts() {
        return js.getReconnectAttempts();
    }

    public String getAddressListIterations() {
        return js.getAddresslistIterations();
    }

    public String getAddressListBehaviour() {
        return js.getAddresslistBehavior();
    }

    public void setMdbContainerProperties(){
        MdbContainer mdbc = null;
        try {

            mdbc = Globals.get(MdbContainer.class);

        }
        catch (Exception e) {
            _mdblogger.log(Level.WARNING, "containers.mdb.config_exception",
                        new Object[]{e.getMessage()});
            if (_mdblogger.isLoggable(Level.FINE)) {
                _mdblogger.log(Level.FINE, e.getClass().getName(), e);
            }
        }

        if (mdbc != null) {
            List props = mdbc.getProperty();//        getElementProperty();
            if (props != null) {
                for (int i = 0; i < props.size(); i++) {
                    Property p = (Property) props.get(i);
                    if (p == null) continue;
                    String name = p.getName();
                    if (name == null) continue;
                    try {
                        if (name.equals(propName_reconnect_enabled)) {
                            if (p.getValue() == null) continue;
                            reconnectEnabled =
                                Boolean.valueOf(p.getValue()).booleanValue();
                        }
                        else if (name.equals
                                 (propName_reconnect_delay_in_seconds)) {
                            try {
                                reconnectDelayInSeconds =
                                    Integer.parseInt(p.getValue());
                            } catch (Exception e) {
                                _mdblogger.log(Level.WARNING,
                                    "containers.mdb.config_exception",
                                    new Object[]{e.getMessage()});
                            }
                        }
                        else if (name.equals(propName_reconnect_max_retries)) {
                            try {
                                reconnectMaxRetries =
                                    Integer.parseInt(p.getValue());
                            } catch (Exception e) {
                                _mdblogger.log(Level.WARNING,
                                    "containers.mdb.config_exception",
                                    new Object[]{e.getMessage()});
                            }
                        }
                        else if (name.equals
                                 (propName_cmt_max_runtime_exceptions)) {
                            try {
                                cmtMaxRuntimeExceptions =
                                    Integer.parseInt(p.getValue());
                            } catch (Exception e) {
                                _mdblogger.log(Level.WARNING,
                                    "containers.mdb.config_exception",
                                    new Object[]{e.getMessage()});
                            }
                        }
                    } catch (Exception e) {
                        _mdblogger.log(Level.WARNING,
                                    "containers.mdb.config_exception",
                                    new Object[]{e.getMessage()});
                        if (_mdblogger.isLoggable(Level.FINE)) {
                            _mdblogger.log(Level.FINE, e.getClass().getName(), e);
                        }
                    }
                }
            }
        }
        if (reconnectDelayInSeconds < 0) {
            reconnectDelayInSeconds = DEFAULT_RECONNECT_DELAY;
        }
        if (reconnectMaxRetries < 0) {
            reconnectMaxRetries = DEFAULT_RECONNECT_RETRIES;
        }
        if (_mdblogger.isLoggable(Level.FINE)) {
            _mdblogger.log(Level.FINE,
                propName_reconnect_delay_in_seconds+"="+
                reconnectDelayInSeconds +", "+
                propName_reconnect_max_retries+ "="+reconnectMaxRetries + ", "+
                propName_reconnect_enabled+"="+reconnectEnabled);
        }

        //Now set all these properties in the active resource adapter
        MdbContainerProps.setReconnectDelay(reconnectDelayInSeconds);
        MdbContainerProps.setReconnectMaxRetries(reconnectMaxRetries);
        MdbContainerProps.setReconnectEnabled(reconnectEnabled);
        MdbContainerProps.setMaxRuntimeExceptions(cmtMaxRuntimeExceptions);

    }

    public void configureDescriptor(ConnectorDescriptor cd) {

        Object[] envProps = cd.getConfigProperties().toArray();

        for (int i = 0; i < envProps.length; i++) {
            ConnectorConfigProperty  envProp = (ConnectorConfigProperty ) envProps[i];
            String name = envProp.getName();
        if (!name.equals("ConnectionURL")) {
            continue;
        }
            String userValue = getUrl();
            if (userValue != null) {
                cd.removeConfigProperty(envProp);
                cd.addConfigProperty(new ConnectorConfigProperty (
                              name, userValue, userValue, envProp.getType()));
            }

        }

    }

    /**
     * Obtains the Implementation-Version from the MQ Client libraries
     * that are deployed in the application server and in MQ installation
     * directory.
     */
    public void upgradeIfNecessary() {

        String installedMqVersion = null;
        String deployedMqVersion = null;

        try {
           installedMqVersion = getInstalledMqVersion();
           _rarlogger.log(Level.FINE,"installedMQVersion :: " + installedMqVersion);
           deployedMqVersion =  getDeployedMqVersion();
           _rarlogger.log(Level.FINE,"deployedMQVersion :: " + deployedMqVersion);
        }catch (Exception e) {
        return;
        }

        String deployed_dir =
           java.lang. System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY)
           + File.separator + SYSTEM_APP_DIR + File.separator
           + ConnectorConstants.DEFAULT_JMS_ADAPTER;

        // If the Manifest entry has different versions, then attempt to
        // explode the MQ resource adapter.
        if (!installedMqVersion.equals(deployedMqVersion)) {
           try {
               _rarlogger.log(Level.INFO, "jmsra.upgrade_started" );
           ZipFile rarFile = new ZipFile(System.getProperty
                                 (SystemPropertyConstants.IMQ_LIB_PROPERTY) +
                                 File.separator + MQ_RAR, deployed_dir);
               rarFile.explode();
               _rarlogger.log(Level.INFO, "jmsra.upgrade_completed");
       } catch(ZipFileException ze) {
               _rarlogger.log(Level.SEVERE,"jmsra.upgrade_failed",ze.getMessage());
           }
        }

    }

    private String getInstalledMqVersion() throws Exception {
       String ver = null;
       // Full path of installed Mq Client library
       String installed_dir =
           System.getProperty(SystemPropertyConstants.IMQ_LIB_PROPERTY);
       String jarFile = installed_dir + File.separator + MQ_RAR;
       _rarlogger.log(Level.FINE,"Installed MQ JAR " + jarFile);
    JarFile jFile = null;
       try {
       if ((new File(jarFile)).exists()) {
        /* This is for a file based install
           * RAR has to be present in this location
           * ASHOME/imq/lib
           */
        jFile = new JarFile(jarFile);
       } else {
        /* This is for a package based install
           * RAR has to be present in this location
           * /usr/lib
           */
        jFile = new JarFile(installed_dir + File.separator + ".." + File.separator + MQ_RAR);
       }
           Manifest mf = jFile.getManifest();
           ver = mf.getMainAttributes().getValue(MANIFEST_TAG);
           return ver;
       } catch (Exception e) {
           _rarlogger.log(Level.WARNING, "jmsra.upgrade_check_failed",
                       e.getMessage() + ":" + jarFile );
           throw e;
       } finally {
           if (jFile != null) {
               jFile.close();
           }
       }
    }

    private String getDeployedMqVersion() throws Exception {
       String ver = null;
        // Full path of Mq client library that is deployed in appserver.
       String deployed_dir =
           System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY)
           + File.separator + SYSTEM_APP_DIR;
       String manifestFile = deployed_dir + File.separator +
                             MQ_RAR_MANIFEST;
       _rarlogger.log(Level.FINE,"Deployed MQ version Manifest file" + manifestFile);
       try {
           Manifest mf = new Manifest(new FileInputStream(manifestFile));
           ver = mf.getMainAttributes().getValue(MANIFEST_TAG);
           return ver;
       } catch (Exception e) {
           _rarlogger.log(Level.WARNING, "jmsra.upgrade_check_failed",
                       e.getMessage() + ":" + manifestFile );
           throw e;
       }
   }

   private static ConnectorRuntimeException handleException(Exception e) {
        ConnectorRuntimeException cre =
             new ConnectorRuntimeException(e.getMessage());
        cre.initCause(e);
        return cre;
    }

    public static String getJMSPropertyValue(Server as){

        SystemProperty sp = as.getSystemProperty("JMS_PROVIDER_PORT");
        if (sp != null) return sp.getValue();

        Cluster cluster = as.getCluster();
        if (cluster != null) {
            sp = cluster.getSystemProperty("JMS_PROVIDER_PORT");
            if (sp != null) return sp.getValue();
        }

        Config config = as.getConfig();
        if (config != null)
            sp = config.getSystemProperty("JMS_PROVIDER_PORT");

        if (sp != null) return sp.getValue();

        return null;
    }

    public static String getUnAliasedPwd(String alias){
        try{
            String unalisedPwd = RelativePathResolver.getRealPasswordFromAlias(alias);
            if (unalisedPwd != null && "".equals(unalisedPwd))
               return unalisedPwd;

        }catch(Exception e){

        }
         return alias;
    }
}
