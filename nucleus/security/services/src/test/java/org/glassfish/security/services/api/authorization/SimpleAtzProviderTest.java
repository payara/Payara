/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.security.services.api.authorization;


import org.glassfish.security.services.impl.authorization.*;
import java.net.URI;

import javax.security.auth.Subject;
import org.glassfish.security.common.PrincipalImpl;
import org.glassfish.security.services.api.common.Attributes;
import org.glassfish.security.services.api.context.SecurityContextService;
import org.glassfish.security.services.impl.authorization.AuthorizationServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.jvnet.hk2.testing.junit.HK2Runner;

import org.glassfish.security.services.spi.authorization.AuthorizationProvider;

public class SimpleAtzProviderTest extends HK2Runner {
    
    private AuthorizationProvider simpleAtzPrv = null;
    private SecurityContextService contextService = null;
    
    
    @Before
    public void before() {
        super.before();
        
        String pf = System.getProperty("java.security.policy");
        System.out.println("policy file = " + pf);
        String bd = System.getProperty("build.dir");
        System.out.println("build dir = " + bd);

        String apsd = System.getProperty("appserver_dir");
        System.out.println("appserver dir = " + apsd);

        String local = System.getProperty("localRepository");
        System.out.println("local repository dir = " + local);

        simpleAtzPrv = testLocator.getService(AuthorizationProvider.class, "simpleAuthorization");
        contextService = testLocator.getService(SecurityContextService.class);

        Assert.assertNotNull(simpleAtzPrv);
        Assert.assertNotNull(contextService);
        
        contextService.getEnvironmentAttributes().addAttribute(
                AuthorizationAdminConstants.ISDAS_ATTRIBUTE, "true", true);
    }
    
    @Test
    public void testService() throws Exception {
        final AuthorizationService authorizationService = new AuthorizationServiceImpl();
        Assert.assertNotNull(simpleAtzPrv);
        final AzEnvironment env = new AzEnvironmentImpl();
        final Attributes attrs = contextService.getEnvironmentAttributes();
        for (String attrName : attrs.getAttributeNames()) {
            env.addAttribute(attrName, attrs.getAttributeValue(attrName), true);
        }
        AzSubject azS = authorizationService.makeAzSubject(adminSubject());
        AzResult rt = simpleAtzPrv.getAuthorizationDecision(
                azS,
                authorizationService.makeAzResource(URI.create("admin://some/path")),
                authorizationService.makeAzAction("read"),
                env,
                null
              );
        
        AzResult.Decision ds = rt.getDecision();
        
        Assert.assertEquals(AzResult.Decision.PERMIT, ds);

    }
    
    private Subject adminSubject() {
        final Subject result = new Subject();
        result.getPrincipals().add(new PrincipalImpl("asadmin"));
        return result;
    }
}