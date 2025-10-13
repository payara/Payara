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

import static com.sun.enterprise.security.permissionsxml.GlobalPolicyUtil.getDeclaredPermissions;

import java.security.PermissionCollection;
import java.security.PrivilegedExceptionAction;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;

import com.sun.enterprise.security.integration.DDPermissionsLoader;

/**
 * Action to get declared permissions for a given module type, process them if needed, and attempt
 * to set them in a class loader.
 * 
 * <p>
 * This action will only actually do work if there's a security manager installed. If there is no such
 * manager this will be a NO-OP.
 * 
 * <p>
 * If the client VM doesn't have the required privileges for the requested permissions an AccessControlException
 * will be thrown.
 *
 */
public class SetPermissionsAction implements PrivilegedExceptionAction<Object> {

    private final CommponentType type;
    private final DeploymentContext context;
    private final ClassLoader classLoader;

    public SetPermissionsAction(CommponentType type, DeploymentContext deploymentContext, ClassLoader classLoader) {
        this.type = type;
        this.context = deploymentContext;
        this.classLoader = classLoader;
    }

    @Override
    public Object run() throws SecurityException {
        processModuleDeclaredAndEEPermissions(type, context, classLoader);
        return null;
    }
    
    /**
     * Get the declared permissions and EE permissions, then add them to the classloader
     *
     * @param type module type
     * @param context deployment context
     * @param classloader throws AccessControlException if caller has no privilege
     */
    private static void processModuleDeclaredAndEEPermissions(CommponentType type, DeploymentContext context, ClassLoader classloader) throws SecurityException {
        if (System.getSecurityManager() != null) {

            if (!(classloader instanceof DDPermissionsLoader)) {
                return;
            }

            if (!(context instanceof ExtendedDeploymentContext)) {
                return;
            }

            DDPermissionsLoader permissionsLoader = (DDPermissionsLoader) classloader;

            if (((ExtendedDeploymentContext) context).getParentContext() == null) {
                permissionsLoader.addDeclaredPermissions(getDeclaredPermissions(type, context));
            }

            permissionsLoader.addEEPermissions(processEEPermissions(type, context));
        }
    }

    /**
     * Get the EE permissions for the specified module type
     *
     * @param type module type
     * @param context the deployment context
     * @return the ee permissions
     */
    private static PermissionCollection processEEPermissions(CommponentType type, DeploymentContext context) {
        return new ModuleEEPermissionsProcessor(type, context).getAdjustedEEPermission();
    }
    
    
}