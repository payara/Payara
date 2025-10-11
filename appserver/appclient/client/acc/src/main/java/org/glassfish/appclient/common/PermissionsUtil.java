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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright 2018-2022 Payara Foundation and/or its affiliates
// Payara Foundation and/or its affiliates elects to include this software in this distribution under the GPL Version 2 license
package org.glassfish.appclient.common;

import com.sun.enterprise.security.permissionsxml.CommponentType;
import com.sun.enterprise.security.permissionsxml.GlobalPolicyUtil;
import com.sun.enterprise.security.permissionsxml.PermissionsXMLLoader;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.NoSuchAlgorithmException;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.URIParameter;
import java.security.cert.Certificate;

public class PermissionsUtil {

    protected static final String PERMISSIONS_XML = "META-INF/permissions.xml";

    protected static final String CLIENT_EE_PERMS_FILE = "javaee.client.policy";
    protected static final String CLIENT_EE_PERMS_PKG = "META-INF/" + CLIENT_EE_PERMS_FILE;

    protected static final String CLIENT_RESTRICT_PERMS_FILE = "restrict.client.policy";
    protected static final String CLIENT_RESTRICT_PERMS_PKG = "META-INF/" + CLIENT_RESTRICT_PERMS_FILE;

    // Get client declared permissions which is packaged on the client's generated jar,
    // or in the client's module jar if standalone.
    // Result could be null
    public static PermissionCollection getClientDeclaredPermissions(ClassLoader classLoader) throws IOException {
        URL permissionsURL = classLoader.getResource(PERMISSIONS_XML);
        if (permissionsURL == null) {
            return null;
        }
        
        try {
            return new PermissionsXMLLoader(null, permissionsURL.openStream(), CommponentType.car)
                    .getAppDeclaredPermissions();
        } catch (XMLStreamException | FileNotFoundException e) {
            throw new IOException(e);
        }

    }

    // Get the permissions configured inside the javaee.client.policy,
    // which might be packaged inside the client jar,
    // or from the installed folder lib/appclient
    // result could be null if either of the above is found
    public static PermissionCollection getClientEEPolicy(ClassLoader classLoader) throws IOException {
        return getClientPolicy(classLoader, CLIENT_EE_PERMS_PKG, CLIENT_EE_PERMS_FILE);
    }

    // get the permissions configured inside the javaee.client.policy,
    // which might be packaged inside the client jar,
    // or from the installed folder lib/appclient
    // result could be null if either of the above is found
    public static PermissionCollection getClientRestrictPolicy(ClassLoader classLoader) throws IOException {
        return getClientPolicy(classLoader, CLIENT_RESTRICT_PERMS_PKG, CLIENT_RESTRICT_PERMS_FILE);
    }

    private static PermissionCollection getClientPolicy(ClassLoader classLoader, String pkgedFile, String policyFileName) throws IOException {

        // 1st try to find from the packaged client jar
        URL eeClientUrl = classLoader.getResource(pkgedFile);
        if (eeClientUrl != null) {
            return getEEPolicyPermissions(eeClientUrl);
        }

        // 2nd try to find from client's installation at lib/appclient folder
        String clientPolicyClocation = getClientInstalledPath();
        if (clientPolicyClocation != null) {
            return getPolicyPermissions(clientPolicyClocation + policyFileName);
        }

        return null;
    }

    private static PermissionCollection getPolicyPermissions(String policyFilename) throws IOException {
        if (!new File(policyFilename).exists()) {
            return null;
        }

        return getEEPolicyPermissions(new URL("file:" + policyFilename));
    }
    
    private static PermissionCollection getEEPolicyPermissions(URL fileUrl) throws IOException {
        try {
            return 
                Policy.getInstance("JavaPolicy", new URIParameter(fileUrl.toURI()))
                      .getPermissions(new CodeSource(
                              new URL(GlobalPolicyUtil.CLIENT_TYPE_CODESOURCE), (Certificate[]) null));
        } catch (NoSuchAlgorithmException | MalformedURLException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String getClientInstalledPath() {
        String policyPath = System.getProperty("java.security.policy");
        if (policyPath == null) {
            return null;
        }

        return new File(policyPath).getParent() + File.separator;
    }

}
