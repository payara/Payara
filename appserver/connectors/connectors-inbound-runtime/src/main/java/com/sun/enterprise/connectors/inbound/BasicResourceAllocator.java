/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.connectors.inbound;

import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.resource.ResourceSpec;
import com.sun.enterprise.resource.XAResourceWrapper;
import com.sun.enterprise.resource.allocator.AbstractConnectorAllocator;
import com.sun.logging.LogDomains;
import com.sun.appserv.connectors.internal.api.PoolingException;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.transaction.xa.XAResource;

public final class BasicResourceAllocator extends AbstractConnectorAllocator {

    private static final Logger logger =
            LogDomains.getLogger(BasicResourceAllocator.class, LogDomains.RSR_LOGGER);

    private static final String JMS_RESOURCE_FACTORY = "JMS";

    public BasicResourceAllocator() {
    }

    public ResourceHandle createResource()
            throws PoolingException {
        throw new UnsupportedOperationException();
    }

    public ResourceHandle createResource(XAResource xaResource)
            throws PoolingException {

        ResourceHandle resourceHandle = null;
        ResourceSpec spec =
                new ResourceSpec(JMS_RESOURCE_FACTORY,
                        ResourceSpec.JMS);

        if (xaResource != null) {

            logger.logp(Level.FINEST,
                    "BasicResourceAllocator", "createResource",
                    "NOT NULL", xaResource);

            try {
                resourceHandle = new ResourceHandle(
                        null,  //no object present
                        spec,
                        this, null);

                if (logger.isLoggable(Level.FINEST)) {
                    xaResource = new XAResourceWrapper(xaResource);
                }

                resourceHandle.fillInResourceObjects(null, xaResource);

            } catch (Exception e) {
                throw (PoolingException) (new PoolingException()).initCause(e);
            }
        } else {
            logger.logp(Level.FINEST,
                    "BasicResourceAllocator", "createResource", "NULL");
        }

        return resourceHandle;
    }


    public void closeUserConnection(ResourceHandle resourceHandle)
            throws PoolingException {
        throw new UnsupportedOperationException();
    }


    public boolean matchConnection(ResourceHandle resourceHandle) {
        return false;
    }

    public boolean supportsReauthentication() {
        return false;
    }

    public void cleanup(ResourceHandle resourceHandle)
            throws PoolingException {
        throw new UnsupportedOperationException();
    }

    public Set getInvalidConnections(Set connectionSet) throws ResourceException {
        throw new UnsupportedOperationException();
    }

    public boolean isConnectionValid(ResourceHandle resource) {
        throw new UnsupportedOperationException();
    }

}
