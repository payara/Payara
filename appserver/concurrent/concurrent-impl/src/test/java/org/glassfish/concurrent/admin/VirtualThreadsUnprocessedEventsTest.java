/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package org.glassfish.concurrent.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Resources;
import org.glassfish.concurrent.admin.listener.ResourcesConfigListener;
import org.glassfish.concurrent.config.ManagedExecutorService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.resources.admin.cli.ResourceConstants;
import org.glassfish.tests.utils.ConfigApiTest;
import org.glassfish.tests.utils.Utils;
import org.junit.Test;
import org.jvnet.hk2.config.*;

import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class VirtualThreadsUnprocessedEventsTest extends ConfigApiTest implements TransactionListener {

    private ServiceLocator habitat = Utils.instance.getHabitat(this);

    private PropertyChangeEvent propertyChangeEvent = null;
    private Resources resources = habitat.<Domain>getService(Domain.class).getResources();
    private long timeBeforeChange;

    /**
     * Returns the DomainTest file name without the .xml extension to load the test configuration
     * from.
     *
     * @return the configuration file name
     */
    public String getFileName() {
        return "VirtualThreadsUnprocessedEventsTest";
    }

    @Test
    public void restartRequiredTest() throws TransactionFailure {
        Map<String, String> attributes = new HashMap<>();

        attributes.put(ResourceConstants.USE_VIRTUAL_THREADS, "true");
        attributes.put(ResourceConstants.JNDI_NAME, "concurrent/TestManagedExecutorService");

        ConfigBean managedExecutorServiceConfigBean =
                ConfigSupport.createAndSet(
                        (ConfigBean) ConfigBean.unwrap(resources),
                        ManagedExecutorService.class, attributes);

        Transactions transactions = getHabitat().getService(Transactions.class);

        transactions.waitForDrain();

        ManagedExecutorService managedExecutorService =
                managedExecutorServiceConfigBean.getProxy(ManagedExecutorService.class);

        ResourcesConfigListener resourcesConfigListener = new ResourcesConfigListener();
        ObservableBean bean = (ObservableBean) ConfigSupport.getImpl(resources);

        bean.addListener(resourcesConfigListener);

        try {
            transactions.addTransactionsListener(this);
            timeBeforeChange = System.currentTimeMillis();

            try {
                ConfigSupport.apply(
                        param -> {param.setUseVirtualThreads("false"); return null;},
                        managedExecutorService);
            }
            finally {
                ConfigSupport.apply(
                        param -> {param.setUseVirtualThreads("true"); return null;},
                        managedExecutorService);
            }

            transactions.waitForDrain();
            assertNotNull(propertyChangeEvent);

            bean.removeListener(resourcesConfigListener);
        }
        finally {
            transactions.removeTransactionsListener(this);
            ConfigSupport.deleteChild((ConfigBean) ConfigBean.unwrap(resources), managedExecutorServiceConfigBean);
        }
    }


    public void transactionCommited(List<PropertyChangeEvent> changes) {
    }

    public void unprocessedTransactedEvents(List<UnprocessedChangeEvents> changes) {
        changes.stream().map(
                UnprocessedChangeEvents::getUnprocessed
        ).flatMap(
                List::stream
        ).filter(
                unprocessedChangeEvent -> unprocessedChangeEvent.getWhen() >= timeBeforeChange
        ).map(
                UnprocessedChangeEvent::getEvent
        ).filter(
                event -> event.getSource() instanceof ManagedExecutorService
        ).forEach(
                param -> propertyChangeEvent = param
        );
    }
}
