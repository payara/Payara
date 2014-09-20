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

package com.sun.enterprise.v3.admin;

import com.sun.enterprise.v3.common.HTMLActionReporter;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.api.admin.*;
import org.glassfish.tests.utils.ConfigApiTest;
import org.glassfish.tests.utils.Utils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Ignore;
import org.junit.Test;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.ObservableBean;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import java.beans.PropertyChangeEvent;

/**
 * test the set command
 * @author Jerome Dochez
 */
// Ignored temporarily because it fails to inject CommandRunnerImpl as ModulesRegistry is not available
@Ignore 
public class ConfigAttributeSetTest  extends ConfigApiTest implements ConfigListener {

    ServiceLocator habitat = Utils.instance.getHabitat(this);
    PropertyChangeEvent event = null;

    public DomDocument getDocument(ServiceLocator habitat) {
        return new TestDocument(habitat);
    }

    /**
     * Returns the DomainTest file name without the .xml extension to load the test configuration
     * from.
     *
     * @return the configuration file name
     */
    public String getFileName() {
        return "DomainTest";
    }     

    @Test
     public void simpleAttributeSetTest() {

        CommandRunnerImpl runner = habitat.getService(CommandRunnerImpl.class);
        assertNotNull(runner);

        // let's find our target
        NetworkListener listener = null;
        NetworkListeners service = habitat.getService(NetworkListeners.class);
        for (NetworkListener l : service.getNetworkListener()) {
            if ("http-listener-1".equals(l.getName())) {
                listener = l;
                break;
            }
        }
        assertNotNull(listener);        

        // Let's register a listener
        ObservableBean bean = (ObservableBean) ConfigSupport.getImpl(listener);
        bean.addListener(this);

        // parameters to the command
        ParameterMap parameters = new ParameterMap();
        parameters.set("value", "8090");
        parameters.set("DEFAULT", "configs.config.server-config.http-service.http-listener.http-listener-1.port");

        // execute the set command.
        runner.getCommandInvocation("set", new HTMLActionReporter(), adminSubject()).parameters(parameters).execute();
                                                                                                                                                                                                                           
        // check the result.
        String port = listener.getPort();
        assertEquals(port, "8090");

        // ensure events are delivered.
        habitat.<Transactions>getService(Transactions.class).waitForDrain();
        
        // finally
        bean.removeListener(this);

        // check we recevied the event
        assertNotNull(event);
        assertEquals("8080", event.getOldValue());
        assertEquals("8090", event.getNewValue());
        assertEquals("port", event.getPropertyName());
        
    }

    public UnprocessedChangeEvents changed(PropertyChangeEvent[] propertyChangeEvents) {
        assertEquals("Array size", propertyChangeEvents.length, 1 );
        event = propertyChangeEvents[0];
        return null;
    }
}
