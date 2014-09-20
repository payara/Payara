/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

/*
 * MappingGenerator.java
 *
 * Created on Aug 18, 2003
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.jdo.api.persistence.mapping.ejb.AbstractNameMapper;
import com.sun.jdo.api.persistence.mapping.ejb.ConversionException;
import com.sun.jdo.api.persistence.mapping.ejb.MappingFile;
import com.sun.jdo.api.persistence.mapping.ejb.beans.CmpFieldMapping;
import com.sun.jdo.api.persistence.mapping.ejb.beans.CmrFieldMapping;
import com.sun.jdo.api.persistence.mapping.ejb.beans.ColumnPair;
import com.sun.jdo.api.persistence.mapping.ejb.beans.EntityMapping;
import com.sun.jdo.api.persistence.mapping.ejb.beans.SunCmpMapping;
import com.sun.jdo.api.persistence.mapping.ejb.beans.SunCmpMappings;
import com.sun.jdo.api.persistence.model.Model;
import com.sun.jdo.api.persistence.model.ModelException;
import com.sun.jdo.api.persistence.model.mapping.MappingClassElement;
import com.sun.jdo.spi.persistence.generator.database.DatabaseGenerator;
import com.sun.jdo.spi.persistence.support.ejb.codegen.GeneratorException;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.DeploymentHelper;
import com.sun.jdo.spi.persistence.utility.StringHelper;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.api.ActionReport;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.persistence.common.DatabaseConstants;
import org.glassfish.persistence.common.I18NHelper;
import org.glassfish.persistence.common.Java2DBProcessorHelper;
import org.glassfish.persistence.common.database.DBVendorTypeHelper;
import org.netbeans.modules.dbschema.DBException;
import org.netbeans.modules.dbschema.DBIdentifier;
import org.netbeans.modules.dbschema.SchemaElement;
import org.netbeans.modules.dbschema.jdbcimpl.ConnectionProvider;
import org.netbeans.modules.dbschema.jdbcimpl.SchemaElementImpl;
import org.netbeans.modules.dbschema.util.NameUtil;
import org.netbeans.modules.schema2beans.Schema2BeansException;
import org.netbeans.modules.schema2beans.ValidateException;

/*
 * This class will generate mapping classes from sun-cmp-mappings.xml
 * and dbschema if they are available in the jar, or it will generate mapping 
 * classes based on ejb-jar.xml, bean classes and policy by invoking the 
 * database generation backend.
 *
 * @author Jie Leng 
 */
public class MappingGenerator extends 
    com.sun.jdo.api.persistence.mapping.ejb.MappingGenerator {

    // XXX To be removed when all callers are switched to use 
    // DatabaseConstants.JAVA_TO_DB_FLAG directly.
    public static final String JAVA_TO_DB_FLAG = DatabaseConstants.JAVA_TO_DB_FLAG;

    private static final String DBSCHEMA_EXTENSION = ".dbschema"; // NOI18N
    private static final char DOT = '.'; // NOI18N

     /** The logger */
    private static final Logger logger = LogHelperEJBCompiler.getLogger();

    private final EjbBundleDescriptorImpl bundle;

    private String dbVendorName = null;
    private boolean isJavaToDatabaseFlag = false;
    private boolean isVerifyFlag = false;
        
    /**
     * I18N message handler
     */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
            MappingGenerator.class);

    /** 
     * Constructor
     * @param bundle an ejb bundle
     * @param model a model containing mapping class and 
     * persistence class information
     * @param nameMapper a nameMapper for name lookup
     * @param loader a class loader
     */
    public MappingGenerator(EjbBundleDescriptorImpl bundle, Model model,
            NameMapper nameMapper, ClassLoader loader) {
        super(new EJBBundleInfoHelper(bundle, nameMapper, model, null), loader, false);
        this.bundle = bundle;
    }

    /** 
     * This method will load mapping classes if there is sun-cmp-mappings.xml,
     * otherwise it will call the database generation backend to create 
     * mapping classes and schema.  It also generates *.dbschema and 
     * sun-cmp-mappings.xml in application dir if it is
     * in creating mapping classes mode.
     * @param ctx an object containing CLI options for 
     * the database generation backend
     * @param inputFilesPath the directory where sun-cmp-mappings.xml is located
     * @param generatedXmlsPath the directory where the generated files are located
     * @param classout the directory where the classes are located
     * @param ignoreSunDeploymentDescriptors use java2db generation if set to <code>true</code>.
     * @return a SchemaElement for mapping classes mapped to
     * @throws IOException
     * @throws DBException
     * @throws ModelException
     * @throws Schema2BeansException
     * @throws SQLException
     * @throws GeneratorException 
     * @throws ConversionException 
     */
    public SchemaElement generateMapping(
            DeploymentContext ctx, String inputFilesPath,
            String generatedXmlsPath,  File classout,
            boolean ignoreSunDeploymentDescriptors) 
            throws IOException, DBException, ModelException, 
            Schema2BeansException, SQLException, GeneratorException, 
            ConversionException {

        SchemaElement schema = null;
        if (ctx == null)
            isVerifyFlag = true;
        File cmpMappingFile = getSunCmpMappingFile(inputFilesPath);
        boolean mappedBeans = !ignoreSunDeploymentDescriptors 
                && cmpMappingFile.exists();
        ResourceReferenceDescriptor cmpResource = checkOrCreateCMPResource(
                mappedBeans);

        // Remember whether or not this mapping was created by Java2DB.
        isJavaToDatabaseFlag = DeploymentHelper.isJavaToDatabase(
                cmpResource.getSchemaGeneratorProperties());

        // We *must* get a vendor name if either the beans are not mapped, or
        // they are mapped and the javaToDatabase flag is set.
        boolean mustHaveDBVendorName =
            !mappedBeans || (mappedBeans && isJavaToDatabaseFlag);
        
        // Read deployment settings from the deployment descriptor
        // and CLI options.
        Results deploymentArguments = getDeploymentArguments(
                ctx, cmpResource, mustHaveDBVendorName);
        dbVendorName = deploymentArguments.getDatabaseVendorName();
        if (mappedBeans) {
            // If sun-cmp-mappings.xml exists and we are doing a deployment,
            // validate some arguments and make sure we have dbschema.
            // If it is from verify, skip deployment arguments check.
            if (!isVerifyFlag) {
                String warning = null; // Warning for user, if required.

                if (isJavaToDatabaseFlag) {
                    // If beans were already mapped, we will generate tables, but
                    // they will be as per the existing mapping.  So if the user
                    // gave --uniquetablenames, warn them that we will not take
                    // that flag into account.  I.e., the tables will be generated
                    // as per the mapping.
                    if (deploymentArguments.hasUniqueTableNames()) {
                        warning = 
                            I18NHelper.getMessage(
                                messages,
                                "EXC_DisallowJava2DBUniqueTableNames", //NOI18N
                                bundle.getApplication().getRegistrationName(),
                                JDOCodeGeneratorHelper.getModuleName(bundle));
                        logger.warning(warning);
                    }
                } else if (deploymentArguments.hasJavaToDatabaseArgs()) {

                    // If beans are already mapped but the user gave any Java2DB
                    // command line arguments, warn the user that these args
                    // should not be used when module is already mapped.
                    warning = 
                        I18NHelper.getMessage(
                            messages,
                            "EXC_DisallowJava2DBCLIOverrides", //NOI18N
                            bundle.getApplication().getRegistrationName(),
                            JDOCodeGeneratorHelper.getModuleName(bundle));
                    logger.warning(warning);
                }

                if (warning != null) {
                    ActionReport subActionReport = ctx.getActionReport().addSubActionsReport();
                    // Propagte warning to client side so that the deployer can see the warning.
                    Java2DBProcessorHelper.warnUser(subActionReport, warning);
                }
            }
            // Sun-cmp-mapping.xml exists, use normal MappingClass loading
            SunCmpMappings sunCmpMappings = getSunCmpMappings(cmpMappingFile);

            // Ensure that there is a dbschema for each element of
            // sunCmpMappings.
            ensureDBSchemaExistence(cmpResource, sunCmpMappings, inputFilesPath,
                classout);

            // load real mapping model and jdo model in memory
            Map mappingClasses = loadMappingClasses(sunCmpMappings, getClassLoader());

            // Get schema from one of the mapping classes. 
            // The mapping class element may be null if there is inconsistency 
            // in sun-cmp-mappings.xml and ejb-jar.xml. For example, 
            // the bean has mapping information in sun-cmp-mappings.xml but 
            // no definition in the ejb-jar.xml.
            // So iterate over the mappings until the 1st non-null is found.
            MappingClassElement mc = null;
            Iterator iter = mappingClasses.values().iterator();
            while (iter.hasNext()) {
                mc = (MappingClassElement)iter.next();
                if (mc != null) {
                    schema = SchemaElement.forName(mc.getDatabaseRoot());
                    break;
                }
            }

            if (logger.isLoggable(Logger.FINE)){
                logger.fine("Loaded mapped beans for " // NOI18N
                            + cmpResource.getJndiName()
                            + ", isJavaToDatabase=" + isJavaToDatabaseFlag); // NOI18N
            }
        }
        else {
            // Generate mapping file and dbschema, since either
            // sun-cmp-mappings.xml does not exist (e.g. user didn't yet map)
            // or DeploymentContext is null (e.g. running under auspices of AVK).
            DatabaseGenerator.Results results  = generateMappingClasses(
                    dbVendorName, deploymentArguments.getUseUniqueTableNames(), 
                    deploymentArguments.getUserPolicy(), inputFilesPath);

            // java2db from verifier should not save anything to disk
            if (!isVerifyFlag) {
                // save SunCmpMapping to sun-cmp-mappings.xml 
                // in generated XML dir
                writeSunCmpMappingFile(results.getMappingClasses(), 
                    getSunCmpMappingFile(generatedXmlsPath));

                schema = results.getSchema();

                // save schema to dbschema file in generated XML dir
                writeSchemaFile(schema, classout);

                setJavaToDatabase(cmpResource, true);
            }
        }

        return schema;
    }

    public String getDatabaseVendorName() {
        return dbVendorName;
    }

    /**
     * Returns javatodb flag in cmpResource.
     * @return true if there is name as "javatodb" and value as "true"
     */
    public boolean isJavaToDatabase() {
        return isJavaToDatabaseFlag;
    }

    /**
     * Set javatodb flag into SchemaGeneratorProperties
     * @param cmpResource a ResourceReferenceDescriptor
     * @param value a string containing true or false
     */
    private void setJavaToDatabase(ResourceReferenceDescriptor 
            cmpResource, boolean value) {

        if (logger.isLoggable(Logger.FINE)) {
            logger.fine("set javatodb flag to " + value + " in cmpResource"); // NOI18N
        }

        Properties schemaGeneratorProperties = cmpResource.
                getSchemaGeneratorProperties();
        if (schemaGeneratorProperties == null) {
            schemaGeneratorProperties = new Properties();
            cmpResource.setSchemaGeneratorProperties(schemaGeneratorProperties);
        }

        schemaGeneratorProperties.setProperty(DatabaseConstants.JAVA_TO_DB_FLAG, 
                String.valueOf(value));

        isJavaToDatabaseFlag = value;
    }

    /**
     * Loads sun-cmp-mapping.xml into memory as SunCmpMappings
     * @param cmpMappingFile a file of sun-cmp-mappings.xml
     * @return a SunCmpMappings object
     * @throws IOException 
     * @throws Schema2BeansException
     */
    private SunCmpMappings getSunCmpMappings(File cmpMappingFile)
        throws IOException, Schema2BeansException, GeneratorException {
        InputStream is = null;
        BufferedInputStream iasMapping = null;
        SunCmpMappings sunCmpMapping = null;

        if (cmpMappingFile.length() == 0) {
            throw JDOCodeGeneratorHelper.createGeneratorException(
                    "CMG.BeansFileSizeIsZero", bundle); // NOI18N
        }

        try {
            is = new FileInputStream(cmpMappingFile);
            iasMapping = new BufferedInputStream(is);
            sunCmpMapping = SunCmpMappings.createGraph(iasMapping);
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch(Exception ex) {
                    if (logger.isLoggable(Logger.FINE))
                        logger.fine(ex.toString());
                }
            }
            if (iasMapping != null) {
                try {
                    iasMapping.close();
                } catch(Exception ex) {
                    if (logger.isLoggable(Logger.FINE))
                        logger.fine(ex.toString());
                }
            }
        }

        try {
            sunCmpMapping.validate();
        } catch (ValidateException ex) {
            throw JDOCodeGeneratorHelper.createGeneratorException(
                    "CMG.InvalidSunCmpMappingsFile", bundle, ex); // NOI18N
        }

        return sunCmpMapping;
    }

    /** 
     * Gets sun-cmp-mappings.xml file
     * @param filesPath a string consisting file path
     * @return a file of sun-cmp-mappings.xml
     */
    private static File getSunCmpMappingFile(String filesPath) {
        String cmpMappingFile = (new StringBuffer(filesPath).
                append(File.separator).
                append(MappingFile.DEFAULT_LOCATION_IN_EJB_JAR)).toString();

        // if the file contains directory structure, we need
        // to create those directories if they do not exist.
        if (cmpMappingFile.lastIndexOf(File.separatorChar) != -1) {
            String dirs = cmpMappingFile.substring(
                0, cmpMappingFile.lastIndexOf(File.separatorChar));
            File fileDirs = new File(dirs);
            if (!fileDirs.exists())
                fileDirs.mkdirs();
        }

        return new File(cmpMappingFile);
    }
    

    /** 
     * Writes to sun-cmp-mappings.xml from mappings classes
     * @param mappingClasses a set of mapping classes
     * @param cmpMappingFile corresponds to sun-cmp-mappings.xml 
     * @throws IOException
     * @throws ConversionException
     * @throws Schema2BeansException
     */
    private void writeSunCmpMappingFile(Set mappingClasses, File cmpMappingFile)
        throws IOException, ConversionException, Schema2BeansException {
        // Construct the input to MappingFile.fromMappingClasses(): a Map
        // object containing ejbName and MappingClassElement.  Use the
        // elements of iteration and NameMapper to create the input for
        // MappingFile.
        Map mappingMap = new HashMap();
        AbstractNameMapper nameMapper = getNameMapper();
        Iterator iter = mappingClasses.iterator();
        while (iter.hasNext()) {
            MappingClassElement mappingClass = (MappingClassElement)iter.next();
            String ejbName = nameMapper.getEjbNameForPersistenceClass(
                    mappingClass.getName());
            mappingMap.put(ejbName, mappingClass);
        }
        MappingFile mf = new MappingFile();
        OutputStream sunCmpMapping = null; 
        try {
            sunCmpMapping = new FileOutputStream(
                cmpMappingFile);
            mf.fromMappingClasses(sunCmpMapping, mappingMap, 
                getConversionHelper());
        } catch (IOException ex) {
            throw ex;
        } finally {
            try {
                if (sunCmpMapping != null) {
                    sunCmpMapping.close();
                }
            } catch (IOException ex) {
                if (logger.isLoggable(Logger.FINE))
                    logger.fine(ex.toString());
            }
        }
    }

    /** 
     * Writes to *.dbschema file from schema
     * @param schema a SchemaElement
     * @param filePath a directory where *.dbschema is located
     * @throws IOException
     */
    private static void writeSchemaFile(SchemaElement schema, File filePath) 
            throws IOException {
        OutputStream schemaStream = null;
        try {
            schemaStream = new FileOutputStream(
                new File(filePath, NameUtil.getSchemaResourceName(
                schema.getName().getName())));
            schema.save(schemaStream);
        } catch (IOException ex) {
           throw ex;
        } finally {
            try {
                if (schemaStream != null) {
                    schemaStream.close();
                }
            } catch (IOException ex) {
               if (logger.isLoggable(Logger.FINE))
                    logger.fine(ex.toString());
            }
        }
    }

    /**
     * Contains the results of getDeploymentArguments()
     */
    private class Results {
        private final Boolean useUniqueTableNames;
        private final String dbVendorName;
        private final Properties userPolicy;

        /**
         * If true, then the user explicitly provided one or more command line
         * args that are specific to Java2DB.
         */
        private final boolean javaToDatabaseArgs;

	Results(Boolean useUniqueTableNames, String dbVendorName,
                Properties userPolicy, boolean javaToDatabaseArgs) {
            this.useUniqueTableNames = useUniqueTableNames;
            this.dbVendorName = dbVendorName;
            this.userPolicy = userPolicy;
            this.javaToDatabaseArgs = javaToDatabaseArgs;
        }

        // XXX Get rid of getUseUniqueTableNames from all call sites that need a
        // boolean (probably all of them), using hasUniqueTableNames instead
        
        /** @return useUniqueTableNames */
        public Boolean getUseUniqueTableNames() {
            return useUniqueTableNames;
        }

        /**
         * @return true if --uniquetablenames was given on the command line.
         */
        public boolean hasUniqueTableNames() {
            return (useUniqueTableNames != null);
        }

        /**
         * Returns true if any Java2DB arguments were given on the command
         * line. 
         * @return javaToDatabaseArgs */
        public boolean hasJavaToDatabaseArgs() {
            return javaToDatabaseArgs;
        }

        /** @return dbVendorName */
        public String getDatabaseVendorName() {
            return dbVendorName;
        }

        /** @return userPolicy */
        public Properties getUserPolicy() {
            return userPolicy;
        }
    }

    /** Reads deployment settings from the deployment descriptor and CLI options
     * and populates the corresponding variables.
     * @param ctx CLI arguments are obtained from here.
     * @param cmpResource Parameters from deployment descriptor are obtained
     * from here.
     * @param connectToDatabase If true, then connect to database to get
     * database vendor name if not otherwise available.
     */
    private Results getDeploymentArguments(
            DeploymentContext ctx,
            ResourceReferenceDescriptor cmpResource,
            boolean connectToDatabase) {

        Boolean useUniqueTableNames = null;
        String dbVendorName = null;
        Properties userPolicy = null;

        //Indicates that one or more Java2DB arguments were given on the command
        //line.
        boolean javaToDatabaseArgs = false;

        // If DeploymentContext is not available, then use what is specified by
        // cmpResource.
        if (null == ctx) {
            dbVendorName = cmpResource.getDatabaseVendorName();

        } else {
            // Otherwise, get the vendor name from one of the CLI overrides,
            // cmpResource, or the actual database (in that order).
            DeployCommandParameters cliOverrides = ctx.getCommandParameters(DeployCommandParameters.class);
            useUniqueTableNames = cliOverrides.uniquetablenames;

            // In javaToDatabaseArgs, we collect whether or not we have seen
            // any of the java to database - related arguments, starting with
            // --uniquetablenames.
            javaToDatabaseArgs =(useUniqueTableNames != null);

            dbVendorName = cliOverrides.dbvendorname;

            javaToDatabaseArgs |= isPropertyDefined(dbVendorName);

            // XXX This check can be removed when DeployCommand guarantees to
            // not return UNDEFINED.
            if (null == dbVendorName /** || dbVendorName.equals(Constants.UNDEFINED) **/) {
                dbVendorName = cmpResource.getDatabaseVendorName();
            }

            // If there is no CLI override, and nothing specified in the
            // cmp-resource, try to get the dbvendorname from the database.
            if (null == dbVendorName && connectToDatabase) {
                try {
                    Connection conn = DeploymentHelper.getConnection(
                            cmpResource.getJndiName());
                    dbVendorName = conn.getMetaData().getDatabaseProductName();
                } catch (Exception ex) {
                    // Ignore exceptions and use default.
                }
            }                    
            Boolean createTables = cliOverrides.createtables;
            javaToDatabaseArgs |= (createTables != null);

            Boolean dropAndCreateTables = cliOverrides.dropandcreatetables;
            javaToDatabaseArgs |= (dropAndCreateTables != null);

        }

        if (null == dbVendorName) {
            dbVendorName = DBVendorTypeHelper.DEFAULT_DB;
        } else {
            dbVendorName = DBVendorTypeHelper.getDBType(dbVendorName);
        }

        userPolicy = cmpResource.getSchemaGeneratorProperties();

        return new Results(useUniqueTableNames, dbVendorName, userPolicy, javaToDatabaseArgs);
    }

    /**
     * Check if cmp resource is specified in the deployment descriptor.
     * If the beans are mapped (sun-cmp-mapping.xml is present), the cmp
     * resource must be present, otherwise (in java2db case) we will create
     * a default one. If it's java2db, we will also parse the CLI overrides,
     * as the cmp resource provides the default values.
     *
     * @param mappedBeans true if beans are mapped in this module.
     * @throws GeneratorException if beans are mapped but cmp resource is not
     * specified.
     */
    private ResourceReferenceDescriptor checkOrCreateCMPResource(
            boolean mappedBeans)
            throws GeneratorException {
        ResourceReferenceDescriptor cmpResource = 
                bundle.getCMPResourceReference();
        if (mappedBeans) {
            if (cmpResource == null) {
                // If mapping exists, the cmpResource must specify a
                // database or a PMF JNDI name.
                throw JDOCodeGeneratorHelper.createGeneratorException( 
                        "EXC_MissingCMPResource", bundle); //NOI18N
            }
        } else {
            if (cmpResource == null) {

                // In JavaToDB case we can deploy to the default jdbc-resource.
                cmpResource = new ResourceReferenceDescriptor();
                cmpResource.setJndiName("jdbc/__default");
                cmpResource.setDatabaseVendorName(DBVendorTypeHelper.DERBY);
                cmpResource.setCreateTablesAtDeploy(true);
                cmpResource.setDropTablesAtUndeploy(true);
                bundle.setCMPResourceReference(cmpResource);
            }
        }
        return cmpResource;
    }

    /**
    * Check that there is a dbschema for each element of the SunCmpMappings.
    * For those which are missing, create a corresponding .dbschema file.
    * @param cmpResource Provides JNDI name for getting database connection
    * @param sunCmpMappings SunCmpMappings which is checked for having schema
    * @param inputFilesPath the directory where this bundle's files are located
    * @param classout the directory where the classes are located 
    * @exception DBException Thrown if database model throws it
    * @exception IOException Thrown if .dbschema file cannot be created.
    * @exception SQLException Thrown if we cannot get get required info from
    * the database.
    */
    private void ensureDBSchemaExistence(
            ResourceReferenceDescriptor cmpResource,
            SunCmpMappings sunCmpMappings, 
            String inputFilesPath,
            File classout)
            throws DBException, SQLException, GeneratorException {

        String generatedSchemaName = getInfoHelper().getSchemaNameToGenerate();
        Set tables = new HashSet();
        int size = sunCmpMappings.sizeSunCmpMapping();

        // Sweep through the mappings to check dbschema existence.  If a
        // mapping does not have a dbschema, get a list of tables to be
        // captured, then capture them, create the corresponding dbschema,
        // and save it.
        for (int i = 0; i < size; i++) {
            SunCmpMapping sunCmpMapping = sunCmpMappings.getSunCmpMapping(i);

            String schemaName = sunCmpMapping.getSchema();
            if (StringHelper.isEmpty(schemaName)) {
                if (!isVerifyFlag) {
                    // The tables in this section need to be captured.
                    addAllTables(sunCmpMapping, tables);
                    sunCmpMapping.setSchema(generatedSchemaName);
                } else {
                    // If it is from verifier, capture schema internally
                    // to perform sun-cmp-mappings.xml and EJB validation
                    getConversionHelper().setEnsureValidation(false);
                }
                 
            } else {
                File dbschemaFile = new File(
                        new StringBuffer(inputFilesPath)
                            .append(File.separator)
                            .append(schemaName)
                            .append(DBSCHEMA_EXTENSION).toString());
                if (! (dbschemaFile.exists()
                       && dbschemaFile.isFile()
                       && dbschemaFile.canRead())) {
                    throw new GeneratorException(
                            I18NHelper.getMessage(
                            messages, "CMG.MissingDBSchema", // NOI18N
                            bundle.getApplication().getRegistrationName(),
                            JDOCodeGeneratorHelper.getModuleName(bundle), 
                            schemaName));
                }
            }
        }

        // If there were tables to be captured, they will be in the list.
        // Now we need to go and capture those tables.
        if (tables.size() > 0) {
            String userSchema = null;
            Connection con = DeploymentHelper.getConnection(cmpResource.getJndiName());
            DatabaseMetaData dmd = con.getMetaData();
            if (DBVendorTypeHelper.requireUpperCaseSchema(dmd)) {
                userSchema = dmd.getUserName().trim().toUpperCase();
            }

            ConnectionProvider cp = new ConnectionProvider(con, dmd.getDriverName().trim());
            if (userSchema != null) {
                cp.setSchema(userSchema);
            }

            OutputStream outstream = null;

            try {
                SchemaElementImpl outSchemaImpl = new SchemaElementImpl(cp);
                SchemaElement schemaElement = new SchemaElement(outSchemaImpl);
                schemaElement.setName(DBIdentifier.create(generatedSchemaName));

                if(dmd.getDatabaseProductName().compareToIgnoreCase("MYSQL") == 0)
                    outSchemaImpl.initTables(cp, new LinkedList(tables), new LinkedList(), true);
                else
                    outSchemaImpl.initTables(cp, new LinkedList(tables), new LinkedList(), false);
                outstream = new FileOutputStream(
                        new File(classout,
                        new StringBuffer(generatedSchemaName)
                            .append(DBSCHEMA_EXTENSION).toString()));

                // XXX Unfortunately, if SchemaElement.save gets an
                // IOException, it prints the stack trace but does not
                // let us handle it :-(
                schemaElement.save(outstream);

            } catch (IOException ex) {
                // Catch FileNotFound, etc.
                throw JDOCodeGeneratorHelper.createGeneratorException( 
                        "CMG.CannotSaveDBSchema", bundle, ex); // NOI18N
            } finally {
                cp.closeConnection();
                try {
                    if (outstream != null) {
                        outstream.close();
                    } 
                } catch (IOException ex) {
                    if (logger.isLoggable(Logger.FINE))
                        logger.fine(ex.toString());
                }
            }
        }
    }

    /**
     * Adds all table names referenced by this <code>SunCmpMapping</code> element
     * to this Set.
     * 
     * @param sunCmpMapping the SunCmpMapping element to check.
     * @param tables the Set to update.
     */
    private void addAllTables(SunCmpMapping sunCmpMapping, Set tables) {
        EntityMapping[] beans = sunCmpMapping.getEntityMapping();
        for (int i = 0; i < beans.length; i++) {
            // Always add the table name.
            addTableName(beans[i].getTableName(), tables);

            // Check if there are table names specified in the
            // cmp-field-mapping.
            CmpFieldMapping[] cmpfields = beans[i].getCmpFieldMapping();
            for (int j = 0; j < cmpfields.length; j++) {
                // There might be more than one column-name for each cmp field.
                String[] names = cmpfields[j].getColumnName();
                for (int jj = 0; jj < names.length; jj++) {
                    addRelatedTableName(names[jj], tables);
                }
            }

            // Check the table names specified in the cmr-field-mapping.
            CmrFieldMapping[] cmrfields = beans[i].getCmrFieldMapping();
            for (int j = 0; j < cmrfields.length; j++) {
                // There might be more than one column-pair for each cmr field.
                ColumnPair[] pairs = cmrfields[j].getColumnPair();
                for (int jj = 0; jj < pairs.length; jj++) {
                    String[] names = pairs[jj].getColumnName();
                    for (int jjj = 0; jjj < names.length; jjj++) {
                        addRelatedTableName(names[jjj], tables);
                    }
                }
            }
        }
    }

    /** 
     * Add a valid (not null and not all spaces) table name to the
     * Set of known table names.
     * 
     * @param name the table name to add if it's a valid name.
     * @param tables the Set to update.
     */
    private void addTableName(String name, Set tables) {
        if (!StringHelper.isEmpty(name)) {
            if (logger.isLoggable(Logger.FINE)){
                logger.fine("Adding Table to Capture Set: " + name); // NOI18N
            }

            tables.add(name);
        }
    }

    /**
     * Adds a table name, if it is specified as a part of the column name,
     * to the Set of known table names.
     *   
     * @param columnName the name of the column to use.
     * @param tables the Set to update.
     */  
    private void addRelatedTableName(String columnName, Set tables) {
        if (!StringHelper.isEmpty(columnName)) {
            int l = columnName.indexOf(DOT);
            if (l > 0) {
                addTableName(columnName.substring(0, l), tables);
            }
        }
    }
}
