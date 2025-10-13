/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018-2019] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.ee;

import org.glassfish.api.container.Container;
import org.glassfish.api.deployment.Deployer;
import org.glassfish.deployment.common.SecurityRoleMapperFactory;
import org.glassfish.hk2.api.PostConstruct;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.deployment.interfaces.SecurityRoleMapperFactoryMgr;

/**
 * Security container service
 *
 */
@Service(name = "com.sun.enterprise.security.ee.SecurityContainer")
public class SecurityContainer implements Container, PostConstruct {

    static {
        initRoleMapperFactory();
    }

    /**
     * The system-assigned default web module's name/identifier.
     *
     * This has to be the same value as is in j2ee/WebModule.cpp.
     */
    public static final String DEFAULT_WEB_MODULE_NAME = "__default-web-module";

    @Override
    public String getName() {
        return "Security";
    }

    @Override
    public Class<? extends Deployer> getDeployer() {
        return SecurityDeployer.class;
    }

    @Override
    public void postConstruct() {
    }

    private static void initRoleMapperFactory() {
        Object o = null;
        Class c = null;
        
        // this should never fail.
        try {
            c = Class.forName("com.sun.enterprise.security.acl.RoleMapperFactory");
            if (c != null) {
                o = c.newInstance();
                if (o != null && o instanceof SecurityRoleMapperFactory) {
                    SecurityRoleMapperFactoryMgr.registerFactory((SecurityRoleMapperFactory) o);
                }
            }
            if (o == null) {
            }
        } catch (Exception cnfe) {
            
        }
    }
}
