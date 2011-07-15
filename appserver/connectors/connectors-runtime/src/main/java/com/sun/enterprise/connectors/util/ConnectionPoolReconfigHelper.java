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

package com.sun.enterprise.connectors.util;

import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.enterprise.connectors.ConnectorConnectionPool;
import com.sun.enterprise.connectors.ConnectorDescriptorInfo;
import com.sun.enterprise.connectors.authentication.ConnectorSecurityMap;
import com.sun.enterprise.connectors.authentication.RuntimeSecurityMap;
import com.sun.logging.LogDomains;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


public final class ConnectionPoolReconfigHelper {

    private static final Logger _logger = LogDomains.getLogger(ConnectionPoolReconfigHelper.class, LogDomains.RSR_LOGGER);


    public enum ReconfigAction {
        RECREATE_POOL, UPDATE_MCF_AND_ATTRIBUTES, NO_OP
    }

    public static ReconfigAction compare(ConnectorConnectionPool oldPool,
                                         ConnectorConnectionPool newPool, Set excludedProps)
            throws ConnectorRuntimeException {

        if (isEqualConnectorConnectionPool(oldPool, newPool, excludedProps)
                == ReconfigAction.NO_OP) {

            return ReconfigAction.UPDATE_MCF_AND_ATTRIBUTES;
        }

        return ReconfigAction.RECREATE_POOL;
    }

    /*
     * Compare the Original ConnectorConnectionPool with the passed one
     * If MCF properties are changed, indicate that pool recreation is
     * required 
     * We only check the MCF properties since a pool restart is required
     * for changes in MCF props. For pool specific properties we can get
     * away without restart
     * If the new pool and old pool have identical MCF properties returns 
     * true
     */
    private static ReconfigAction isEqualConnectorConnectionPool(ConnectorConnectionPool
            oldCcp, ConnectorConnectionPool newCcp, Set excludedProps) {
        boolean poolsEqual = true;

        //for all the following properties, we need to recreate pool if they
        //have changed
        if(newCcp.isPoolingOn() != oldCcp.isPoolingOn()) {
            return ReconfigAction.RECREATE_POOL;
        }
        
        if (newCcp.getTransactionSupport() != oldCcp.getTransactionSupport()) {
            return ReconfigAction.RECREATE_POOL;
        }

        if (newCcp.isAssociateWithThread() != oldCcp.isAssociateWithThread()) {
            return ReconfigAction.RECREATE_POOL;
        }

        if (newCcp.isLazyConnectionAssoc() != oldCcp.isLazyConnectionAssoc()) {
            return ReconfigAction.RECREATE_POOL;
        }

        if (newCcp.isPartitionedPool() != oldCcp.isPartitionedPool()) {
            return ReconfigAction.RECREATE_POOL;
        }
        if (newCcp.getPoolDataStructureType() == null && oldCcp.getPoolDataStructureType() != null) {
            return ReconfigAction.RECREATE_POOL;
        }
        if (newCcp.getPoolDataStructureType() != null && oldCcp.getPoolDataStructureType() == null) {
            return ReconfigAction.RECREATE_POOL;
        }

        if (((newCcp.getPoolDataStructureType() != null) && (oldCcp.getPoolDataStructureType() != null)
                && !(newCcp.getPoolDataStructureType().equals(oldCcp.getPoolDataStructureType())))) {
            return ReconfigAction.RECREATE_POOL;
        }

        if ((newCcp.getPoolWaitQueue() != null) && (oldCcp.getPoolWaitQueue() == null)) {
            return ReconfigAction.RECREATE_POOL;
        }

        if ((newCcp.getPoolWaitQueue() == null) && (oldCcp.getPoolWaitQueue() != null)) {
            return ReconfigAction.RECREATE_POOL;
        }

        if ((newCcp.getPoolWaitQueue() != null) && (oldCcp.getPoolWaitQueue() != null)
                && (!newCcp.getPoolWaitQueue().equals(oldCcp.getPoolWaitQueue()))) {
            return ReconfigAction.RECREATE_POOL;
        }

        if ((newCcp.getDataStructureParameters() != null) && (oldCcp.getDataStructureParameters() == null)) {
            return ReconfigAction.RECREATE_POOL;
        }

        if ((newCcp.getDataStructureParameters() == null) && (oldCcp.getDataStructureParameters() != null)) {
            return ReconfigAction.RECREATE_POOL;
        }


        if ((newCcp.getDataStructureParameters() != null) && (oldCcp.getDataStructureParameters() != null)
                && !(newCcp.getDataStructureParameters().equals(oldCcp.getDataStructureParameters()))) {
            return ReconfigAction.RECREATE_POOL;
        }

        ConnectorDescriptorInfo oldCdi = oldCcp.getConnectorDescriptorInfo();
        ConnectorDescriptorInfo newCdi = newCcp.getConnectorDescriptorInfo();

        if (!oldCdi.getResourceAdapterClassName().equals(
                newCdi.getResourceAdapterClassName())) {

            logFine(
                    "isEqualConnectorConnectionPool: getResourceAdapterClassName:: " +
                            oldCdi.getResourceAdapterClassName() + " -- " +
                            newCdi.getResourceAdapterClassName());
            return ReconfigAction.RECREATE_POOL;
        }

        if (!oldCdi.getConnectionDefinitionName().equals(
                newCdi.getConnectionDefinitionName())) {

            logFine(
                    "isEqualConnectorConnectionPool: getConnectionDefinitionName:: " +
                            oldCdi.getConnectionDefinitionName() + " -- " +
                            newCdi.getConnectionDefinitionName());

            return ReconfigAction.RECREATE_POOL;
        }

        ConnectorSecurityMap[] newSecurityMaps = newCcp.getSecurityMaps();
        RuntimeSecurityMap newRuntimeSecurityMap =
                SecurityMapUtils.processSecurityMaps(newSecurityMaps);
        ConnectorSecurityMap[] oldSecurityMaps = oldCcp.getSecurityMaps();
        RuntimeSecurityMap oldRuntimeSecurityMap =
                SecurityMapUtils.processSecurityMaps(oldSecurityMaps);
        if (!(oldRuntimeSecurityMap.equals(newRuntimeSecurityMap))) {
            logFine("isEqualConnectorConnectionPool: CCP.getSecurityMaps:: " +
                    "New set of Security Maps is not equal to the existing" +
                    " set of security Maps.");
            return ReconfigAction.RECREATE_POOL;
        }
        return oldCdi.compareMCFConfigProperties(newCdi, excludedProps);
    }

    private static void logFine(String msg) {
        if (_logger.isLoggable(Level.FINE) && msg != null) {
            _logger.fine(msg);
        }
    }
}
