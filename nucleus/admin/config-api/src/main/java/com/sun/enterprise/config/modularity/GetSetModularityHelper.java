/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.modularity;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.config.support.Singleton;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * The main driver to make the getset commands compatible with the config modularity.
 *
 * @author Masoud Kalali
 */
@Service
@Singleton
public class GetSetModularityHelper {

    @Inject
    private ConfigModularityUtils configModularityUtils;

    @Inject
    private Domain domain;

    /**
     * checks and see if a class has an attribute with he specified name or not.
     *
     * @param classToQuery  the class toc heck the attribute presence
     * @param attributeName the attribute to check its presence in the class.
     * @return true if present and false if not.
     */
    private boolean checkAttributePresence(Class classToQuery, String attributeName) {
        String fieldName = convertAttributeToPropertyName(attributeName);
        String methodName = "set" + fieldName.replaceFirst(fieldName.substring(0, 1), String.valueOf(Character.toUpperCase(fieldName.charAt(0))));
        Method[] methods = classToQuery.getMethods();
        for (Method m : methods) {
            if (m.getName().equals(methodName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * convert an xml attribute name to variable name representing it.
     *
     * @param attributeName the attribute name in "-" separated form as appears in the domain.xml
     * @return the class instance variable which represent that attributeName
     */
    private String convertAttributeToPropertyName(String attributeName) {
        StringTokenizer tokenizer = new StringTokenizer(attributeName, "-", false);
        StringBuilder propertyName = new StringBuilder();
        boolean isFirst = true;
        while (tokenizer.hasMoreTokens()) {
            String part = tokenizer.nextToken();
            if (!isFirst) {
                Locale loc = Locale.getDefault();
                part = part.replaceFirst(part.substring(0, 1), part.substring(0, 1).toUpperCase(loc));
            }
            isFirst = false;
            propertyName.append(part);
        }
        return propertyName.toString();
    }

    /**
     * @param prefix   the entire . separated string
     * @param position starts with one
     * @return the configbean class matching the element in the given position or null
     */
    private Class getElementClass(String prefix, int position) {

        StringTokenizer tokenizer = new StringTokenizer(prefix, ".");
        String token = null;
        for (int i = 0; i < position; i++) {
            if (tokenizer.hasMoreTokens()) {
                token = tokenizer.nextToken();
            } else {
                return null;
            }
        }
        if (token != null) {
            return configModularityUtils.getClassFor(token);
        }
        return null;
    }

    /**
     * @param string   the entire . separated string
     * @param position starts with one
     * @return String in that position
     */
    private String getElement(String string, int position) {

        StringTokenizer tokenizer = new StringTokenizer(string, ".");
        String token = null;
        for (int i = 0; i < position; i++) {
            if (tokenizer.hasMoreTokens()) {
                token = tokenizer.nextToken();
            } else {
                return null;
            }
        }
        return token;
    }

    public void getLocationForDottedName(String dottedName) {
//        TODO temporary hard coded service names till all elements are supported, being tracked as part of FPP-121
        if (dottedName.contains("monitor")) return;
        if (
                dottedName.contains("mdb-container")
                || dottedName.contains("ejb-container")
                || dottedName.contains("web-container")
                || dottedName.contains("cdi-service")
                || dottedName.contains("batch-runtime-configuration")
                || dottedName.contains("managed-job-config")
                ) {
            //TODO improve performance to improve command execution time
            checkForDependentElements(dottedName);
            if (dottedName.startsWith("configs.config.")) {
                Config c = null;
                if ((getElement(dottedName, 3) != null)) {
                    c = getConfigForName(getElement(dottedName, 3));

                }
                if (c != null && getElementClass(dottedName, 4) != null) {
                    c.getExtensionByType(getElementClass(dottedName, 4));
                }

            } else if (!dottedName.startsWith("domain.")) {
                Config c = null;
                if ((getElement(dottedName, 1) != null)) {
                    c = getConfigForName(getElement(dottedName, 1));

                }
                if (c != null && getElementClass(dottedName, 2) != null) {
                    c.getExtensionByType(getElementClass(dottedName, 2));
                }
            }

        }
    }

    private void checkForDependentElements(String dottedName) {
        //Go over the dependent elements of custom configured config beans and try finding if the dependent elements match the dottedName
    }

    private Config getConfigForName(String name) {
        if (domain.getConfigNamed(name) != null) return domain.getConfigNamed(name);
        if (domain.getServerNamed(name) != null) return domain.getServerNamed(name).getConfig();
        return null;
    }
}
