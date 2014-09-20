/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.internal.grizzly;

import java.nio.channels.SelectableChannel;

import org.jvnet.hk2.annotations.Contract;

/**
 * This interface is meant for all services that wish to be initialized lazily.
 * Such services are expected to implement this interface and those
 * implementatons should be available to HK2 for lookup. 
 * 
 * 
 * @author Vijay Ramachandran
*/
@Contract
public interface LazyServiceInitializer {

    /**
     * Upon accepting the first request on the port (to which this listener is
     * bound), the listener will select the appropriate provider and call this
     * method to let the actual service initialize itself. All further accept
     * requests on this port will wait while the service is initialized.
     * Upon successful completion of service initialization, all pending
     * requests are passed to the service using the handleRequest method
     *
     * @return Return true if service initialization went through fine; false
     *         otherwise
     */
    public boolean initializeService();

    /**
     * Upon successful ACCEPT of every request on this port, the service
     * is called upon to handle the request. The service is provided the 
     * channel itself. The service can setup connection, its characteristics,
     * decide on blocking/non-blocking modes etc. The service is expected to
     * return control back to the listener ASAP without consuming this thread
     * for processing the requst completely.
     *
     * @param channel where the incoming request was accepted.
     */
    public void handleRequest(SelectableChannel channel);
}
