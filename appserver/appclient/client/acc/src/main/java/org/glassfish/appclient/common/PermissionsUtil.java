
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

package org.glassfish.appclient.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.cert.Certificate;

import javax.xml.stream.XMLStreamException;

import com.sun.enterprise.security.perms.SMGlobalPolicyUtil;
import com.sun.enterprise.security.perms.XMLPermissionsHandler;

import sun.security.provider.PolicyFile;

public class PermissionsUtil {

    protected static final String PERMISSIONS_XML = "META-INF/permissions.xml";  
    
    protected static final String CLIENT_EE_PERMS_FILE = "javaee.client.policy";
    protected static final String CLIENT_EE_PERMS_PKG = 
        "META-INF/" + CLIENT_EE_PERMS_FILE;
    
    protected static final String CLIENT_RESTRICT_PERMS_FILE = "restrict.client.policy";
    protected static final String CLIENT_RESTRICT_PERMS_PKG = 
        "META-INF/" + CLIENT_RESTRICT_PERMS_FILE;
    
    
    //get client declared permissions which is packaged on the client's generated jar, 
    //or in the client's module jar if standalone
    //result could be null
    public static PermissionCollection getClientDeclaredPermissions(ClassLoader cl) throws IOException {
        
        URL permUrl = cl.getResource(PERMISSIONS_XML);
        
        if (permUrl == null )
            return null;
        
        InputStream declaredPermInput = permUrl.openStream(); 
        
        XMLPermissionsHandler pHdlr = null;
        
        try {
            pHdlr = new XMLPermissionsHandler(
                null, declaredPermInput, SMGlobalPolicyUtil.CommponentType.car);
        } catch (XMLStreamException e) {
            throw new IOException(e);
        } catch (FileNotFoundException e) {
            throw new IOException(e);
        }

        return pHdlr.getAppDeclaredPermissions();
    }
    
    //get the permissions configured inside the javaee.client.policy, 
    // which might be packaged inside the client jar, 
    // or from the installed folder lib/appclient
    //result could be null if either of the above is found
    public static PermissionCollection getClientEEPolicy(ClassLoader cl)
            throws IOException {
        
        return getClientPolicy(cl, CLIENT_EE_PERMS_PKG, CLIENT_EE_PERMS_FILE);
    }

    //get the permissions configured inside the javaee.client.policy, 
    // which might be packaged inside the client jar, 
    // or from the installed folder lib/appclient
    //result could be null if either of the above is found
    public static PermissionCollection getClientRestrictPolicy(ClassLoader cl)
        throws IOException {

        return getClientPolicy(cl, CLIENT_RESTRICT_PERMS_PKG, CLIENT_RESTRICT_PERMS_FILE);
    }

    
    private static PermissionCollection getClientPolicy(ClassLoader cl, String pkgedFile, 
            String policyFileName) throws IOException {
        
        
        //1st try to find from the packaged client jar
        URL eeClientUrl = cl.getResource(pkgedFile);
        if (eeClientUrl != null)
            return getEEPolicyPermissions(eeClientUrl);
        
        
        //2nd try to find from client's installation at lib/appclient folder
        String clientPolicyClocation = getClientInstalledPath();
        if (clientPolicyClocation != null) {            
            String clietEEFile = clientPolicyClocation + policyFileName;
            return getPolicyPermissions(clietEEFile);
        }
        
        return null;
        
    }
    
    
    private static PermissionCollection getEEPolicyPermissions(URL fileUrl) throws IOException {
        
        //System.out.println("Loading policy from " + fileUrl);
        PolicyFile pf = new PolicyFile(fileUrl);
        
        CodeSource cs = 
            new CodeSource(
                    new URL(SMGlobalPolicyUtil.CLIENT_TYPE_CODESOURCE), (Certificate[])null );
        PermissionCollection pc = pf.getPermissions(cs);

        return pc;        
    }

    
    
    private static PermissionCollection  getPolicyPermissions(String policyFilename) throws IOException {

        File f = new File(policyFilename);
        if (!f.exists())
            return null;
        
        URL furl = new URL("file:" + policyFilename);
            
        return getEEPolicyPermissions(furl);
    }


    private static String getClientInstalledPath() {
        String policyPath = System.getProperty("java.security.policy");
        
        if (policyPath == null)
            return null;
        
        File pf = new File(policyPath);
        
        String  clientPath = pf.getParent() + File.separator;

        //System.out.println("clientPath  " + clientPath );
        
        return clientPath;
    }

}
