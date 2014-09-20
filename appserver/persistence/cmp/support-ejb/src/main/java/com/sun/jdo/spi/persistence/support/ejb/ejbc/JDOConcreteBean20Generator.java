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
 * JDOConcreteBean20Generator.java
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
import com.sun.jdo.api.persistence.model.ModelException;
import com.sun.jdo.api.persistence.model.jdo.*;

import com.sun.jdo.api.persistence.support.JDOUserException;
import com.sun.jdo.spi.persistence.support.ejb.model.util.NameMapper;
import com.sun.jdo.spi.persistence.support.ejb.ejbqlc.EJBQLC;
import com.sun.jdo.spi.persistence.support.ejb.ejbqlc.JDOQLElements;
import com.sun.jdo.spi.persistence.support.ejb.ejbqlc.EJBQLException;

import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.spi.persistence.utility.generator.*;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
 
/*
 * This is the JDO specific generator for the concrete CMP beans for EJB2.0
 *
 * @author Marina Vatkina
 */
class JDOConcreteBean20Generator extends JDOConcreteBeanGenerator {

    // EJBQLC ejbqlc
    private EJBQLC ejbqlc;

    /**
     * Map that holds EJBQL compilation results for CMP 2.x beans queries. 
     * <tt>key</tt> is java.lang.reflect.Method object for the bean's 
     * finder or selector and the <tt>value</tt> is JDOQLElements object
     * that represents EJBQL compilation results.
     */
    private Map jdoqlElementsMap;

    // StringBuffer for cascade-delete operations on ejbRemove
    private StringBuffer cascadeDelete = null;

    // String for getter method body
    private String gbody = null;

    // String for setter method body
    private String sbody = null;

    /**
     * Signature with CVS keyword substitution for identifying the generated code
     */
    static final String SIGNATURE = 
            "$RCSfile: JDOConcreteBean20Generator.java,v $ $Revision: 1.2 $"; //NOI18N
    
    JDOConcreteBean20Generator(ClassLoader loader,
                             Model model,
                             NameMapper nameMapper)
                             throws IOException {

        super(loader, model, nameMapper);
        CMP20TemplateFormatter.initHelpers();

        // Add the code generation signature of the generic and 2.x-specific 
        // generator classes.
        addCodeGeneratorClassSignature(getSignaturesOfGeneratorClasses());

        // init EJBQL compiler
        ejbqlc = new EJBQLC(model, nameMapper);
    }

    /**
     * Validate this CMP bean. At this point, only EJBQL validation is done for
     * 2.0 CMP beans. Adds validation result to that of the super class.
     * @param methodHelper the AbstractMethodHelper instance that contains
     * all categorized methods and some other convenience methods for this bean.
     * @param beanName the ejb name for this bean.
     * @return a Collection of Exception instances with a separate instance for 
     * each failed validation.
     */
    Collection validate(AbstractMethodHelper methodHelper, String beanName) {
        Collection rc = super.validate(methodHelper, beanName);

        this.beanName = beanName;
        rc.addAll(validateEJBQL(methodHelper)); 

        return rc;
    }


    /** Add interfaces to the class declarations.
     */
    void addInterfaces() throws IOException {
        super.addInterfaces();
        jdoHelperWriter.addInterface(CMP20TemplateFormatter.helper20Interface_);
    }

    /** Set super class for the helper class.
     */
    void setHelperSuperclass() throws IOException {
        jdoHelperWriter.setSuperclass(CMP20TemplateFormatter.helper20Impl_);
    }

    /** Add import statements for for the generated classes.
     */
    void addImportStatements(JavaFileWriter concreteImplFileWriter,
            JavaFileWriter helperFileWriter) throws IOException {

        super.addImportStatements(concreteImplFileWriter, helperFileWriter);
        concreteImplFileWriter.addImport(
                CMP20TemplateFormatter.ejbHashSetImport_, null);
    }

    /** Generate CMP2.0 specific methods.
     */
    void generateTypeSpecificMethods(PersistenceFieldElement[] allFields,
            AbstractMethodHelper methodHelper) throws IOException {

        super.generateTypeSpecificMethods(allFields, methodHelper);

        // Print getters and setters for CMP2.0 only.
        generateGetSetMethods(allFields);

        // Add selectors.
        generateSelectors(methodHelper);

    }

    /** Adds getters and setters.
     */
    private void generateGetSetMethods(PersistenceFieldElement[] fields)
                       throws IOException{
        int i, count = ((fields != null) ? fields.length : 0);
        setPKField = null; // reset to null to clean it up.
        cascadeDelete = new StringBuffer();

        // jdoCleanCollectionRef() body
        StringBuffer cmrcleanbodyBuf = new StringBuffer(CMP20TemplateFormatter.none_);

        for (i = 0; i < count; i++) {
            PersistenceFieldElement pfe = fields[i];

            if (PersistenceFieldElement.PERSISTENT == pfe.getPersistenceType()) {

                // Reset the strings.
                gbody = null;
                sbody = null;

                FieldInfo fieldInfo = new FieldInfo(model, nameMapper, pfe, beanName, pcname);

                if (fieldInfo.isGeneratedField) {
                // Skip generated fields as they are not present in the bean class.
                // A field is generated for the unknown PK class, version consistency, or 
                // a 2 way managed relationship.
                    if (fieldInfo.isKey) {
                        // This is an extra field for the unknown PK class.
                        // PK setter name is used to generate the line for ejbCreate
                        // to set the PK value in _JDOState.
                        setPKField = fieldInfo.setter;
                    }
                    continue;
                }

                if (!(pfe instanceof RelationshipElement)) {
                    generateCMPGetSetBodies(fieldInfo);

                } else { // CMR
                    if (isUpdateable) {
                        generateCMRGetSetBodies(fieldInfo, cmrcleanbodyBuf);

                    } else {
                        logger.log(Logger.WARNING, I18NHelper.getMessage(messages,
                                "CMG.CMRAccessNotAllowed", beanName, fieldInfo.name)); // NOI18N

                        gbody = CMPROTemplateFormatter.accessNotAllowedTemplate;
                        sbody = CMPROTemplateFormatter.updateNotAllowedTemplate;
                    }
                }

                // Now generate getter and setter:
                CMPTemplateFormatter.addGenericMethod(
                    fieldInfo.getter, Modifier.PUBLIC, fieldInfo.type, 
                    CMP20TemplateFormatter.getBodyAsStrings(gbody),
                    concreteImplWriter);

                oneParam[0] = fieldInfo.type;
                concreteImplWriter.addMethod(fieldInfo.setter, // name
                    Modifier.PUBLIC, // modifiers
                    CMP20TemplateFormatter.void_, // returnType
                    param0, // parameterNames
                    oneParam,// parameterTypes
                    null,// exceptions
                    CMP20TemplateFormatter.getBodyAsStrings(sbody), // body
                    null);// comments
            }
        }

        // Now generate jdoCleanCollectionRef method
        CMPTemplateFormatter.addGenericMethod(
               CMP20TemplateFormatter.jdoCleanCollectionRef_,
               CMP20TemplateFormatter.getBodyAsStrings(cmrcleanbodyBuf.toString()),
               concreteImplWriter);
    }

    /** Generate bodies of getters and setters for CMP field 
     * @param fieldInfo the field information as FieldInfo instance.
     */
    private void generateCMPGetSetBodies(FieldInfo fieldInfo) {

        // For read-only beans it will be the same. For updateable
        // beans it will be generated per field type.
        sbody = CMPROTemplateFormatter.updateNotAllowedTemplate;

        // Add code to load non-DFG field if necessary.
        loadNonDFGField(fieldInfo);

        if( fieldInfo.requireCloneOnGetAndSet ) {
            // CMP field should have copy-in, copy-out semantics
            // via clone.
            twoParams[0] = fieldInfo.getter;
            twoParams[1] = fieldInfo.type;
            gbody = CMP20TemplateFormatter.copygformatter.format(twoParams);

            if (isUpdateable) {
                twoParams[0] = fieldInfo.setter;
                if (!fieldInfo.isKey) {
                    sbody = CMP20TemplateFormatter.copysformatter.format(twoParams);
                } else {
                    String[] params = new String[] {concreteImplName, fieldInfo.name};
                    sbody = CMP20TemplateFormatter.assertpksformatter.format(params) +
                        CMP20TemplateFormatter.pkcopysformatter.format(twoParams);
                }
            }

        } else if( fieldInfo.isByteArray ) {
            // A byte[] CMP field should have copy-in, copy-out semantics
            // via System.arraycopy.
            oneParam[0] = fieldInfo.getter;
            gbody = CMP20TemplateFormatter.arraygformatter.format(oneParam);

            if (isUpdateable) {
                oneParam[0] = fieldInfo.setter;
                sbody = CMP20TemplateFormatter.arraysformatter.format(oneParam);
            }
        } else if( fieldInfo.isSerializable ) {
            // A special case for a Serializable CMP field (but not byte[]) -
            // it should be serialized to/from a byte[] in PC instance.
            
            threeParams[0] = fieldInfo.getter;
            threeParams[1] = fieldInfo.type;
            threeParams[2] = concreteImplName;
            gbody = CMP20TemplateFormatter.sfldgformatter.format(threeParams);

            if (isUpdateable) {
                twoParams[0] = fieldInfo.setter;
                twoParams[1] = concreteImplName;
                sbody = CMP20TemplateFormatter.sfldsformatter.format(twoParams);
            }
        } else {
            oneParam[0] = fieldInfo.getter;
            gbody = CMP20TemplateFormatter.gformatter.format(oneParam);

            if (isUpdateable) {
                oneParam[0] = fieldInfo.setter;
                if (!fieldInfo.isKey) {
                    sbody = CMP20TemplateFormatter.sformatter.format(oneParam);

                } else {
                    StringBuffer sb = new StringBuffer();
                    if (!fieldInfo.isPrimitive) {
                        twoParams[0] = concreteImplName;
                        twoParams[1] = fieldInfo.name;
                        sb.append(
                            CMP20TemplateFormatter.assertpksformatter.format(twoParams));
                    }
    
                    sb.append(requireTrimOnSet(fieldInfo.type) ? 
                        CMP20TemplateFormatter.pkstringsformatter.format(oneParam) :
                        CMP20TemplateFormatter.pksformatter.format(oneParam));

                    sbody = sb.toString();
                }
            }
        }

    }

    /** Generate bodies of getters and setters for CMR field 
     * @param fieldInfo the field information as FieldInfo instance.
     * @param cmrcleanbodyBuf the StringBuffer to append code for CMR cleanup
     * if necessary.
     */
    private void generateCMRGetSetBodies(FieldInfo fieldInfo, 
            StringBuffer cmrcleanbodyBuf) throws IOException {

        RelationshipElement rel = (RelationshipElement)fieldInfo.pfe;

        String otherPC = model.getRelatedClass(rel);
        boolean manySide = model.isCollection(fieldInfo.type);

        if (logger.isLoggable(Logger.FINE)) {
            RelationshipElement otherField = rel.getInverseRelationship(model);
            String otherFieldName = ((otherField != null) ?
                    nameMapper.getEjbFieldForPersistenceField(otherPC, 
                            otherField.getName()) :
                    null);

            logger.fine("manySide: " + manySide); // NOI18N
            logger.fine("Field: " + otherFieldName); // NOI18N
        }

        String otherEJB = nameMapper.getEjbNameForPersistenceClass(otherPC);
        String otherImpl = nameMapper.getConcreteBeanClassForEjbName(otherEJB);
        MessageFormat mformat = null;

        if (manySide) {
            threeParams[0] = fieldInfo.getter;
            threeParams[1] = fieldInfo.name;
            threeParams[2] = otherImpl;
            gbody = CMP20TemplateFormatter.cmrCgformatter.format(threeParams);

            fourParams[0] = otherImpl;
            fourParams[1] = fieldInfo.setter;
            fourParams[2] = fieldInfo.getter;
            fourParams[3] = fieldInfo.name;
            sbody = CMP20TemplateFormatter.cmrCsformatter.format(fourParams);

            mformat = CMP20TemplateFormatter.cmrcdCformatter;

            twoParams[0] = fieldInfo.type;
            twoParams[1] = fieldInfo.name;
            CMP20TemplateFormatter.addPrivateField(
                CMP20TemplateFormatter.cmrvformatter.format(twoParams),
                0, concreteImplWriter);

            oneParam[0] = fieldInfo.name;
            cmrcleanbodyBuf.append(CMP20TemplateFormatter.cleancmrformatter.format(oneParam));

        } else { // 1 side
            fourParams[0] = otherPC;
            fourParams[1] = fieldInfo.getter;
            fourParams[2] = fieldInfo.type;
            fourParams[3] = otherImpl;
            gbody = CMP20TemplateFormatter.cmrgformatter.format(fourParams);

            threeParams[0] = otherPC;
            threeParams[1] = otherImpl;
            threeParams[2] = fieldInfo.setter;
            sbody = CMP20TemplateFormatter.cmrsformatter.format(threeParams);

            mformat = CMP20TemplateFormatter.cmrcdformatter;

        }

        if (rel.getDeleteAction() == RelationshipElement.CASCADE_ACTION) {
            twoParams[0] = fieldInfo.getter;
            twoParams[1] = otherImpl;
            cascadeDelete.append(mformat.format(twoParams));
            try {
                // Reset DeleteAction to NONE to suppress it in PM.deletePersistent().
                rel.setDeleteAction(RelationshipElement.NONE_ACTION);
            } catch (ModelException me) {
                logger.log(Logger.SEVERE, I18NHelper.getMessage(messages,
                        "CMG.ModelExceptionOnDeleteAction", me)); //NOI18N
            }
        }

    }

    /** Validate EJBQL for ejbFind and ejbSelect methods by calling compilation.
     * The method stores compilation results in the {@link #jdoqlElementsMap} map.
     * This method is called only for CMP 2.x beans as there is no validation of
     * CMP 1.1 queries at the deployment time.
     * @param methodHelper the AbstractMethodHelper instance that contains
     * all categorized methods and some other convenience methods for this bean.
     * @return a Collection of found exceptions.
     */
    private Collection validateEJBQL(AbstractMethodHelper methodHelper) {
        Collection rc = new ArrayList();
        jdoqlElementsMap = new HashMap();

        List methods = new ArrayList(methodHelper.getFinders());
        methods.addAll(methodHelper.getSelectors());
        for (int i = 0; i < methods.size(); i++) {
            Method m = (Method)methods.get(i);
            String mname = m.getName();
            if (mname.equals(CMP20TemplateFormatter.findByPrimaryKey_)) {
                // No EJBQL is defined for findByPrimaryKey.
                continue;
            }

            try {
                // EJBQLC needs to know if we are processing a finder or a selector.
                jdoqlElementsMap.put(m,
                        ejbqlc.compile(methodHelper.getQueryString(m), m, 
                               methodHelper.getQueryReturnType(m), 
                               mname.startsWith(CMP20TemplateFormatter.find_), 
                               beanName)); 
            } catch (EJBQLException e) {
                rc.add(e);
            }
        }

        return rc;
    }

    /** Returns JDOQLElements instance for this finder method.
     * @param m the finder method as a java.lang.reflect.Method
     * @param methodHelper the AbstractMethodHelper instance that contains
     * all categorized methods and some other convenience methods for this bean.
     * @return JDOQLElements instance.
     */
    JDOQLElements getJDOQLElements(Method m,
            AbstractMethodHelper methodHelper) throws IOException{
        // Call the EJBQL compiler if there is no known result
        // from validate call.
        JDOQLElements rs  = (JDOQLElements)jdoqlElementsMap.get(m);
        if (rs == null) {
            if (logger.isLoggable(Logger.FINE)) {
                logger.fine("JDOQLElements NOT FOUND for: " + m.getName());
            }

            rs = ejbqlc.compile(methodHelper.getQueryString(m), m, 
                    methodHelper.getQueryReturnType(m), true, beanName);
        }

        return rs;
    }

    /** Adds ejbSelectBy methods.
     */
    private void generateSelectors(AbstractMethodHelper methodHelper) throws IOException{
        List selectors = methodHelper.getSelectors();
        boolean debug = logger.isLoggable(Logger.FINE);
        if (debug) {
            logger.fine("Selectors: " + selectors.size()); // NOI18N
        }

        for (int i = 0; i < selectors.size(); i++) {
            Method m = (Method)selectors.get(i);
            String mname = m.getName();

            if (debug) {
                logger.fine("Selector: " + mname); // NOI18N
            }

            JDOQLElements rs = (JDOQLElements)jdoqlElementsMap.get(m);
            if (rs == null) {
                if (debug) {
                    logger.fine("JDOQLElements NOT FOUND for: " + mname);
                }

                // calling EJBQL compiler
                rs = ejbqlc.compile(methodHelper.getQueryString(m), m,
                    methodHelper.getQueryReturnType(m), false, beanName);
            }

            String returnType = m.getReturnType().getName();
            CMP20TemplateFormatter.addGenericMethod(
                m, mname, returnType,
                generateSelectorMethodBody(methodHelper, rs, mname, m, returnType, i),
                concreteImplWriter);
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
    String getEJBCreateMethodBody(String createName,
            String[] exc, String parametersList,
            String parametersListWithSeparator) {

        // ejbCreate in the superclass will have the same suffix, so we need
        // to pass the actual name to the formatter - see 'createName' parameter.
        if (!containsException(exc, CMP20TemplateFormatter.CreateException_)) {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "EXC_NoCreateException", createName, abstractBean)); // NOI18N
        }

        // For read-only beans it will be the same. For updateable
        // beans it will be generated as required.
        String body = CMPROTemplateFormatter.accessNotAllowedTemplate;
        if (isUpdateable) {
            sixParams[0] = pcname;
            sixParams[1] = createName;
            sixParams[2] = parametersList;
            sixParams[4] = concreteImplName;
            sixParams[5] = parametersListWithSeparator;

            if (pkClass.equals(Object.class.getName())) {
                sixParams[3] = setPKField;

                body = CMP20TemplateFormatter.cunpkformatter.format(sixParams);
            } else {
                sixParams[3] = pkClass;
                body = CMP20TemplateFormatter.cformatter.format(sixParams);
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

        // For read-only beans it will be the same. For updateable
        // beans it will be generated as required.
        String body = CMPTemplateFormatter.none_;

        if (isUpdateable) {
            twoParams[0] = postCreateName;
            twoParams[1] = parametersList;

            body = CMP20TemplateFormatter.postcformatter.format(twoParams);
        }

        return body;
    }

    /** Returns method body for EJBRemove method.
     * @return method body as String.
     */
    String getEJBRemoveMethodBody() {
        // For read-only beans it will throw an exception. For updateable
        // beans it will be generated as required.
        String body = CMPROTemplateFormatter.updateNotAllowedTemplate;
        if (isUpdateable) {
            // CMP2.0 might have cascade-delete requirement.
            if (cascadeDelete.length() > 0) {
                oneParam[0] = CMP20TemplateFormatter.startCascadeDeleteTemplate +
                        cascadeDelete.append(
                        CMP20TemplateFormatter.endCascadeDeleteTemplate).
                        toString();
            } else {
                oneParam[0] = CMP20TemplateFormatter.none_;
            }

            body = CMP20TemplateFormatter.rmformatter.format(oneParam);
         }

        return body;
    }

    /** Adds other known required methods identified by properties that do
     * not need formatting but differ between CMP types.
     * CMP20TemplateFormatter.otherPublicMethods_ differ between CMP types.
     */
    void generateKnownMethods(AbstractMethodHelper methodHelper)
                       throws IOException {

        super.generateKnownMethods(methodHelper);

        String[] exc = null;
        String[] st = CMP20TemplateFormatter.otherPublicMethodsArray;
        for (int i = 0; i < st.length; i++) {
            String mname = st[i];
            exc = getExceptionList(methodHelper, mname);

            String body = CMPROTemplateFormatter.updateNotAllowedTemplate;
            // Only ejbLoad from this list doesn't differ for read-only beans.
            if (isUpdateable || mname.equals(CMPTemplateFormatter.ejbLoad_)) {
                body = CMP20TemplateFormatter.helpers.getProperty(mname);
            } else if (mname.equals(CMPTemplateFormatter.jdoCleanAllRefs_)) {
                body = CMPROTemplateFormatter.jdoCleanAllRefsTemplate;
            }

            concreteImplWriter.addMethod(mname, // name
                Modifier.PUBLIC, // modifiers
                CMP20TemplateFormatter.void_, // returnType
                null, // parameterNames
                null,// parameterTypes
                exc,// exceptions
                CMP20TemplateFormatter.getBodyAsStrings(body), // body
                null);// comments
        }

    }

    /**
     * Generates helper methods for the helper class.
     */
    void generateHelperClassMethods() throws IOException {

        super.generateHelperClassMethods();
        // Add Helper.assertInstanceOfLocalInterfaceImpl() method for cmp2.0 only.
        oneParam[0] = CMP20TemplateFormatter.assertInstanceOfLocalInterfaceImplTemplate;

        jdoHelperWriter.addMethod(CMP20TemplateFormatter.assertInstanceOfLocalInterfaceImpl_, // name
                Modifier.PUBLIC, // modifiers
                CMP20TemplateFormatter.void_, // returnType
                param0, // parameterNames
                objectType,// parameterTypes
                null,// exceptions
                oneParam, // body
                null);// comments
    }

    /**
     * Generates conversion methods from PC to EJBObject and back
     * to the helper class.
     */
    void generateConversions() throws IOException {

        super.generateConversions();

        // For EJBLocalObject.
        if (hasLocalInterface == false) {
            String[] pcParams = new String[] {CMP20TemplateFormatter.pc_,
                    CMP20TemplateFormatter.jdoPersistenceManager_};
            String[] pcParamTypes = new String[] {CMP20TemplateFormatter.Object_,
                    CMP20TemplateFormatter.jdoPersistenceManagerClass_};

            String[] body = CMP20TemplateFormatter.getBodyAsStrings(
                    CMP20TemplateFormatter.returnNull_);

            jdoHelperWriter.addMethod(CMP20TemplateFormatter.convertPCToEJBLocalObject_, // name
                Modifier.PUBLIC, // modifiers
                CMP20TemplateFormatter.ejbLocalObject_, // returnType
                pcParams, // parameterNames
                pcParamTypes,// parameterTypes
                null,// exceptions
                body, // body
                null);// comments

            String[] pcParamsX = new String[] {CMP20TemplateFormatter.pc_,
                    CMP20TemplateFormatter.jdoPersistenceManager_,
                    CMP20TemplateFormatter.context_};
            String[] pcParamTypesX = new String[] {CMP20TemplateFormatter.Object_,
                    CMP20TemplateFormatter.jdoPersistenceManagerClass_,
                    CMP20TemplateFormatter.ejbContext_};
            jdoHelperWriter.addMethod(CMP20TemplateFormatter.convertPCToEJBLocalObject_, // name
                Modifier.PUBLIC, // modifiers
                CMP20TemplateFormatter.ejbLocalObject_, // returnType
                pcParamsX, // parameterNames
                pcParamTypesX,// parameterTypes
                null,// exceptions
                body, // body
                null);// comments


            twoParams[0] = CMP20TemplateFormatter.ejbLocalObject_;
            twoParams[1] = CMP20TemplateFormatter.jdoPersistenceManagerClass_;
            jdoHelperWriter.addMethod(CMP20TemplateFormatter.convertEJBLocalObjectToPC_, // name
                Modifier.PUBLIC, // modifiers
                CMP20TemplateFormatter.Object_, // returnType
                param0PM, // parameterNames
                twoParams,// parameterTypes
                null,// exceptions
                body, // body
                null);// comments
        }
    }

    /**
     * Generates the body of the Entity-Bean selector methods.
     * This is the special result set handling.
     * @param methodHelper the AbstractMethodHelper instance that contains
     * all categorized methods and some other convenience methods for this bean
     * @param jdoqlElements the result of the EJBQL-Compiler
     * @param mname name of the selector method in
     * the concrete entity bean implementation
     * @param m selector method object
     * @param index index of selector method in selectors list
     * @param returnType the returntype of the selectormethod
     * @return the generated body
     * @exception IOException
     */
    private String generateSelectorMethodBody(AbstractMethodHelper methodHelper,
                                              JDOQLElements jdoqlElements,
                                              String mname,
                                              Method m,
                                              String returnType,
                                              int index) throws IOException {

        StringBuffer body = new StringBuffer();

        // add preSelect callback
        oneParam[0] = concreteImplName;
        body.append(CMP20TemplateFormatter.preselectformatter.format(oneParam));

        // common body for finder/selectors
        body.append(generateFinderSelectorCommonBody(methodHelper,
                                                     jdoqlElements,
                                                     mname,
                                                     m,
                                                     returnType,
                                                     index));

        // body with resulthandling depending on the type of the selector
        // (single or multivalue)
        if (isSingleObjectSelector(m)) {
            body.append(generateResultHandlingForSingleSelector(
                jdoqlElements, mname, m, methodHelper, returnType));
        } else {
            body.append(generateResultHandlingForMultiSelector(
                jdoqlElements, m, methodHelper));
        }

        return body.toString();
    }


    /**
     * Generates the result handling for a multi-value selector method.
     * The generated code converts the JDO query result set into the appropriate
     * selector result.
     * @param jdoqlElements the result of the JDOQL compiler
     * @param m selector method object
     * @param methodHelper the AbstractMethodHelper instance that contains
     * all categorized methods and some other convenience methods for this bean
     * @return the generated result set handling
     */
    private String generateResultHandlingForMultiSelector(
                                                    JDOQLElements jdoqlElements,
                                                    Method m,
                                                    AbstractMethodHelper methodHelper) {

        boolean convertToSet = false;
        String body = null;
        MessageFormat mformat = null;
        // getting the catch-clause body from the properties
        oneParam[0] = m.getName();

        // depending of the kind of returntype a different convertermethodcall
        // is generated
        if (isSelectorReturningSet(m)) convertToSet = true;

        int queryReturnType = methodHelper.getQueryReturnType(m);
        if ((queryReturnType == AbstractMethodHelper.NO_RETURN) && 
            jdoqlElements.isPCResult()) {
            // Use LOCAL_RETURN as default, 
            // if there is no result-type-mapping specified and 
            // the JDOQL query returns a collection of pc instances 
            queryReturnType = AbstractMethodHelper.LOCAL_RETURN;
        }

        switch (queryReturnType) {
        case (AbstractMethodHelper.LOCAL_RETURN):
            mformat = CMP20TemplateFormatter.multiselectorconvformatter;
            threeParams[0] =
                getConcreteBeanForPCClass(jdoqlElements.getResultType());
            threeParams[1] = convertToSet ?
                CMP20TemplateFormatter.convertCollectionPCToEJBLocalObjectSet_ :
                CMP20TemplateFormatter.convertCollectionPCToEJBLocalObject_;
            threeParams[2] = CMP20TemplateFormatter.catchClauseTemplate;
            body = mformat.format(threeParams);
            break;
        case (AbstractMethodHelper.REMOTE_RETURN):
            mformat = CMP20TemplateFormatter.multiselectorconvformatter;
            threeParams[0] =
                getConcreteBeanForPCClass(jdoqlElements.getResultType());
            threeParams[1] = convertToSet ?
                CMP20TemplateFormatter.convertCollectionPCToEJBObjectSet_ :
                CMP20TemplateFormatter.convertCollectionPCToEJBObject_;
            threeParams[2] = CMP20TemplateFormatter.catchClauseTemplate;
            body = mformat.format(threeParams);
            break;
        case (AbstractMethodHelper.NO_RETURN):
        default:
            mformat = convertToSet ?
                CMP20TemplateFormatter.multiselectorsetformatter :
                CMP20TemplateFormatter.multiselectorformatter;
            oneParam[0] = CMP20TemplateFormatter.catchClauseTemplate;
            body = mformat.format(oneParam);
            break;
        }

        return body;
    }

    /**
     * Generates the result handling for a single-object selector method.
     * The generated code converts the JDO query result set into the appropriate
     * selector result.
     * @param jdoqlElements the result of the JDOQL compiler
     * @param mname name of the selector method in
     * the concrete entity bean implementation
     * @param m selector method object
     * @param methodHelper the AbstractMethodHelper instance that contains
     * all categorized methods and some other convenience methods for this bean
     * @param returnType the returntype of the selectormethod
     * @return the generated result set handling
     */
    private String generateResultHandlingForSingleSelector(
                                                    JDOQLElements jdoqlElements,
                                                    String mname,
                                                    Method m,
                                                    AbstractMethodHelper methodHelper,
                                                    String returnType) {

        StringBuffer body = new StringBuffer();
        MessageFormat mformat = null;
        String jdoResultType = jdoqlElements.getResultType();
        String ejbName = null;
        // generated code that tests the cardinality of the JDO query
        // a JDOQL aggregate query returns a single object => no check needed
        if (!jdoqlElements.isAggregate()) {
            mformat = CMP20TemplateFormatter.singleselectorformatter;
            oneParam[0] = mname;
            body.append(mformat.format(oneParam));
        }

        // getting the catch-clause body from the properties
        oneParam[0] = CMP20TemplateFormatter.none_;

        int queryReturnType = methodHelper.getQueryReturnType(m);
        if ((queryReturnType == AbstractMethodHelper.NO_RETURN) && 
            jdoqlElements.isPCResult()) {
            // Use LOCAL_RETURN as default, 
            // if there is no result-type-mapping specified and 
            // the JDOQL query returns a collection of pc instances 
            queryReturnType = AbstractMethodHelper.LOCAL_RETURN;
        }

        // generate different converter method call depending on return type
        switch (queryReturnType) {
        case (AbstractMethodHelper.LOCAL_RETURN):
            ejbName = nameMapper.getEjbNameForPersistenceClass(jdoResultType);
            mformat = CMP20TemplateFormatter.singleselectorreturnconvformatter;
            fourParams[0] = nameMapper.getLocalInterfaceForEjbName(ejbName);
            fourParams[1] = nameMapper.getConcreteBeanClassForEjbName(ejbName);
            fourParams[2] = CMP20TemplateFormatter.convertPCToEJBLocalObject_;
            fourParams[3] = CMP20TemplateFormatter.catchClauseTemplate;
            body.append(mformat.format(fourParams));
            break;
        case (AbstractMethodHelper.REMOTE_RETURN):
            ejbName = nameMapper.getEjbNameForPersistenceClass(jdoResultType);
            mformat = CMP20TemplateFormatter.singleselectorreturnconvformatter;
            fourParams[0] = nameMapper.getRemoteInterfaceForEjbName(ejbName);
            fourParams[1] = nameMapper.getConcreteBeanClassForEjbName(ejbName);
            fourParams[2] = CMP20TemplateFormatter.convertPCToEJBObject_;
            fourParams[3] = CMP20TemplateFormatter.catchClauseTemplate;
            body.append(mformat.format(fourParams));
            break;
        case (AbstractMethodHelper.NO_RETURN):
        default:
            Class returnTypeClass = m.getReturnType();
            // tests if it is aggregate function and proceed it first
            if (jdoqlElements.isAggregate()) {
                if (returnTypeClass.isPrimitive()) {
                    mformat = CMP20TemplateFormatter.aggregateselectorprimitivereturnformatter;
                    fourParams[0] = mname;
                    fourParams[1] = jdoResultType;
                    fourParams[2] = CMP20TemplateFormatter.dot_ +
                        CMP20TemplateFormatter.getUnwrapMethodName(returnTypeClass);
                    fourParams[3] = CMP20TemplateFormatter.catchClauseTemplate;
                    body.append(mformat.format(fourParams));
                } else if (returnTypeClass.getName().equals(jdoResultType)) {
                    mformat = CMP20TemplateFormatter.aggregateselectorreturnformatter;
                    twoParams[0] = jdoResultType;
                    twoParams[1] = CMP20TemplateFormatter.catchClauseTemplate;
                    body.append(mformat.format(twoParams));
                } else if (returnTypeClass.isAssignableFrom(
                    java.math.BigDecimal.class)) {
                    mformat = CMP20TemplateFormatter.aggregateselectorreturnbigdecimalconvformatter;
                    twoParams[0] = jdoResultType;
                    twoParams[1] = CMP20TemplateFormatter.catchClauseTemplate;
                    body.append(mformat.format(twoParams));
                } else if (returnTypeClass.isAssignableFrom(
                    java.math.BigInteger.class)) {
                    mformat = CMP20TemplateFormatter.aggregateselectorreturnbigintegerconvformatter;
                    twoParams[0] = jdoResultType;
                    twoParams[1] = CMP20TemplateFormatter.catchClauseTemplate;
                    body.append(mformat.format(twoParams));

                } else {
                    mformat = CMP20TemplateFormatter.aggregateselectorreturnconvformatter;
                    fourParams[0] = returnType;
                    fourParams[1] = jdoResultType;
                    fourParams[2] = CMP20TemplateFormatter.dot_ +
                        CMP20TemplateFormatter.getUnwrapMethodName(
                        CMP20TemplateFormatter.getPrimitiveClass(
                        CMP20TemplateFormatter.getPrimitiveName(returnTypeClass)));
                    fourParams[3] = CMP20TemplateFormatter.catchClauseTemplate;
                    body.append(mformat.format(fourParams));
                }
            } else {
                // tests if the returntype is a primitive java type
                // if so, the cast parameter is the wrapperclass of the
                // primitive type and the getterMethod for the primitive
                // value of the wrapper class is added.
                // This is necessary because the JDOQuery returns collections
                // of objects only, but the selector returns a primitive type.
                mformat = CMP20TemplateFormatter.singleselectorreturnformatter;
                if (returnTypeClass.isPrimitive()) {
                    threeParams[0] = CMP20TemplateFormatter.getWrapperName(returnType);
                    threeParams[1] = CMP20TemplateFormatter.dot_ +
                       CMP20TemplateFormatter.getUnwrapMethodName(returnTypeClass);
                    threeParams[2] = CMP20TemplateFormatter.catchClauseTemplate;
                } else {
                    threeParams[0] = returnType;
                    threeParams[1] = CMP20TemplateFormatter.none_;
                    threeParams[2] = CMP20TemplateFormatter.catchClauseTemplate;
                }
                body.append(mformat.format(threeParams));
            }

            break;
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

        Class[] paramTypes = m.getParameterTypes();
        int paramLength = paramTypes.length;
        String paramClassName = null;
        for (int i = 0; i < paramLength; i++) {
            if (parameterEjbNames[i] != null) {
                paramClassName = paramTypes[i].getName();
                String concreteImplName =
                    nameMapper.getConcreteBeanClassForEjbName(
                    parameterEjbNames[i]);
                twoParams[0] = concreteImplName;
                twoParams[1] = CMP20TemplateFormatter.param_ + i;

                if (nameMapper.isLocalInterface(paramClassName)) {
                    checkBody.append(CMP20TemplateFormatter.finderselectorchecklocalformatter.format(twoParams));
                } else { // Remote
                    checkBody.append(CMP20TemplateFormatter.finderselectorcheckremoteformatter.format(twoParams));
                }
            }
        }

        return checkBody.toString();
    }

   /**
     * Checks if the finder returns an Enumeration. Returns <code>false</code>
     * for CMP2.0.
     * @param finder Methodobject of the finder
     * @return <code>true</code> if the finder returns a Enumeration
     */
    boolean isFinderReturningEnumeration(Method finder) {
        return false;
    }

   /**
     * Checks if the selector method is a single-object or multi-object selector.
     * @param finder Method object of the finder
     * @return <code>true</code> if it is a single-object-value selector
     */
    private boolean isSingleObjectSelector(Method finder) {
        return (!(finder.getReturnType().equals(java.util.Collection.class) ||
                  finder.getReturnType().equals(java.util.Set.class)));
    }

   /**
     * Checks if the a selector returns a Set (for CMP2.0)
     * @param selector Methodobject of the selector
     * @return <code>true</code> if the selector returns a Set
     */
    private boolean isSelectorReturningSet(Method selector) {
        return (selector.getReturnType().equals(java.util.Set.class));
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

            // adding signature of JDOConcreteBean20Generator
            append(JDOConcreteBean20Generator.SIGNATURE).
            append(CMPTemplateFormatter.signatureDelimiter_).

            // adding signature of CMP20Templates.properties
            append(CMP20TemplateFormatter.signature2_0Template).
            append(CMPTemplateFormatter.signatureDelimiter_).

            // adding signature of EJBQLC
            append(EJBQLC.SIGNATURE);

        return signatures.toString();
    }

}

