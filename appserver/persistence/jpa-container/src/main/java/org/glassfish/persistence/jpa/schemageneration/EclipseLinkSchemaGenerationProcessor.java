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

package org.glassfish.persistence.jpa.schemageneration;

import com.sun.enterprise.deployment.PersistenceUnitDescriptor;

import org.glassfish.persistence.common.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.logging.LogDomains;
import org.glassfish.persistence.jpa.PersistenceUnitInfoImpl;


import javax.persistence.spi.PersistenceUnitTransactionType;

/**
 * SchemaGenerationProcessor that handles schema generation while
 * running against EclipseLink in pre JPA 2.1 mode
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
public class EclipseLinkSchemaGenerationProcessor implements SchemaGenerationProcessor {

    // Defining the persistence provider class names here that we would use to
    // check if schema generation is supported.
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

    // property names for Toplink and EclipseLink
    private static final String TOPLINK_DDL_GENERATION     = "toplink.ddl-generation"; // NOI18N
    private static final String ECLIPSELINK_DDL_GENERATION = "eclipselink.ddl-generation"; // NOI18N

    private static final String TOPLINK_DDL_GENERATION_OUTPUT_MODE = "toplink.ddl-generation.output-mode"; // NOI18N
    private static final String ECLIPSELINK_DDL_GENERATION_OUTPUT_MODE = "eclipselink.ddl-generation.output-mode"; // NOI18N

    private static final String TOPLINK_APP_LOCATION         = "toplink.application-location"; // NOI18N
    private static final String ECLIPSELINK_APP_LOCATION     = "eclipselink.application-location"; // NOI18N

    private static final String TOPLINK_CREATE_JDBC_DDL_FILE     = "toplink.create-ddl-jdbc-file-name"; // NOI18N
    private static final String ECLIPSELINK_CREATE_JDBC_DDL_FILE = "eclipselink.create-ddl-jdbc-file-name"; // NOI18N

    private static final String TOPLINK_DROP_JDBC_DDL_FILE       = "toplink.drop-ddl-jdbc-file-name"; // NOI18N
    private static final String ECLIPSELINK_DROP_JDBC_DDL_FILE   = "eclipselink.drop-ddl-jdbc-file-name"; // NOI18N

    private static Logger logger = LogDomains.getLogger(EclipseLinkSchemaGenerationProcessor.class, LogDomains.PERSISTENCE_LOGGER);

    /**
     * Holds name of provider specific properties.
     */
    private ProviderPropertyNamesHolder providerPropertyNamesHolder;

    private Java2DBProcessorHelper helper;

    private Map<String, String> overrides;

    private boolean isSchemaGenerationPU;

    /**
     * Creates a new instance of EclipseLinkSchemaGenerationProcessor using Java2DBProcessorHelper
     * @param helper the Java2DBProcessorHelper instance to be used
     * with this processor.
     * @param bundle The PersistenceUnitDescriptor for pu being deployed
     * @param isSchemaGenerationRequired whether schema generation is required from the context that the PU is being created
     */
    public EclipseLinkSchemaGenerationProcessor(Java2DBProcessorHelper helper, PersistenceUnitDescriptor bundle, boolean isSchemaGenerationRequired) {

        String providerClassName = getProviderClassName(bundle);

        if (isSupportedPersistenceProvider(providerClassName) ) {
            overrides = new HashMap<>();

            initializeProviderPropertyHolder(providerClassName);


            if (isSchemaGenerationRequired) {
                this.helper = helper;
                this.helper.init();

                String ddlGenerate = getPersistencePropVal(bundle,
                        providerPropertyNamesHolder.ddlGeneration, NONE);
                String ddlMode = getPersistencePropVal(bundle,
                        providerPropertyNamesHolder.ddlGenerationOutputMode, DDL_BOTH_GENERATION);

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

                if (createTables || userDropTables) {
                    helper.setProcessorType("JPA", bundle.getName()); // NOI18N
                    helper.setDropTablesValue(userDropTables, bundle.getName());
                    helper.setCreateTablesValue(userCreateTables && !ddlMode.equals(DDL_SQL_SCRIPT_GENERATION),
                            bundle.getName());


                    // For a RESOURCE_LOCAL, managed pu, only non-jta-data-source should be specified.
                    String dataSourceName =
                            (PersistenceUnitTransactionType.JTA == PersistenceUnitTransactionType.valueOf(bundle.getTransactionType())) ?
                                    bundle.getJtaDataSource() : bundle.getNonJtaDataSource();
                    helper.setJndiName(dataSourceName, bundle.getName());
                    constructJdbcFileNames(bundle);
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Processing request to create files - create file: " + //NOI18N
                                helper.getCreateJdbcFileName(bundle.getName())
                                + ", drop  file: " + //NOI18N
                                helper.getDropJdbcFileName(bundle.getName()));
                    }

                    addSchemaGenerationPropertiesToOverrides(bundle, overrides);
                    isSchemaGenerationPU = true;
                }

            } else {
                // Schema generation is not required in the context. Add properties that suppress it
                overrides.put(providerPropertyNamesHolder.ddlGenerationOutputMode, NONE);
            }
        } else {
            // Persistence provider is not supported, hence exit from schema generation code
            if (helper.hasDeployCliOverrides()) {
                helper.logI18NWarnMessage(
                        "JPAJava2DBProcessor.nondefaultprovider",
                        getProviderClassName(bundle),
                        bundle.getName(), null);
            }
        }

    }

    private void initializeProviderPropertyHolder(String providerClassName) {
        // Initialize with EL names for each bundle to handle apps that have
        // multiple pus
        providerPropertyNamesHolder = new ProviderPropertyNamesHolder();

        // Override with TLE names if running against TLE
        if (TOPLINK_PERSISTENCE_PROVIDER_CLASS_NAME_NEW.equals(providerClassName) ||
                TOPLINK_PERSISTENCE_PROVIDER_CLASS_NAME_OLD.equals(providerClassName)) {
            // for backward compatibility
            providerPropertyNamesHolder.appLocation = TOPLINK_APP_LOCATION;
            providerPropertyNamesHolder.createJdbcDdlFile = TOPLINK_CREATE_JDBC_DDL_FILE;
            providerPropertyNamesHolder.dropJdbcDdlFile = TOPLINK_DROP_JDBC_DDL_FILE;
            providerPropertyNamesHolder.ddlGeneration = TOPLINK_DDL_GENERATION;
            providerPropertyNamesHolder.ddlGenerationOutputMode = TOPLINK_DDL_GENERATION_OUTPUT_MODE;
        }
    }

    @Override
    public boolean isDDLExecutionRequired() {
        // DDL execution is required if this is a schema generation pu
        return isSchemaGenerationPU;
    }

    @Override
    public Map<String, String> getSchemaGenerationOverrides() {
        return overrides;
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
    @Override
    public void executeCreateDDL() {
        helper.createOrDropTablesInDB(true, "JPA"); // NOI18N
    }

    private void addSchemaGenerationPropertiesToOverrides(PersistenceUnitDescriptor puDescriptor, Map<String, String> overrides) {
        addPropertyToOverride(puDescriptor, overrides, providerPropertyNamesHolder.appLocation,
                helper.getGeneratedLocation(puDescriptor.getName()));
        addPropertyToOverride(puDescriptor, overrides, providerPropertyNamesHolder.createJdbcDdlFile,
                helper.getCreateJdbcFileName(puDescriptor.getName()));
        addPropertyToOverride(puDescriptor, overrides, providerPropertyNamesHolder.dropJdbcDdlFile,
                helper.getDropJdbcFileName(puDescriptor.getName()));

        // The puDescriptor might not have this property if schema generation is triggered by deployment CLI override
        addPropertyToOverride(puDescriptor, overrides,
                providerPropertyNamesHolder.ddlGeneration, DROP_AND_CREATE);
        // If we are doing schema generation, we want DDL scripts to be generated
        addPropertyToOverride(puDescriptor, overrides,
                providerPropertyNamesHolder.ddlGenerationOutputMode, DDL_SQL_SCRIPT_GENERATION);

    }

    /**
     * Utility method that is used to actually set the property into the persistence unit descriptor.
     * @param descriptor the persistence unit descriptor that is being worked on.
     * @param propertyName the name of the property.
     * @param propertyValue the value of the property.
     */
    private static void addPropertyToOverride(PersistenceUnitDescriptor descriptor, Map<String, String> overrides,
                                       String propertyName, String propertyValue) {
        String oldPropertyValue = descriptor.getProperties().getProperty(propertyName);
        if(null == oldPropertyValue) { //Do not override any value explicitly specified by the user
            overrides.put(propertyName, propertyValue);
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
     * This processor only supports EclipseLink, the default
     * persistence povider in glassfish; or Toplink, the default provder for GF 2.x.
     *
     * @return true if persistence provider is EclipseLink or Toplink.
     */
    private boolean isSupportedPersistenceProvider(final String providerClassName) {

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
            String ddlGenerationOutputMode = ECLIPSELINK_DDL_GENERATION_OUTPUT_MODE;
    }

}
