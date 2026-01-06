/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.samples.clustered.singleton;

import fish.payara.cluster.Clustered;
import fish.payara.micro.cdi.Outbound;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

/**
 * @author lprimak
 */
@Clustered
@Singleton @Startup
public class ClusteredEventBusStartup implements Serializable {
    private static final Logger log = Logger.getLogger(ClusteredEventBusStartup.class.getName());
    private static final long serialVersionUID = 1L;
    private Stock stock;
    private int numInvocations;
    @Inject
    @Outbound(loopBack = true)
    private Event<Stock> stockEvents;

    @Schedule(hour = "*", minute = "*", second = "*/1", persistent = false)
    private void generatePrice() {
        ++numInvocations;
        stock = new Stock("PYA", "Some very long description of Payara", Math.random() * 100.0);
        log.log(Level.INFO, "Sock: {0}, numInvocations: {1}", new Object[] { stock, numInvocations });
        stockEvents.fire(stock);
    }
}
