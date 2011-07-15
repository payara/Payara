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
 * CMPROTemplateFormatter.java
 *
 * Created on March 03, 2004
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbc;

import java.io.*;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;

/*
 * This is the helper class for JDO specific generation of
 * a read-only concrete bean implementation.
 * This class does not extend CMPTemplateFormatter but only references
 * its variables when necessary. This allows to reuse CMPTemplateFormatter's
 * properties but the corresponding templates and formatters must be accessed
 * directly.
 *
 * @author Marina Vatkina
 */
public class CMPROTemplateFormatter {

    private final static String templateFile =
        "com/sun/jdo/spi/persistence/support/ejb/ejbc/CMPROTemplates.properties"; // NOI18N

    // Strings for CMP code generation:
    public final static String updateNotAllowed_    = "updateNotAllowed"; // NOI18N
    public final static String accessNotAllowed_    = "accessNotAllowed"; // NOI18N
    public final static String loadNonDFG_          = "loadNonDFG"; // NOI18N

    public final static String jdoGetPersistenceManagerByPK_ = "jdoGetPersistenceManagerByPK"; // NOI18N

    // Code generator templates.
    public static Properties helpers = null;

    // variables
    public static MessageFormat privatetransientvformatter = null; // privateTransientVariables

    // methods
    public static MessageFormat giformatter = null; // jdoGetInstance
    public static MessageFormat jdolookuppmfformatter = null; // jdoLookupPersistenceManagerFactory
    public static MessageFormat ejb__refreshformatter = null; // ejb__refresh
    public static MessageFormat loadNonDFGformatter       = null; // loadNonDFG

    // standard templates for the corresponding keys, so that a template "xxxTemplate"
    // corresponds to a "xxx" key.
    public static String privateStaticFinalVariablesTemplate = null;
    public static String signatureTemplate = null;
    public static String updateNotAllowedTemplate = null;
    public static String accessNotAllowedTemplate = null;
    public static String jdoCleanAllRefsTemplate = null;

    // standard method bodies for the corresponding keys, so that a method body with
    // a name "xxxBody" corresponds to a "xxx" key.
    public static String[] jdoGetPersistenceManagerBody      = null;
    public static String[] jdoGetPersistenceManager0Body     = null;
    public static String[] jdoReleasePersistenceManager0Body = null;
    public static String[] jdoGetPersistenceManagerByPKBody  = null;
    public static String[] jdoClosePersistenceManagerBody    = null;

    /**
     * Constructs a new <code>CMPROTemplateFormatter</code> instance.
     */
    CMPROTemplateFormatter() {
    }

    /**
     * Initializes templates for code generation.
     */
    static synchronized void initHelpers() throws IOException {
        if (helpers == null) {
            helpers = new Properties();
            CMPTemplateFormatter.loadProperties(helpers, templateFile);

            initFormatters();
            initTemplates();
        }
    }

    /**
     * Initializes MessageFormats for code generation.
     */
    private static void initFormatters() {
        // variables
        privatetransientvformatter = new MessageFormat(helpers.getProperty(
                CMPTemplateFormatter.privateTransientVariables_));

        // methods
        giformatter = new MessageFormat(helpers.getProperty(
                CMPTemplateFormatter.getInstance_));
        jdolookuppmfformatter = new MessageFormat(helpers.getProperty(
                CMPTemplateFormatter.jdoLookupPersistenceManagerFactory_));
        ejb__refreshformatter = new MessageFormat(helpers.getProperty(
                CMPTemplateFormatter.ejb__refresh_));
        loadNonDFGformatter = new MessageFormat(helpers.getProperty(loadNonDFG_));
    }

    /**
     * Initializes standard templates for code generation.
     */
    private static void initTemplates() {
        privateStaticFinalVariablesTemplate = helpers.getProperty(
                CMPTemplateFormatter.privateStaticFinalVariables_);
        signatureTemplate = helpers.getProperty(CMPTemplateFormatter.signature_);
        updateNotAllowedTemplate = helpers.getProperty(updateNotAllowed_);
        accessNotAllowedTemplate = helpers.getProperty(accessNotAllowed_);
        jdoCleanAllRefsTemplate = helpers.getProperty(CMPTemplateFormatter.jdoCleanAllRefs_);

        jdoGetPersistenceManagerBody = CMPTemplateFormatter.getBodyAsStrings(
                helpers.getProperty(CMPTemplateFormatter.jdoGetPersistenceManager_));
        jdoGetPersistenceManager0Body = CMPTemplateFormatter.getBodyAsStrings(
                helpers.getProperty(CMPTemplateFormatter.jdoGetPersistenceManager0_));
        jdoGetPersistenceManagerByPKBody = CMPTemplateFormatter.getBodyAsStrings(
                helpers.getProperty(jdoGetPersistenceManagerByPK_));
        jdoClosePersistenceManagerBody = CMPTemplateFormatter.getBodyAsStrings(
                helpers.getProperty(CMPTemplateFormatter.jdoClosePersistenceManager_));
        jdoReleasePersistenceManager0Body = CMPTemplateFormatter.getBodyAsStrings(
                helpers.getProperty(CMPTemplateFormatter.jdoReleasePersistenceManager0_));
    }
}
