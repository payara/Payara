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
 * CMPTemplateFormatter.java
 *
 * Created on December 03, 2001
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbc;

import java.io.*;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;

import com.sun.jdo.spi.persistence.utility.generator.JavaClassWriterHelper;

/*
 * This is the helper class for JDO specific generation of
 * a concrete bean implementation.
 *
 * @author Marina Vatkina
 */
public class CMPTemplateFormatter extends JavaClassWriterHelper{

    private final static String templateFile =
        "com/sun/jdo/spi/persistence/support/ejb/ejbc/CMPTemplates.properties"; // NOI18N

    // Strings for CMP code generation:
    public final static String ejb_                       = "ejb"; // NOI18N
    public final static String ejbObject_                 = "EJBObject"; // NOI18N
    public final static String ejbHome_                   = "EJBHome"; // NOI18N
    public final static String ejbContext_                = "EJBContext"; // NOI18N
    public final static String context_                   = "context"; // NOI18N
    public final static String key_                       = "key"; // NOI18N
    public final static String keys_                      = "keys"; // NOI18N
    public final static String oid_                       = "jdoObjectId"; // NOI18N
    public final static String oids_                      = "oids"; // NOI18N
    public final static String pc_                        = "pc"; // NOI18N
    public final static String cmpImplComments_           = "cmpImplComments"; // NOI18N
    public final static String imports_                   = "imports"; // NOI18N
    public final static String interfaces_                = "interfaces"; // NOI18N
    public final static String helperImports_             = "helperImports"; // NOI18N
    public final static String staticTransientPackageVariables_  = "staticTransientPackageVariables"; // NOI18N
    public final static String privateStaticVariables_    = "privateStaticVariables"; // NOI18N
    public final static String privateTransientVariables_ = "privateTransientVariables"; // NOI18N
    public final static String privateStaticFinalVariables_  = "privateStaticFinalVariables"; // NOI18N
    public final static String publicStaticFinalVariables_ = "publicStaticFinalVariables"; // NOI18N
    public final static String finderSelectorStaticVariables_  = "finderSelectorStaticVariables"; // NOI18N
    public final static String finderSelectorStaticFinalVariables_  = "finderSelectorStaticFinalVariables"; // NOI18N
    public final static String otherVariables_            = "otherVariables"; // NOI18N
    public final static String setNull_                   = "setNull"; // NOI18N
    public final static String commonPrivateMethods_      = "commonPrivateMethods"; // NOI18N
    public final static String commonPublicMethods_       = "commonPublicMethods"; // NOI18N
    public final static String otherPublicMethods_        = "otherPublicMethods"; // NOI18N
    public final static String jdoLookupPersistenceManagerFactory_
                                                          = "jdoLookupPersistenceManagerFactory"; // NOI18N
    public final static String helperMethods_             = "helperMethods"; // NOI18N
    public final static String getInstance_               = "jdoGetInstance"; // NOI18N
    public final static String findByPrimaryKey_          = "findByPrimaryKey"; // NOI18N
    public final static String ejbFindByPrimaryKey_       = "ejbFindByPrimaryKey"; // NOI18N
    public final static String ejbFinderSelectorBody_     = "ejbFinderSelectorBody"; // NOI18N
    public final static String ejbQueryExecutionParamConvBody_  = "ejbQueryExecutionParamConvBody"; // NOI18N
    public final static String ejbQueryExecutionParamConvBodyArgument_  = "ejbQueryExecutionParamConvBodyArgument"; // NOI18N
    public final static String ejbQueryExecutionBody_     = "ejbQueryExecutionBody"; // NOI18N
    public final static String ejbAggregateQueryExecutionBody_     = "ejbAggregateQueryExecutionBody"; // NOI18N
    public final static String ejbAggregateQueryExecutionParamConvBody_  = "ejbAggregateQueryExecutionParamConvBody"; // NOI18N
    public final static String ejbMultiFinderBody_        = "ejbMultiFinderBody"; // NOI18N
    public final static String ejbMultiFinderBodyEnumeration_  = "ejbMultiFinderBodyEnumeration"; // NOI18N
    public final static String ejbSingleFinderBody_       = "ejbSingleFinderBody"; // NOI18N
    public final static String jdoGetJdoInstanceClass_    = "jdoGetJdoInstanceClass"; // NOI18N
    public final static String find_                      = "find"; // NOI18N
    public final static String catchClause_               = "catchClause"; //NOI18N
    public final static String ejbSelect_                 = "ejbSelect"; // NOI18N
    public final static String create_                    = "create"; // NOI18N
    public final static String remove_                    = "remove"; // NOI18N
    public final static String ejbCreate_                 = "ejbCreate"; // NOI18N
    public final static String ejbCreateUnknownPK_        = "ejbCreateUnknownPK"; // NOI18N
    public final static String ejbPostCreate_             = "ejbPostCreate"; // NOI18N
    public final static String ejbRemove_                 = "ejbRemove"; // NOI18N
    public final static String ejbLoad_                   = "ejbLoad"; // NOI18N
    public final static String CreateException_           = "javax.ejb.CreateException"; // NOI18N
    public final static String DuplicateKeyException_     = "javax.ejb.DuplicateKeyException"; // NOI18N
    public final static String RemoveException_           = "javax.ejb.RemoveException"; // NOI18N
    public final static String ejbException_              = "EJBException"; // NOI18N
    public final static String finderException_           = "javax.ejb.FinderException"; // NOI18N
    public final static String assertInTransaction_       = "assertInTransaction"; // NOI18N
    public final static String assertPersistenceManagerIsNull_       
                                                          = "assertPersistenceManagerIsNull"; // NOI18N
    public final static String assertPersistenceManagerIsNullCall_       
                                                          = "assertPersistenceManagerIsNullCall"; // NOI18N
    public final static String setEntityContext_          = "setEntityContext"; // NOI18N
    public final static String EntityContext_             = "javax.ejb.EntityContext"; // NOI18N
    public final static String castKey_                   = "castKey"; // NOI18N
    public final static String castOid_                   = "castOid"; // NOI18N
    public final static String getPK_                     = "getPK"; // NOI18N
    public final static String getPKClone_                = "getPKClone"; // NOI18N
    public final static String newPK_                     = "newPK"; // NOI18N
    public final static String newOid_                    = "newOid"; // NOI18N
    public final static String getPK1_                    = "getPK1"; // NOI18N
    public final static String getPK1Clone_               = "getPK1Clone"; // NOI18N
    public final static String getPK1primitive_           = "getPK1primitive"; // NOI18N
    public final static String getOid_                    = "getOid"; // NOI18N
    public final static String getOidString_              = "getOidString"; // NOI18N
    public final static String getOidClone_               = "getOidClone"; // NOI18N
    public final static String getOid1_                   = "getOid1"; // NOI18N
    public final static String getOid1primitive_          = "getOid1primitive"; // NOI18N
    public final static String getOid1String_             = "getOid1String"; // NOI18N
    public final static String getOid1Clone_              = "getOid1Clone"; // NOI18N
    public final static String getObjectId_               = "jdoGetObjectId"; // NOI18N
    public final static String convertObjectIdToPrimaryKey_
                                     = "convertObjectIdToPrimaryKey"; // NOI18N
    public final static String convertPrimaryKeyToObjectId_
                                     = "convertPrimaryKeyToObjectId"; // NOI18N

    public final static String convertPCToEJBObject_   = "convertPCToEJBObject"; // NOI18N
    public final static String convertEJBObjectToPC_   = "convertEJBObjectToPC"; // NOI18N
    public final static String convertEJBLocalObjectToPC_
                                     = "convertEJBLocalObjectToPC"; // NOI18N

    public final static String jdoPersistenceManager_     = "_jdoPersistenceManager"; // NOI18N
    public final static String jdoPersistenceManagerClass_
                                     = "com.sun.jdo.api.persistence.support.PersistenceManager"; // NOI18N
    public final static String jdoGetPersistenceManager_  = "jdoGetPersistenceManager"; // NOI18N
    public final static String jdoGetPersistenceManager0_ = "jdoGetPersistenceManager0"; // NOI18N
    public final static String jdoClosePersistenceManager_= "jdoClosePersistenceManager"; // NOI18N
    public final static String jdoReleasePersistenceManager_  
                                     = "jdoReleasePersistenceManager"; // NOI18N
    public final static String jdoReleasePersistenceManager0_  
                                     = "jdoReleasePersistenceManager0"; // NOI18N

    public final static String jdoArrayCopy_              = "jdoArrayCopy"; // NOI18N
    public final static String jdoCleanAllRefs_           = "jdoCleanAllRefs"; // NOI18N
    public final static String returnKey_                 = "return key;"; // NOI18N
    public final static String returnOid_                 = "return jdoObjectId;"; // NOI18N

    // Inner helper Class strings.
    public final static String helperComments_            = "helperComments"; // NOI18N
    public final static String helperVariables_           = "helperVariables"; // NOI18N
    public final static String getHelperInstance_         = "getHelperInstance"; // NOI18N

    public final static String getPCClass_                = "getPCClass"; // NOI18N
    public final static String getContainer_              = "getContainer"; // NOI18N
    public final static String assertInstanceOfRemoteInterfaceImpl_
                                                          = "assertInstanceOfRemoteInterfaceImpl"; // NOI18N
    public final static String assertPKNotNull_           = "assertPKNotNull"; // NOI18N
    public final static String assertPKFieldNotNull_      = "assertPKFieldNotNull"; // NOI18N
    public final static String assertOidNotNull_          = "assertOidNotNull"; // NOI18N
    public final static String afterCompletion_           = "afterCompletion"; // NOI18N
    public final static String ejb__flush_                = "ejb__flush"; // NOI18N
    public final static String ejb__refresh_              = "ejb__refresh"; // NOI18N
    public final static String ejb__remove_               = "ejb__remove"; // NOI18N

    // property key for the CVS keyword substitution 
    public final static String signature_ = "signature"; //NOI18N

    public final static String signatureDelimiter_ = " ## "; //NOI18N

    // Code generator templates.
    public static Properties helpers = null;

    // JDOHelper comments have the concrete impl name.
    public static MessageFormat hcomformatter = null;

    // variables
    public static MessageFormat privatetransientvformatter = null; // private transient varibales
    public static MessageFormat privatestaticfinalvformatter = null; // final static variables
    public static MessageFormat publicstaticfinalvformatter = null; // static final variables
    public static MessageFormat finderselectorstaticvformatter = null; // finder / selector static variables
    public static MessageFormat finderselectorstaticfinalvformatter = null; // finder / selector static final variables
    public static MessageFormat hvformatter = null; // helper class variables

    // methods
    public static MessageFormat giformatter = null; // jdoGetInstance
    public static MessageFormat goidformatter = null; // jdoGetObjectId
    public static MessageFormat intxformatter = null; // assertInTransaction
    public static MessageFormat jdolookuppmfformatter = null; // jdoLookupPersistenceManagerFactory
    public static MessageFormat jdoarraycopyformatter = null; // jdoCopyArray

    // finder/selector methods
    public static MessageFormat finderselectorformatter = null; //common body for selector and finder
    public static MessageFormat queryexecformatter = null; //body for queryexec without paramconversion
    public static MessageFormat aggqueryexecformatter = null; //body for aggqueryexec without paramconversion
    public static MessageFormat queryexecparamconvformatter = null; //body for queryexec with paramconversion
    public static MessageFormat queryexecparamconvargumentformatter = null; //body for the argument of the queryexec with paramconversion
    public static MessageFormat aggqueryexecparamconvformatter = null; //body for aggqueryexec with paramconversion
    public static MessageFormat multifinderformatter = null; // finder body for multi-value-objects
    public static MessageFormat multifinderenumerationformatter = null; // finder body for multi-value-objects with conversion to Enumeration (CMP11))
    public static MessageFormat singlefinderformatter = null; // finder body for single-value-objects

    // PK and Oid handling
    public static MessageFormat pkcformatter = null; // cast PK statement
    public static MessageFormat oidcformatter = null; // cast Oid statement
    public static MessageFormat npkformatter = null; // new PK statement
    public static MessageFormat noidformatter = null; // new Oid statement
    public static MessageFormat pkformatter = null;
    public static MessageFormat oidformatter = null;
    public static MessageFormat oidstringformatter = null; // PK -> Oid for String PK field
    public static MessageFormat pkcloneformatter = null; // Oid -> PK for mutable PK field
    public static MessageFormat oidcloneformatter = null; // PK -> Oid for mutable PK field
    public static MessageFormat pk1formatter = null;
    public static MessageFormat oid1formatter = null;
    public static MessageFormat pk1pformatter = null;
    public static MessageFormat oid1pformatter = null;
    public static MessageFormat oid1stringformatter = null; // PK -> Oid for String PK class
    public static MessageFormat pk1cloneformatter = null; // Oid -> PK for mutable PK class
    public static MessageFormat oid1cloneformatter = null; // PK -> Oid for mutable PK class
    public static MessageFormat assertpkfieldformatter = null; // for assertPKFieldNotNull_

    // Other JDOHelper methods.
    public static MessageFormat pcclassgetterformatter = null;

    // standard templates for the corresponding keys, so that a template "xxxTemplate"
    // corresponds to a "xxx" key.
    public static String cmpImplCommentsTemplate = null;
    public static String privateStaticVariablesTemplate = null;
    public static String otherVariablesTemplate = null;
    public static String jdoGetJdoInstanceClassTemplate = null;
    public static String assertPersistenceManagerIsNullTemplate = null;
    public static String assertInstanceOfRemoteInterfaceImplTemplate = null;
    public static String getHelperInstanceTemplate = null;
    public static String catchClauseTemplate = null;
    public static String signatureTemplate = null;
    public static String assertPKNotNullTemplate = null;
    public static String assertOidNotNullTemplate = null;

    // standard templates that had been converted to String[] to be used
    // in a 'for' loop.
    public static String[] importsArray = null;
    public static String[] interfacesArray = null;
    public static String[] helperImportsArray = null;
    public static String[] commonPublicMethodsArray = null;
    public static String[] otherPublicMethodsArray = null;
    public static String[] commonPrivateMethodsArray = null;


    // standard method bodies for the corresponding keys, so that a method body with
    // a name "xxxBody" corresponds to a "xxx" key.
    public static String[] ejbFindByPrimaryKeyBody = null;
    public static String[] afterCompletionBody = null;
    public static String[] jdoGetPersistenceManagerBody = null;
    public static String[] jdoGetPersistenceManager0Body = null;
    public static String[] assertPersistenceManagerIsNullBody = null;
    public static String[] jdoClosePersistenceManagerBody = null;
    public static String[] jdoReleasePersistenceManagerBody = null;
    public static String[] jdoReleasePersistenceManager0Body = null;
    public static String[] setEntityContextBody = null;
    public static String[] getContainerBody = null;

    /**
     * Constructs a new <code>CMPTemplateFormatter</code> instance.
     */
    CMPTemplateFormatter() {
    }

    /**
     * Initializes templates for code generation.
     */
    static synchronized void initHelpers() throws IOException {
        if (helpers == null) {
            helpers = new Properties();
            loadProperties(helpers, templateFile);
            initFormatters();
            initTemplates();
        }
    }

    /**
     * Loads Properties object from the specified template file.
     */
    static synchronized void loadProperties(Properties helpers, 
            final String templateFile) throws IOException {

        BufferedInputStream bin = null;
        try {
            final ClassLoader loader = CMPTemplateFormatter.class.getClassLoader();
            InputStream in = (InputStream)java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction() {
                    public Object run() {
                        if (loader != null) {
                            return loader.getResourceAsStream(templateFile);
                        } else {
                            return ClassLoader.getSystemResourceAsStream(templateFile);
                        }
                    }
                }
            );

            bin = new BufferedInputStream(in);
            helpers.load(bin);
        } finally {
            try {
                bin.close();
            } catch (Exception e) {
                // no action
            }
        }
    }

    /**
     * Initializes MessageFormats for code generation.
     */
    private static void initFormatters() {
        // JDOHelper comments have the concrete impl name.
        hcomformatter = new MessageFormat(helpers.getProperty(helperComments_));

        // variables
        privatetransientvformatter = new MessageFormat(helpers.getProperty(privateTransientVariables_));
        privatestaticfinalvformatter = new MessageFormat(helpers.getProperty(privateStaticFinalVariables_));
        publicstaticfinalvformatter = new MessageFormat(helpers.getProperty(publicStaticFinalVariables_));
        finderselectorstaticvformatter = new MessageFormat(helpers.getProperty(finderSelectorStaticVariables_));
        finderselectorstaticfinalvformatter = new MessageFormat(helpers.getProperty(finderSelectorStaticFinalVariables_));
        hvformatter = new MessageFormat(helpers.getProperty(helperVariables_));

        // methods
        giformatter = new MessageFormat(helpers.getProperty(getInstance_));
        goidformatter = new MessageFormat(helpers.getProperty(getObjectId_));
        intxformatter = new MessageFormat(helpers.getProperty(assertInTransaction_));
        jdolookuppmfformatter = new MessageFormat(helpers.getProperty(jdoLookupPersistenceManagerFactory_));
        jdoarraycopyformatter = new MessageFormat(helpers.getProperty(jdoArrayCopy_));

        // finder/selector methods
        finderselectorformatter = new MessageFormat(helpers.getProperty(ejbFinderSelectorBody_));
        queryexecparamconvformatter = new MessageFormat(helpers.getProperty(ejbQueryExecutionParamConvBody_));
        queryexecparamconvargumentformatter = new MessageFormat(helpers.getProperty(ejbQueryExecutionParamConvBodyArgument_));
        queryexecformatter = new MessageFormat(helpers.getProperty(ejbQueryExecutionBody_));
        aggqueryexecformatter = new MessageFormat(helpers.getProperty(ejbAggregateQueryExecutionBody_));
        aggqueryexecparamconvformatter = new MessageFormat(helpers.getProperty(ejbAggregateQueryExecutionParamConvBody_));
        multifinderformatter = new MessageFormat(helpers.getProperty(ejbMultiFinderBody_));
        multifinderenumerationformatter = new MessageFormat(helpers.getProperty(ejbMultiFinderBodyEnumeration_));
        singlefinderformatter = new MessageFormat(helpers.getProperty(ejbSingleFinderBody_));

        // PK and Oid handling
        pkcformatter = new MessageFormat(helpers.getProperty(castKey_));
        oidcformatter = new MessageFormat(helpers.getProperty(castOid_));
        npkformatter = new MessageFormat(helpers.getProperty(newPK_));
        noidformatter = new MessageFormat(helpers.getProperty(newOid_));
        pk1formatter = new MessageFormat(helpers.getProperty(getPK1_));
        pk1cloneformatter = new MessageFormat(helpers.getProperty(getPK1Clone_));
        oid1formatter = new MessageFormat(helpers.getProperty(getOid1_));
        pk1pformatter = new MessageFormat(helpers.getProperty(getPK1primitive_));
        oid1pformatter = new MessageFormat(helpers.getProperty(getOid1primitive_));
        oid1stringformatter = new MessageFormat(helpers.getProperty(getOid1String_));
        oid1cloneformatter = new MessageFormat(helpers.getProperty(getOid1Clone_));
        pkformatter = new MessageFormat(helpers.getProperty(getPK_));
        pkcloneformatter = new MessageFormat(helpers.getProperty(getPKClone_));
        oidformatter = new MessageFormat(helpers.getProperty(getOid_));
        oidstringformatter = new MessageFormat(helpers.getProperty(getOidString_));
        oidcloneformatter = new MessageFormat(helpers.getProperty(getOidClone_));

        assertpkfieldformatter = new MessageFormat(helpers.getProperty(assertPKFieldNotNull_));

        // Other JDOHelper methods.
        pcclassgetterformatter = new MessageFormat(helpers.getProperty(getPCClass_));

    }

    /**
     * Initializes standard templates for code generation.
     */
    private static void initTemplates() {
        cmpImplCommentsTemplate = helpers.getProperty(cmpImplComments_);
        privateStaticVariablesTemplate = helpers.getProperty(privateStaticVariables_);
        otherVariablesTemplate = helpers.getProperty(otherVariables_);
        jdoGetJdoInstanceClassTemplate = helpers.getProperty(jdoGetJdoInstanceClass_);
        assertPersistenceManagerIsNullTemplate = helpers.getProperty(
            assertPersistenceManagerIsNullCall_);
        assertInstanceOfRemoteInterfaceImplTemplate = helpers.getProperty(
            assertInstanceOfRemoteInterfaceImpl_);
        getHelperInstanceTemplate = helpers.getProperty(getHelperInstance_);
        catchClauseTemplate = helpers.getProperty(catchClause_);
        signatureTemplate = helpers.getProperty(signature_);
        assertPKNotNullTemplate = helpers.getProperty(assertPKNotNull_);
        assertOidNotNullTemplate = helpers.getProperty(assertOidNotNull_);

        importsArray = tokenize(imports_);
        interfacesArray = tokenize(interfaces_);
        helperImportsArray = tokenize(helperImports_);
        commonPublicMethodsArray = tokenize(commonPublicMethods_);
        otherPublicMethodsArray = tokenize(otherPublicMethods_);
        commonPrivateMethodsArray = tokenize(commonPrivateMethods_);

        ejbFindByPrimaryKeyBody = getBodyAsStrings(helpers.getProperty(ejbFindByPrimaryKey_));
        afterCompletionBody = getBodyAsStrings(helpers.getProperty(afterCompletion_));
        jdoGetPersistenceManagerBody = getBodyAsStrings(helpers.getProperty(jdoGetPersistenceManager_));
        jdoGetPersistenceManager0Body = getBodyAsStrings(helpers.getProperty(jdoGetPersistenceManager0_));
        jdoClosePersistenceManagerBody = getBodyAsStrings(helpers.getProperty(jdoClosePersistenceManager_));
        jdoReleasePersistenceManagerBody = getBodyAsStrings(helpers.getProperty(jdoReleasePersistenceManager_));
        jdoReleasePersistenceManager0Body = getBodyAsStrings(helpers.getProperty(jdoReleasePersistenceManager0_));
        setEntityContextBody = getBodyAsStrings(helpers.getProperty(setEntityContext_));
        getContainerBody = getBodyAsStrings(helpers.getProperty(getContainer_));
        assertPersistenceManagerIsNullBody = getBodyAsStrings(helpers.getProperty(assertPersistenceManagerIsNull_));
    }

    static String[] tokenize(String template) {
        StringTokenizer st = new StringTokenizer(helpers.getProperty(template), delim_);
        String[] rc = new String[st.countTokens()];

        int i = 0;
        while (st.hasMoreElements()) {
            rc[i++] =  st.nextToken();
        }

        return rc;
    }
}
