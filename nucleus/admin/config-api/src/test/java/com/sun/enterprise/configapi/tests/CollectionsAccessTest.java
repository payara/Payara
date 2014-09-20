/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.configapi.tests;

import org.junit.Test;
import static org.junit.Assert.*;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.glassfish.api.admin.config.ApplicationName;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Application;

import java.util.List;
import java.beans.PropertyVetoException;
import org.glassfish.api.admin.config.ApplicationName;

/**
 * User: Jerome Dochez
 * Date: Apr 8, 2008
 * Time: 9:45:21 PM
 */
public class CollectionsAccessTest extends ConfigApiTest  {


    public String getFileName() {
        return "DomainTest";
    }

    @Test(expected=IllegalStateException.class)
    public void unprotectedAccess() throws IllegalStateException {
        Applications apps = getHabitat().getService(Applications.class);
        assertTrue(apps!=null);
        apps.getModules().add(null);
    }

    @Test(expected= TransactionFailure.class)
    public void semiProtectedTest() throws TransactionFailure {
        final Applications apps = getHabitat().getService(Applications.class);
        assertTrue(apps!=null);
        ConfigSupport.apply(new SingleConfigCode<Applications>() {
            public Object run(Applications param) throws PropertyVetoException, TransactionFailure {
                // this is the bug, we should not get the list from apps but from param.
                List<ApplicationName> modules = apps.getModules();
                Application m = param.createChild(Application.class);
                modules.add(m); // should throw an exception
                return m;
            }
        }, apps);
    }

    @Test
    public void protectedTest() throws TransactionFailure {
        final Applications apps = getHabitat().getService(Applications.class);
        assertTrue(apps!=null);
        ConfigSupport.apply(new SingleConfigCode<Applications>() {
            public Object run(Applications param) throws PropertyVetoException, TransactionFailure {
                List<ApplicationName> modules = param.getModules();
                Application m = param.createChild(Application.class);
                m.setName( "ejb-test" );
                m.setLocation("test-location");
                m.setObjectType("ejb");
                modules.add(m);
                modules.remove(m);
                return m;
            }
        }, apps);
    }    
}

