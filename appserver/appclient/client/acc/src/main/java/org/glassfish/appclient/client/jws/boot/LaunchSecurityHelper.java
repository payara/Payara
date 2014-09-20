/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.client.jws.boot;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.security.Policy;
import org.glassfish.appclient.client.acc.AppClientContainer;
import org.glassfish.appclient.client.acc.Util;

/**
 *
 * @author tjquinn
 */
public class LaunchSecurityHelper {

    private static final String PERMISSIONS_TEMPLATE_NAME = "jwsclient.policy";
    private static final String SYSTEM_CODEBASE_PROPERTY = "appclient.system.codebase";
    private static final int BUFFER_SIZE = 1024;

    public static void setPermissions() {
        try {
            /*
             * Get the permissions template and write it to a temporary file.
             */
            final String permissionsTemplate = loadResource(LaunchSecurityHelper.class, PERMISSIONS_TEMPLATE_NAME);

            /*
             * The Java security logic will process property references in
             * the policy file template automatically.
             */
            boolean retainTempFiles = Boolean.getBoolean(AppClientContainer.APPCLIENT_RETAIN_TEMP_FILES_PROPERTYNAME);
            File policyFile = Util.writeTextToTempFile(permissionsTemplate, "jwsacc", ".policy", retainTempFiles);
            refreshPolicy(policyFile);

        } catch (IOException ioe) {
            throw new RuntimeException("Error loading permissions template", ioe);
        }
    }

    /**
     * Retrieves a resource as a String.
     * <p>
     * This method does not save the template in a cache.  Use the instance method
     * getTemplate for that purpose.
     *
     * @param a class, the class loader of which should be used for searching for the template
     * @param the path of the resource to load, relative to the contextClass
     * @return the resource's contents
     * @throws IOException if the resource is not found or in case of error while loading it
     */
    private static String loadResource(Class contextClass, String resourcePath) throws IOException {
        String result = null;
        InputStream is = null;
        BufferedReader reader = null;
        try {
            is = contextClass.getResourceAsStream(resourcePath);
            if (is == null) {
                throw new IOException("Could not locate the requested resource relative to class " + contextClass.getName());
            }

            StringBuilder sb = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(is));
            int charsRead;
            char [] buffer = new char [BUFFER_SIZE];
            while ((charsRead = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, charsRead);
            }

            result= sb.toString();
            return result;
        } catch (IOException ioe) {
            throw new IOException("Error loading resource " + resourcePath, ioe);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
    /**
     * Locates the first free policy.url.x setting.
     * @return the int value for the first unused policy setting
     */
    private static int firstFreePolicyIndex() {
        int i = 0;
        String propValue;
        do {
            propValue = java.security.Security.getProperty("policy.url." + String.valueOf(++i));
        } while ((propValue != null) && ( ! propValue.equals("")));

        return i;
    }

    /**
     * Refreshes the current policy object using the contents of the specified file
     * as additional policy.
     * @param policyFile the file containing additional policy
     */
    private static void refreshPolicy(File policyFile) {
        int idx = firstFreePolicyIndex();
        URI policyFileURI = policyFile.toURI();
        java.security.Security.setProperty("policy.url." + idx, policyFileURI.toASCIIString());
        Policy p = Policy.getPolicy();
        p.refresh();
    }
}
