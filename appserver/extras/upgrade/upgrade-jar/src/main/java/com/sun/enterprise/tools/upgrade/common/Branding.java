/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.upgrade.common;

import com.sun.enterprise.tools.upgrade.logging.LogService;
import com.sun.enterprise.util.i18n.StringManager;
import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * To allow (very) simple rebranding of the upgrade tool, this class
 * encapsulates the logic needed to look for an external set of
 * string properties and an image location.
 *
 * All getString methods take a StringManager object so that they
 * can return the default strings if no external branding jar
 * is present.
 */
public final class Branding {

    private static final Logger logger = LogService.getLogger();
    private static final Level DEBUG_LEVEL = Level.FINE;

    // external optional branding bundle
    private static final String BUNDLE_PACKAGE =
        "com.sun.enterprise.tools.upgrade.branding";
    private static final String BUNDLE_NAME = BUNDLE_PACKAGE + ".LocalStrings";

    private static final StringManager sm = resolveStringManager();
    static {
        if (logger.isLoggable(DEBUG_LEVEL) && sm != null) {
            logger.log(DEBUG_LEVEL, "Test message. Here's GUI title:" +
                sm.getString("upgrade.gui.mainframe.titleMessage", "---"));
        }
    }

    /*
     * We want to check for the presence of the bundle ourselves
     * first to avoid having StringManager log an exception if it's
     * missing. This bundle is optional.
     */
    private static StringManager resolveStringManager() {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        try {
            if (logger.isLoggable(DEBUG_LEVEL)) {
                logger.fine("Looking for external branding bundle.");
            }
            ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault(), loader);
            if (logger.isLoggable(DEBUG_LEVEL)) {
                logger.fine("Found external branding bundle.");
            }
        } catch (MissingResourceException mre) {            
            if (logger.isLoggable(DEBUG_LEVEL)) {
                logger.log(DEBUG_LEVEL,
                    "Did not find branding bundle. Will use default strings.",
                    mre);
            }
            return null;
        }
        if (logger.isLoggable(DEBUG_LEVEL)) {
            logger.fine(String.format(
                "Retrieving StringManager with package %s", BUNDLE_PACKAGE));
        }
        return StringManager.getManager(BUNDLE_PACKAGE, loader);
    }

    public static String getString(String key, StringManager defaultSM) {
        if (sm == null) {
            return defaultSM.getString(key);
        }
        return sm.getString(key);
    }

    public static String getString(String key,
        StringManager defaultSM, Object arg) {
        if (sm == null) {
            return defaultSM.getString(key, arg);
        }
        return sm.getString(key, arg);
    }

    /*
     * Am using the existence of the string manager to know if
     * the external branding jar is present or not.
     */
    public static URL getWizardUrl(String defaultURLString) {
        String brandedURLString =
            "com/sun/enterprise/tools/upgrade/branding/Appserv_upgrade_wizard.gif";
        if (sm == null) {
            // use default
            brandedURLString = defaultURLString;
        }
        return ClassLoader.getSystemClassLoader().getResource(brandedURLString);
    }

    // again, just return default if there is no external branding
    public static String getHSString(String defaultHSString) {
        return (sm == null ? defaultHSString :
            "com/sun/enterprise/tools/upgrade/branding/javahelp/UpgradeToolHelp.hs");
    }

}
