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
// Portions Copyright [2017-2019] [Payara Foundation and/or its affiliates]
/*
 * PrincipalGroupFactory.java
 *
 * Created on October 28, 2004, 12:34 PM
 */

package com.sun.enterprise.security.web.integration;

import java.lang.ref.WeakReference;

import org.glassfish.internal.api.Globals;
import org.glassfish.security.common.Group;
import org.glassfish.security.common.PrincipalImpl;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.security.PrincipalGroupFactory;

/**
 *
 * @author Harpreet Singh
 */
@Service
public class PrincipalGroupFactoryImpl implements PrincipalGroupFactory {

    private static WeakReference<WebSecurityManagerFactory> webSecurityManagerFactory = new WeakReference<WebSecurityManagerFactory>(null);

    @Override
    public PrincipalImpl getPrincipalInstance(String name, String realm) {
        PrincipalImpl principal = (PrincipalImpl) getWebSecurityManagerFactory().getAdminPrincipal(name, realm);
        if (principal == null) {
            principal = new PrincipalImpl(name);
        }
        
        return principal;
    }

    @Override
    public Group getGroupInstance(String name, String realm) {
        Group group = (Group) getWebSecurityManagerFactory().getAdminGroup(name, realm);
        if (group == null) {
            group = new Group(name);
        }
        
        return group;
    }
    
    
    // ### Private methods
    
    private static WebSecurityManagerFactory getWebSecurityManagerFactory() {
        if (webSecurityManagerFactory.get() != null) {
            return webSecurityManagerFactory.get();
        }
        
        return _getWebSecurityManagerFactory();
    }
    
    private static synchronized WebSecurityManagerFactory _getWebSecurityManagerFactory() {
        if (webSecurityManagerFactory.get() == null) {
            webSecurityManagerFactory = new WeakReference<WebSecurityManagerFactory>(Globals.get(WebSecurityManagerFactory.class));
        }

        return webSecurityManagerFactory.get();
    }
}
