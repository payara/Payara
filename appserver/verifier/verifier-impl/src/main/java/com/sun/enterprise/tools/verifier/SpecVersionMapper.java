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

package com.sun.enterprise.tools.verifier;

import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * This class is responsible for mapping Java EE platform version to
 * various component spec versions.
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class SpecVersionMapper {

    private static LocalStringManagerImpl smh = 
                                StringManagerHelper.getLocalStringsManager();

    public static final String JavaEEVersion_1_2 = "1.2"; // NOI18N

    public static final String JavaEEVersion_1_3 = "1.3"; // NOI18N

    public static final String JavaEEVersion_1_4 = "1.4"; // NOI18N

    public static final String JavaEEVersion_5 = "5"; // NOI18N

    private static String[][] PlatformVersionToEJBVersionMap = {
        {JavaEEVersion_1_2, "1.1"}, // NOI18N
        {JavaEEVersion_1_3, "2.0"}, // NOI18N
        {JavaEEVersion_1_4, "2.1"}, // NOI18N
        {JavaEEVersion_5, "3.0"} // NOI18N
    };

    private static String[][] PlatformVersionToAppClientVersionMap = {
        {JavaEEVersion_1_2, "1.2"}, // NOI18N
        {JavaEEVersion_1_3, "1.3"}, // NOI18N
        {JavaEEVersion_1_4, "1.4"}, // NOI18N
        {JavaEEVersion_5, "5"} // NOI18N
    };

    private static String[][] PlatformVersionToJCAVersionMap = {
        {JavaEEVersion_1_3, "1.0"}, // NOI18N
        {JavaEEVersion_1_4, "1.5"}, // NOI18N
        {JavaEEVersion_5, "1.5"} // NOI18N
    };

    private static String[][] PlatformVersionToWebAppVersionMap = {
        {JavaEEVersion_1_2, "2.2"}, // NOI18N
        {JavaEEVersion_1_3, "2.3"}, // NOI18N
        {JavaEEVersion_1_4, "2.4"}, // NOI18N
        {JavaEEVersion_5, "2.5"} // NOI18N
    };

    private static String[][] PlatformVersionToWebServiceVersionMap = {
        {JavaEEVersion_1_4, "1.1"}, // NOI18N
        {JavaEEVersion_5, "1.2"} // NOI18N
    };

    private static String[][] PlatformVersionToWebServiceClientVersionMap = {
        {JavaEEVersion_1_4, "1.1"}, // NOI18N
        {JavaEEVersion_5, "1.2"} // NOI18N
    };
    
    private static String throwException (String platformVersion) 
            throws IllegalArgumentException {
        throw new IllegalArgumentException(
                smh.getLocalString("com.sun.enterprise.tools.verifier.SpecVersionMapper.exception", // NOI18N
                        "Not able to map platform version [ {0} ] component version.", // NOI18N
                        new Object[] {platformVersion}));
    }

    public static String getEJBVersion(String platformVersion)
            throws IllegalArgumentException {
        for (String[] row : PlatformVersionToEJBVersionMap) {
            if (row[0].equals(platformVersion)) {
                return row[1];
            }
        }
        return throwException(platformVersion);
    }

    public static String getJCAVersion(String platformVersion)
            throws IllegalArgumentException {
        for (String[] row : PlatformVersionToJCAVersionMap) {
            if (row[0].equals(platformVersion)) {
                return row[1];
            }
        }
        return throwException(platformVersion);
    }

    public static String getWebAppVersion(String platformVersion)
            throws IllegalArgumentException {
        for (String[] row : PlatformVersionToWebAppVersionMap) {
            if (row[0].equals(platformVersion)) {
                return row[1];
            }
        }
        return throwException(platformVersion);
    }

    public static String getAppClientVersion(String platformVersion)
            throws IllegalArgumentException {
        for (String[] row : PlatformVersionToAppClientVersionMap) {
            if (row[0].equals(platformVersion)) {
                return row[1];
            }
        }
        return throwException(platformVersion);
    }

    public static String getWebServiceVersion(String platformVersion)
            throws IllegalArgumentException {
        for (String[] row : PlatformVersionToWebServiceVersionMap) {
            if (row[0].equals(platformVersion)) {
                return row[1];
            }
        }
        return throwException(platformVersion);
    }

    public static String getWebServiceClientVersion(String platformVersion) {
        for (String[] row : PlatformVersionToWebServiceClientVersionMap) {
            if (row[0].equals(platformVersion)) {
                return row[1];
            }
        }
        return throwException(platformVersion);
    }
}
