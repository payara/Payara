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

package com.sun.enterprise.tools.verifier.apiscan.classfile;

import java.util.logging.Logger;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class Util {

    private static Logger logger = Logger.getLogger("apiscan.classfile"); // NOI18N

    private static final String myClassName = "Util"; // NOI18N

    /**
     * @param internalClsName is the name in internal format
     * @return the class name in external format, i,e. format used in reflection
     *         API (e.g. Class.forName())
     */
    public static String convertToExternalClassName(String internalClsName) {
        return internalClsName.replace('/', '.');
    }

    /**
     * @param externalClsName is the name in internal format
     * @return the class name in internal format, i,e. format used in byte code
     */
    public static String convertToInternalClassName(String externalClsName) {
        return externalClsName.replace('.', '/');
    }

    public static boolean isPrimitive(String className) {
        logger.entering(myClassName, "isPrimitive", new Object[]{className}); // NOI18N
        boolean result = ("B".equals(className) || // NOI18N
                "C".equals(className) || // NOI18N
                "D".equals(className) || // NOI18N
                "F".equals(className) || // NOI18N
                "I".equals(className) || // NOI18N
                "J".equals(className) || // NOI18N
                "S".equals(className) || // NOI18N
                "Z".equals(className)); // NOI18N
        logger.exiting(myClassName, "isPrimitive", result); // NOI18N
        return result;
    }
}
