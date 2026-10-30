/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.enterprise.security.ee.web.integration;

import java.lang.ref.WeakReference;

import org.glassfish.deployment.common.SecurityRoleMapperFactory;
import org.glassfish.internal.api.Globals;

/**
 * @author nasradu8 2009
 */
public class SecurityRoleMapperFactoryGen {

    private static WeakReference<SecurityRoleMapperFactory> securityRoleMapperFactory = new WeakReference<>(null);

    public static SecurityRoleMapperFactory getSecurityRoleMapperFactory() {
        if (securityRoleMapperFactory.get() != null) {
            return securityRoleMapperFactory.get();
        }

        return _getSecurityRoleMapperFactory();
    }

    private static synchronized SecurityRoleMapperFactory _getSecurityRoleMapperFactory() {
        if (securityRoleMapperFactory.get() == null) {
            securityRoleMapperFactory = new WeakReference<>(Globals.get(SecurityRoleMapperFactory.class));
        }

        return securityRoleMapperFactory.get();
    }
}
