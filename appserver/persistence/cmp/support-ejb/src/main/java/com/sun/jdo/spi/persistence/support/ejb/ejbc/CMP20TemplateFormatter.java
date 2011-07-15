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
 * CMP20TemplateFormatter.java
 *
 * Created on February 25, 2004
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
 * a concrete bean implementation for CMP 2.0 beans.
 * Because both CMP11TemplateFormatter and this class extend
 * CMPTemplateFormatter, and all references are static, properties
 * in this class must differ in names if there is a similar property
 * in the super class. Such properties have 2_0 or 20 in them.
 *
 * @author Marina Vatkina
 */
class CMP20TemplateFormatter extends CMPTemplateFormatter {

    private final static String templateFile =
        "com/sun/jdo/spi/persistence/support/ejb/ejbc/CMP20Templates.properties"; // NOI18N

    // Strings for CMP 2.0 code generation:
    public final static String local_                     = "Local"; // NOI18N
    public final static String ejbLocalObject_            = "EJBLocalObject"; // NOI18N
    public final static String ejbLocalHome_              = "EJBLocalHome"; // NOI18N
    public final static String getter_                    = "getter"; // NOI18N
    public final static String setter_                    = "setter"; // NOI18N
    public final static String assertPKsetter_            = "assertPKsetter"; // NOI18N
    public final static String pksetter_                  = "pksetter"; // NOI18N
    public final static String pkstringsetter_            = "pkstringsetter"; // NOI18N
    public final static String pkcopysetter_              = "pkcopysetter"; // NOI18N
    public final static String copygetter_                = "copygetter"; // NOI18N
    public final static String copysetter_                = "copysetter"; // NOI18N
    public final static String arraygetter_               = "arraygetter"; // NOI18N
    public final static String arraysetter_               = "arraysetter"; // NOI18N
    public final static String sfldgetter_                = "sfldGetter"; // NOI18N
    public final static String sfldsetter_                = "sfldSetter"; // NOI18N
    public final static String cmrGetter_                 = "cmrGetter"; // NOI18N
    public final static String cmrSetter_                 = "cmrSetter"; // NOI18N
    public final static String cmrGetterCollection_       = "cmrGetterCollection"; // NOI18N
    public final static String cmrSetterCollection_       = "cmrSetterCollection"; // NOI18N
    public final static String startCascadeDelete_        = "startCascadeDelete"; // NOI18N
    public final static String endCascadeDelete_          = "endCascadeDelete"; // NOI18N
    public final static String cmrCascadeDelete_          = "cmrCascadeDelete"; // NOI18N
    public final static String cmrCascadeDeleteCollection_ = "cmrCascadeDeleteCollection"; // NOI18N
    public final static String localCmrVariables_         = "localCmrVariables"; // NOI18N
    public final static String cleanCollectionCmr_        = "cleanCollectionCmr"; // NOI18N
    public final static String ejbFinderSelectorParamCheckLocalInterface_
                                     = "ejbFinderSelectorParamCheckLocalInterface"; // NOI18N
    public final static String ejbFinderSelectorParamCheckRemoteInterface_
                                     = "ejbFinderSelectorParamCheckRemoteInterface"; // NOI18N
    public final static String ejbMultiSelectorBodyConversion_   = "ejbMultiSelectorBodyConversion"; // NOI18N
    public final static String ejbMultiSelectorBody_           = "ejbMultiSelectorBody"; // NOI18N
    public final static String ejbMultiSelectorBodySet_        = "ejbMultiSelectorBodySet"; // NOI18N
    public final static String ejbSingleSelectorReturnBodyConversion_    = "ejbSingleSelectorReturnBodyConversion"; // NOI18N
    public final static String ejbSingleSelectorReturnBody_    = "ejbSingleSelectorReturnBody"; // NOI18N
    public final static String ejbAggregateSelectorPrimitiveReturnBody_    = "ejbAggregateSelectorPrimitiveReturnBody"; // NOI18N
    public final static String ejbAggregateSelectorReturnBody_    = "ejbAggregateSelectorReturnBody"; // NOI18N
    public final static String ejbAggregateSelectorReturnBodyConversion_    = "ejbAggregateSelectorReturnBodyConversion"; // NOI18N
    public final static String ejbAggregateSelectorReturnBigDecimalConversion_    = "ejbAggregateSelectorReturnBigDecimalConversion"; // NOI18N
    public final static String ejbAggregateSelectorReturnBigIntegerConversion_    = "ejbAggregateSelectorReturnBigIntegerConversion"; // NOI18N
    public final static String ejbSingleSelectorBody_    = "ejbSingleSelectorBody"; // NOI18N
    public final static String preSelect_                = "preSelect"; // NOI18N
    public final static String convertPCToEJBLocalObject_
                                     = "convertPCToEJBLocalObject"; // NOI18N
    public final static String convertCollectionPCToEJBObject_
                                     = "convertCollectionPCToEJBObject"; // NOI18N
    public final static String convertCollectionPCToEJBObjectSet_
                                     = "convertCollectionPCToEJBObjectSet"; // NOI18N
    public final static String convertCollectionPCToEJBLocalObject_
                                     = "convertCollectionPCToEJBLocalObject"; // NOI18N
    public final static String convertCollectionPCToEJBLocalObjectSet_
                                     = "convertCollectionPCToEJBLocalObjectSet"; // NOI18N
    public final static String jdoCleanCollectionRef_    = "jdoCleanCollectionRef"; // NOI18N

    // Inner helper Class strings.
    public final static String helper20Interface_
                      = "com.sun.jdo.spi.persistence.support.sqlstore.ejb.JDOEJB20Helper"; // NOI18N
    public final static String helper20Impl_
                      = "com.sun.jdo.spi.persistence.support.ejb.cmp.JDOEJB20HelperImpl"; // NOI18N

    public final static String ejbHashSetImport_
                      = "com.sun.jdo.spi.persistence.support.ejb.cmp.EJBHashSet"; // NOI18N

    public final static String assertInstanceOfLocalInterfaceImpl_
                                                          = "assertInstanceOfLocalInterfaceImpl"; // NOI18N
    // property key for the CVS keyword substitution
    public final static String signature2_0_ = "signature2_0"; //NOI18N

    // getters and setters
    public static MessageFormat gformatter = null; // CMP field getter
    public static MessageFormat sformatter = null; // CMP field setter
    public static MessageFormat copygformatter = null; // Date CMP field uses copygetter
    public static MessageFormat copysformatter = null; // Date CMP field uses copysetter
    public static MessageFormat arraygformatter = null; // byte[] CMP field uses arraygetter
    public static MessageFormat arraysformatter = null; // byte[] CMP field uses arraysetter
    public static MessageFormat assertpksformatter = null; // assert not null PK field setter
    public static MessageFormat pksformatter = null; // PK field setter
    public static MessageFormat pkstringsformatter = null; // String PK CMP field setter (calls trim())
    public static MessageFormat pkcopysformatter = null; // Mutable PK CMP field setter (calls clone())
    public static MessageFormat sfldgformatter = null; // Serializable CMP field getter
    public static MessageFormat sfldsformatter = null; // Serializable CMP field setter

    public static MessageFormat cmrgformatter = null; // 1-side CMR getter
    public static MessageFormat cmrsformatter = null; // 1-side CMR setter
    public static MessageFormat cmrCgformatter = null; // many-side CMR getter
    public static MessageFormat cmrCsformatter = null; // many-side CMR setter
    public static MessageFormat cmrcdformatter = null; // cascade-delete for one-one CMR
    public static MessageFormat cmrcdCformatter = null; // cascade-delete for one-many CMR

    // 2.0 variables
    public static MessageFormat cmrvformatter = null; // local cmr variables
    public static MessageFormat cleancmrformatter = null; // clean collection cmr references

    // 2.0 methods
    public static MessageFormat cformatter = null; // ejbCreate
    public static MessageFormat cunpkformatter = null; // ejbCreateUnknownPK
    public static MessageFormat postcformatter = null; // ejbPostCreate
    public static MessageFormat rmformatter = null; // ejbRemove

    // finder/selector methods
    public static MessageFormat finderselectorchecklocalformatter  = null; //common body for selector and finder param check for local interface impl class
    public static MessageFormat finderselectorcheckremoteformatter  = null; //common body for selector and finder param check for remote interface impl class
    public static MessageFormat preselectformatter = null; // preSelect callback
    public static MessageFormat multiselectorconvformatter = null; // selector for multi-value-objects body with resultset-conversion
    public static MessageFormat multiselectorformatter = null; // selector for multi-value-objects body
    public static MessageFormat multiselectorsetformatter = null; // selector for multi-value-objects body and conversion to Set
    public static MessageFormat singleselectorreturnconvformatter = null; // selector for single-value-objects body with resultset-conversion
    public static MessageFormat singleselectorreturnformatter = null; // selector for single-value-objects body with resultset-conversion
    public static MessageFormat aggregateselectorprimitivereturnformatter = null; // selector for aggregate functions return primitive without object conversion
    public static MessageFormat aggregateselectorreturnformatter = null; // selector for aggregate functions without object conversion
    public static MessageFormat aggregateselectorreturnconvformatter = null; // selector for aggregate functions with object conversion
    public static MessageFormat aggregateselectorreturnbigdecimalconvformatter = null; // selector for aggregate functions with BigDecimal conversion
    public static MessageFormat aggregateselectorreturnbigintegerconvformatter = null; // selector for aggregate functions with BigInteger conversion
    public static MessageFormat singleselectorformatter = null; // selector for single-value-objects body

    // standard templates for the corresponding keys, so that a template "xxxTemplate"
    // corresponds to a "xxx" key.
    public static String startCascadeDeleteTemplate = null;
    public static String endCascadeDeleteTemplate = null;
    public static String assertInstanceOfLocalInterfaceImplTemplate = null;
    public static String signature2_0Template = null;

    private static boolean is20HelpersLoaded = false;

    /**
     * Constructs a new <code>CMP20TemplateFormatter</code> instance.
     */
    CMP20TemplateFormatter() {
    }

    /**
     * Initializes templates for code generation.
     */
    static synchronized void initHelpers() throws IOException {
        if (is20HelpersLoaded == false) {
            loadProperties(helpers, templateFile);
            init20Formatters();
            init20Templates();

            is20HelpersLoaded = true;

        }
    }

    /**
     * Initializes MessageFormats for code generation.
     */
    private static void init20Formatters() {
        // getters and setters
        gformatter = new MessageFormat(helpers.getProperty(getter_));
        sformatter = new MessageFormat(helpers.getProperty(setter_));
        copygformatter = new MessageFormat(helpers.getProperty(copygetter_));
        copysformatter = new MessageFormat(helpers.getProperty(copysetter_));
        arraygformatter = new MessageFormat(helpers.getProperty(arraygetter_));
        arraysformatter = new MessageFormat(helpers.getProperty(arraysetter_));
        assertpksformatter = new MessageFormat(helpers.getProperty(assertPKsetter_));
        pksformatter = new MessageFormat(helpers.getProperty(pksetter_));
        pkstringsformatter = new MessageFormat(helpers.getProperty(pkstringsetter_));
        pkcopysformatter = new MessageFormat(helpers.getProperty(pkcopysetter_));
        sfldsformatter = new MessageFormat(helpers.getProperty(sfldsetter_));
        sfldgformatter = new MessageFormat(helpers.getProperty(sfldgetter_));
        cmrgformatter = new MessageFormat(helpers.getProperty(cmrGetter_));
        cmrsformatter = new MessageFormat(helpers.getProperty(cmrSetter_));
        cmrCgformatter = new MessageFormat(helpers.getProperty(cmrGetterCollection_));
        cmrCsformatter = new MessageFormat(helpers.getProperty(cmrSetterCollection_));
        cmrcdformatter = new MessageFormat(helpers.getProperty(cmrCascadeDelete_));
        cmrcdCformatter = new MessageFormat(helpers.getProperty(cmrCascadeDeleteCollection_));

        // 2.0 variables
        cmrvformatter = new MessageFormat(helpers.getProperty(localCmrVariables_));
        cleancmrformatter = new MessageFormat(helpers.getProperty(cleanCollectionCmr_));

        // 2.0 methods
        cformatter = new MessageFormat(helpers.getProperty(ejbCreate_));
        cunpkformatter = new MessageFormat(helpers.getProperty(ejbCreateUnknownPK_));
        postcformatter = new MessageFormat(helpers.getProperty(ejbPostCreate_));
        rmformatter = new MessageFormat(helpers.getProperty(ejbRemove_));

        // 2.0 finder/selector methods
        finderselectorchecklocalformatter = new MessageFormat(helpers.getProperty(ejbFinderSelectorParamCheckLocalInterface_));
        finderselectorcheckremoteformatter = new MessageFormat(helpers.getProperty(ejbFinderSelectorParamCheckRemoteInterface_));
        preselectformatter = new MessageFormat(helpers.getProperty(preSelect_));
        multiselectorconvformatter = new MessageFormat(helpers.getProperty(ejbMultiSelectorBodyConversion_));
        multiselectorformatter = new MessageFormat(helpers.getProperty(ejbMultiSelectorBody_));
        multiselectorsetformatter = new MessageFormat(helpers.getProperty(ejbMultiSelectorBodySet_));
        singleselectorreturnconvformatter = new MessageFormat(helpers.getProperty(ejbSingleSelectorReturnBodyConversion_));
        singleselectorreturnformatter = new MessageFormat(helpers.getProperty(ejbSingleSelectorReturnBody_));
        aggregateselectorprimitivereturnformatter = new MessageFormat(helpers.getProperty(ejbAggregateSelectorPrimitiveReturnBody_));
        aggregateselectorreturnformatter = new MessageFormat(helpers.getProperty(ejbAggregateSelectorReturnBody_));
        aggregateselectorreturnconvformatter = new MessageFormat(helpers.getProperty(ejbAggregateSelectorReturnBodyConversion_));
        aggregateselectorreturnbigdecimalconvformatter = new MessageFormat(helpers.getProperty(ejbAggregateSelectorReturnBigDecimalConversion_));
        aggregateselectorreturnbigintegerconvformatter = new MessageFormat(helpers.getProperty(ejbAggregateSelectorReturnBigIntegerConversion_));
        singleselectorformatter = new MessageFormat(helpers.getProperty(ejbSingleSelectorBody_));
    }

    /**
     * Initializes standard templates for code generation.
     */
    private static void init20Templates() {
        startCascadeDeleteTemplate = helpers.getProperty(startCascadeDelete_);
        endCascadeDeleteTemplate = helpers.getProperty(endCascadeDelete_);
        assertInstanceOfLocalInterfaceImplTemplate = helpers.getProperty(
            assertInstanceOfLocalInterfaceImpl_);
        signature2_0Template = helpers.getProperty(signature2_0_);
    }
}
