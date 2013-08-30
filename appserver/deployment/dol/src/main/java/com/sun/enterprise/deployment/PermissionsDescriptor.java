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

package com.sun.enterprise.deployment;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;

import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import com.sun.enterprise.security.integration.PermissionCreator;

public class PermissionsDescriptor extends RootDeploymentDescriptor {

    private RootDeploymentDescriptor parent;
    
    private PermissionCollection declaredPerms;
    
    
    public PermissionsDescriptor() {
        
    }
    
    public RootDeploymentDescriptor getParent() {
        return parent;
    }

    public void setParent(RootDeploymentDescriptor parent) {
        this.parent = parent;
    }

    
    @Override
    public String getModuleID() {
        throw new RuntimeException();
    }

    @Override
    public String getDefaultSpecVersion() {
        
        return "7";
    }

    @Override
    public boolean isEmpty() {
        return declaredPerms != null &&
                declaredPerms.elements().hasMoreElements();
    }

    @Override
    public ArchiveType getModuleType() {
        throw new RuntimeException();
    }

    @Override
    public ClassLoader getClassLoader() {
        if (parent == null)
            return null;
        return parent.getClassLoader();
    }

    @Override
    public boolean isApplication() {

        return false;
    }

    
    public void addPermissionItemdescriptor(PermissionItemDescriptor permItem) {
        permItem.setParent(this);
        addPermission(permItem);
    }
    
    public PermissionCollection getDeclaredPermissions() {
        return declaredPerms;
    }
    
    private void addPermission(PermissionItemDescriptor permItem)  {
        if (permItem == null)
            return;
        
        String classname = permItem.getPermissionClassName();
        String target = permItem.getTargetName();
        String actions = permItem.getActions();
        
        try {
            Permission pm = PermissionCreator.getInstance(classname, target, actions);
            
            if (pm != null) {
                if(declaredPerms == null)
                    declaredPerms = new Permissions();
                this.declaredPerms.add(pm);
            }
        } catch (ClassNotFoundException e) {
            throw new SecurityException(e);
        } catch (NoSuchMethodException e) {
            throw new SecurityException(e);
        } catch (InstantiationException e) {
            throw new SecurityException(e);
        } catch (IllegalAccessException e) {
            throw new SecurityException(e);
        } catch (InvocationTargetException e) {
            throw new SecurityException(e);
        }
    }

}
