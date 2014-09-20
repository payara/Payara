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
 * JDOConcreteBean11Generator.java
 *
 * Created on February 24, 2004
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbc;

import java.util.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;

import com.sun.jdo.api.persistence.model.Model;
import com.sun.jdo.api.persistence.model.jdo.*;

import com.sun.jdo.spi.persistence.support.ejb.model.util.NameMapper;
import com.sun.jdo.spi.persistence.support.ejb.ejbqlc.JDOQLElements;

import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.spi.persistence.utility.generator.*;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
 
/*
 * This is the JDO specific generator for the concrete CMP beans for EJB1.1.
 *
 * @author Marina Vatkina
 */
class JDOConcreteBean11Generator extends JDOConcreteBeanGenerator {

    /**
     * Signature with CVS keyword substitution for identifying the generated code
     */
    static final String SIGNATURE = "$RCSfile: JDOConcreteBean11Generator.java,v $ $Revision: 1.2 $"; //NOI18N
    
    JDOConcreteBean11Generator(ClassLoader loader,
                             Model model,
                             NameMapper nameMapper)
                             throws IOException {

        super (loader, model, nameMapper);
        CMP11TemplateFormatter.initHelpers();

        // Add the code generation signature of the generic and 1.1-specific 
        // generator classes.
        addCodeGeneratorClassSignature(getSignaturesOfGeneratorClasses());
    }

    /** Add interfaces to the class declarations.
     */
    void addInterfaces() throws IOException {
        super.addInterfaces();
        jdoHelperWriter.addInterface(CMP11TemplateFormatter.helper11Interface_);
    }

    /** Set super class for the helper class.
     */
    void setHelperSuperclass() throws IOException {
        jdoHelperWriter.setSuperclass(CMP11TemplateFormatter.helper11Impl_);
    }

    /** Generate CMP1.1 specific methods.
     */
    void generateTypeSpecificMethods(PersistenceFieldElement[] allFields,
            AbstractMethodHelper methodHelper) throws IOException {

        super.generateTypeSpecificMethods(allFields, methodHelper);
        generateLoadStoreMethods(allFields);
    }

    /**
     * Generates required internal variables.
     */
    void generateFields() throws IOException {

        super.generateFields();
        CMP11TemplateFormatter.addPrivateField(
                CMP11TemplateFormatter.one_oneVariablesTemplate,
                0, concreteImplWriter);
    }


    /** Adds jdoLoadFields/jdoStoreFields for CMP1.1
     */
    void generateLoadStoreMethods(PersistenceFieldElement[] fields)
                       throws IOException {
        int i, count = ((fields != null) ? fields.length : 0);
        StringBuffer lbody = new StringBuffer();
        StringBuffer sbody = new StringBuffer(CMPTemplateFormatter.none_);

        for (i = 0; i < count; i++) {
            PersistenceFieldElement pfe = fields[i];

            if (PersistenceFieldElement.PERSISTENT == pfe.getPersistenceType()) {
                FieldInfo fieldInfo = new FieldInfo(model, nameMapper, pfe, beanName, pcname);

                if (fieldInfo.isGeneratedField) {
                // Skip generated fields as they are not present in the bean class.
                // A field is generated for the unknown PK class or version consistency.
                // There are no relationship fields in CMP1.1 beans.
                    if (fieldInfo.isKey) {
                        // This is an extra field for the unknown PK class.
                        // PK setter name is used to generate the line for ejbCreate
                        // to set the PK value in _JDOState.
                        setPKField = fieldInfo.setter;
                    }
                    continue;
                }

                // Add code to load non-DFG field if necessary.
                loadNonDFGField(fieldInfo);

                if( fieldInfo.isByteArray) {
                    // A byte[] CMP field should have copy-in, copy-out semantics
                    // via System.arraycopy.
                    twoParams[0] = fieldInfo.name;
                    twoParams[1] = fieldInfo.getter;
                    lbody.append(CMP11TemplateFormatter.l11arrayformatter.format(twoParams));

                    if (isUpdateable) {
                        threeParams[0] = fieldInfo.getter;
                        threeParams[1] = fieldInfo.name;
                        threeParams[2] = fieldInfo.setter;
                        sbody.append(CMP11TemplateFormatter.s11arrayformatter.format(threeParams));
                    }

                } else if( fieldInfo.isSerializable ) {
                    // A special case for a Serializable CMP field (but not byte[]) -
                    // it should be serialized to/from a byte[] in PC instance.
                        
                    fourParams[0] = fieldInfo.name;
                    fourParams[1] = fieldInfo.getter;
                    fourParams[2] = fieldInfo.type;
                    fourParams[3] = concreteImplName;
                    lbody.append(CMP11TemplateFormatter.l11Serializableformatter.format(fourParams));

                    if (isUpdateable) {
                        fourParams[0] = fieldInfo.getter;
                        fourParams[1] = fieldInfo.name;
                        fourParams[2] = fieldInfo.setter;
                        fourParams[3] = concreteImplName;
                        sbody.append(CMP11TemplateFormatter.s11Serializableformatter.format(fourParams));
                    }

                } else if (fieldInfo.requireCloneOnGetAndSet) {
                    threeParams[0] = fieldInfo.getter;
                    threeParams[1] = fieldInfo.type;
                    threeParams[2] = fieldInfo.name;
                    lbody.append(CMP11TemplateFormatter.l11copyformatter.format(threeParams));

                    if (isUpdateable) {
                        fourParams[0] = fieldInfo.getter;
                        fourParams[1] = fieldInfo.name;
                        fourParams[2] = fieldInfo.setter;
                        fourParams[3] = fieldInfo.type;
                        if (!pfe.isKey()) {
                            sbody.append(CMP11TemplateFormatter.s11copyformatter.format(fourParams));
                        } else {
                            twoParams[0] = concreteImplName;
                            twoParams[1] = fieldInfo.name;
                            sbody.append(CMP11TemplateFormatter.assertpks11formatter.format(twoParams)).
                                append(CMP11TemplateFormatter.pkcopy11formatter.format(fourParams));
                        }
                    }

                } else {
                    twoParams[0] = fieldInfo.name;
                    twoParams[1] = fieldInfo.getter;
                    lbody.append(CMP11TemplateFormatter.l11formatter.format(twoParams));

                    if (isUpdateable) {
                        threeParams[0] = fieldInfo.getter;
                        threeParams[1] = fieldInfo.name;
                        threeParams[2] = fieldInfo.setter;
                        if (!pfe.isKey()) {
                            sbody.append(CMP11TemplateFormatter.s11formatter.format(threeParams));
                        } else {
                            if (!fieldInfo.isPrimitive) {
                                twoParams[0] = concreteImplName;
                                twoParams[1] = fieldInfo.name;
                                sbody.append(
                                    CMP11TemplateFormatter.assertpks11formatter.format(twoParams));
                            }

                            sbody.append(requireTrimOnSet(fieldInfo.type) ?
                                CMP11TemplateFormatter.pkstring11formatter.format(threeParams) :
                                CMP11TemplateFormatter.pks11formatter.format(threeParams));
                        }
                    }

                }
           }
       }

       // Add jdoLoadFields
       CMPTemplateFormatter.addGenericMethod(
               CMP11TemplateFormatter.loadFields1_1_,
               CMP11TemplateFormatter.getBodyAsStrings(lbody.toString()),
               concreteImplWriter);

       // Add jdoStoreFields
       CMPTemplateFormatter.addGenericMethod(
               CMP11TemplateFormatter.storeFields1_1_,
               CMP11TemplateFormatter.getBodyAsStrings(sbody.toString()),
               concreteImplWriter);
    }

    /** Returns JDOQLElements instance for this finder method.
     * @param m the finder method as a java.lang.reflect.Method
     * @param methodHelper the AbstractMethodHelper instance that contains
     * all categorized methods and some other convenience methods for this bean.
     * @return JDOQLElements instance.
     */
    JDOQLElements getJDOQLElements(Method m,
            AbstractMethodHelper methodHelper) throws IOException{
      
        // CMP11 : get JDO query settings from DD
        return getJDOQLElementsForCMP11(m, methodHelper);
    }

    /** Returns method body for EJBCreate method.
     * @param createName the actual name of the method as String.
     * @param exc a String[] of decleared exceptions for this method.
     * @param parametersList the list of method parameters as String.
     * @param parametersListWithSeparator the list of concatenated method
     * parameters to be passed to another method as String.
     * @return method body as String.
     */
    String getEJBCreateMethodBody(String createName,
            String[] exc, String parametersList,
            String parametersListWithSeparator) {

        // For read-only beans it will throw an exception on access.
        // For updateable beans it will be generated to do the create.
        String body = CMPROTemplateFormatter.accessNotAllowedTemplate;

        if (isUpdateable) {
            // no suffixes in ejbCreate for CMP1.1 beans but we need to check what
            // exception is available.
            if (pkClass.equals(Object.class.getName())) {
                fiveParams[0] = pcname;
                fiveParams[1] = parametersList;
                fiveParams[2] = setPKField;
                fiveParams[3] = concreteImplName;
                fiveParams[4] = parametersListWithSeparator;
                body = CMP11TemplateFormatter.c11unpkformatter.format(fiveParams);

            } else {
                sixParams[0] = pcname;
                sixParams[1] = parametersList;
                sixParams[2] = pkClass;
                sixParams[3] = concreteImplName;
                String s = getException(exc, CMP11TemplateFormatter.DuplicateKeyException_, 
                        CMP11TemplateFormatter.CreateException_);
                int l = s.lastIndexOf(CMP11TemplateFormatter.dot_);
                sixParams[4] = (l > 0)? s.substring(l + 1) : s;
                sixParams[5] = parametersListWithSeparator;
                body = CMP11TemplateFormatter.c11formatter.format(sixParams);

            }
        }
        return body;
    }

    /** Returns method body for EJBPostCreate method.
     * @param postCreateName the actual name of the method as String.
     * @param parametersList the list of method parameters as String.
     * @param parametersListWithSeparator the list of concatenated method
     * parameters to be passed to another method as String.
     * @return method body as String.
     */
    String getEJBPostCreateMethodBody(String postCreateName,
            String parametersList, String parametersListWithSeparator) {

        // For read-only beans it will be a no-op. For updateable
        // beans it will be generated.
        String body = CMPTemplateFormatter.none_;
 
        if (isUpdateable) {
            twoParams[0] = parametersList;
            twoParams[1] = parametersListWithSeparator;
            body = CMP11TemplateFormatter.postc11formatter.format(twoParams);
        }

        return body;
    }

    /** Returns method body for EJBRemove method.
     * @return method body as String.
     */
    String getEJBRemoveMethodBody() {

        // For read-only beans it will throw an exception on access. 
        // For updateable beans it will be generated.
        String body = CMPROTemplateFormatter.updateNotAllowedTemplate;
 
        if (isUpdateable) {
            // CMP1.1 does not need any variables to substitute.
            body = CMP11TemplateFormatter.ejbRemove1_1Template;
        }

        return body;
    }

    /** Adds other known required methods identified by properties that do
     * not need formatting but differ between CMP types.
     * CMP11TemplateFormatter.otherPublicMethods_ differ between CMP types.
     */
    void generateKnownMethods(AbstractMethodHelper methodHelper)
                       throws IOException {

        super.generateKnownMethods(methodHelper);

        String[] exc = null;
        String[] st = CMP11TemplateFormatter.otherPublicMethodsArray;
        for (int i = 0; i < st.length; i++) {
            String mname = st[i];
            exc = getExceptionList(methodHelper, mname);

            String body = CMPROTemplateFormatter.updateNotAllowedTemplate;
            // Only ejbLoad from this list doesn't differ for read-only beans.
            if (isUpdateable || mname.equals(CMPTemplateFormatter.ejbLoad_)) {
                body = CMP11TemplateFormatter.helpers.getProperty(
                        mname + "1_1"); // NOI18N
            } else if (mname.equals(CMPTemplateFormatter.jdoCleanAllRefs_)) {
                body = CMPROTemplateFormatter.jdoCleanAllRefsTemplate;
            }

            concreteImplWriter.addMethod(mname, // name
                Modifier.PUBLIC, // modifiers
                CMP11TemplateFormatter.void_, // returnType
                null, // parameterNames
                null,// parameterTypes
                exc,// exceptions
                CMP11TemplateFormatter.getBodyAsStrings(body), // body
                null);// comments
        }

    }

   /**
     * Checks if the finder returns an Enumeration (for CMP1.1)
     * @param finder Methodobject of the finder
     * @return <code>true</code> if the finder returns a Enumeration
     */
    boolean isFinderReturningEnumeration(Method finder) {
        return (finder.getReturnType().equals(java.util.Enumeration.class));
    }

    /**
     * Generates a setIgnoreCache(true) call for a JDOQL query, 
     * in the case of a EJB 1.1 finder.
     * @return the codefragment to set the ignoreCache flag of a JDOQL query.
     */
    String generateQueryIgnoreCache()
    {
        oneParam[0] =  CMP11TemplateFormatter.true_;
        return CMP11TemplateFormatter.querysetignorecacheformatter.format(oneParam);
    }

    /**
     * Creating a JDOQLElements object for CMP11 support. For CMP11 there is no
     * EJBQL, thus we get the filter expression and parameter declaration from
     * the DD.
     * @param m CMP1.1 method instance
     * @param methodHelper the AbstractMethodHelper instance that contains
     * all categorized methods and some other convenience methods for this bean
     * @return a filled JDOQElementsobject for further codegeneration
     */
    private JDOQLElements getJDOQLElementsForCMP11(Method m,
            AbstractMethodHelper methodHelper) {

        String params = methodHelper.getJDOParameterDeclaration(m);
        String variables = methodHelper.getJDOVariableDeclaration(m);
        String filter = methodHelper.getJDOFilterExpression(m);
        String ordering = methodHelper.getJDOOrderingSpecification(m);
        return new JDOQLElements(
            pcname, // use the pc class for this bean as candidateClass
            params, // JDO parameter declarations from DD
            variables, // JDO variables declarations from DD
            filter, // JDO filter expression from DD
            ordering, // JDO ordering expression from DD
            "this", // finders return PK instances =>
                    // Project this to prevent generation of distinct
            pcname, // finders return PK instances =>
                    // the jdo query returns a set of pc instances
            true,   // JDO query returns candidate class instances =>
                    // isPCResult = true
            false,  // not associate to aggregate function
            null    // not available and not supported for 1.1 finder
            );

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
            append(super.getSignaturesOfGeneratorClasses()).
            append(CMPTemplateFormatter.signatureDelimiter_).

            // adding signature of JDOConcreteBean11Generator
            append(JDOConcreteBean11Generator.SIGNATURE).
            append(CMPTemplateFormatter.signatureDelimiter_).

            // adding signature of CMP11Templates.properties
            append(CMP11TemplateFormatter.signature1_1Template);

        return signatures.toString();
    }

}

