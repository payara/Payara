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

import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.*;
import org.jvnet.hk2.component.*;
import org.junit.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.tests.utils.*;

import com.sun.enterprise.config.serverbeans.*;

import java.beans.*;

/**
 * This test registers an vetoable change listener on a config bean and vetoes
 * any change on that object.
 *
 * @author Jerome Dochez
 */
public class VetoableChangeListenerTest extends ConfigApiTest implements VetoableChangeListener {

    ServiceLocator habitat;
    boolean result = false;

    public String getFileName() {
        return "DomainTest";
    }

    @Before
    public void setup() {
        habitat = Utils.instance.getHabitat(this);
    }

    @Test
    public void propertyChangeEventReceptionTest() throws TransactionFailure {

        HttpService httpService = habitat.getService(HttpService.class);
        assertNotNull(httpService);

       // let's find a acceptable target.
        VirtualServer target =null;
        for (VirtualServer vs : httpService.getVirtualServer()) {
            if (!vs.getProperty().isEmpty()) {
                target = vs;
                break;
            }
        }

        assertNotNull(target);

        ((ConfigBean) ConfigSupport.getImpl(target)).getOptionalFeature(ConstrainedBeanListener.class).addVetoableChangeListener(this);

        try {
            ConfigSupport.apply(new SingleConfigCode<VirtualServer>() {

                public Object run(VirtualServer param) throws PropertyVetoException, TransactionFailure {
                    param.setId("foo");
                    param.setAccessLog("Foo");
                    return null;
                }
            }, target);
        } catch(TransactionFailure e) {
            //e.printStackTrace();
            System.out.println("Got exception: " + e.getClass().getName() + " as expected, with message: " + e.getMessage());
            result=true;
        }

        assertTrue(result);

        result=false;
        // let's do it again.
        try {
            ConfigSupport.apply(new SingleConfigCode<VirtualServer>() {

                public Object run(VirtualServer param) throws PropertyVetoException, TransactionFailure {
                    param.setId("foo");
                    param.setAccessLog("Foo");
                    return null;
                }
            }, target);
        } catch(TransactionFailure e) {
            //e.printStackTrace();
            System.out.println("Got exception: " + e.getClass().getName() + " as expected, with message: " + e.getMessage());
            result=true;
        }

        ((ConfigBean) ConfigSupport.getImpl(target)).getOptionalFeature(ConstrainedBeanListener.class).removeVetoableChangeListener(this);
        assertTrue(result);


        // this time it should work !
        try {
            ConfigSupport.apply(new SingleConfigCode<VirtualServer>() {

                public Object run(VirtualServer param) throws PropertyVetoException, TransactionFailure {
                    // first one is fine...
                    param.setAccessLog("Foo");
                    return null;
                }
            }, target);
        } catch(TransactionFailure e) {
            //e.printStackTrace();
            System.out.println("Got exception: " + e.getClass().getName() + " as expected, with message: " + e.getMessage());
            result=false;
        }

        assertTrue(result);
    }


    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        throw new PropertyVetoException("I don't think so !", evt);
    }
}
