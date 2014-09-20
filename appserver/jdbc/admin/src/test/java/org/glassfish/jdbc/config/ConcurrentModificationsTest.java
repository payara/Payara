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

package org.glassfish.jdbc.config;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jdbc.config.JdbcResource;
import org.junit.Test;
import static org.junit.Assert.*;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;

import java.beans.PropertyVetoException;

public class ConcurrentModificationsTest extends ConfigApiTest{

    /**
     * Returns the file name without the .xml extension to load the test configuration
     * from. By default, it's the name of the TestClass.
     *
     * @return the configuration file name
     */
    public String getFileName() {
        return "DomainTest";
    }

    @Test(expected= TransactionFailure.class)
    public void collectionTest() throws TransactionFailure {

        ServiceLocator habitat = super.getHabitat();
        final Resources resources = habitat.<Domain>getService(Domain.class).getResources();
        assertTrue(resources!=null);

        ConfigSupport.apply(new SingleConfigCode<Resources>() {

            public Object run(Resources writeableResources) throws PropertyVetoException, TransactionFailure {

                assertTrue(writeableResources!=null);
                JdbcResource newResource = writeableResources.createChild(JdbcResource.class);
                newResource.setJndiName("foo");
                newResource.setDescription("Random ");
                newResource.setPoolName("bar");
                newResource.setEnabled("true");
                writeableResources.getResources().add(newResource);

                // now let's check I have my copy...
                boolean found=false;
                for (Resource resource : writeableResources.getResources()) {
                    if (resource instanceof JdbcResource) {
                        JdbcResource jdbc = (JdbcResource) resource;
                        if (jdbc.getJndiName().equals("foo")) {
                            found = true;
                            break;
                        }
                    }
                }
                assertTrue(found);

                // now let's check that my readonly copy does not see it...
                boolean shouldNot = false;
                for (Resource resource : resources.getResources()) {
                    if (resource instanceof JdbcResource) {
                        JdbcResource jdbc = (JdbcResource) resource;
                        if (jdbc.getJndiName().equals("foo")) {
                            shouldNot = true;
                            break;
                        }
                    }
                }
                assertFalse(shouldNot);

                // now I am throwing a transaction failure since I don't care about saving it
                throw new TransactionFailure("Test passed", null);
            }        
        },resources);
    }
}
