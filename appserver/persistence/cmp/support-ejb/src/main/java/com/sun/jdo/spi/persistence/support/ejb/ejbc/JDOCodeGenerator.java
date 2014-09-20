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
 * JDOCodeGenerator.java
 *
 * Created on November 14, 2001
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.ResourceBundle;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.io.DescriptorConstants;
import com.sun.jdo.api.persistence.enhancer.generator.Main;
import com.sun.jdo.api.persistence.mapping.ejb.ConversionException;
import com.sun.jdo.api.persistence.model.Model;
import com.sun.jdo.api.persistence.model.ModelException;
import com.sun.jdo.api.persistence.model.mapping.MappingClassElement;
import com.sun.jdo.api.persistence.support.JDOUserException;
import com.sun.jdo.spi.persistence.generator.database.DDLGenerator;
import com.sun.jdo.spi.persistence.generator.database.DatabaseOutputStream;
import com.sun.jdo.spi.persistence.support.ejb.codegen.CMPGenerator;
import com.sun.jdo.spi.persistence.support.ejb.codegen.GeneratorException;
import com.sun.jdo.spi.persistence.support.ejb.ejbqlc.EJBQLException;
import com.sun.jdo.spi.persistence.support.ejb.enhancer.meta.EJBMetaDataModelImpl;
import com.sun.jdo.spi.persistence.support.ejb.model.DeploymentDescriptorModel;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.DeploymentHelper;
import com.sun.jdo.spi.persistence.support.sqlstore.query.jqlc.JDOQLParameterDeclarationParser;
import com.sun.jdo.spi.persistence.utility.MergedBundle;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.IASEjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.QueryParser;
import org.glassfish.persistence.common.DatabaseConstants;
import org.glassfish.persistence.common.I18NHelper;
import org.netbeans.modules.dbschema.DBException;
import org.netbeans.modules.dbschema.SchemaElement;
import org.netbeans.modules.schema2beans.Schema2BeansException;

//import com.sun.enterprise.deployment.backend.Deployer;

/*
 * This is the JDO specific generator for the concrete CMP beans and any
 * other dependent files.
 *
 * @author Marina Vatkina
 */
public class JDOCodeGenerator implements CMPGenerator, DatabaseConstants {

    /**
     * Signature with CVS keyword substitution for identifying the generated code
     */
    public static final String SIGNATURE = "$RCSfile: JDOCodeGenerator.java,v $ $Revision: 1.7 $"; //NOI18N

    private static final String MAPPING_EXTENSION = ".mapping";      // NOI18N

     /** The logger */
    private static final Logger logger = LogHelperEJBCompiler.getLogger();

     /** The resource bundle used for validation */
    private static final ResourceBundle validationBundle = new MergedBundle(
        I18NHelper.loadBundle(DeploymentDescriptorModel.class), 
        I18NHelper.loadBundle(Model.class));

    private ArrayList<File> files = new ArrayList<File>();

    private DeploymentContext ctx = null;
    private ClassLoader loader = null;
    private JDOConcreteBeanGenerator cmp11Generator;
    private JDOConcreteBeanGenerator cmp20Generator;
    private EjbBundleDescriptorImpl bundle = null;
    private NameMapper nameMapper;
    private Model model;
    private EJBMetaDataModelImpl ejbModel;
    private static QueryParser jdoqlParamDeclParser = new JDOQLParameterDeclarationParser();
    private String inputFilesPath;
    private String generatedXmlsPath;
    private String appName;
    private File classout;
    private MappingGenerator mappingGenerator = null;
        
    /**
     * Flag set to <code>true</code> if code generation should proceed
     * even if model validation fails
     */
    private static final boolean ignoreValidationResults = false;//Deployer.getKeepFailedStubsValue();

    /** String representing the signatures of generic code-gen files */
    private static String signatures = null;

    /**
     * @see CMPGenerator#init(org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl, DeploymentContext, String, String)
     */
    public void init(EjbBundleDescriptorImpl bundle, DeploymentContext ctx,
        String bundlePathName, String generatedXmlsPathName) 
        throws GeneratorException {

        if (logger.isLoggable(Logger.FINE))
            logger.fine("cmp gen init"); // NOI18N

        this.ctx = ctx;
        this.generatedXmlsPath = generatedXmlsPathName;
        this.inputFilesPath = bundlePathName;
        this.classout = ctx.getScratchDir("ejb"); // "generated/ejb" dir
        this.appName = ctx.getModuleMetaData(Application.class).getRegistrationName();

        init(bundle, ctx.getClassLoader(), bundlePathName, false);
    }

    /**
     * @see CMPGenerator#init(org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl, ClassLoader, String)
     *
     * This method should be merged with {@link #init(org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl, DeploymentContext, String)}
     * when TestFramework is fixed for optional pm-descriptors and java2db support.
     *
     * @deprecated
     */
    public void init(EjbBundleDescriptorImpl bundle, ClassLoader loader,
        String bundlePathName) throws GeneratorException {

        init(bundle, loader, bundlePathName, false);
    }

    /**
     * Called by static verifier to bypass mapping validation or
     * internally by other init() methods.
     * Will force java2db generation if <code>ignoreSunDeploymentDescriptors</code>
     * is <code>true</code>.
     *
     * @see CMPGenerator#init(org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl, ClassLoader, String)
     */
    public void init(EjbBundleDescriptorImpl bundle, ClassLoader loader,
            String bundlePathName, boolean ignoreSunDeploymentDescriptors) 
            throws GeneratorException {
        if (logger.isLoggable(Logger.FINE))
            logger.fine("cmp gen init"); // NOI18N

        this.bundle = bundle;
        this.loader = loader;

        inputFilesPath = bundlePathName;

        try {
            nameMapper = new NameMapper(bundle);
            model = new DeploymentDescriptorModel(nameMapper, loader);
            mappingGenerator = new MappingGenerator(bundle, model, nameMapper, loader);
            loadOrCreateMappingClasses(ignoreSunDeploymentDescriptors);

            ejbModel = new EJBMetaDataModelImpl(model);

        } catch (IOException e) {
            // Problems storing properties file(s)
            throw JDOCodeGeneratorHelper.createGeneratorException(
                    "CMG.IOExceptionInInit", bundle, e); //NOI18N
        }
    }

   /**
    * @see CMPGenerator#validate(IASEjbCMPEntityDescriptor descr)
    */
    public Collection validate(IASEjbCMPEntityDescriptor descr) {

        Collection c = new ArrayList();

        c.addAll(validateModel(descr));

        // only do EJBQL validation if the mapping info is present
        if (getMappingMissingException(c) == null)
            c.addAll(validateEJB(descr));

        if (logger.isLoggable(Logger.FINE)) {
            for (Iterator i = c.iterator(); i.hasNext();) {
                logger.log(Logger.FINE, "validation exception: ", //NOI18N
                        (Exception)i.next()); 
            }
        }

        return c;
    }

    /**
     * Validate the mapping file for the given class. First check if 
     * the mapping for the class exists in the sun-cmp-mappings.  If so, 
     * continue to validate using {@link Model#validate(String, ClassLoader, 
     * ResourceBundle)}.  If the resulting collection returned is not empty, 
     * it means it failed the test.
     * The user should edit the mapping information, either in the 
     * IDE or the deploytool, or directly update 
     * the sun-cmp-mapping file.
     *
     * @param descr the IASEjbCMPEntityDescriptor for this CMP bean.
     * @return a Collection of one GeneratorException if the mapping is 
     * totally missing or Collection of ModelException instances 
     * for each found validation error.
     */
    private Collection validateModel(IASEjbCMPEntityDescriptor descr) {
        String beanName = descr.getName();
        String className = nameMapper.getPersistenceClassForEjbName(beanName);

        if (model.getPersistenceClass(className) == null) {
           return Collections.singletonList(
               JDOCodeGeneratorHelper.createGeneratorException(
               "CMG.MissingBeanMapping", beanName, bundle)); //NOI18N
        }

        return model.validate(className, loader, validationBundle);
    }

    /**
     * Checks a collection as returned by 
     * {@link #validateModel(IASEjbCMPEntityDescriptor)} to see if 
     * it contains (only) the {@link GeneratorException} used when the  
     * entire mapping definition is missing.  In that case, we will 
     * want to skip other checks.
     *
     * @param c a collection of validation exceptions as returned by
     * validateModel.
     * @return if the collection contains exactly one GeneratorException
     * return it, otherwise return <code>null</code>
     */
    private GeneratorException getMappingMissingException(Collection c) {
        if (c.size() == 1) {
            Object firstElement = c.iterator().next();

            if (firstElement instanceof GeneratorException)
                return (GeneratorException)firstElement;
        }

        return null;
    }

    /**
     * Validate the bean. For now, validate EJBQL for all finders and 
     * selectors of this bean.
     *   
     * @param descr the IASEjbCMPEntityDescriptor for this CMP bean.
     * @return a Collection of Exception instances for each found
     * validation error.
     */  
    private Collection validateEJB(IASEjbCMPEntityDescriptor descr) {
        Collection c = null;
        try {
            JDOConcreteBeanGenerator cmpGenerator = getCMPGenerator(descr);
            c = cmpGenerator.validate(new MethodHelper(descr), descr.getName());
        } catch (GeneratorException e) {
            c = new ArrayList();
            c.add(e);
        }

        return c;
    }

    /**  
     * Validate if this bean is of a supported type. 
     *   
     * @param descr the IASEjbCMPEntityDescriptor for this CMP bean.
     * @return a Collection of Exception instances for each found
     * validation error.
     */  
    private Collection validateSupported(IASEjbCMPEntityDescriptor descr) {
        Collection rc = new ArrayList();
        /*
         * XXX Add validation of read-only configuration?
         */

        return rc;
    }

    /**
     * @see CMPGenerator#generate(IASEjbCMPEntityDescriptor, File, File)
     */
    public void generate(IASEjbCMPEntityDescriptor ejbcmp, File srcout, 
        File classout) 
        throws GeneratorException {

        String beanName = ejbcmp.getName();

        // StringBuffer to store validation exception messages if there are any.
        // If there are no validation exceptions, the reference will be null.
        StringBuffer validateex = null;
    
        boolean debug = logger.isLoggable(Logger.FINE);
        if (debug)
            logger.fine("gen file in " + srcout.getAbsolutePath()); // NOI18N
    
        // We need to create a new ArrayList because model validation 
        // returns an unmodifiable list.  This may be a place to look 
        // for a performance improvement later for the case of empty 
        // or singleton collection (extra copies).
        Collection c = new ArrayList(validateModel(ejbcmp));

        // if the mapping info is not present, throw the exception and 
        // stop the generation process 
        GeneratorException mappingMissingEx = getMappingMissingException(c);
        if (mappingMissingEx != null)
            throw mappingMissingEx;

        c.addAll(validateSupported(ejbcmp));

        JDOConcreteBeanGenerator cmpGenerator = getCMPGenerator(ejbcmp);
        MethodHelper mh = new MethodHelper(ejbcmp);
        c.addAll(cmpGenerator.validate(mh, beanName));

        if (!c.isEmpty()) {
            // Validation failed the test. We will try to display all the
            // exceptions in a concatenated message and a GeneratorException 
            // is thrown.
            validateex = new StringBuffer();
            Iterator iter = c.iterator();
    
            while (iter.hasNext()) {
                Exception ex = (Exception)iter.next();
                if (debug)
                    logger.log(Logger.FINE,"validation exception: " , ex); //NOI18N
                validateex.append(ex.getMessage()).append('\n'); //NOI18N
            }
    
            if (!ignoreValidationResults)
                throw JDOCodeGeneratorHelper.createGeneratorException(
                        "CMG.ExceptionInValidate", //NOI18N
                        beanName, bundle, validateex.toString());    
        }

        try {
            Collection<File> newfiles = null;
    
            if (!ejbcmp.isEJB20())
                ejbcmp.setQueryParser(jdoqlParamDeclParser);
    
            // IMPORTANT:
            // Concrete impl class generation must happen before generation of 
            // PC class as cmpGenerator will override cascadeDelete (DeleteAction)
            // property if it is set, and generatePC() saves .mapping file.
            newfiles = cmpGenerator.generate(mh, beanName, appName, srcout, classout);
            files.addAll(newfiles);

            newfiles = generatePC(ejbcmp, srcout, classout);
            files.addAll(newfiles);

            if (validateex != null)
                throw JDOCodeGeneratorHelper.createGeneratorException(
                        "CMG.ExceptionInValidate", //NOI18N
                        beanName, bundle, validateex.toString());
        } catch (JDOUserException e) {
            // User error found. Append this exception's message to validation
            // messages if there are any.
            throw JDOCodeGeneratorHelper.createGeneratorException(
                    "CMG.ExceptionInGenerate", //NOI18N
                    beanName, bundle, e, validateex); 

        } catch (EJBQLException e) {
            // EJBQL parsing error found. Append this exception's message to
            // validation messages if there are any.
            throw JDOCodeGeneratorHelper.createGeneratorException(
                    "CMG.ExceptionInGenerate", //NOI18N
                    beanName, bundle, e, validateex); 

        } catch (IOException e) {
            // Problems generating file(s). Append this exception's message to 
            // validation messages if there are any.
            throw JDOCodeGeneratorHelper.createGeneratorException(
                    "CMG.IOExceptionInGenerate", //NOI18N
                    beanName, bundle, e, validateex); 
        }

    }

    /** Generate PC class for the ConcreteImpl bean.
     * @see CMPGenerator#generate(IASEjbCMPEntityDescriptor, File, File)
     */
    private Collection<File> generatePC(IASEjbCMPEntityDescriptor ejbcmp,
        File srcout, File classout)
        throws IOException {

        ArrayList<File> fileList = new ArrayList<File>();
        Main gen = new Main(ejbModel, srcout);
        String className = nameMapper.getPersistenceClassForEjbName(ejbcmp.getName());

        if (className != null) {
            // generate PC class
            //@olsen, 4653156: the enhancer-generator deals with class names
            // in JVM format, i.e., with '/' for '.' as separator
            String jvmClassName = className.replace('.', '/');
            File file = gen.generate(jvmClassName);
            fileList.add(file);

            // write mapping file
            MappingClassElement mappingClass
                = model.getMappingClass(className);
            BufferedOutputStream mapOut = null;
            try {
                String mapPath = className.replace('.', File.separatorChar);
                String mappingFile = mapPath + MAPPING_EXTENSION;
                mapOut = new BufferedOutputStream(
                    new FileOutputStream(new File(classout, mappingFile)));
                //"touch" need to create the output stream first since the
                //classout directory is not in the classpath and
                //therefore the standard storeMappingClass can't be used
                model.storeMappingClass(mappingClass, mapOut);
            } finally {
                if (mapOut != null) {
                    try {
                        mapOut.close();
                    } catch(Exception ex) {
                        if (logger.isLoggable(Logger.FINE))
                            logger.fine(ex.getMessage());  
                    }
                }
            }
            
        }
        return fileList;
    }

   /*
    * @see CMPGenerator#cleanup()
    */
    public Collection cleanup() throws GeneratorException { 
        // Remove the strong references to MappingClassElements
        // needed during deployment. The mapping class cache
        // can now be cleaned up by the garbage collector.
        // It is done by mappingGenerator.
        mappingGenerator.cleanup();

        // Reset cmp generators.
        cmp11Generator = null;
        cmp20Generator = null;

        return files;
    }

    /** Returns CMP bean classes generator of the appropriate type.
     * @param descr the bean descriptor as IASEjbCMPEntityDescriptor.
     * @return instance of JDOConcreteBeanGenerator.
     */
    private JDOConcreteBeanGenerator getCMPGenerator(
            IASEjbCMPEntityDescriptor descr) throws GeneratorException {

        JDOConcreteBeanGenerator cmpGenerator = null;
        try {
            if (descr.isEJB20()) {
                cmpGenerator = getCMP20Generator();
            } else {
                cmpGenerator = getCMP11Generator();
            }

            cmpGenerator.setUpdateable(
                    !descr.getIASEjbExtraDescriptors().isIsReadOnlyBean());

        } catch (IOException e) {
            // Problems reading file(s)
            throw JDOCodeGeneratorHelper.createGeneratorException(
                    "CMG.IOExceptionInInit", bundle, e); //NOI18N
        }

        return cmpGenerator;
    }

   /**
     * Returns instance of generator for CMP2.x beans in this module.
     */
    private JDOConcreteBeanGenerator getCMP20Generator() throws IOException {
        if (cmp20Generator == null) {
            cmp20Generator = new JDOConcreteBean20Generator(loader, model,
                    nameMapper);

            addSignatures(cmp20Generator);
        }

        return cmp20Generator;
    }

    /**
     * Returns instance of generator for CMP1.1 beans in this module.
     */
    private JDOConcreteBeanGenerator getCMP11Generator() throws IOException {
        if (cmp11Generator == null) {
            cmp11Generator = new JDOConcreteBean11Generator(loader, model,
                    nameMapper);

            addSignatures(cmp11Generator);
        }

        return cmp11Generator;
    }

    /**
     * Add required signatures to the generator instance.
     */
    private void addSignatures(JDOConcreteBeanGenerator cmpGenerator) 
            throws IOException {

        // Add the code generation signature of the input files
        // Note, this is per bundle, so it needs to be set per
        // cmpGenerator instance.
        cmpGenerator.addCodeGenInputFilesSignature(getSignaturesOfInputFiles());
    
        // Add the code generation signature of the S1AS-specific generator classes.
        cmpGenerator.addCodeGeneratorClassSignature(
                getS1ASSpecificGeneratorClassesSignature());
    }

    /**
     * Returns the signatures of the classes and properties which are
     * involved in the codegen for S1AS specific part.
     * @return The signatures as a string.
     */
    private static synchronized String 
            getS1ASSpecificGeneratorClassesSignature() 
    {
        if (signatures == null) {
            StringBuffer sb = new StringBuffer().
        
            // adding signature of JDOCodeGenerator
            append(JDOCodeGenerator.SIGNATURE).
            append(CMPTemplateFormatter.signatureDelimiter_).
        
            // adding signature of NameMapper
            append(NameMapper.SIGNATURE);
        
            signatures = sb.toString();       
        }
        return signatures;
    }

    /**
     * Returns the signatures (file length) of the input files for the codegen.
     * Inputfiles are ejb-jar.xml, sun-ejb-jar.xml, sun-cmp-mappings.xml. 
     * @return The signatures as a string.
     * @throws IOException
     */    
    private String getSignaturesOfInputFiles() throws IOException
    {
        StringBuffer sb = new StringBuffer().
                       
            append(getFileInfoOfInputFile(inputFilesPath +
                File.separator + DescriptorConstants.EJB_JAR_ENTRY)).
            append(CMPTemplateFormatter.signatureDelimiter_).
               
            append(getFileInfoOfInputFile(inputFilesPath +
                File.separator + DescriptorConstants.S1AS_EJB_JAR_ENTRY)).
            append(CMPTemplateFormatter.signatureDelimiter_).

            append(getFileInfoOfInputFile(inputFilesPath +
                File.separator + DescriptorConstants.S1AS_CMP_MAPPING_JAR_ENTRY));               
               
        return sb.toString();        
    }

    /**
     * Returns a string consisting of the fully path and name of the input file
     * and its length in bytes.
     * @param pathname The path and file name of the input file.
     * @return A string consisting of the fully path and name of the input file
     * and its length in bytes.
     * @throws IOException
     */
    private String getFileInfoOfInputFile(String pathname) throws IOException
    {   
        File inputFile = new File(pathname);
        
        StringBuffer inputFileInfo = new StringBuffer().
            append(inputFile.getCanonicalPath().replace('\\', '/')).
            append(CMPTemplateFormatter.space_).
            append(String.valueOf(inputFile.length())).
            append(" bytes");

        return inputFileInfo.toString();               
    }

    /**
     * It will load mapping classes if there is sun-cmp-mappings.xml,
     * otherwise it will call database generation backend to create
     * mapping classes and schema.
     * Generates *.dbschema and sun-cmp-mappings.xml in application dir if it is
     * in creating mapping classes mode
     *
     * @param ignoreSunDeploymentDescriptors Will force java2db generation 
     * if <code>true</code>.
     */
    private void loadOrCreateMappingClasses(boolean ignoreSunDeploymentDescriptors) 
        throws IOException, GeneratorException {

        try {
            SchemaElement schema = mappingGenerator.generateMapping(
                    ctx, inputFilesPath, generatedXmlsPath, classout,
                    ignoreSunDeploymentDescriptors);
            // If this is from verify, do not create DDL.
            if (ctx != null 
                    && mappingGenerator.isJavaToDatabase()) {
                createDDLs(schema, mappingGenerator.getDatabaseVendorName(), null);
            }
        } catch (SQLException ex) {
            // Problems talking to the database.
            throw JDOCodeGeneratorHelper.createGeneratorException(
                    "CMG.SQLException", bundle, ex);
        } catch (DBException ex) {
            // Problems reading or creating DBModel.
            throw JDOCodeGeneratorHelper.createGeneratorException(
                    "CMG.DBException", bundle, ex); //NOI18N

        } catch (ModelException ex) {
            // Problems reading or creating model.
            throw JDOCodeGeneratorHelper.createGeneratorException(
                    "CMG.ModelException", bundle, ex); //NOI18N

        } catch (Schema2BeansException ex) {
            // Problems reading or creating sun-cmp-mapping.xml
            throw JDOCodeGeneratorHelper.createGeneratorException(
                    "CMG.Schema2BeansException", bundle, ex); //NOI18N
        } catch (ConversionException ex) {
            // Problems converting between sun-cmp-mappings and mapping model
            throw JDOCodeGeneratorHelper.createGeneratorException(
                    "CMG.MappingConversionException", bundle, ex); //NOI18N
        }
    }

    /**
     * Generates DDLs for specified database vendor name.
     * @param schema a schema representing database in memory
     * @param dbVendorName a string for database vendor name
     * @param conn a database connection
     * @throws IOException
     * @throws DBException
     */
    private void createDDLs(SchemaElement schema, String dbVendorName, Connection conn)
            throws IOException, DBException {

        // Make sure that generatedXmlsPath directory is created
        // before writing *.sql files to this location.
        File fileDir = new File(generatedXmlsPath);

        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }

        // Can't use schema name as at the time of the event listener
        // processing, the name will be recalculated again, and it might
        // not match.
        String namePrefix = DeploymentHelper.getDDLNamePrefix(bundle);

        // sql generation file names.
        OutputStream createDDLSql = new FileOutputStream(new File(generatedXmlsPath,
                namePrefix + NAME_SEPARATOR + dbVendorName + CREATE_SQL_FILE_SUFFIX));
        OutputStream dropDDLSql = new FileOutputStream(new File(generatedXmlsPath,
                namePrefix + NAME_SEPARATOR + dbVendorName + DROP_SQL_FILE_SUFFIX));

        OutputStream dropDDLTxt = new FileOutputStream(new File(classout,
                namePrefix + DROP_DDL_JDBC_FILE_SUFFIX));
        OutputStream createDDLTxt = new FileOutputStream(new File(classout,
                namePrefix + CREATE_DDL_JDBC_FILE_SUFFIX));

        try {
            // XXX This code might be used in the future with the create
            // and drop flags to be set appropriately.
            // If we have a live connection, we'll need to add flags that
            // specify if it's necessary to create and drop tables at deploy.
            // For now it's never executed so we set those booleans to false.
            OutputStream dbStream = null;
            boolean createTablesAtDeploy = false;
            boolean dropTablesAtDeploy = false;
            if ((conn != null) && (createTablesAtDeploy || dropTablesAtDeploy)) {
                dbStream = new DatabaseOutputStream(conn);
            }
            // XXX This is the end of the code that might be used in the future.

            DDLGenerator.generateDDL(schema, dbVendorName, createDDLSql,
                dropDDLSql, dropDDLTxt, createDDLTxt, dbStream, dropTablesAtDeploy);
        } catch (SQLException ex) {
            if (logger.isLoggable(Logger.WARNING))
                logger.warning(ex.toString());
        }
    }

}
