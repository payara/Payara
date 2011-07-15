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
 * CMP11TemplateFormatter.java
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
 * a concrete bean implementation for CMP 1.1 beans.
 * Because both CMP20TemplateFormatter and this class extend 
 * CMPTemplateFormatter, and all references are static, properties 
 * in this class must differ in names. This is why they have 1_1
 * or 11 in them.
 *
 * @author Marina Vatkina
 */
class CMP11TemplateFormatter extends CMPTemplateFormatter {

    private final static String templateFile =
        "com/sun/jdo/spi/persistence/support/ejb/ejbc/CMP11Templates.properties"; // NOI18N

    // Strings for CMP 1.1 code generation:
    public final static String one_oneVariables_          = "one_oneVariables"; // NOI18N
    public final static String ejbQuerySetIgnoreCache_    = "ejbQuerySetIgnoreCache"; //NOI18N

    // Inner helper Class strings.
    public final static String helper11Interface_
                      = "com.sun.jdo.spi.persistence.support.sqlstore.ejb.JDOEJB11Helper"; // NOI18N
    public final static String helper11Impl_
                      = "com.sun.jdo.spi.persistence.support.ejb.cmp.JDOEJB11HelperImpl"; // NOI18N

    // CMP1.1 properties
    public final static String ejbCreate1_1_              = "ejbCreate1_1"; // NOI18N
    public final static String ejbCreateUnknownPK1_1_     = "ejbCreateUnknownPK1_1"; // NOI18N
    public final static String ejbPostCreate1_1_          = "ejbPostCreate1_1"; // NOI18N
    public final static String ejbRemove1_1_              = "ejbRemove1_1"; // NOI18N
    public final static String storeFields1_1_            = "jdoStoreFields"; // NOI18N
    public final static String loadFields1_1_             = "jdoLoadFields"; // NOI18N
    public final static String setEntityContext1_1_       = "setEntityContext1_1"; // NOI18N
    public final static String internalMethods1_1_        = "internalMethods1_1"; // NOI18N
    public final static String helperMethods1_1_          = "helperMethods1_1"; // NOI18N
    public final static String load1_1_                   = "load1_1"; // NOI18N
    public final static String store1_1_                  = "store1_1"; // NOI18N
    public final static String assertPKStore1_1_          = "assertPKStore1_1"; // NOI18N
    public final static String pkstore1_1_                = "pkstore1_1"; // NOI18N
    public final static String pkstringstore1_1_          = "pkstringstore1_1"; // NOI18N
    public final static String pkcopystore1_1_            = "pkcopystore1_1"; // NOI18N
    public final static String copyload1_1_               = "copyload1_1"; // NOI18N
    public final static String copystore1_1_              = "copystore1_1"; // NOI18N
    public final static String arrayload1_1_              = "arrayload1_1"; // NOI18N
    public final static String arraystore1_1_             = "arraystore1_1"; // NOI18N
    public final static String loadSerializable1_1_       = "loadSerializable1_1"; // NOI18N
    public final static String storeSerializable1_1_      = "storeSerializable1_1"; // NOI18N

    // property key for the CVS keyword substitution
    public final static String signature1_1_ = "signature1_1"; //NOI18N

    // finder/selector methods
    public static MessageFormat querysetignorecacheformatter = null; // statement to set the ignoreCache flag for a JDOQL query 

    // CMP1.1 formatter
    public static MessageFormat c11formatter = null; // ejbCreate1_1
    public static MessageFormat c11unpkformatter = null; // ejbCreateUnknownPK1_1
    public static MessageFormat postc11formatter = null; // ejbPostCreate1_1
    public static MessageFormat l11formatter = null; // CMP field load for 1.1
    public static MessageFormat s11formatter = null; // CMP field store for 1.1
    public static MessageFormat l11copyformatter = null; // Mutable CMP field load for 1.1
    public static MessageFormat s11copyformatter = null; // Mutable CMP field store for 1.1
    public static MessageFormat l11arrayformatter = null; // byte[] CMP field load for 1.1
    public static MessageFormat s11arrayformatter = null; // byte[] CMP field store for 1.1
    public static MessageFormat assertpks11formatter = null; // assert not null PK CMP field store for 1.1
    public static MessageFormat pks11formatter = null; // PK CMP field store for 1.1
    public static MessageFormat pkstring11formatter = null; // String CMP field store for 1.1
    public static MessageFormat pkcopy11formatter = null; // Mutable CMP field store for 1.1
    public static MessageFormat l11Serializableformatter = null; // serializable CMP field load for 1.1
    public static MessageFormat s11Serializableformatter = null; // serializable CMP field store for 1.1

    // standard 1.1 templates for the corresponding keys, so that a template "xxxTemplate"
    // corresponds to a "xxx" key.
    public static String one_oneVariablesTemplate = null;
    public static String ejbRemove1_1Template = null;
    public static String signature1_1Template = null;

    private static boolean is11HelpersLoaded = false;

    /**
     * Constructs a new <code>CMP11TemplateFormatter</code> instance.
     */
    CMP11TemplateFormatter() {
    }

    /**
     * Initializes templates for code generation.
     */
    static synchronized void initHelpers() throws IOException {
        if (is11HelpersLoaded == false) {
            loadProperties(helpers, templateFile);
            init11Formatters();
            init11Templates();

            is11HelpersLoaded = true;

        }
    }

    /**
     * Initializes MessageFormats for code generation.
     */
    private static void init11Formatters() {
        // 1.1 finder methods
        querysetignorecacheformatter = new MessageFormat(helpers.getProperty(ejbQuerySetIgnoreCache_));

        // CMP1.1 formatters
        c11formatter = new MessageFormat(helpers.getProperty(ejbCreate1_1_));
        c11unpkformatter = new MessageFormat(helpers.getProperty(ejbCreateUnknownPK1_1_));
        postc11formatter = new MessageFormat(helpers.getProperty(ejbPostCreate1_1_));
        l11formatter = new MessageFormat(helpers.getProperty(load1_1_));
        s11formatter = new MessageFormat(helpers.getProperty(store1_1_));
        assertpks11formatter = new MessageFormat(helpers.getProperty(assertPKStore1_1_));
        pks11formatter = new MessageFormat(helpers.getProperty(pkstore1_1_));
        pkstring11formatter = new MessageFormat(helpers.getProperty(pkstringstore1_1_));
        pkcopy11formatter = new MessageFormat(helpers.getProperty(pkcopystore1_1_));
        l11copyformatter = new MessageFormat(helpers.getProperty(copyload1_1_));
        s11copyformatter = new MessageFormat(helpers.getProperty(copystore1_1_));
        l11arrayformatter = new MessageFormat(helpers.getProperty(arrayload1_1_));
        s11arrayformatter = new MessageFormat(helpers.getProperty(arraystore1_1_));
        l11Serializableformatter = new MessageFormat(helpers.getProperty(loadSerializable1_1_));
        s11Serializableformatter = new MessageFormat(helpers.getProperty(storeSerializable1_1_));
    }

    /**
     * Initializes standard templates for code generation.
     */
    private static void init11Templates() {
        one_oneVariablesTemplate = helpers.getProperty(one_oneVariables_);
        ejbRemove1_1Template = helpers.getProperty(ejbRemove1_1_);
        signature1_1Template = helpers.getProperty(signature1_1_);
    }
}
