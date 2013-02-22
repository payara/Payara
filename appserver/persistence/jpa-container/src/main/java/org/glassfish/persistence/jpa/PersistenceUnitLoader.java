/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deployment.PersistenceUnitDescriptor;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import com.sun.enterprise.deployment.PersistenceUnitsDescriptor;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.glassfish.persistence.jpa.schemageneration.SchemaGenerationProcessor;
import org.glassfish.persistence.jpa.schemageneration.SchemaGenerationProcessorFactory;

import javax.persistence.EntityManagerFactory;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitTransactionType;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads emf corresponding to a PersistenceUnit. Executes java2db if required.
 * @author Mitesh Meswani
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class PersistenceUnitLoader {

    /**
     * Conduit to talk with container
     */
    private ProviderContainerContractInfo providerContainerContractInfo;

    private EntityManagerFactory emf;

    /**
     * The schemaGenerationProcessor instance for the Java2DB work.
     */
    private SchemaGenerationProcessor schemaGenerationProcessor;

    private static Logger logger = LogDomains.getLogger(PersistenceUnitLoader.class, LogDomains.PERSISTENCE_LOGGER);

    private static final StringManager localStrings = StringManager.getManager(PersistenceUnitLoader.class);    

    private static Map<String, String> integrationProperties;

    /** EclipseLink property name to enable/disable weaving **/
    private static final String ECLIPSELINK_WEAVING_PROPERTY = "eclipselink.weaving"; // NOI18N

    /** Name of property used to specify validation mode */
    private static final String VALIDATION_MODE_PROPERTY = "javax.persistence.validation.mode";

    /** Name of property used to specify validator factory */
    private static final String VALIDATOR_FACTORY = "javax.persistence.validation.factory";

    private static final String DISABLE_UPGRADE_FROM_TOPLINK_ESSENTIALS = "org.glassfish.persistence.jpa.disable.upgrade.from.toplink.essentials";

    public PersistenceUnitLoader(PersistenceUnitDescriptor puToInstatntiate, ProviderContainerContractInfo providerContainerContractInfo) {
       this.providerContainerContractInfo = providerContainerContractInfo;

       // A hack to work around EclipseLink issue https://bugs.eclipse.org/bugs/show_bug.cgi?id=248328 for prelude
       // This should be removed once version of EclipseLink which fixes the issue is integrated.
       // set the system property required by EclipseLink before we load it.
       setSystemPropertyToEnableDoPrivilegedInEclipseLink();

       emf = loadPU(puToInstatntiate);
   }

    /**
     * @return The emf loaded.
     */
    public EntityManagerFactory getEMF() {
        return emf;
    }

    private void setSystemPropertyToEnableDoPrivilegedInEclipseLink() {
        final String PROPERTY_NAME = "eclipselink.security.usedoprivileged";
        // Need not invoke in doPrivileged block as the whole call stack consist of trusted code when this code
        // is invoked
        if(System.getProperty(PROPERTY_NAME) == null) {
            // property not set. Set it to true
            System.setProperty(PROPERTY_NAME, String.valueOf(Boolean.TRUE) );
        }
    }

    /**
     * Loads an individual PersistenceUnitDescriptor and registers the
     * EntityManagerFactory in appropriate DOL structure.
     *
     * @param pud PersistenceUnitDescriptor to be loaded.
     */
    private EntityManagerFactory loadPU(PersistenceUnitDescriptor pud) {


        checkForUpgradeFromTopLinkEssentials(pud);

        checkForDataSourceOverride(pud);

        calculateDefaultDataSource(pud);

        PersistenceUnitInfo pInfo = new PersistenceUnitInfoImpl(pud, providerContainerContractInfo);

        String applicationLocation = providerContainerContractInfo.getApplicationLocation();
        final boolean fineMsgLoggable = logger.isLoggable(Level.FINE);
        if(fineMsgLoggable) {
            logger.fine("Loading persistence unit for application: \"" + applicationLocation + "\"pu Root is: " +
                    pud.getPuRoot());
            logger.fine("PersistenceInfo for this pud is :\n" + pInfo); // NOI18N
        }

        PersistenceProvider provider;
        try {
            // See we use application CL as opposed to system CL to loadPU
            // provider. This allows user to get hold of provider specific
            // implementation classes in their code. But this also means
            // provider must not use appserver implementation classes directly
            // because once we implement isolation in our class loader hierarchy
            // the only classes available to application class loader would be
            // our appserver interface classes. By Sahoo
            provider =
                    PersistenceProvider.class.cast(
                    providerContainerContractInfo.getClassLoader()
                    .loadClass(pInfo.getPersistenceProviderClassName())
                    .newInstance());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        Map<String, Object> schemaGenerationOverrides;
        schemaGenerationProcessor = SchemaGenerationProcessorFactory.createSchemaGenerationProcessor(pud);
        if(providerContainerContractInfo.isJava2DBRequired() ) {
            schemaGenerationProcessor.init(pud, providerContainerContractInfo.getDeploymentContext());
            schemaGenerationOverrides = schemaGenerationProcessor.getOverridesForSchemaGeneration();
        } else {
            // schema generation is not required if this EMF is being created for
            // -appserver restarting or,
            // -on an instance or,
            // -appclient
            // Suppress schema generation in this case
            schemaGenerationOverrides = schemaGenerationProcessor.getOverridesForSuppressingSchemaGeneration();
        }

        Map<String, Object> overRides = new HashMap<String, Object>(integrationProperties);
        if(schemaGenerationOverrides != null) {
            overRides.putAll(schemaGenerationOverrides);
        }

        // Check if the persistence unit requires Bean Validation
        ValidationMode validationMode = getValidationMode(pud);
        if(validationMode == ValidationMode.AUTO || validationMode == ValidationMode.CALLBACK ) {
            overRides.put(VALIDATOR_FACTORY, providerContainerContractInfo.getValidatorFactory());
        }

        if(!providerContainerContractInfo.isWeavingEnabled()) {
            overRides.put(ECLIPSELINK_WEAVING_PROPERTY, System.getProperty(ECLIPSELINK_WEAVING_PROPERTY,"false")); // NOI18N
        }

        EntityManagerFactory emf = provider.createContainerEntityManagerFactory(pInfo, overRides);

        if (fineMsgLoggable) {
            logger.logp(Level.FINE, "PersistenceUnitLoader", "loadPU", // NOI18N
                        "emf = {0}", emf); // NOI18N
        }

        PersistenceUnitsDescriptor parent = pud.getParent();
        RootDeploymentDescriptor containingBundle = parent.getParent();
        providerContainerContractInfo.registerEMF(pInfo.getPersistenceUnitName(), pud.getPuRoot(), containingBundle, emf);

        if(fineMsgLoggable) {
            logger.fine("Finished loading persistence unit for application: " +  // NOI18N
                    applicationLocation);
        }
        return emf;
    }

    /**
     * If use provided data source is overridden, update PersistenceUnitDescriptor with it
     */
    private void checkForDataSourceOverride(PersistenceUnitDescriptor pud) {
        String jtaDataSourceOverride = providerContainerContractInfo.getJTADataSourceOverride();
        if(jtaDataSourceOverride != null) {
            pud.setJtaDataSource(jtaDataSourceOverride);
        }
    }

    /** Calculate and set the default data source in given <code>pud</code> **/
    private void calculateDefaultDataSource(PersistenceUnitDescriptor pud) {
        String jtaDataSourceName = calculateJtaDataSourceName(pud.getTransactionType(), pud.getJtaDataSource(), pud.getNonJtaDataSource(), pud.getName());
        String nonJtaDataSourceName = calculateNonJtaDataSourceName(pud.getJtaDataSource(), pud.getNonJtaDataSource());
        pud.setJtaDataSource(jtaDataSourceName);
        pud.setNonJtaDataSource(nonJtaDataSourceName);
    }

    /**
     * @return DataSource Name to be used as JTA data source.
     */
    private String calculateJtaDataSourceName(String transactionType, String userSuppliedJTADSName, String userSuppliedNonJTADSName, String puName) {
        /*
         * Use DEFAULT_DS_NAME iff user has not specified both jta-ds-name
         * and non-jta-ds-name; and user has specified transaction-type as JTA.
         * See Gf issue #1204 as well.
         */
        if (PersistenceUnitTransactionType.valueOf(transactionType) != PersistenceUnitTransactionType.JTA) {
            logger.logp(Level.FINE,
                    "PersistenceUnitInfoImpl", // NOI18N
                    "_getJtaDataSource", // NOI18N
                    "This PU is configured as non-jta, so jta-data-source is null"); // NOI18N
            return null; // this is a non-jta-data-source
        }
        String DSName;
        if (!isNullOrEmpty(userSuppliedJTADSName)) {
            DSName = userSuppliedJTADSName; // use user supplied jta-ds-name
        } else if (isNullOrEmpty(userSuppliedNonJTADSName )) {
            DSName = providerContainerContractInfo.getDefaultDataSourceName();
        } else {
            String msg = localStrings.getString("puinfo.jta-ds-not-configured", // NOI18N
                    new Object[] {puName});
            throw new RuntimeException(msg);
        }
        logger.logp(Level.FINE, "PersistenceUnitLoaderImpl", // NOI18N
                "_getJtaDataSource", "JTADSName = {0}", // NOI18N
                DSName);
        return DSName;
    }

    private String calculateNonJtaDataSourceName(String userSuppliedJTADSName, String userSuppliedNonJTADSName ) {
        /*
         * If non-JTA name is *not* provided
         * - use the JTA DS name (if supplied)
         * If non-JTA name is provided
         * - use non-JTA DS name
         * (this is done for ease of use, because user does not have to
         * explicitly mark a connection pool as non-transactional.
         * Calling lookupNonTxDataSource() with a resource which is
         * already configured as non-transactional has no side effects.)
         * If neither non-JTA nor JTA name is provided
         * use DEFAULT_DS_NAME.
         */
        String DSName;
        if (!isNullOrEmpty(userSuppliedNonJTADSName)) {
            DSName = userSuppliedNonJTADSName;
        } else {
            if (!isNullOrEmpty(userSuppliedJTADSName)) {
                DSName = userSuppliedJTADSName;
            } else {
                DSName = providerContainerContractInfo.getDefaultDataSourceName();
            }
        }
        logger.logp(Level.FINE,
                "PersistenceUnitInfoImpl", // NOI18N
                "_getNonJtaDataSource", "nonJTADSName = {0}", // NOI18N
                DSName);
        return DSName;
    }

    static boolean isNullOrEmpty(String s) {
        return s == null || s.length() == 0;
    }

    /**
     * If the app is using Toplink Essentials as the provider and TopLink Essentials is not available in classpath
     * We try to upgrade the app to use EclipseLink.
     * Change the provider to EclipseLink and translate "toplink.*" properties to "eclipselink.*" properties
     */
    private void checkForUpgradeFromTopLinkEssentials(PersistenceUnitDescriptor pud) {
        if(Boolean.getBoolean(DISABLE_UPGRADE_FROM_TOPLINK_ESSENTIALS) ) {
            //Return if instructed by System property
            return;
        }
        boolean upgradeTopLinkEssentialsProperties = false;
        String providerClassName = pud.getProvider();

        if (providerClassName == null || providerClassName.isEmpty() ) {
            // This might be a JavaEE app running against V2 and relying in provider name being defaulted.
            upgradeTopLinkEssentialsProperties = true;
        } else if( "oracle.toplink.essentials.PersistenceProvider".equals(providerClassName) ||
                "oracle.toplink.essentials.ejb.cmp3.EntityManagerFactoryProvider".equals(providerClassName) ) {
            try {
                providerContainerContractInfo.getClassLoader().loadClass(providerClassName);
            } catch (ClassNotFoundException e) {
                // Toplink Essentials classes are not available to an application using it as the provider
                // Migrate the application to use EclipseLink

                String defaultProvider = PersistenceUnitInfoImpl.getDefaultprovider();
                if(logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, "puloader.defaulting.provider.on.upgrade", new Object[] {pud.getName(), defaultProvider});
                }

                // Change the provider name
                pud.setProvider(defaultProvider);
                upgradeTopLinkEssentialsProperties = true;
            }
        }

        if (upgradeTopLinkEssentialsProperties) {
            // For each "toplink*" property, add a "eclipselink* property
            final String TOPLINK = "toplink";
            final String ECLIPSELINK = "eclipselink";
            Properties properties = pud.getProperties();
            for (Map.Entry entry : properties.entrySet()) {
                String key = (String) entry.getKey();
                if(key.startsWith(TOPLINK) ) {
                    String translatedKey = ECLIPSELINK + key.substring(TOPLINK.length());
                    pud.addProperty(translatedKey, entry.getValue());
                }
            }
        }
    }

    /**
     * Called during load when the correct classloader and transformer had been
     * already set.
     * For emf that require Java2DB, call createEntityManager() to populate
     * the DDL files, then iterate over those files and execute each line in them.
     */
    void doJava2DB() {
        if (schemaGenerationProcessor.isContainerDDLExecutionRequired()) {
            final boolean fineMsgLoggable = logger.isLoggable(Level.FINE);
            if(fineMsgLoggable) {
                logger.fine("<--- To Create Tables"); // NOI18N
            }

            schemaGenerationProcessor.executeCreateDDL();

            if(fineMsgLoggable) {
                logger.fine("---> Done Create Tables"); // NOI18N
            }
        }
    }

    private ValidationMode getValidationMode(PersistenceUnitDescriptor pud) {
        ValidationMode validationMode = pud.getValidationMode(); //Initialize with value element <validation-mode> in persitence.xml
        //Check is overridden in properties
        String validationModeFromProperty = (String) pud.getProperties().get(VALIDATION_MODE_PROPERTY);
        if(validationModeFromProperty != null) {
            //User would get IllegalArgumentException if he has specified invalid mode
            validationMode = ValidationMode.valueOf(validationModeFromProperty);
        }
        return validationMode;
    }


    static {
        /*
         * We set all the provider specific integration level properties here.
         * It knows about all the integration level properties that
         * are needed to integrate a provider with our container. When we add
         * support for other containers, we should modify this code so that user
         * does not have to specify such properties in their persistence.xml file.
         * These properties can be overriden by persistence.xml as per
         * the spec. Before applying default values for properties, this method
         * first checks if the properties have been set in the system
         * (typically done using -D option in domain.xml).
         *
         */
        // ------------------- The Base -------------------------

        Map<String, String> props = new HashMap<>();

        final String ECLIPSELINK_SERVER_PLATFORM_CLASS_NAME_PROPERTY = "eclipselink.target-server"; // NOI18N
        props.put(ECLIPSELINK_SERVER_PLATFORM_CLASS_NAME_PROPERTY,
                System.getProperty(ECLIPSELINK_SERVER_PLATFORM_CLASS_NAME_PROPERTY, "SunAS9")); // NOI18N

        // TopLink specific properties:
        // See https://glassfish.dev.java.net/issues/show_bug.cgi?id=249
        final String TOPLINK_SERVER_PLATFORM_CLASS_NAME_PROPERTY = "toplink.target-server"; // NOI18N
        props.put(TOPLINK_SERVER_PLATFORM_CLASS_NAME_PROPERTY,
                System.getProperty(TOPLINK_SERVER_PLATFORM_CLASS_NAME_PROPERTY, "SunAS9")); // NOI18N

        // Hibernate specific properties:
        final String HIBERNATE_TRANSACTION_MANAGER_LOOKUP_CLASS_PROPERTY = "hibernate.transaction.manager_lookup_class"; // NOI18N
        props.put(HIBERNATE_TRANSACTION_MANAGER_LOOKUP_CLASS_PROPERTY,
                System.getProperty(HIBERNATE_TRANSACTION_MANAGER_LOOKUP_CLASS_PROPERTY, "org.hibernate.transaction.SunONETransactionManagerLookup")); // NOI18N

        integrationProperties = Collections.unmodifiableMap(props);

    }


}
