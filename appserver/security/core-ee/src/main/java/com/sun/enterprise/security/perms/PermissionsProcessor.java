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
package com.sun.enterprise.security.perms;

import java.io.File;
import java.io.FilePermission;
import java.net.MalformedURLException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.deployment.DeploymentContext;

import com.sun.logging.LogDomains;

public class PermissionsProcessor {

    public static final String CURRENT_FOLDER = "*";

    public static final String TEMP_FOLDER = "SERVLET-CONTEXT-TEMPDIR";

    protected DeploymentContext context;
    protected SMGlobalPolicyUtil.CommponentType type;
    
    protected static final Logger logger = Logger.getLogger(LogDomains.SECURITY_LOGGER);


    public PermissionsProcessor(SMGlobalPolicyUtil.CommponentType type, 
            DeploymentContext dc) throws SecurityException {
        
        this.type = type;
        this.context = dc;
        
    }
    
    
    protected static PermissionCollection processPermisssonsForPath(PermissionCollection originalPC, 
            DeploymentContext dc) throws MalformedURLException {
        
        if (originalPC == null)
            return originalPC;
        
        Permissions revisedPC = new Permissions();
        
        Enumeration<Permission> pcEnum =  originalPC.elements();
        while (pcEnum.hasMoreElements()) {
            Permission perm = pcEnum.nextElement();
            if (perm instanceof FilePermission) {
                processFilePermission(revisedPC, dc, (FilePermission)perm);    
            } else
                revisedPC.add(perm);
        }
        
        if (logger.isLoggable(Level.FINE)){
            logger.fine("Revised permissions = " + revisedPC);
        }

        
        return revisedPC;
    }

    //for file permission, make the necessary path change, then add permssion to classloader
    protected static void processFilePermission(PermissionCollection revisedPC, DeploymentContext dc,
            FilePermission fp ) throws MalformedURLException {
        
        if (isFilePermforCurrentDir(fp)) {
            addFilePermissionsForCurrentDir(revisedPC, dc, fp);
        } else if (isFilePermforTempDir(fp)) {
            convertTempDirPermission(revisedPC, dc, fp);
        } else {
            revisedPC.add(fp);
        }        
    }
    
    //check if a FilePermssion with target path as the "current" 
    protected static boolean isFilePermforCurrentDir(FilePermission fp) {
        
        if (fp == null)
            return false;
        
        String name = fp.getName();
        if (!CURRENT_FOLDER.equals(name)) 
            return false;
        
        return true;
    }

    //check if a FilePermssion with target path as the "servlet temp dir"
    protected static boolean isFilePermforTempDir(FilePermission fp) {
        
        if (fp == null)
            return false;
        
        String name = fp.getName();
        if (!TEMP_FOLDER.equals(name)) 
            return false;
        
        return true;
    }

    //add the current folder for the file permission
    protected static void addFilePermissionsForCurrentDir(PermissionCollection revisedPC, 
            DeploymentContext context, 
            FilePermission perm) throws MalformedURLException {
        
        if (!isFilePermforCurrentDir(perm)) {             
            //not recognized, add it as is
            revisedPC.add(perm);
            return;
        }
        
        String actions = perm.getActions();
                
        String rootDir = context.getSource().getURI().toURL().toString();
        Permission rootDirPerm = new FilePermission(rootDir, actions);
        revisedPC.add(rootDirPerm);
        Permission rootPerm = new FilePermission(rootDir + File.separator + "-", actions);
        revisedPC.add(rootPerm);
        
        if (context.getScratchDir("ejb") != null) {
            String ejbTmpDir = context.getScratchDir("ejb").toURI().toURL().toString();
            Permission ejbDirPerm = new FilePermission(ejbTmpDir, actions);
            revisedPC.add(ejbDirPerm);            
            Permission ejbPerm = new FilePermission(ejbTmpDir + File.separator + "-", actions);
            revisedPC.add(ejbPerm);
        }
        
        if (context.getScratchDir("jsp") != null) {
            String jspdir = context.getScratchDir("jsp").toURI().toURL().toString();
            Permission jpsDirPerm = new FilePermission(jspdir, actions);
            revisedPC.add(jpsDirPerm);            
            Permission jpsPerm = new FilePermission(jspdir + File.separator + "-", actions);
            revisedPC.add(jpsPerm);
        }
    }
    
    //convert 'temp' dir to the absolute path for permission of 'temp' path
    protected static Permission convertTempDirPermission(PermissionCollection revisedPC,
            DeploymentContext context, 
            FilePermission perm) throws MalformedURLException {
        
        if (!isFilePermforTempDir(perm)) { 
            return perm;
        }
        
        String actions = perm.getActions();
                
        
        if (context.getScratchDir("jsp") != null) {
            String jspdir = context.getScratchDir("jsp").toURI().toURL().toString();
            Permission jspDirPerm = new FilePermission(jspdir, actions);
            revisedPC.add(jspDirPerm);            
            Permission jspPerm = new FilePermission(jspdir + File.separator + "-", actions);
            revisedPC.add(jspPerm);
            return jspPerm;
        }
        
        return perm;
    }

    
    
}
