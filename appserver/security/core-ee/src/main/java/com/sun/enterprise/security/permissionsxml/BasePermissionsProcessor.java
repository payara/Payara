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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.permissionsxml;

import static java.util.logging.Level.FINE;

import java.io.File;
import java.io.FilePermission;
import java.net.MalformedURLException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.Enumeration;
import java.util.logging.Logger;

import org.glassfish.api.deployment.DeploymentContext;

import com.sun.logging.LogDomains;

/**
 * Base class for the concrete permissions processors.
 * 
 * <p>
 * These classes process the declared permissions and modify them where necessary.
 * For instance, permissions for relative paths are changed into absolute paths.
 */
public class BasePermissionsProcessor {
    
    protected static final Logger logger = Logger.getLogger(LogDomains.SECURITY_LOGGER);
    
    public static final String CURRENT_FOLDER = "*";
    public static final String TEMP_FOLDER = "SERVLET-CONTEXT-TEMPDIR";

    protected DeploymentContext context;
    protected CommponentType type;

    public BasePermissionsProcessor(CommponentType type, DeploymentContext context) throws SecurityException {
        this.type = type;
        this.context = context;
    }

    protected static PermissionCollection processPermisssonsForPath(PermissionCollection originalPermissions, DeploymentContext context) throws MalformedURLException {
        if (originalPermissions == null) {
            return originalPermissions;
        }
            
        Permissions revisedPermissions = new Permissions();

        Enumeration<Permission> pcEnum = originalPermissions.elements();
        while (pcEnum.hasMoreElements()) {
            Permission permissions = pcEnum.nextElement();
            if (permissions instanceof FilePermission) {
                processFilePermission(revisedPermissions, context, (FilePermission) permissions);
            } else {
                revisedPermissions.add(permissions);
            }
        }

        if (logger.isLoggable(FINE)) {
            logger.fine("Revised permissions = " + revisedPermissions);
        }

        return revisedPermissions;
    }

    // For file permission, make the necessary path change, then add the permission to the classloader
    protected static void processFilePermission(PermissionCollection revisedPC, DeploymentContext deploymentContext, FilePermission filePermission) throws MalformedURLException {
        if (isFilePermforCurrentDir(filePermission)) {
            addFilePermissionsForCurrentDir(revisedPC, deploymentContext, filePermission);
        } else if (isFilePermissionForTempDir(filePermission)) {
            convertTempDirPermission(revisedPC, deploymentContext, filePermission);
        } else {
            revisedPC.add(filePermission);
        }
    }

    // Check if a FilePermssion with target path as the "current"
    protected static boolean isFilePermforCurrentDir(FilePermission filePermission) {
        if (filePermission == null) {
            return false;
        }

        String name = filePermission.getName();
        
        if (!CURRENT_FOLDER.equals(name)) {
            return false;
        }

        return true;
    }

    // check if a FilePermssion with target path as the "servlet temp dir"
    protected static boolean isFilePermissionForTempDir(FilePermission filePermission) {
        if (filePermission == null) {
            return false;
        }

        String name = filePermission.getName();
        
        if (!TEMP_FOLDER.equals(name)) {
            return false;
        }

        return true;
    }

    // Add the current folder for the file permission
    protected static void addFilePermissionsForCurrentDir(PermissionCollection revisedPermissions, DeploymentContext context, FilePermission perm) throws MalformedURLException {
        if (!isFilePermforCurrentDir(perm)) {
            // not recognized, add it as is
            revisedPermissions.add(perm);
            return;
        }

        String actions = perm.getActions();

        String rootDir = context.getSource().getURI().toURL().toString();
        Permission rootDirPerm = new FilePermission(rootDir, actions);
        revisedPermissions.add(rootDirPerm);
        Permission rootPerm = new FilePermission(rootDir + File.separator + "-", actions);
        revisedPermissions.add(rootPerm);

        if (context.getScratchDir("ejb") != null) {
            String ejbTmpDir = context.getScratchDir("ejb").toURI().toURL().toString();
            Permission ejbDirPerm = new FilePermission(ejbTmpDir, actions);
            revisedPermissions.add(ejbDirPerm);
            Permission ejbPerm = new FilePermission(ejbTmpDir + File.separator + "-", actions);
            revisedPermissions.add(ejbPerm);
        }

        if (context.getScratchDir("jsp") != null) {
            String jspdir = context.getScratchDir("jsp").toURI().toURL().toString();
            Permission jpsDirPerm = new FilePermission(jspdir, actions);
            revisedPermissions.add(jpsDirPerm);
            Permission jpsPerm = new FilePermission(jspdir + File.separator + "-", actions);
            revisedPermissions.add(jpsPerm);
        }
    }

    // Convert 'temp' dir to the absolute path for permission of 'temp' path
    protected static Permission convertTempDirPermission(PermissionCollection revisedPermissions, DeploymentContext context, FilePermission filePermission) throws MalformedURLException {
        if (!isFilePermissionForTempDir(filePermission)) {
            return filePermission;
        }

        String actions = filePermission.getActions();

        if (context.getScratchDir("jsp") != null) {
            String jspdir = context.getScratchDir("jsp").toURI().toURL().toString();
            Permission jspDirPerm = new FilePermission(jspdir, actions);
            revisedPermissions.add(jspDirPerm);
            Permission jspPermission = new FilePermission(jspdir + File.separator + "-", actions);
            revisedPermissions.add(jspPermission);
            
            return jspPermission;
        }

        return filePermission;
    }

}
