/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.servermgmt.stringsubs.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.admin.servermgmt.SLogger;

/**
 * Load and retrieves the string substitution properties.
 */
public class StringSubstitutionProperties {

    private static final Logger _logger = SLogger.getLogger();
    
    private static final String STRINGSUBS_PROPERTIES = "/com/sun/enterprise/admin/servermgmt/stringsubs/stringsubs.properties";
    private static Properties _properties = null;

    /**
     * Loads the string substitution properties i.e {@link StringSubstitutionProperties#STRINGSUBS_PROPERTIES} file
     */
    private static void load() {
        InputStream in = null;
        try {
            in = StringSubstitutionProperties.class.getResourceAsStream(STRINGSUBS_PROPERTIES);
            _properties = new Properties();
            _properties.load(in);
        } catch (IOException e) {
            _logger.log(Level.INFO, SLogger.INVALID_FILE_LOCATION, STRINGSUBS_PROPERTIES);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception io)
                { /** ignore*/ }
            }
        }
    }

    /**
     * Searches for the property with the specified key in this property list.
     * The method returns <code>null</code> if the property is not found.
     *
     * @param   key   the property key.
     * @return  the value in this property list with the specified key value.
     */
    public static String getProperty(String key) {
        if (_properties == null) {
            load();
        }
        return _properties.getProperty(key);
    }
}