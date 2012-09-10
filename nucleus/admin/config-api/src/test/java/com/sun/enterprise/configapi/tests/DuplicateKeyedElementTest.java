/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.beans.PropertyVetoException;

import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import org.jvnet.hk2.config.types.Property;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Test for invalid duplicate keyed entries
 *
 * @author Jerome Dochez
 */
public class DuplicateKeyedElementTest extends ConfigApiTest {
    boolean result = false;

    public String getFileName() {
        return "DomainTest";
    }

    @Test(expected = TransactionFailure.class)
    public void duplicateKeyTest() throws TransactionFailure {
        HttpService httpService = getHabitat().getService(HttpService.class);
        assertNotNull(httpService);
        // let's find a acceptable target.
        VirtualServer target = null;
        for (VirtualServer vs : httpService.getVirtualServer()) {
            if (!vs.getProperty().isEmpty()) {
                target = vs;
                break;
            }
        }
        assertNotNull(target);
        final Property prop = target.getProperty().get(0);
        Property newProp = (Property) ConfigSupport.apply(new SingleConfigCode<VirtualServer>() {
            public Object run(VirtualServer param) throws PropertyVetoException, TransactionFailure {
                // first one is fine...
                Property dupProp = param.createChild(Property.class);
                dupProp.setName(prop.getName());
                dupProp.setValue(prop.getValue().toUpperCase());
                // this should fail...
                param.getProperty().add(dupProp);
                return dupProp;
            }
        }, target);
        // if we arrive here, this is an error, we succeeded adding a property with
        // the same key name.
        assertTrue(false);
    }

    @Test(expected = TransactionFailure.class)
    public void identicalKeyTest() throws TransactionFailure {
        HttpService httpService = getHabitat().getService(HttpService.class);
        assertNotNull(httpService);
        // let's find a acceptable target.
        VirtualServer target = null;
        for (VirtualServer vs : httpService.getVirtualServer()) {
            if (!vs.getProperty().isEmpty()) {
                target = vs;
                break;
            }
        }
        assertNotNull(target);
        Property newProp = (Property) ConfigSupport.apply(new SingleConfigCode<VirtualServer>() {
            public Object run(VirtualServer param) throws PropertyVetoException, TransactionFailure {
                // first one is fine...
                Property firstProp = param.createChild(Property.class);
                firstProp.setName("foo");
                firstProp.setValue("bar");
                param.getProperty().add(firstProp);
                // this should fail...
                Property secondProp = param.createChild(Property.class);
                secondProp.setName("foo");
                secondProp.setValue("bar");
                param.getProperty().add(secondProp);
                return secondProp;
            }
        }, target);
        // if we arrive here, this is an error, we succeeded adding a property with
        // the same key name.
        assertTrue(false);
    }
}
