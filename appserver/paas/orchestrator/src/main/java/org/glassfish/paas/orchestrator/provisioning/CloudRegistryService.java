/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.orchestrator.provisioning;

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Resources;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.provisioning.iaas.CloudProvisioner;
import org.glassfish.resources.config.JdbcResource;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.component.Singleton;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Jagadish Ramu
 */
@Scoped(Singleton.class)
@Service
public class CloudRegistryService implements PostConstruct {

    @Inject
    private Domain domain;

    @Inject
    private CommandRunner commandRunner;

    @Inject
    private Habitat habitat;

    @Inject
    private ProvisionerFactory provisionerFactory;

    public static final String CONNECTION_POOL_NAME = "cloud-registry-pool";
    public static final String RESOURCE_NAME = "jdbc/cloud-registry-resource";

    public static final String CLOUD_TABLE_NAME = "GF_CLOUD_INFO";
    public static final String CLOUD_DB_TABLE_NAME = "DB_CLOUD_INFO";
    public static final String CLOUD_LB_TABLE_NAME = "LB_CLOUD_INFO";

    public static final String CLOUD_COLUMN_CLOUD_NAME = "CLOUD_NAME";
    public static final String CLOUD_COLUMN_IP_ADDRESS = "IP_ADDRESS";
    public static final String CLOUD_COLUMN_INSTANCE_ID = "INSTANCE_ID";
    public static final String CLOUD_COLUMN_SERVER_TYPE = "SERVER_TYPE";
    public static final String CLOUD_COLUMN_STATE = "STATE";

    private DataSource ds;
    private Properties cloudConfig = null;

    private CloudProvisioner cloudProvisioner;
    private Map<String, ApplicationServerProvisioner> appserverProvisioners =
            new HashMap<String, ApplicationServerProvisioner>();
    private DatabaseProvisioner databaseProvisioner = null;
    private LBProvisioner lbProvisioner = null;

    public DataSource getDataSource() {
        return ds;
    }

    public void postConstruct() {

        Resources resources = domain.getResources();
        if (ConnectorsUtil.getResourceByName(resources, JdbcResource.class, RESOURCE_NAME) == null) {
            debug("initializing resources");
            initializeRegistry();
        }
        try {
            InitialContext ic = new InitialContext();
            ds = (DataSource) ic.lookup(RESOURCE_NAME);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    private void initializeRegistry() {
        createJdbcConnectionPool();
        createJdbcResource();
        Resources resources = domain.getResources();
        if (ConnectorsUtil.getResourceByName(resources, JdbcResource.class, RESOURCE_NAME) == null) {
            throw new RuntimeException("Unable to find resource by name : " + RESOURCE_NAME);
        } else {
            debug("resource " + RESOURCE_NAME + " found");
        }
        populateTables();
    }

    private void populateTables() {
        ServiceUtil serviceUtil = habitat.getComponent(ServiceUtil.class);
        serviceUtil.createTable(ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
        serviceUtil.createTable(ServiceUtil.SERVICE_TYPE.DATABASE);
        serviceUtil.createTable(ServiceUtil.SERVICE_TYPE.LOAD_BALANCER);
    }

    private void createJdbcConnectionPool() {
        org.glassfish.api.admin.ParameterMap params = new org.glassfish.api.admin.ParameterMap();
        params.add("DEFAULT", CONNECTION_POOL_NAME);
        params.add("datasourceClassname", "org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource");
        params.add("resType", "javax.sql.ConnectionPoolDataSource");
        params.add("property",
                "user=APP:password=APP:databaseName=${com.sun.aas.installRoot}/databases/cloud-registry-db:" +
                        "connectionAttributes=;create\\=true");

        ActionReport actionReport = habitat.getComponent(ActionReport.class);
        executeCommand("create-jdbc-connection-pool", params, actionReport);
    }

    private void createJdbcResource() {
        org.glassfish.api.admin.ParameterMap params = new org.glassfish.api.admin.ParameterMap();
        params.add("DEFAULT", RESOURCE_NAME);
        params.add("poolName", CONNECTION_POOL_NAME);

        ActionReport actionReport = habitat.getComponent(ActionReport.class);
        executeCommand("create-jdbc-resource", params, actionReport);
    }

    private void executeCommand(String commandName, org.glassfish.api.admin.ParameterMap params, ActionReport actionReport) {
        commandRunner.getCommandInvocation(commandName, actionReport).parameters(params).execute();
    }

    private void debug(String message) {
        System.out.println("[CloudRegistryService] " + message);
    }

    public Properties getProperties() {
        if (cloudConfig == null) {
            String installRoot = System.getProperty("com.sun.aas.installRoot");
            File propertiesFile = new File(installRoot + File.separator + "config" + File.separator + "cloud-config.properties");
            if (propertiesFile.exists()) {
                Properties properties = null;
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(propertiesFile);
                    properties = new Properties();
                    properties.load(fis);
                    cloudConfig = properties;
                } catch (IOException e) {
                    e.printStackTrace();
                }finally{
                    if(fis != null){
                        try {
                            fis.close();
                        } catch (IOException e) {}
                    }
                }
            }
        }
        //always provide the copy.
        if(cloudConfig != null){
            return (Properties) cloudConfig.clone();
        }else{
            throw new RuntimeException("Unable to find cloud-config.properties file in 'config' directory");
        }
    }

    public CloudProvisioner getCloudProvisioner() {
        if (cloudProvisioner == null) {
            cloudProvisioner = (CloudProvisioner) provisionerFactory.
                    getProvisioner(getProperties(), CloudProvisioner.class);
        }
        return cloudProvisioner;
    }

    public ApplicationServerProvisioner getAppServerProvisioner(String host) {
        if (appserverProvisioners.containsKey(host)) {
            return appserverProvisioners.get(host);
        } else {
            Properties properties = getProperties();
            properties.put("GF_HOST", host);
            ApplicationServerProvisioner provisioner = (ApplicationServerProvisioner)
                    provisionerFactory.getProvisioner(properties, ApplicationServerProvisioner.class);
            provisioner.initialize(properties);
            appserverProvisioners.put(host, provisioner);
            return provisioner;
        }
    }

    public DatabaseProvisioner getDatabaseProvisioner() {

        //TODO ideally, one instance is sufficient ?
        if (databaseProvisioner != null) {
            return databaseProvisioner;
        } else {
            Properties properties = getProperties();
            //properties.put("GF_HOST", host);
            DatabaseProvisioner provisioner = (DatabaseProvisioner)
                    provisionerFactory.getProvisioner(properties, DatabaseProvisioner.class);
            provisioner.initialize(properties);
            databaseProvisioner = provisioner;
            return provisioner;
        }
    }

    public DatabaseProvisioner getDatabaseProvisioner(String vendorName) {
        if (databaseProvisioner == null) {
            getDatabaseProvisioner();
        }
        if (databaseProvisioner.getVendorName().equalsIgnoreCase(vendorName)) {
            return databaseProvisioner;
        } else {
            throw new RuntimeException("No database provisioner available for vendor [" + vendorName + "]");
        }
    }

    public LBProvisioner getLBProvisioner() {

        //TODO ideally, one instance is sufficient ?
        if (lbProvisioner != null) {
            return lbProvisioner;
        } else {
            Properties properties = getProperties();
            LBProvisioner provisioner = (LBProvisioner)
                    provisionerFactory.getProvisioner(properties, LBProvisioner.class);
            provisioner.initialize(properties);
            lbProvisioner = provisioner;
            return provisioner;
        }
    }

    public LBProvisioner getLBProvisioner(String vendorName) {
        if (lbProvisioner == null) {
            getLBProvisioner();
        }
        if (lbProvisioner.getVendorName().equalsIgnoreCase(vendorName)) {
            return lbProvisioner;
        } else {
            throw new RuntimeException("No load-balancer provisioner available for vendor [" + vendorName + "]");
        }
    }
}
