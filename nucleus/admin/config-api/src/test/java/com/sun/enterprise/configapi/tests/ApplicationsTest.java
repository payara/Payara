/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertTrue;
import org.glassfish.api.admin.config.ApplicationName;
import org.jvnet.hk2.config.*;
import com.sun.enterprise.config.serverbeans.*;

import java.util.List;
import java.beans.*;

/**
 * Applications related tests
 * @author Jerome Dochez
 */
public class ApplicationsTest extends ConfigApiTest {


    public String getFileName() {
        return "DomainTest";
    }

    @Test
    public void appsExistTest() {
        Applications apps = getHabitat().getService(Applications.class);
        assertTrue(apps!=null);
    }

    @Test
    public void getModulesTest() {
        Applications apps = getHabitat().getService(Applications.class);
        List<ApplicationName> modules = apps.getModules();
        for (ApplicationName module : modules) {
            logger.fine("Module = " + module.getName());
        }
        assertTrue(modules!=null);
    }

    @Test
    public void getApplicationTest() {
        Applications apps = getHabitat().getService(Applications.class);
        Application app = apps.getApplication("simple");
        assertTrue(app != null);
    }

    /**
     * Test which is expecting an UnsupportedOperationException since we are
     * operating on a copy list of the original getModules() list.
     * 
     * @throws TransactionFailure
     */
    @Test(expected = UnsupportedOperationException.class)
    public void removalTest() throws Throwable {
        Applications apps = getHabitat().getService(Applications.class);
        try {
            ConfigSupport.apply(new SingleConfigCode<Applications>() {
                public Object run(Applications param) throws PropertyVetoException, TransactionFailure {
                    List<Application> appList = param.getApplications();
                    for (Application application : param.getApplicationsWithSnifferType("web")) {
                        assertTrue(appList.remove(application));
                    }
                    return null;
                }
            }, apps);
        } catch(TransactionFailure e) {
            // good, an exception was thrown, hopfully the right one !
            throw e.getCause();
        }
    }
}
