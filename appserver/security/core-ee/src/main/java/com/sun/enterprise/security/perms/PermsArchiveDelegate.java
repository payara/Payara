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
import java.io.FileNotFoundException;
import java.security.PermissionCollection;
import java.security.PrivilegedExceptionAction;

import javax.xml.stream.XMLStreamException;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;

import com.sun.enterprise.security.integration.DDPermissionsLoader;

public class PermsArchiveDelegate {

    
    
    /**
     * Get the application or module packaged permissions
     * @param type  the type of the module, this is used to check the configured restriction for the type
     * @param context  the deployment context
     * @return the module or app declared permissions
     * @throws SecurityException if permissions.xml has syntax failure, or failed for restriction check
     */
    public static PermissionCollection getDeclaredPermissions(SMGlobalPolicyUtil.CommponentType type,
            DeploymentContext context) throws SecurityException {

        try {
            File base = new File(context.getSource().getURI());
            
            XMLPermissionsHandler pHdlr = new XMLPermissionsHandler(base, type);
            
            PermissionCollection declaredPerms = pHdlr.getAppDeclaredPermissions();
            
            //further process the permissions for file path adjustment
            DeclaredPermissionsProcessor dpp = 
                new DeclaredPermissionsProcessor(type, context, declaredPerms);
            
            PermissionCollection revisedWarDeclaredPerms =
                dpp.getAdjustedDeclaredPermissions();
            
            return revisedWarDeclaredPerms;
        } catch (XMLStreamException e) {
            throw new SecurityException(e); 
        } catch (SecurityException e) {
            throw new SecurityException(e);
        } catch (FileNotFoundException e) {
            throw new SecurityException(e);
        }
        
    }
    
    /**
     * Get the EE permissions for the spcified module type
     * @param type module type
     * @param dc the deployment context
     * @return the ee permissions
     */
    public static PermissionCollection processEEPermissions(SMGlobalPolicyUtil.CommponentType type,
            DeploymentContext dc) {
        
        ModuleEEPermissionsProcessor eePp = 
            new ModuleEEPermissionsProcessor(type, dc);
        
        PermissionCollection eePc = eePp.getAdjustedEEPermission();
        
        return eePc;
    }
    
    
    /**
     * Get the declared permissions and EE permissions, then add them to the classloader 
     * @param type   module type
     * @param context  deployment context
     * @param classloader  
     * throws AccessControlException if caller has no privilege 
     */
    public static void processModuleDeclaredAndEEPemirssions(SMGlobalPolicyUtil.CommponentType type, 
            DeploymentContext context, ClassLoader classloader) throws SecurityException {
        
        if (System.getSecurityManager() != null)  {
            
            if (!(classloader instanceof DDPermissionsLoader))
                return;
            
            if (!(context instanceof ExtendedDeploymentContext))
                return;
            
            DDPermissionsLoader ddcl = (DDPermissionsLoader)classloader;
            
            if (((ExtendedDeploymentContext)context).getParentContext() == null) {

                PermissionCollection declPc = getDeclaredPermissions(type, context); 
                ddcl.addDeclaredPermissions(declPc);
            }
            
            PermissionCollection eePc = processEEPermissions(type, context);
                        
            ddcl.addEEPermissions(eePc);
        }
    }
    
    
    public static class SetPermissionsAction implements PrivilegedExceptionAction<Object> {
        
        private SMGlobalPolicyUtil.CommponentType type;
        private DeploymentContext context;
        private ClassLoader cloader;
        
        public SetPermissionsAction(SMGlobalPolicyUtil.CommponentType type,
                DeploymentContext dc, ClassLoader cl) {
            this.type = type;
            this.context = dc;
            this.cloader = cl;
        }
        
        public Object run() throws SecurityException {

            processModuleDeclaredAndEEPemirssions(
                  type, context, cloader);
            return null;
        }
    }
    
}
