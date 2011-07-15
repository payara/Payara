/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * JDOConcreteBeanGenerator.java
 *
 * Created on November 20, 2001
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbc;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;

import com.sun.jdo.api.persistence.model.Model;
import com.sun.jdo.api.persistence.model.jdo.*;
import com.sun.jdo.api.persistence.model.mapping.MappingClassElement;
import com.sun.jdo.api.persistence.model.mapping.MappingFieldElement;

import com.sun.jdo.spi.persistence.support.ejb.model.DeploymentDescriptorModel;
import com.sun.jdo.spi.persistence.support.ejb.model.util.NameMapper;
import com.sun.jdo.spi.persistence.support.ejb.ejbqlc.JDOQLElements;

import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.spi.persistence.utility.StringHelper;
import com.sun.jdo.spi.persistence.utility.generator.*;
import com.sun.jdo.spi.persistence.utility.generator.io.*;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
 
/*
 * This is the base class for JDO specific generator for the concrete CMP 
 * beans.
 *
 * @author Marina Vatkina
 */
abstract class JDOConcreteBeanGenerator {

    static final Logger logger = LogHelperEJBCompiler.getLogger();

    // used to transform b/w various ejb names and pc class names
    NameMapper nameMapper = null;

    Model model = null;        // used to look up various metadata

    String appName = null;
    String beanName = null;
    String helperName = null;
    String concreteImplName = null;
    String abstractBean = null;
    String pkClass = null;
    String pcname = null;
    boolean hasLocalInterface;
    boolean hasRemoteInterface;
    boolean isUpdateable;

    String setPKField = null;

    // Name of the PC or PK Class as a single String parameter.
    String[] pcnameParam = new String[1];
    String[] pkClassParam = new String[1];

    static String[] objectType = new String[]{CMPTemplateFormatter.Object_};
    static String[] param0 = new String[]{CMPTemplateFormatter.param0_};
    static String[] param0PM = new String[] {CMPTemplateFormatter.param0_,
                    CMPTemplateFormatter.jdoPersistenceManager_};

    // Generic parameters
    String[] oneParam = new String[1];
    String[] twoParams = new String[2];
    String[] threeParams = new String[3];
    String[] fourParams = new String[4];
    String[] fiveParams = new String[5];
    String[] sixParams = new String[6];

    String[] queryParams = new String[10];

    // StringBuffer for loading nonDFG fields into read-only beans
    StringBuffer loadNonDFGBody = null;

    ClassLoader loader;

    // Writer for the concrete bean.
    JavaClassWriter concreteImplWriter;

    // Writer for the static _JDOHelper class.
    JavaClassWriter jdoHelperWriter;

    /**
     * I18N message handler
     */
    final static ResourceBundle messages = I18NHelper.loadBundle(
        JDOConcreteBeanGenerator.class);

    /** Name of the SUPPORT_TRAILING_SPACES_IN_VARCHAR_PK_COLUMNS property. */
    public static final String SUPPORT_TRAILING_SPACES_IN_STRING_PK_COLUMNS_PROPERTY =
        "com.sun.jdo.spi.persistence.support.ejb.ejbc.SUPPORT_TRAILING_SPACES_IN_STRING_PK_COLUMNS"; // NOI18N

    /**
     * Property to swich on/off support for trailing spaces for pk of String types. Note, the default is false, meaning
     * we trip trailing spaces in pk columns
     */
    private static final boolean SUPPORT_TRAILING_SPACES_IN_STRING_PK_COLUMNS = Boolean.valueOf(
        System.getProperty(SUPPORT_TRAILING_SPACES_IN_STRING_PK_COLUMNS_PROPERTY, "false")).booleanValue(); // NOI18N


    /**
     * Signature of the input files. 
     */
    String inputFilesSignature;

    /**
     * Signature of the generator classes for the codegen. 
     */
    String generatorClassesSignature;    

    /**
     * Signature with CVS keyword substitution for identifying the generated code
     */
    static final String SIGNATURE = "$RCSfile: JDOConcreteBeanGenerator.java,v $ $Revision: 1.3 $"; //NOI18N
    
    JDOConcreteBeanGenerator(ClassLoader loader,
                             Model model,
                             NameMapper nameMapper)
                             throws IOException {
        this.loader = loader;
        this.model = model;
        this.nameMapper = nameMapper;

        CMPTemplateFormatter.initHelpers();
        CMPROTemplateFormatter.initHelpers();
    }
    
    void setUpdateable(boolean updateable) {
        isUpdateable = updateable;
    }

    /**
     * Validate this CMP bean. To be overridden in subclass if necessary.
     * No generic validation is done at this time.
     * @param methodHelper the AbstractMethodHelper instance that contains
     * all categorized methods and some other convenience methods for this bean.
     * @param beanName the ejb name for this bean.
     * @return a Collection of Exception instances with a separate instance for 
     * each failed validation. This implementation returns an empty collection 
     * because generic validation always succeeds.
     */
    Collection validate(AbstractMethodHelper methodHelper, String beanName) {
        return new ArrayList();
    }

    /**
     * Generate all required classes for this CMP bean. 
     * @param methodHelper the AbstractMethodHelper instance that contains
     * all categorized methods and some other convenience methods for this bean.
     * @param beanName the ejb-name of this bean.
     * @param appName the name of the application that contains this bean.
     * @param srcout that path to the generated source files.
     * @param classout that path to the compiled class files.
     * @return a Collection of generated source files.
     */
    Collection<File> generate(AbstractMethodHelper methodHelper, 
                       String beanName, String appName, 
                       File srcout, File classout)
                       throws IOException {

        Collection<File> files = new ArrayList<File>();

        this.beanName = beanName;
        this.appName = appName;
        this.abstractBean = nameMapper.getAbstractBeanClassForEjbName(beanName);

        String pkgName = CMPTemplateFormatter.getPackageName(abstractBean);
        concreteImplName = nameMapper.getConcreteBeanClassForEjbName(beanName);
        String shortCmpName = CMPTemplateFormatter.getShortClassName(concreteImplName);

        pcname = nameMapper.getPersistenceClassForEjbName(beanName);
        pcnameParam[0] = pcname;

        PersistenceClassElement pcClassElement =
            model.getPersistenceClass(pcname);

        pkClass = nameMapper.getKeyClassForEjbName(beanName).
            replace('$', '.');
        pkClassParam[0] = pkClass;

        PersistenceFieldElement[] allFields = pcClassElement.getFields();

        String prefix = srcout.getPath() + File.separator + 
            concreteImplName.replace('.', File.separatorChar);
    
        String cmp_file_name = prefix + CMPTemplateFormatter.javaExtension_;
    
        String hlp_file_name = prefix + CMPTemplateFormatter.Helper_ + 
            CMPTemplateFormatter.javaExtension_;

        hasLocalInterface =
            (nameMapper.getLocalInterfaceForEjbName(beanName) != null);
        hasRemoteInterface =
            (nameMapper.getRemoteInterfaceForEjbName(beanName) != null);

        if (logger.isLoggable(Logger.FINE)) {
            logger.fine("allFields: " +                              // NOI18N
                ((allFields != null) ? allFields.length : 0));
            logger.fine("cmp_file_name: " + cmp_file_name); // NOI18N
            logger.fine("hlp_file_name: " + hlp_file_name); // NOI18N
            logger.fine("cmp_name: " + concreteImplName); // NOI18N
            logger.fine("pkClass: " + pkClass); // NOI18N
            logger.fine("PCname: " + pcname); // NOI18N
        }

        File cmp_file = new File(cmp_file_name);
        JavaFileWriter concreteImplFileWriter = new IOJavaFileWriter(cmp_file);
        concreteImplWriter = new IOJavaClassWriter();

        File hlp_file = new File(hlp_file_name);
        JavaFileWriter helperFileWriter = new IOJavaFileWriter(hlp_file);
        jdoHelperWriter = new IOJavaClassWriter();

        // Add package statement to both classes.
        if (pkgName != null && pkgName.length() > 0) {
            concreteImplFileWriter.setPackage(pkgName, null);
            helperFileWriter.setPackage(pkgName, null);
        }

        // Add imports statements to both classes.
        addImportStatements(concreteImplFileWriter, helperFileWriter);

        // Generate class name for the concrete impl.
        oneParam[0] = CMPTemplateFormatter.cmpImplCommentsTemplate;
        concreteImplWriter.setClassDeclaration(Modifier.PUBLIC, 
            shortCmpName, oneParam);

        // Add interfaces to the class declarations.
        addInterfaces();

        concreteImplWriter.setSuperclass(abstractBean);

        // Add no-arg constructor.
        concreteImplWriter.addConstructor(shortCmpName, 
            Modifier.PUBLIC, null, null, null,
            CMPTemplateFormatter.super_, null);

        // Add helper class.
        helperName = shortCmpName + CMPTemplateFormatter.Helper_;

        oneParam[0] = shortCmpName;
        jdoHelperWriter.setClassDeclaration(Modifier.PUBLIC,
            helperName, CMPTemplateFormatter.getBodyAsStrings(
                CMPTemplateFormatter.hcomformatter.format(oneParam)));

        setHelperSuperclass();

        // Print internal variables.
        generateFields();

        // Generate type specific methods.
        generateTypeSpecificMethods(allFields, methodHelper);

        // Add finders for all types and selectors for CMP2.0 only.
        generateFinders(methodHelper);

        // Add ejbCreate<XXX> methods.
        generateCreateMethods(methodHelper.getCreateMethods());

        // Add other required methods.
        generateKnownMethods(methodHelper);

        // Add helper methods for the helper class.
        generateHelperClassMethods();

        // Add conversion methods to the helper class.
        generateConversions();

        // Add ObjectId/PrimaryKey conversion methods.
        generatePKObjectIdConversion(getKeyFields(allFields));

        // Print end of classes.
        concreteImplFileWriter.addClass(concreteImplWriter);
        concreteImplFileWriter.save();

        helperFileWriter.addClass(jdoHelperWriter);
        helperFileWriter.save();

        files.add(cmp_file);
        files.add(hlp_file);

        return files;
    }

    /** Add import statements for for the generated classes.
     */
    void addImportStatements(JavaFileWriter concreteImplFileWriter,
            JavaFileWriter helperFileWriter) throws IOException {

        String[] st = CMPTemplateFormatter.importsArray;
        for (int i = 0; i < st.length; i++) {
            concreteImplFileWriter.addImport(st[i], null);
        }

        st = CMPTemplateFormatter.helperImportsArray;
        for (int i = 0; i < st.length; i++) {
            helperFileWriter.addImport(st[i], null);
        }
    }

    /** 
     * Add interfaces to the class declarations.
     */
    void addInterfaces() throws IOException {

        String[] st = CMPTemplateFormatter.interfacesArray;
        for (int i = 0; i < st.length; i++) {
            concreteImplWriter.addInterface(st[i]);
        }
    }

    /**
     * Super class for the helper class is type specific.
     */
    abstract void setHelperSuperclass() throws IOException;

    /**
     * Generate type specific methods for setters, getters,
     * and any other methods that are completely different
     * between bean types.
     */
    void generateTypeSpecificMethods(
            PersistenceFieldElement[] allFields,
            AbstractMethodHelper methodHelper) 
            throws IOException {

        // Initialize loadNonDFGBody for preloading non-DFG fields
        // in read-only beans.
        if (isUpdateable) {
            loadNonDFGBody = null;
        } else {
            loadNonDFGBody = new StringBuffer();
        }
    }

    /**
     * Sets the signature of the input files for the codegen.
     * @param newSignature The signature of the input files.
     */
    void addCodeGenInputFilesSignature(String newSignature)
    {
        if ((inputFilesSignature == null) || 
            (inputFilesSignature.length() == 0)) {
            inputFilesSignature = newSignature;
        }
        else {
            inputFilesSignature = 
                inputFilesSignature + 
                CMPTemplateFormatter.signatureDelimiter_ + 
                newSignature;
        }
    }    
    
    /**
     * Sets the signature of the generator classes for codegen.
     * @param newSignature The signature of the generator classes.
     */
    void addCodeGeneratorClassSignature(String newSignature)
    {
        if ((generatorClassesSignature == null) || 
            (generatorClassesSignature.length() == 0)) {
            generatorClassesSignature = newSignature;
        }
        else {
            generatorClassesSignature = 
                generatorClassesSignature +
                CMPTemplateFormatter.signatureDelimiter_ +
                newSignature;     
        }
    }

    /**
     * Generates required internal variables.
     */
    void generateFields() throws IOException {

        // Add private transient fields:
        CMPTemplateFormatter.addPrivateField(
            CMPTemplateFormatter.privatetransientvformatter.format(pcnameParam),
            Modifier.TRANSIENT, 
            concreteImplWriter);

        // Add private static fields:
        CMPTemplateFormatter.addPrivateField(
            CMPTemplateFormatter.privateStaticVariablesTemplate,
            Modifier.STATIC, 
            concreteImplWriter);

        // Add private static final fields:
        twoParams[0] = pcname;
        twoParams[1] = beanName;
        CMPTemplateFormatter.addPrivateField(
            CMPTemplateFormatter.privatestaticfinalvformatter.format(twoParams),
            Modifier.STATIC + Modifier.FINAL, 
            concreteImplWriter);

        // Add public static final variables for signatures
        twoParams[0] = generatorClassesSignature;
        twoParams[1] = inputFilesSignature;
        CMPTemplateFormatter.addFields(
             CMPTemplateFormatter.publicstaticfinalvformatter.format(twoParams),
             Modifier.PUBLIC + Modifier.STATIC + Modifier.FINAL,
             concreteImplWriter);
        
        // The static fields holding the Query variables and their monitors
        // are generated during finder/selector method handling.

        CMPTemplateFormatter.addPrivateField(
            CMPTemplateFormatter.otherVariablesTemplate,
            0, concreteImplWriter);

        threeParams[0] = concreteImplName;
        threeParams[1] = beanName;
        threeParams[2] = appName;
        CMPTemplateFormatter.addPrivateField(
            CMPTemplateFormatter.hvformatter.format(threeParams),
            Modifier.TRANSIENT+Modifier.STATIC, 
            jdoHelperWriter);

        // Add read-only fields:
        if (!isUpdateable) {
            // private transient fields:
            CMPTemplateFormatter.addPrivateField(
                CMPROTemplateFormatter.privatetransientvformatter.format(pcnameParam),
                Modifier.TRANSIENT, 
                concreteImplWriter);

            // private static fields:
            CMPTemplateFormatter.addPrivateField(
                CMPROTemplateFormatter.privateStaticFinalVariablesTemplate,
                Modifier.STATIC + Modifier.FINAL, 
                concreteImplWriter);
        }

    }

    /** Adds ejbFindBy methods.
     */
    void generateFinders(AbstractMethodHelper methodHelper) 
            throws IOException {

        boolean debug = logger.isLoggable(Logger.FINE);
        List finders = methodHelper.getFinders();
        for (int i = 0; i < finders.size(); i++) {
            Method m = (Method)finders.get(i);
            String mname = CMPTemplateFormatter.ejb_ +
                StringHelper.getCapitalizedString(m.getName());

            if (debug) {
                logger.fine("Finder: " + mname); // NOI18N
            }

            if (mname.equals(CMPTemplateFormatter.ejbFindByPrimaryKey_)) {
                // ejbFindByPrimaryKey
                String[] exceptionTypes =
                    CMPTemplateFormatter.getExceptionNames(m);

                oneParam[0] = CMPTemplateFormatter.key_;

                concreteImplWriter.addMethod(CMPTemplateFormatter.ejbFindByPrimaryKey_, // name
                    Modifier.PUBLIC , // modifiers
                    pkClass, // returnType
                    oneParam, // parameterNames
                    pkClassParam, //parameterTypes
                    exceptionTypes,// exceptions
                    CMPTemplateFormatter.ejbFindByPrimaryKeyBody, //body
                    null);// comments

            } else {
                JDOQLElements rs = getJDOQLElements(m, methodHelper);

                // check for single-object finder vs. multi-object finder
                String returnType = isSingleObjectFinder(m) ?
                    pkClass : m.getReturnType().getName();
                CMPTemplateFormatter.addGenericMethod(
                    m, mname, returnType,
                    generateFinderMethodBody(methodHelper, rs, mname, m, returnType, i), 
                    concreteImplWriter);
            }
        }


    }

    /** Returns JDOQLElements instance for this finder method.
     * @param m the finder method as a java.lang.reflect.Method
     * @param methodHelper the AbstractMethodHelper instance that contains
     * all categorized methods and some other convenience methods for this bean.
     * @return JDOQLElements instance.
     */
    abstract JDOQLElements getJDOQLElements(Method m,
            AbstractMethodHelper methodHelper) throws IOException;

    /** Adds ejbCreate<XXX> methods.
     */
    private void generateCreateMethods(List createMethods) throws IOException {
        Class beanClass = null;
        try {
            beanClass = Class.forName(abstractBean, true, loader);
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }

        // Store generated getters to avoid duplicates.
        HashSet generated = new HashSet();

        for (int i = 0; i < createMethods.size(); i++) {
            Method m = (Method)createMethods.get(i);
            Method m1 = m;

            // Method name is ejbCreate<Method>
            String createName =CMPTemplateFormatter.ejbCreate_;
            String postCreateName =CMPTemplateFormatter.ejbPostCreate_;
            if (m.getName().length() > 6) {
                String suffix = m.getName().substring(6);
                createName += suffix;
                postCreateName += suffix;
            }

            boolean debug = logger.isLoggable(Logger.FINE);
            if (debug) {
                logger.fine("CreateMethod: " + abstractBean + "" + m.getName()); // NOI18N
                logger.fine("ejbCreateMethod: " + createName); // NOI18N
                logger.fine("ejbPostCreateMethod: " + postCreateName); // NOI18N
            }

            // Get actual method in the bean and resolve exception type to generate...
            try {
                Class[] params = m.getParameterTypes();

                // This is a work around the case when the parameter class loader
                // differs from the given class loader ("loader").
                for (int j = 0; j < params.length; j++) {
                    if (params[j].isPrimitive() ||
                        params[j].getClassLoader() == null ||
                        params[j].getClassLoader().equals(loader)) {
                        continue;
                    }
                    String pname = params[j].getName();

                    if (debug) {
                        logger.fine("Replacing parameter class for: " + pname); // NOI18N
                        logger.fine("Param ClassLoader: " + params[j].getClassLoader());
                        logger.fine("Need ClassLoader: " + loader);
                    }

                    params[j] = Class.forName(pname, true, loader);
                }
                // End of the work around the class loader problem.

                // Cannot use getDeclaredMethod() as the actual method can be in a
                // superclass.
                m = beanClass.getMethod(createName, params);
                m1 = beanClass.getMethod(postCreateName, params);
                if (generated.contains(m)) {
                    // Called from more than one interface - skip it.
                    if (debug) {
                        logger.fine("...generated..."); // NOI18N
                    }

                    continue;

                }
                generated.add(m);

            } catch (Exception e) {
                // method does not exist as ejbCreateXxx. It was a business method.
                continue;
            }

            String[] exc = CMPTemplateFormatter.getExceptionNames(m);
            String parametersList = CMPTemplateFormatter.getParametersList(m);
            String parametersListWithSeparator = makeLiteral(
                    CMPTemplateFormatter.getParametersListWithSeparator(
                            m, CMPTemplateFormatter.paramConcatenator_ ));

            String body = getEJBCreateMethodBody(createName, exc, 
                    parametersList, parametersListWithSeparator);

            CMPTemplateFormatter.addGenericMethod(
                    m, createName, pkClass, body, concreteImplWriter);

            body = getEJBPostCreateMethodBody(postCreateName,
                    parametersList, parametersListWithSeparator);

            CMPTemplateFormatter.addGenericMethod(
                    m1, postCreateName, CMPTemplateFormatter.void_, 
                    body, concreteImplWriter);
        }
    }

    /** Returns method body for EJBCreate method.
     * @param createName the actual name of the method as String.
     * @param exc a String[] of decleared exceptions for this method.
     * @param parametersList the list of method parameters as String.
     * @param parametersListWithSeparator the list of concatenated method 
     * parameters to be passed to another method as String.
     * @return method body as String.
     */
    abstract String getEJBCreateMethodBody(String createName,
            String[] exc, String parametersList,
            String parametersListWithSeparator);

    /** Returns method body for EJBPostCreate method.
     * @param postCreateName the actual name of the method as String.
     * @param parametersList the list of method parameters as String.
     * @param parametersListWithSeparator the list of concatenated method 
     * parameters to be passed to another method as String.
     * @return method body as String.
     */
    abstract String getEJBPostCreateMethodBody(String postCreateName,
            String parametersList, String parametersListWithSeparator);

    /** Returns method body for EJBRemove method.
     * @return method body as String.
     */
    abstract String getEJBRemoveMethodBody();

    /** Adds other known required methods 
     * - CMPTemplateFormatter.commonPublicMethods_
     * - CMPTemplateFormatter.commonPrivateMethods
     * - Other generic methods
     * - Special methods that differ between special types (e.g. read-only
     * beans) to be overridden if necessary
     * - CMPTemplateFormatter.otherPublicMethods_ that differ 
     * between CMP 1.1 and 2.x types are added in subclasses.
     */
    void generateKnownMethods(AbstractMethodHelper methodHelper)
                       throws IOException {

        String[] exc = null;
        String[] st = CMPTemplateFormatter.commonPublicMethodsArray;
        for (int i = 0; i < st.length; i++) {
            String mname = st[i];
            exc = getExceptionList(methodHelper, mname);

            String body = null;
            if (mname.equals(CMPTemplateFormatter.ejbRemove_)) {
                body = getEJBRemoveMethodBody();

            } else if (mname.equals(CMPTemplateFormatter.ejb__flush_)) {
                oneParam[0] = CMPTemplateFormatter.DuplicateKeyException_;
                exc = oneParam;
                body = CMPTemplateFormatter.helpers.getProperty(mname);

            } else {
                body = CMPTemplateFormatter.helpers.getProperty(mname);
            }

            concreteImplWriter.addMethod(mname, // name
                Modifier.PUBLIC, // modifiers
                CMPTemplateFormatter.void_, // returnType
                null, // parameterNames
                null,// parameterTypes
                exc,// exceptions
                CMPTemplateFormatter.getBodyAsStrings(body), // body
                null);// comments

        }

        // This is a cleanup method that is public, but has int param.
        oneParam[0] = CMPTemplateFormatter.int_;
        concreteImplWriter.addMethod(CMPTemplateFormatter.afterCompletion_, // name
            Modifier.PUBLIC, // modifiers
            CMPTemplateFormatter.void_, // returnType
            param0, // parameterNames
            oneParam,// parameterTypes
            null,// exceptions
            CMPTemplateFormatter.afterCompletionBody, // body
            null);// comments

        concreteImplWriter.addMethod(CMPTemplateFormatter.ejb__remove_, // name
            Modifier.PUBLIC , // modifiers
            CMPTemplateFormatter.void_, // returnType
            param0, // parameterNames
            objectType, //parameterTypes
            null,// exceptions
            // body is not defined for this method.
            null, // body
            null);// comments

        String body;
        st = CMPTemplateFormatter.commonPrivateMethodsArray;

        for (int i = 0; i < st.length; i++) {
            String mname = st[i];
            body = CMPTemplateFormatter.helpers.getProperty(mname);

            CMPTemplateFormatter.addGenericMethod(mname, 
                CMPTemplateFormatter.getBodyAsStrings(body), concreteImplWriter);
        }

        // Add jdoArrayCopy to return byte[].
        oneParam[0] = CMPTemplateFormatter.byte_;
        body = CMPTemplateFormatter.jdoarraycopyformatter.format(oneParam);

        oneParam[0] = CMPTemplateFormatter.byteArray_;
        concreteImplWriter.addMethod(
            CMPTemplateFormatter.jdoArrayCopy_, // name
            Modifier.PRIVATE, // modifiers
            CMPTemplateFormatter.byteArray_, // returnType
            param0, // parameterNames
            oneParam,// parameterTypes
            null,// exceptions
            CMPTemplateFormatter.getBodyAsStrings(body), // body
            null);// comments

        // Add setEntityContext
        oneParam[0] = CMPTemplateFormatter.EntityContext_;
        concreteImplWriter.addMethod(CMPTemplateFormatter.setEntityContext_, // name
            Modifier.PUBLIC, // modifiers
            CMPTemplateFormatter.void_, // returnType
            param0, // parameterNames
            oneParam,// parameterTypes
            getExceptionList(methodHelper,
                CMPTemplateFormatter.setEntityContext_,
                oneParam),// exceptions
            CMPTemplateFormatter.setEntityContextBody, // body
            null);// comments

        // Add jdoGetObjectId
        oneParam[0] = CMPTemplateFormatter.key_;

        String[] param = new String[]{concreteImplName};

        concreteImplWriter.addMethod(CMPTemplateFormatter.getObjectId_, // name
            Modifier.PRIVATE , // modifiers
            CMPTemplateFormatter.Object_, // returnType
            oneParam, // parameterNames
            pkClassParam,// parameterTypes
            null,// exceptions
            CMPTemplateFormatter.getBodyAsStrings(
                CMPTemplateFormatter.goidformatter.format(param)), // body
            null);// comments

        // Add jdoGetJdoInstanceClass
        oneParam[0] = CMPTemplateFormatter.jdoGetJdoInstanceClassTemplate;

        CMPTemplateFormatter.addGenericMethod(
                CMPTemplateFormatter.jdoGetJdoInstanceClass_, 
                Modifier.PUBLIC + Modifier.STATIC,
                CMPTemplateFormatter.Class_, oneParam,
                concreteImplWriter);

        generateSpecialKnownMethods();
    }

    /** Adds required methods that differ between special types (e.g. read-only
     * beans) to be overridden if necessary;
     */
    void generateSpecialKnownMethods() throws IOException {

        String[] body = null;

        // For the following methods the method body exists only for
        // updateable beans

        // assertPersistenceManagerIsNull 
        if (isUpdateable) {
            body = CMPTemplateFormatter.assertPersistenceManagerIsNullBody;
        }
        CMPTemplateFormatter.addGenericMethod(
                CMPTemplateFormatter.assertPersistenceManagerIsNull_, 
                body, concreteImplWriter);

        // assertInTransaction
        if (isUpdateable) {
            oneParam[0] = I18NHelper.getMessage(messages, "EXC_TransactionNotActive"); // NOI18N
            body = CMPTemplateFormatter.getBodyAsStrings(
                CMPTemplateFormatter.intxformatter.format(oneParam));
        }
        CMPTemplateFormatter.addGenericMethod(
                CMPTemplateFormatter.assertInTransaction_, 
                body, concreteImplWriter);

        // For the following methods the method body exists for
        // all bean types

        // jdoClosePersistenceManager 
        if (isUpdateable) {
            body = CMPTemplateFormatter.jdoClosePersistenceManagerBody;
        } else {
            body = CMPROTemplateFormatter.jdoClosePersistenceManagerBody;
        }

        CMPTemplateFormatter.addGenericMethod(
                CMPTemplateFormatter.jdoClosePersistenceManager_, 
                body, concreteImplWriter);

        // Add jdoGetPersistenceManager as method returning PersistenceManager:
        if (isUpdateable) {
            body = CMPTemplateFormatter.jdoGetPersistenceManagerBody;
        } else {
            body = CMPROTemplateFormatter.jdoGetPersistenceManagerBody;
        }
        CMPTemplateFormatter.addGenericMethod(
                CMPTemplateFormatter.jdoGetPersistenceManager_, 
                CMPTemplateFormatter.jdoPersistenceManagerClass_,
                body, concreteImplWriter);

        // Add methods that use PK as the argument 
        oneParam[0] = CMPTemplateFormatter.key_;
        if (isUpdateable) {
            body = CMPTemplateFormatter.jdoGetPersistenceManager0Body;
        } else {
            body = CMPROTemplateFormatter.jdoGetPersistenceManager0Body;

            // Also add jdoGetPersistenceManagerByPK for RO beans only as method returning 
            // PersistenceManager for this PK:
            concreteImplWriter.addMethod(CMPROTemplateFormatter.jdoGetPersistenceManagerByPK_, // name
                    Modifier.PUBLIC , // modifiers
                    CMPTemplateFormatter.jdoPersistenceManagerClass_, // returnType
                    oneParam, // parameterNames
                    pkClassParam, //parameterTypes
                    null,// exceptions
                    CMPROTemplateFormatter.jdoGetPersistenceManagerByPKBody, //body
                    null);// comments
        }

        concreteImplWriter.addMethod(CMPTemplateFormatter.jdoGetPersistenceManager0_, // name
                Modifier.PUBLIC , // modifiers
                CMPTemplateFormatter.jdoPersistenceManagerClass_, // returnType
                oneParam, // parameterNames
                pkClassParam, //parameterTypes
                null,// exceptions
                body, // body
                null);// comments

        // Add jdoLookupPersistenceManagerFactory as static synchronized method:
        oneParam[0] = concreteImplName;
        MessageFormat mformat = null;
        if (isUpdateable) {
            mformat = CMPTemplateFormatter.jdolookuppmfformatter;
        } else {
            mformat = CMPROTemplateFormatter.jdolookuppmfformatter;
        }
        concreteImplWriter.addMethod(
                CMPTemplateFormatter.jdoLookupPersistenceManagerFactory_, 
                Modifier.PRIVATE + Modifier.STATIC + Modifier.SYNCHRONIZED, // modifiers
                CMPTemplateFormatter.void_, // returnType
                param0, // parameterNames
                objectType, // parameterTypes
                null,// exceptions
                CMPTemplateFormatter.getBodyAsStrings(mformat.format(oneParam)), 
                null);// comments

        // Add jdoGetInstance
        threeParams[0] = pkClass;
        threeParams[1] = pcname;
        threeParams[2] = CMPTemplateFormatter.none_; // will be ignored for updateable beans
        if (isUpdateable) {
            mformat = CMPTemplateFormatter.giformatter;
        } else {
            if (loadNonDFGBody != null) {
                threeParams[2] = loadNonDFGBody.toString();
            }
            mformat = CMPROTemplateFormatter.giformatter;
        }
        CMPTemplateFormatter.addGenericMethod(
                CMPTemplateFormatter.getInstance_, 
                CMPTemplateFormatter.getBodyAsStrings(mformat.format(threeParams)), 
                concreteImplWriter);

        // These are methods that do have arguments.

        // ejb__refresh has method body only for read-only beans
        if (isUpdateable) {
            body = null;
        } else {
            // Reuse threeParams from getInstance_
            body = CMPTemplateFormatter.getBodyAsStrings(
                CMPROTemplateFormatter.ejb__refreshformatter.format(threeParams));
        }
        concreteImplWriter.addMethod(CMPTemplateFormatter.ejb__refresh_, // name
            Modifier.PUBLIC , // modifiers
            CMPTemplateFormatter.void_, // returnType
            param0, // parameterNames
            objectType, //parameterTypes
            null,// exceptions
            body, // body
            null);// comments

        // Add jdoReleasePersistenceManager as method 
        // with PersistenceManager as a param:
        oneParam[0] = CMPTemplateFormatter.jdoPersistenceManagerClass_;
        concreteImplWriter.addMethod(CMPTemplateFormatter.jdoReleasePersistenceManager_, // name
            Modifier.PRIVATE, // modifiers
            CMPTemplateFormatter.void_, // returnType
            param0, // parameterNames
            oneParam,// parameterTypes
            null,// exceptions
            CMPTemplateFormatter.jdoReleasePersistenceManagerBody, // body
            null);// comments

        // Add jdoReleasePersistenceManager0 as method 
        // with PersistenceManager as a param that is different 
        // between updateable and read-only beans:
        if (isUpdateable) {
            body = CMPTemplateFormatter.jdoReleasePersistenceManagerBody;
        } else {
            body = CMPROTemplateFormatter.jdoReleasePersistenceManager0Body;
        }
        concreteImplWriter.addMethod(CMPTemplateFormatter.jdoReleasePersistenceManager0_, // name
            Modifier.PRIVATE, // modifiers
            CMPTemplateFormatter.void_, // returnType
            param0, // parameterNames
            oneParam,// parameterTypes
            null,// exceptions
            body, // body
            null);// comments

    }

    /**
     * Generates helper methods for the helper class.
     */
    void generateHelperClassMethods() throws IOException {
        // Add Helper.assertInstanceOfRemoteInterfaceImpl() method for all beans.
        oneParam[0] = CMPTemplateFormatter.assertInstanceOfRemoteInterfaceImplTemplate;

        jdoHelperWriter.addMethod(CMPTemplateFormatter.assertInstanceOfRemoteInterfaceImpl_, // name
            Modifier.PUBLIC, // modifiers
            CMPTemplateFormatter.void_, // returnType
            param0, // parameterNames
            objectType,// parameterTypes
            null,// exceptions
            oneParam, // body
            null);// comments

        // Add Helper.getHelperInstance() method.
        oneParam[0] = CMPTemplateFormatter.getHelperInstanceTemplate;
        CMPTemplateFormatter.addGenericMethod(
                CMPTemplateFormatter.getHelperInstance_, 
                Modifier.PUBLIC + Modifier.STATIC,
                helperName, oneParam,
                jdoHelperWriter);

        // Add Helper.getContainer() method.
        CMPTemplateFormatter.addGenericMethod(
                CMPTemplateFormatter.getContainer_, 
                Modifier.PUBLIC, CMPTemplateFormatter.Object_, 
                CMPTemplateFormatter.getContainerBody,
                jdoHelperWriter);

        // Add getPCCLass to the helper class
        oneParam[0] = concreteImplName;
        CMPTemplateFormatter.addGenericMethod(
                CMPTemplateFormatter.getPCClass_,
                Modifier.PUBLIC, CMPTemplateFormatter.Class_,
                CMPTemplateFormatter.getBodyAsStrings(
                    CMPTemplateFormatter.pcclassgetterformatter.format(
                    oneParam)),
                jdoHelperWriter);
    }

    private String[] getKeyFields(PersistenceFieldElement[] fields) {
        List returnList = new ArrayList();
        int i, count = ((fields != null) ? fields.length : 0);

        for (i = 0; i < count; i++) {
            PersistenceFieldElement pfe = fields[i];

            if (pfe.isKey())
                returnList.add(pfe.getName());
        }

        return (String[])returnList.toArray(new String[returnList.size()]);
    }

    /** Adds ObjectId/PrimaryKey conversion methods to the helper class.
     */
    private void generatePKObjectIdConversion(String[] keyFields)
                       throws IOException{
        int length = keyFields.length;
        StringBuffer getOid = new StringBuffer(); // PK -> Oid
        StringBuffer getPK = new StringBuffer(); // Oid -> PK
        String[] pkfieldParam = new String[1];

        // Add parameter validation to avoid NullPointerException and
        // other startup lines for the conversions that do not depend
        // on the pk type.
        getOid.append(CMPTemplateFormatter.assertPKNotNullTemplate).
            append(CMPTemplateFormatter.noidformatter.format(pcnameParam));
        getPK.append(CMPTemplateFormatter.assertOidNotNullTemplate).
            append(CMPTemplateFormatter.oidcformatter.format(pcnameParam));

        boolean debug = logger.isLoggable(Logger.FINE);
        if (length == 1) {    
            // only one key field - we don't know yet if there is a special PK class.

            // RESOLVE: Find out what must be null for this case....
            String pkfield = keyFields[0];
            String pkfieldType = model.getFieldType(pcname, pkfield);
            pkfieldParam[0] = pkfield;

            if (debug) {
                logger.fine("pkfield: " + pkfield); // NOI18N
            }

            if (model.isPrimitive(pcname, pkfield) ||
                (!pkClass.equals(pkfieldType) && 
                    !pkClass.equals(Object.class.getName()))) {

                // A single primitive PK field requires a user defined PK class for
                // conversion between Object and primitive value. The same type of conversion
                // is generated for a user defined PK class in case of wrapper field type.
                // Generate conversion as key.id = objectId.id and objectId.id = key.id

                getPK.append(CMPTemplateFormatter.npkformatter.format(pkClassParam));
                getOid.append(CMPTemplateFormatter.pkcformatter.format(pkClassParam));

                pkfieldParam[0] = pkfield;

                getOid.append(
                    CMPTemplateFormatter.oidformatter.format(pkfieldParam));
                getPK.append(
                    CMPTemplateFormatter.pkformatter.format(pkfieldParam));

                getPK.append(CMPTemplateFormatter.returnKey_);

            } else {
                // PK Field is of wrapper type or unknown PK Class - generate conversion
                // as key = objectId.id and objectId.id = key.
                oneParam[0] = pkfieldType;
                getOid.append(CMPTemplateFormatter.pkcformatter.format(oneParam));

                twoParams[0] = pkfield;
                twoParams[1] = pkfieldType;
                getOid.append(
                    requireCloneOnGetAndSet(pkfieldType) ?
                        CMPTemplateFormatter.oid1cloneformatter.format(twoParams) :
                        (requireTrimOnSet(pkfieldType) ?
                            CMPTemplateFormatter.oid1stringformatter.format(pkfieldParam) :
                            CMPTemplateFormatter.oid1formatter.format(pkfieldParam)));

                getPK.append(
                    requireCloneOnGetAndSet(pkfieldType) ?
                        CMPTemplateFormatter.pk1cloneformatter.format(pkfieldParam) :
                        CMPTemplateFormatter.pk1formatter.format(pkfieldParam));

            }
        } else {
            // pkClass declaration with more than 1 field.
            getPK.append(CMPTemplateFormatter.npkformatter.format(pkClassParam));
            getOid.append(CMPTemplateFormatter.pkcformatter.format(pkClassParam));

            for (int i = 0; i < length; i++) {
                String pkfield = keyFields[i];
                pkfieldParam[0] = pkfield;

                if (debug) {
                    logger.fine("pkfield: " + pkfield); // NOI18N
                }

                if (!model.isPrimitive(pcname, pkfield)) {
                    getOid.append(
                        CMPTemplateFormatter.assertpkfieldformatter.format(pkfieldParam));
                }

                String pkfieldType = model.getFieldType(pcname, pkfield);
                twoParams[0] = pkfield;
                twoParams[1] = pkfieldType;
                getOid.append(
                    requireCloneOnGetAndSet(pkfieldType) ?
                        CMPTemplateFormatter.oidcloneformatter.format(twoParams) :
                        (requireTrimOnSet(pkfieldType) ?
                            CMPTemplateFormatter.oidstringformatter.format(pkfieldParam) :
                            CMPTemplateFormatter.oidformatter.format(pkfieldParam)));
                getPK.append(
                    requireCloneOnGetAndSet(pkfieldType) ?
                        CMPTemplateFormatter.pkcloneformatter.format(twoParams) :
                        CMPTemplateFormatter.pkformatter.format(pkfieldParam));
            }

            getPK.append(CMPTemplateFormatter.returnKey_);
        }
        getOid.append(CMPTemplateFormatter.returnOid_);


        // Add ones that can be used for Collection conversion.
        jdoHelperWriter.addMethod(CMPTemplateFormatter.convertPrimaryKeyToObjectId_, // name
            Modifier.PUBLIC, // modifiers
            CMPTemplateFormatter.Object_, // returnType
            param0, // parameterNames
            objectType,// parameterTypes
            null,// exceptions
            CMPTemplateFormatter.getBodyAsStrings(getOid.toString()), // body
            null);// comments

        jdoHelperWriter.addMethod(CMPTemplateFormatter.convertObjectIdToPrimaryKey_, // name
            Modifier.PUBLIC, // modifiers
            CMPTemplateFormatter.Object_, // returnType
            param0, // parameterNames
            objectType,// parameterTypes
            null,// exceptions
            CMPTemplateFormatter.getBodyAsStrings(getPK.toString()), // body
            null);// comments

    }

    /**
     * Returns the name of the concrete bean class for the specified name of
     * the persistence capable class.
     * @param pcClass the name of the persistence capable class
     * @return the name of the corresponding concrete bean class
     */
    String getConcreteBeanForPCClass(String pcClass) {
        return nameMapper.getConcreteBeanClassForEjbName(
            nameMapper.getEjbNameForPersistenceClass(pcClass));
    }

    /**
     * Generates conversion methods from PC to EJBObject and back
     * to the helper class.
     */
    void generateConversions() throws IOException {
        String[] pcParams = new String[] {CMPTemplateFormatter.pc_,
                    CMPTemplateFormatter.jdoPersistenceManager_};
        String[] pcParamTypes = new String[] {CMPTemplateFormatter.Object_,
                    CMPTemplateFormatter.jdoPersistenceManagerClass_};

        String[] collParamTypes = new String[] {CMPTemplateFormatter.Collection_,
                    CMPTemplateFormatter.jdoPersistenceManagerClass_};

        // For PC - PK conversion.
        String[] body = null;

        // Generate for Remote object conversion.
        if (hasRemoteInterface == false) {
            body = CMPTemplateFormatter.getBodyAsStrings(
                CMPTemplateFormatter.returnNull_ );

            jdoHelperWriter.addMethod(CMPTemplateFormatter.convertPCToEJBObject_, // name
                Modifier.PUBLIC, // modifiers
                CMPTemplateFormatter.ejbObject_, // returnType
                pcParams, // parameterNames
                pcParamTypes,// parameterTypes
                null,// exceptions
                body, // body
                null);// comments

            twoParams[0] = CMPTemplateFormatter.ejbObject_;
            twoParams[1] = CMPTemplateFormatter.jdoPersistenceManagerClass_;
            jdoHelperWriter.addMethod(CMPTemplateFormatter.convertEJBObjectToPC_, // name
                Modifier.PUBLIC, // modifiers
                CMPTemplateFormatter.Object_, // returnType
                param0PM, // parameterNames
                twoParams,// parameterTypes
                null,// exceptions
                body, // body
                null);// comments
        }

    }

    /**
     * Verifies if expected exception is part of the throws clause of the
     * corresponding method in the abstract class. 
     * @param exc the list of the Exceptions to check.
     * @param checkExc the Exception to check for as String.
     * @return <code>true</code> if the passed Exception is declared.
     */
    boolean containsException(String[] exc, String checkExc) {
        boolean rc = false;
        if (exc != null) {
            for (int i = 0; i < exc.length; i++) {
                if (exc[i].equals(checkExc)) {
                    rc = true;
                    break;
                }
            }
        }
        return rc;
    }

    /**
     * Verifies if expected exception is part of the throws clause of the
     * corresponding method in the abstract class. Returns EJBException
     * if the requested one is not found.
     * @param exc the list of the Exceptions to check.
     * @param checkExc the Exception to check for as String.
     * @return Exception to be thrown in the try-catch block.
     */
    String getException(String[] exc, String checkExc) {
        return (containsException(exc, checkExc)? checkExc : 
            CMPTemplateFormatter.ejbException_);
    }

    /**
     * Verifies if expected exception or its superclass are part of the 
     * throws clause of the corresponding method in the abstract class. 
     * Returns EJBException if none of the requested exceptions is not found.
     * @param exc the list of the Exceptions to check.
     * @param checkExc the Exception to check for as String.
     * @param superExc the known superclass for the Exception to check for as String.
     * @return Exception to be thrown in the try-catch block.
     */
    String getException(String[] exc, String checkExc, String superExc) {
        String rc = CMPTemplateFormatter.ejbException_;
        if (exc != null) {
            for (int i = 0; i < exc.length; i++) {
                if (exc[i].equals(checkExc) || exc[i].equals(superExc)) {
                    rc = checkExc;
                    break;
                }
            }
        }
        return rc;
    }

    // helper methods to generate finder/selector method bodies

    /**
     * Checks if the finder method is a single-object or multi-object finder.
     * @param finder Method object of the finder
     * @return <code>true</code> if it is a single-object finder
     */
    private boolean isSingleObjectFinder(Method finder) {
        return (!(finder.getReturnType().equals(java.util.Collection.class) ||
                  finder.getReturnType().equals(java.util.Enumeration.class)));
    }

    /**
     * Generates the body of the Entity-Bean finder methods.
     * @param methodHelper the AbstractMethodHelper instance that contains
     * all categorized methods and some other convenience methods for this bean
     * @param jdoqlElements Result of the JDOQL-Compiler
     * @param mname name of the findermethod in
     * the concrete entity bean implementation
     * @param m method instance
     * @param returnType the returnType of this findermethod
     * @param index index of finder method in finders list
     * @return the generated body
     * @exception IOException
     */
    private String generateFinderMethodBody(AbstractMethodHelper methodHelper,
                                            JDOQLElements jdoqlElements,
                                            String mname,
                                            Method m,
                                            String returnType,
                                            int index) throws IOException {

        StringBuffer body = new StringBuffer();
        body.append(CMPTemplateFormatter.assertPersistenceManagerIsNullTemplate);
        body.append(CMPTemplateFormatter.endLine_);
        body.append(generateFinderSelectorCommonBody(methodHelper,
                                                     jdoqlElements,
                                                     mname,
                                                     m,
                                                     returnType,
                                                     index));

        // getting the catch-clause body from the properties
        oneParam[0] = mname;

        // testing if this is a single-object finder
        if (isSingleObjectFinder(m)) {
            // generating the specific single finder method result set handling
            fourParams[0] = mname;
            fourParams[1] = pkClass;
            fourParams[2] = concreteImplName;
            fourParams[3] = CMPTemplateFormatter.catchClauseTemplate;
            body.append(CMPTemplateFormatter.singlefinderformatter.format(fourParams));
        } else {
            // generating the specific multi-object finder method result set handling
            // if CMP11 and the returntype is Enumeration, convert resultCollection
            // to Enumeration else leave Collection
            twoParams[0] = concreteImplName;
            twoParams[1] = CMPTemplateFormatter.catchClauseTemplate;
            if (isFinderReturningEnumeration(m)) {
                body.append(CMPTemplateFormatter.multifinderenumerationformatter.format(twoParams));
            } else {
                body.append(CMPTemplateFormatter.multifinderformatter.format(twoParams));
            }
        }

        return body.toString();
    }

    /**
     * JDOQuery Codegeneration for the common part of the
     * selecter and the finder methods. That consists of:
     * 1. create JDO Query object
     * 2. setting JDO Query elements (declare parameters, set variables etc.)
     * 3. convert parameter values (if available and necessary)
     * 4. execute the JDO Query
     * @param methodHelper the AbstractMethodHelper instance that contains
     * all categorized methods and some other convenience methods for this bean
     * @param jdoqlElements Result of the JDOQL-Compiler
     * @param methodName name of the finder/selector method in
     * the concrete entitybean implementation
     * @param m the method instance
     * @param returnType the return type of the finder/selectormethod
     * @param index index of finder/selector in corresponding list
     * @return the body as a string
     * @exception IOException
     */
    String generateFinderSelectorCommonBody(AbstractMethodHelper methodHelper,
                                                    JDOQLElements jdoqlElements,
                                                    String methodName,
                                                    Method m,
                                                    String returnType,
                                                    int index) throws IOException{
        // unique identifier across finders/selectors
        String queryVariableQualifier = m.getName() + '_' + index;

        // add private static query variables and their monitors
        // no need to check ejbFindByPrimaryKey here
        oneParam[0] = queryVariableQualifier;
        CMPTemplateFormatter.addPrivateField(
            CMPTemplateFormatter.finderselectorstaticvformatter.format(oneParam),
            Modifier.STATIC, 
            concreteImplWriter);
        CMPTemplateFormatter.addPrivateField(
            CMPTemplateFormatter.finderselectorstaticfinalvformatter.format(oneParam),
            Modifier.STATIC + Modifier.FINAL, 
            concreteImplWriter);

        StringBuffer body = new StringBuffer();

        String[] parameterEjbNames = jdoqlElements.getParameterEjbNames();

        // common param check for finder/selector
        body.append(generateFinderSelectorParamCheck(m, parameterEjbNames));
        
        // generating the querydeclaration
        String pcClassName = jdoqlElements.getCandidateClassName();
        String concreteBeanClassName = getConcreteBeanForPCClass(pcClassName);
        queryParams[0] = returnType;
        queryParams[1] = queryVariableQualifier;
        queryParams[2] = concreteBeanClassName;
        queryParams[3] = StringHelper.escape(jdoqlElements.getFilter());
        queryParams[4] = StringHelper.escape(jdoqlElements.getParameters());
        queryParams[5] = StringHelper.escape(jdoqlElements.getVariables());
        queryParams[6] = StringHelper.escape(jdoqlElements.getResult());
        queryParams[7] = StringHelper.escape(jdoqlElements.getOrdering());
        queryParams[8] = Boolean.toString(methodHelper.isQueryPrefetchEnabled(m));
        queryParams[9] = StringHelper.escape(generateQueryIgnoreCache());
        body.append(CMPTemplateFormatter.finderselectorformatter.format(queryParams));

        // now generate the query execution
        // two cases: w/ and w/o query parameters
        String queryParam = generateParamConvBody(m, parameterEjbNames);

        if (jdoqlElements.isAggregate()) {
            if (queryParam == null) {
               oneParam[0] = CMPTemplateFormatter.none_;
               body.append(CMPTemplateFormatter.aggqueryexecformatter.format(oneParam));
            } else {
               oneParam[0] = queryParam;
               body.append(
                 CMPTemplateFormatter.aggqueryexecparamconvformatter.format(oneParam));
            }
        } else {
            if (queryParam == null) {
               oneParam[0] = CMPTemplateFormatter.none_;
               body.append(CMPTemplateFormatter.queryexecformatter.format(oneParam));
            } else {
               oneParam[0] = queryParam;
               body.append(
                 CMPTemplateFormatter.queryexecparamconvformatter.format(oneParam));
            }
        }

        return body.toString();
    }

    /**
     * Generates code that check the finder/selector parameters for the
     * Query.execute call (if necessary).
     * @param m Method instance of the specific finder/selector method
     * @param parameterEjbNames array of ejb names
     * @return the codefragment for the checking local/remote parameters
     *         for method if EJB name is known from ejbql.
     */
    String generateFinderSelectorParamCheck(Method m,
            String[] parameterEjbNames) {
        StringBuffer checkBody = new StringBuffer();

        return checkBody.toString();
    }

    /**
     * Generates a setIgnoreCache(true) call for a JDOQL query, 
     * if necessary.
     * @return the codefragment to set the ignoreCache flag of a JDOQL query.
     */
    String generateQueryIgnoreCache()
    {
        return CMPTemplateFormatter.none_;
    }

    /**
     * Checks if the finder returns an Enumeration.
     * @param finder Methodobject of the finder
     * @return <code>true</code> if the finder returns a Enumeration
     */
    abstract boolean isFinderReturningEnumeration(Method finder); 

    /**
     * Generates code that converts the finder/selector parameters for the
     * Query.execute call (if necessary). It maps local and remote interface
     * values to their corresponding pc instance using JDOHelper methods
     * provided by the concrete entity bean. Primitive type values are wrapped.
     * @param m Method instance of the specific finder/selector method
     * @param parameterEjbNames array of ejb names
     * @return the codefragment for the conversions as a string or null if there
     * aren't any parameters
     */
    private String generateParamConvBody(Method m, String[] parameterEjbNames) {

        StringBuffer paramString = new StringBuffer();
        Class[] paramTypes = m.getParameterTypes();
        int paramLength = paramTypes.length;
        MessageFormat mformat = null;
        String paramClassName = null;

        if (paramLength > 0) {
            // iterate over all paramclasses
            for (int i = 0; i < paramLength; i++) {
                paramClassName = paramTypes[i].getName();

                // if local interface
                if (nameMapper.isLocalInterface(paramClassName) ||
                        nameMapper.isRemoteInterface(paramClassName)) {

                    if (parameterEjbNames[i] != null) {
                        mformat = CMPTemplateFormatter.queryexecparamconvargumentformatter;
                        String concreteImplName =
                            nameMapper.getConcreteBeanClassForEjbName(
                            parameterEjbNames[i]);
                        threeParams[0] = concreteImplName;
                        threeParams[1] = String.valueOf(i);
                        threeParams[2] =
                            nameMapper.isLocalInterface(paramClassName) ?
                            CMPTemplateFormatter.convertEJBLocalObjectToPC_ :
                            CMPTemplateFormatter.convertEJBObjectToPC_;
                        paramString.append(mformat.format(threeParams));
                    } else {
                        paramString.append(CMPTemplateFormatter.param_ + i);
                    }

                // if primitive type, do some wrapping
                } else if(paramTypes[i].isPrimitive()) {
                    paramString.append(
                          JavaClassWriterHelper.getWrapperExpr(
                                             paramTypes[i],
                                             JavaClassWriterHelper.param_ + i
                                             ));

                // else take the param as it is
                } else {
                    paramString.append(CMPTemplateFormatter.param_ + i);
                }
                // normal delimiter
                if (i < paramLength - 1) paramString.append(
                                          CMPTemplateFormatter.paramSeparator_);
            }
        } else return null;

        return paramString.toString();
    }

    /**
     * Returns list of the declared exceptions for the method with this name
     * in the abstract bean. Returns null if such method does not exist
     * or does not have checked exceptions.
     * @param methodHelper the AbstractMethodHelper instance that contains
     * all categorized methods and some other convenience methods for this bean.
     * @param mname method name to check.
     * @param paramTypeNames list of parameter types to the method
     * @return list of the declared exceptions as String[].
     */
    String[] getExceptionList(AbstractMethodHelper methodHelper,
                                      String mname,
                                      String[] paramTypeNames) {
        String[] rc = null;
        Class[] paramTypes = null;

        Map methodNames = methodHelper.getMethodNames();
        Method m = (Method) methodNames.get(mname);

        boolean debug = logger.isLoggable(Logger.FINE);
        if (debug) {
            logger.fine("Processing method: " + mname);
            logger.fine("Known method: " + m);
        }

        if (m == null) {
            // Check the bean class:
            if( paramTypeNames != null ) {
                paramTypes = new Class[ paramTypeNames.length ];
                try {
                    for( int i = paramTypeNames.length - 1; i >= 0; i-- ) {
                        paramTypes[i] = Class.forName( paramTypeNames[i], true, loader );
                    }
                } catch( Exception e ) {
                    // Ignore
                }
            }

            try {
                Class beanClass = Class.forName(abstractBean, true, loader);
                m = beanClass.getMethod(mname, paramTypes);
                if (debug) {
                    logger.fine("Found method: " + m);
                }

            } catch (Exception e) {
                // Ignore. Generate what we know.
            }
        } 

        if (m != null) { 
            rc = CMPTemplateFormatter.getExceptionNames(m);
        }

        return rc;
    }

    /**
     * Returns list of the declared exceptions for the method with this name
     * in the abstract bean. Returns null if such method does not exist
     * or does not have checked exceptions.
     * @param methodHelper the AbstractMethodHelper instance that contains
     * all categorized methods and some other convenience methods for this bean.
     * @param mname method name to check.
     * @return list of the declared exceptions as String[].
     */
    String[] getExceptionList( AbstractMethodHelper methodHelper, String mname ) {
        return getExceptionList( methodHelper, mname, null );
    }

    /**
     * This method will return the given string or "" if it is null or
     * empty string.
     * @param st input string
     */
    private String makeLiteral(String st) {
        return (StringHelper.isEmpty(st)) ? 
            CMPTemplateFormatter.escapedEmptyString_ :
            CMPTemplateFormatter.paramInitializer_ + st;
    }

    /**
     * Returns the signatures of the classes and properties which are
     * involved in the codegen.
     * @return The signatures as a string.
     */
    String getSignaturesOfGeneratorClasses() 
    {
        StringBuffer signatures = new StringBuffer().

            // adding signature of JDOConcreteBeanGenerator
            append(JDOConcreteBeanGenerator.SIGNATURE).
            append(CMPTemplateFormatter.signatureDelimiter_).

            // adding signature of CMPTemplates.properties
            append(CMPTemplateFormatter.signatureTemplate).
            append(CMPTemplateFormatter.signatureDelimiter_).
                
            // adding signature of DeploymentDescriptorModel
            append(DeploymentDescriptorModel.SIGNATURE);
        
        return signatures.toString();
    }

    /** Verifies if this field type requires clone for copy-in, copy-out
     * semantics.
     * @param fieldType the field type as String.
     * @return <code>true</code> if field type requires clone.
     */
    boolean requireCloneOnGetAndSet(String fieldType) {
        return (CMPTemplateFormatter.Date_.equals(fieldType) ||
            CMPTemplateFormatter.SqlDate_.equals(fieldType) ||
            CMPTemplateFormatter.SqlTime_.equals(fieldType) ||
            CMPTemplateFormatter.SqlTimestamp_.equals(fieldType));
    }

    /** Verifies if this field type requires trim on set operation.
     * @param fieldType the field type as String.
     * @return <code>true</code> if field type is java.lang.String.
     */
    boolean requireTrimOnSet(String fieldType) {
         // Strings require trim on set
         boolean requireTrimOnSet = CMPTemplateFormatter.String_.equals(fieldType);
         // do not trim if user has overriden it by specifying to support trailing spaces in pk columns
         // See https://glassfish.dev.java.net/issues/show_bug.cgi?id=7491 for more details
         return requireTrimOnSet && !SUPPORT_TRAILING_SPACES_IN_STRING_PK_COLUMNS;
    }

    /** Generates code that preloads non-DFG fields for read-only beans.
     * @param fieldInfo the FieldInfo instance for this CMP field.
     */
    void loadNonDFGField(FieldInfo fieldInfo) {
        if( !isUpdateable && !fieldInfo.isDFG ) {
            oneParam[0] = fieldInfo.getter;
            loadNonDFGBody.append(
                    CMPROTemplateFormatter.loadNonDFGformatter.format(oneParam));
        }
    }

    /*
     * This class contains the field information to generate get/set methods.
     *
     */
    class FieldInfo {
    
        final PersistenceFieldElement pfe;
    
        final String name;
        final String type;
        final String getter;
        final String setter;
    
        final boolean isKey;
        final boolean isPrimitive;
        final boolean isByteArray;
        final boolean isSerializable;
        final boolean requireCloneOnGetAndSet;
        final boolean isGeneratedField;
        final boolean isDFG;
    
        FieldInfo(Model model, NameMapper nameMapper, 
                PersistenceFieldElement pfe, 
                String beanName, String pcname) {
    
            this.pfe = pfe;
    
            String pfn = pfe.getName();
            name = nameMapper.getEjbFieldForPersistenceField(pcname, pfn);
    
            String fname = StringHelper.getCapitalizedString(name);
            getter = CMPTemplateFormatter.get_ + fname;
            setter = CMPTemplateFormatter.set_ + fname;
    
            boolean debug = logger.isLoggable(Logger.FINE);
            if (debug) {
                logger.fine("-Methods: " + getter + " " + setter); // NOI18N
            }
    
            isKey = pfe.isKey();
            isPrimitive = model.isPrimitive(pcname, pfn);
            isByteArray = model.isByteArray(beanName, name);
            isSerializable = model.isByteArray(pcname, pfn);

            if (isSerializable) {
                // Replace '$' with '.' if it's an inner class.
                type = model.getFieldType(beanName, name).replace('$', '.');
            } else {
                type = model.getFieldType(beanName, name);
            }

            if (debug) {
                logger.fine("Field: " + name + " " + type); // NOI18N
            }
    
            requireCloneOnGetAndSet = requireCloneOnGetAndSet(type);
            isGeneratedField = nameMapper.isGeneratedField(beanName, name);

            // Check if the field is in DFG.
            MappingClassElement mce = model.getMappingClass(pcname);
            MappingFieldElement mfe = mce.getField(name);
            isDFG = (mfe.getFetchGroup() == MappingFieldElement.GROUP_DEFAULT);
        }
    }

}

