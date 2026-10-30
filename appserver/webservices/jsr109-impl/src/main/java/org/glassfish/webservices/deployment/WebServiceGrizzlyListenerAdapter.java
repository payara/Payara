/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package org.glassfish.webservices.deployment;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.api.container.GrizzlyNetworkListenerRestartAdapter;

/**
 * Grizzly calls {@link #restartEndpoints()} whenever a network listener is enabled,
 * reconfigured or restarted
 * <p>
 * EJB Web Services use this to re-register it's endpoints that are outside of Servlet container
 *
 * @author lprimak
 */
@Service
public class WebServiceGrizzlyListenerAdapter implements GrizzlyNetworkListenerRestartAdapter {
    private final Set<WebServiceGrizzlyRestartListener> listeners =
            Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));

    /**
     * adds listener
     *
     * @param listener
     * @return
     * @see Set#add(java.lang.Object)
     */
    public boolean addListener(WebServiceGrizzlyRestartListener listener) {
        return listeners.add(listener);
    }

    /**
     * removes  listener
     *
     * @param listener
     * @return
     * @see Set#remove(java.lang.Object)
     */
    public boolean removeListener(WebServiceGrizzlyRestartListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Called by Grizzly
     */
    @Override
    public void restartEndpoints() {
        listeners.forEach(WebServiceGrizzlyRestartListener::restartEndpoints);
    }
}
