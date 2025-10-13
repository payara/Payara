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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
// Portions Copyright [2018-2024] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.ee.authorization.cache;

import java.security.Permission;

/**
 * This class is
 * 
 * @author Ron Monzillo
 */
public class CachedPermissionImpl implements CachedPermission {

    private PermissionCache permissionCache;
    private Permission permission;
    private Epoch epoch;

    public CachedPermissionImpl(PermissionCache permissionCache, Permission permission) {
        this.permissionCache = permissionCache;
        this.permission = permission;
        epoch = new Epoch();
    }

    @Override
    public Permission getPermission() {
        return permission;
    }

    @Override
    public PermissionCache getPermissionCache() {
        return permissionCache;
    }

    // synchronization done in PermissionCache
    @Override
    public boolean checkPermission() {
        if (permissionCache == null) {
            return false;
        }
        
        return permissionCache.checkPermission(permission, epoch);
    }

    // used to hold last result obtained from cache and cache epoch.
    // epoch is used by PermissionCache to determine when result is out of date.
    static class Epoch {

        int epoch;
        boolean granted;

        Epoch() {
            this.epoch = 0;
            this.granted = false;
        }
    }

}
