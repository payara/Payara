/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.persistence.jpa;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.PersistenceUnitsDescriptor;
import com.sun.enterprise.deployment.PersistenceUnitDescriptor;
import org.glassfish.api.deployment.DeploymentContext;

import org.glassfish.persistence.common.*;

import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.component.Habitat;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.logging.LogDomains;

import javax.naming.NamingException;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceProvider;

import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * For each persistence unit descriptors that is defined for 
 * an application create the ddl scripts. Additionally if the
 * user has requested the tables to be created or dropped from
 * the database complete that action too.
 *
 * These are the principles and expectations of the implementation.
 * We don't want TopLink code to execute the DDLs, it should only 
 * generate them. So, we always set the *generation-mode* to *script* 
 * in the PUInfo object before passing it to createContainerEMF(). 
 * As a result TopLink never creates the actual tables, nor does it drop 
 * them. The DDLs are executed by our code based on user preference which 
 * considers inputs from persistence.xml and CLI. We set the TopLink 
 * property to DROP_AND_CREATE in that map because we want it to always 
 * generate both create- and dropDDL.jdbc files.
 * @author pramodg
 */
public class JPAJava2DBProcessor {

    // Defining the persistence provider class names here that we would use to
    // check if java2db is supported.
    private static final String TOPLINK_PERSISTENCE_PROVIDER_CLASS_NAME_OLD =
        "oracle.toplink.essentials.ejb.cmp3.EntityManagerFactoryProvider"; // NOI18N
    private static final String TOPLINK_PERSISTENCE_PROVIDER_CLASS_NAME_NEW =
        "oracle.toplink.essentials.PersistenceProvider"; // NOI18N
    private static final String ECLIPSELINK_PERSISTENCE_PROVIDER_CLASS_NAME =
        "org.eclipse.persistence.jpa.PersistenceProvider"; // NOI18N

    // Constants for various property values.

    // Following constants are actually defined in
    // oracle.toplink.essentials.ejb.cmp3.EntityManagerFactoryProvider
    // and org.eclipse.persistence.jpa.config.PersistenceUnitProperties
    // This code assumes that the value of constant at both the place is same
    private static final String CREATE_ONLY             = "create-tables"; //NOI18N
    private static final String DROP_AND_CREATE         = "drop-and-create-tables"; //NOI18N
    private static final String NONE                    = "none"; //NOI18N
    private static final String DDL_BOTH_GENERATION     = "both"; //NOI18N
    private static final String DDL_DATABASE_GENERATION = "database"; //NOI18N
    private static final String DDL_SQL_SCRIPT_GENERATION = "sql-script"; //NOI18N

    // Constatns defined in oracle.toplink.essentials.config.TargetServer
    // and org.eclipse.persistence.jpa.config.TargetServer
    private static final String  SunAS9 = "SunAS9"; //NOI18N

    // property names for Toplink and EclipseLink
    private static final String TOPLINK_DDL_GENERATION     = "toplink.ddl-generation"; // NOI18N
    private static final String ECLIPSELINK_DDL_GENERATION = "eclipselink.ddl-generation"; // NOI18N

    private static final String TOPLINK_DDL_GENERATION_MODE     = "toplink.ddl-generation.output-mode"; // NOI18N
    private static final String ECLIPSELINK_DDL_GENERATION_MODE = "eclipselink.ddl-generation.output-mode"; // NOI18N

    private static final String TOPLINK_APP_LOCATION         = "toplink.application-location"; // NOI18N
    private static final String ECLIPSELINK_APP_LOCATION     = "eclipselink.application-location"; // NOI18N

    private static final String TOPLINK_CREATE_JDBC_DDL_FILE     = "toplink.create-ddl-jdbc-file-name"; // NOI18N
    private static final String ECLIPSELINK_CREATE_JDBC_DDL_FILE = "eclipselink.create-ddl-jdbc-file-name"; // NOI18N

    private static final String TOPLINK_DROP_JDBC_DDL_FILE       = "toplink.drop-ddl-jdbc-file-name"; // NOI18N
    private static final String ECLIPSELINK_DROP_JDBC_DDL_FILE   = "eclipselink.drop-ddl-jdbc-file-name"; // NOI18N

    private static Logger logger = LogDomains.getLogger(JPAJava2DBProcessor.class, LogDomains.PERSISTENCE_LOGGER);

    /**
     * Holds name of provider specific properties.
     */
    private ProviderPropertyNamesHolder providerPropertyNamesHolder;

    private String ddlMode;

    private Java2DBProcessorHelper helper = null;

    /**
     * Creates a new instance of JPAJava2DBProcessor using Java2DBProcessorHelper
     * @param helper the Java2DBProcessorHelper instance to be used
     * with this processor.
     */
    public JPAJava2DBProcessor(Java2DBProcessorHelper helper) {
        this.helper = helper;
        this.helper.init();
    }

    /**
     * This method does parses all relevant info from the PU
     * @param bundle the persistence unit descriptor that is being worked on.
     * @return true if this PU requires java2db processing
     */
    public boolean isJava2DbPU(PersistenceUnitDescriptor bundle) {
        String providerClassName = getProviderClassName(bundle);

        // Initialize with EL names for each bundle to handle apps that have
        // multiple pus
        providerPropertyNamesHolder = new ProviderPropertyNamesHolder();

        // Override with TLE names if running against TLE
        if (TOPLINK_PERSISTENCE_PROVIDER_CLASS_NAME_NEW.equals(providerClassName) ||
                TOPLINK_PERSISTENCE_PROVIDER_CLASS_NAME_OLD.equals(providerClassName)) {
            // for backward compatibility
            providerPropertyNamesHolder.appLocation       = TOPLINK_APP_LOCATION;
            providerPropertyNamesHolder.createJdbcDdlFile = TOPLINK_CREATE_JDBC_DDL_FILE;
            providerPropertyNamesHolder.dropJdbcDdlFile   = TOPLINK_DROP_JDBC_DDL_FILE;
            providerPropertyNamesHolder.ddlGeneration     = TOPLINK_DDL_GENERATION;
            providerPropertyNamesHolder.ddlGenerationMode = TOPLINK_DDL_GENERATION_MODE;
        }

        String ddlGenerate = getPersistencePropVal(bundle,
                providerPropertyNamesHolder.ddlGeneration, NONE);
        ddlMode = getPersistencePropVal(bundle,
                providerPropertyNamesHolder.ddlGenerationMode, DDL_BOTH_GENERATION);

        // If CLI options are not set, use value from the the ddl-generate property
        // if defined in persistence.xml
        boolean userCreateTables = (ddlGenerate.equals(CREATE_ONLY)
                || ddlGenerate.equals(DROP_AND_CREATE))
                && !ddlMode.equals(NONE);

        boolean createTables = helper.getCreateTables(userCreateTables);

        boolean userDropTables = ddlGenerate.equals(DROP_AND_CREATE)
                && (ddlMode.equals(DDL_DATABASE_GENERATION)
                        || ddlMode.equals(DDL_BOTH_GENERATION));

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Processing request with create tables: " + createTables //NOI18N
                    + ", drop tables: " + userDropTables); //NOI18N
        }

        if (!createTables && !userDropTables) {
            // Nothing to do.
            return false;
        }

        if (! isSupportedPersistenceProvider(bundle)) {
            // Persistence provider is not supported, hence exit from java2db code
            if (helper.hasDeployCliOverrides()) {
                helper.logI18NWarnMessage(
                    "Java2DBProcessorHelper.nondefaultprovider",
                    getProviderClassName(bundle),
                    bundle.getName(), null);
            }
            return false;
        }

        helper.setProcessorType("JPA", bundle.getName()); // NOI18N
        helper.setDropTablesValue(userDropTables, bundle.getName());
        helper.setCreateTablesValue(userCreateTables && !ddlMode.equals(DDL_SQL_SCRIPT_GENERATION), 
                bundle.getName());


        // For a RESOURCE_LOCAL, managed pu, only non-jta-data-source should be specified.
        String dataSourceName =
                (PersistenceUnitTransactionType.JTA == PersistenceUnitTransactionType.valueOf(bundle.getTransactionType()) )?
                        bundle.getJtaDataSource() : bundle.getNonJtaDataSource();
        helper.setJndiName(dataSourceName, bundle.getName());
        constructJdbcFileNames(bundle);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Processing request to create files - create file: " + //NOI18N
                    helper.getCreateJdbcFileName(bundle.getName())
                    + ", drop  file: " + //NOI18N
                    helper.getDropJdbcFileName(bundle.getName())); 
        }

        addPropertiesToPU(bundle);
        return true;
    }

    /**
     * Construct the name of the create and
     * drop jdbc ddl files that would be
     * created. These name would be either
     * obtained from the persistence.xml file
     * (if the user has defined them) or we would
     * create default filenames
     * @param parBundle the persistence unit descriptor that is being worked on.
     */
    private void constructJdbcFileNames(PersistenceUnitDescriptor parBundle)  {
        String createJdbcFileName =
                getPersistencePropVal(parBundle,
                providerPropertyNamesHolder.createJdbcDdlFile, null);
        String dropJdbcFileName =
                getPersistencePropVal(parBundle,
                providerPropertyNamesHolder.dropJdbcDdlFile, null);

        if((null != createJdbcFileName) && (null != dropJdbcFileName)) {
            return;
        }

        String filePrefix =
                    Java2DBProcessorHelper.getDDLNamePrefix(parBundle.getParent().getParent());

        if(null == createJdbcFileName) {
            createJdbcFileName = filePrefix + DatabaseConstants.NAME_SEPARATOR + parBundle.getName() +
                DatabaseConstants.CREATE_DDL_JDBC_FILE_SUFFIX;
        }
        if(null == dropJdbcFileName) {
            dropJdbcFileName = filePrefix + DatabaseConstants.NAME_SEPARATOR + parBundle.getName() +
                DatabaseConstants.DROP_DDL_JDBC_FILE_SUFFIX;
        }

        helper.setCreateJdbcFileName(createJdbcFileName, parBundle.getName());
        helper.setDropJdbcFileName(dropJdbcFileName, parBundle.getName());
    }

    /**
     * This method is called after the jdbc files have been created. 
     * Iterate over all created jdbc ddl files and
     * execute it against the database to have the required objects created.
     */
    public void createTablesInDB() {
        helper.createOrDropTablesInDB(true, "JPA"); // NOI18N
    }

    /**
     * Since the ddl files are actually created
     * by the toplink code, we ensure that the
     * correct properties are put in the persistence
     * unit info object.
     * @param puDescriptor the persistence unit descriptor that is being worked on.
     */
    private void addPropertiesToPU(PersistenceUnitDescriptor puDescriptor) {
        addPropertyToDescriptor(puDescriptor, providerPropertyNamesHolder.appLocation,
                helper.getGeneratedLocation(puDescriptor.getName()));
        addPropertyToDescriptor(puDescriptor, providerPropertyNamesHolder.createJdbcDdlFile,
                helper.getCreateJdbcFileName(puDescriptor.getName()));
        addPropertyToDescriptor(puDescriptor, providerPropertyNamesHolder.dropJdbcDdlFile,
                helper.getDropJdbcFileName(puDescriptor.getName()));
    }

    /**
     * Utility method that is used to actually set the property into the persistence unit descriptor.
     * @param descriptor the persistence unit descriptor that is being worked on.
     * @param propertyName the name of the property.
     * @param propertyValue the value of the property.
     */
    private void addPropertyToDescriptor(PersistenceUnitDescriptor descriptor,
            String propertyName, String propertyValue) {
        String oldPropertyValue = descriptor.getProperties().getProperty(propertyName);
        if(null == oldPropertyValue) {
            descriptor.addProperty(propertyName, propertyValue);
        }
    }

    /**
     * Given a persistence unit descriptor
     * return the value of a property if the
     * user has specified it.
     * If the user has not defined this property
     * return the default value.
     * @param parBundle the persistence unit descriptor that is being worked on.
     * @param propertyName the property name being checked.
     * @param defaultValue the default value to be used.
     * @return the property value.
     */
    private String getPersistencePropVal(PersistenceUnitDescriptor parBundle,
            String propertyName, String defaultValue) {
        return parBundle.getProperties().getProperty(propertyName, defaultValue);
    }

    /**
     * The java2db feature is currently implemented only for toplink (the default
     * persistence povider in glassfish). Hence we check for the name of the
     * persistence provider class name. It it is not toplink, stop processing.
     *
     * @param persistenceUnitDescriptor the persistence unit descriptor that is being worked on.
     * @return true if persistence provider is toplink.
     */
    private boolean isSupportedPersistenceProvider(
            final PersistenceUnitDescriptor persistenceUnitDescriptor) {

        String providerClassName = getProviderClassName(persistenceUnitDescriptor);

        return providerClassName.equals(TOPLINK_PERSISTENCE_PROVIDER_CLASS_NAME_OLD) ||
                providerClassName.equals(TOPLINK_PERSISTENCE_PROVIDER_CLASS_NAME_NEW) ||
                providerClassName.equals(ECLIPSELINK_PERSISTENCE_PROVIDER_CLASS_NAME);
    }

    /**
     * Return provider class name as specified in the persistence.xml
     * or the default provider as known to the system.
     * @param persistenceUnitDescriptor the persistence unit descriptor.
     * @return provider class name as a String
     */
     private String getProviderClassName(
            PersistenceUnitDescriptor persistenceUnitDescriptor) {

        return PersistenceUnitInfoImpl.getPersistenceProviderClassNameForPuDesc(
                persistenceUnitDescriptor);
     }

    /**
     * Holds names of provider specific property
     */
    private static class ProviderPropertyNamesHolder {
        // Initialize property names with EL specific properties
            String appLocation       = ECLIPSELINK_APP_LOCATION;
            String createJdbcDdlFile = ECLIPSELINK_CREATE_JDBC_DDL_FILE;
            String dropJdbcDdlFile   = ECLIPSELINK_DROP_JDBC_DDL_FILE;
            String ddlGeneration     = ECLIPSELINK_DDL_GENERATION;
            String ddlGenerationMode = ECLIPSELINK_DDL_GENERATION_MODE;
    }

}
