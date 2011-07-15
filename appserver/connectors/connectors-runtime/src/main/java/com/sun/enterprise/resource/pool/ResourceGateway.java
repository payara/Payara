/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.resource.pool;

import com.sun.logging.LogDomains;
import com.sun.appserv.connectors.internal.api.PoolingException;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resource gateway used to restrict the resource access. eg: based on priority.
 *
 * @author Jagadish Ramu
 */
public class ResourceGateway {

    protected final static Logger _logger = LogDomains.getLogger(ResourceGateway.class, LogDomains.RSR_LOGGER);

    /**
     * indicates whether resource access is allowed or not.
     *
     * @return boolean
     */
    public boolean allowed() {
        return true;
    }

    /**
     * used to indicate the gateway that a resource is acquired.
     */
    public void acquiredResource() {
        // no-op
    }

    public static ResourceGateway getInstance(String className) throws PoolingException {
        ResourceGateway gateway = null;
        if (className != null) {
            gateway = initializeCustomResourceGatewayInPrivilegedMode(className);
        } else {
            gateway = new ResourceGateway();
        }
        return gateway;
    }

    private static ResourceGateway initializeCustomResourceGatewayInPrivilegedMode(final String className)
            throws PoolingException {
        Object result = AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {

                Object result = null;
                try {
                    result = initializeCustomResourceGateway(className);
                } catch (Exception e) {
                    _logger.log(Level.WARNING, "pool.resource.gateway.init.failure", className);
                    _logger.log(Level.WARNING, "pool.resource.gateway.init.failure", e);
                }
                return result;
            }
        });
        if (result != null) {
            return (ResourceGateway) result;
        } else {
            throw new PoolingException("Unable to initalize custom ResourceGateway : " + className);
        }
    }

    private static ResourceGateway initializeCustomResourceGateway(String className) throws Exception {
        ResourceGateway gateway;
        Class class1 = Class.forName(className);
        gateway = (ResourceGateway) class1.newInstance();
        return gateway;
    }


    protected static void debug(String debugStatement) {
        if (_logger.isLoggable(Level.FINE))
            _logger.log(Level.FINE, debugStatement);
    }
}
