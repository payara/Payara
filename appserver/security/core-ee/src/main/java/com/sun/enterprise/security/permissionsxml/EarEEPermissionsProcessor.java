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

import java.net.MalformedURLException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.glassfish.api.deployment.DeploymentContext;

public class EarEEPermissionsProcessor extends BasePermissionsProcessor {

    // Map recording the 'Java EE component type' to its EE adjusted granted permissions
    private static final Map<CommponentType, PermissionCollection> compTypeToEEGrantedPermissions = new HashMap<CommponentType, PermissionCollection>();

    public EarEEPermissionsProcessor(DeploymentContext dc) throws SecurityException {
        super(CommponentType.ear, dc);

        try {
            convertEEPermissionPaths(CommponentType.ejb);
            convertEEPermissionPaths(CommponentType.war);
            convertEEPermissionPaths(CommponentType.rar);
            convertEEPermissionPaths(CommponentType.car);

            // Combine all EE permissions then assign to ear
            combineAllEEPermisssonsForEar();

        } catch (MalformedURLException e) {
            throw new SecurityException(e);
        }
    }

    /**
     * get the EE permissions which have the file path adjusted for the right module
     * 
     * @return adjusted EE permissions
     */
    public PermissionCollection getAdjustedEEPermission(CommponentType type) {
        return compTypeToEEGrantedPermissions.get(type);
    }

    public Map<CommponentType, PermissionCollection> getAllAdjustedEEPermission() {
        return compTypeToEEGrantedPermissions;
    }

    // Convert the path for permissions
    private void convertEEPermissionPaths(CommponentType cmpType) throws MalformedURLException {
        // Get server suppled default policy
        PermissionCollection defWarPc = GlobalPolicyUtil.getEECompGrantededPerms(cmpType);

        // Revise the file permission's path
        PermissionCollection eePc = processPermisssonsForPath(defWarPc, context);

        if (logger.isLoggable(FINE)) {
            logger.fine("Revised permissions = " + eePc);
        }

        compTypeToEEGrantedPermissions.put(cmpType, eePc);
    }

    private PermissionCollection combineAllEEPermisssonsForEar() {
        if (compTypeToEEGrantedPermissions == null) {
            return null;
        }

        Permissions allEEPermissions = new Permissions();

        addPermissions(allEEPermissions, getAdjustedEEPermission(CommponentType.war));
        addPermissions(allEEPermissions, getAdjustedEEPermission(CommponentType.ejb));
        addPermissions(allEEPermissions, getAdjustedEEPermission(CommponentType.rar));

        compTypeToEEGrantedPermissions.put(CommponentType.ear, allEEPermissions);

        return allEEPermissions;
    }

    private void addPermissions(Permissions combinedPermissions, PermissionCollection toAdd) {
        if (toAdd == null) {
            return;
        }

        Enumeration<Permission> permissionsToAdd = toAdd.elements();
        while (permissionsToAdd.hasMoreElements()) {
            combinedPermissions.add(permissionsToAdd.nextElement());
        }
    }

}
