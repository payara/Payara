/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.connectors.service;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.appserv.connectors.internal.spi.ConnectorNamingEvent;
import com.sun.enterprise.connectors.ConnectorConnectionPool;
import com.sun.enterprise.connectors.ConnectorDescriptorInfo;
import com.sun.enterprise.connectors.naming.ConnectorNamingEventNotifier;
import com.sun.enterprise.connectors.naming.ConnectorResourceNamingEventNotifier;
import org.glassfish.resources.naming.SerializableObjectRefAddr;
import org.glassfish.resourcebase.resources.api.PoolInfo;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.glassfish.resourcebase.resources.naming.ResourceNamingService;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.StringRefAddr;
import java.util.Hashtable;
import java.util.logging.Level;

/**
 * This is connector resource admin service. It creates and deletes the
 * connector resources.
 *
 * @author Srikanth P
 */
public class ConnectorResourceAdminServiceImpl extends ConnectorService {

    private ResourceNamingService namingService = _runtime.getResourceNamingService();
    /**
     * Default constructor
     */
    public ConnectorResourceAdminServiceImpl() {
        super();
    }

    /**
     * Creates the connector resource on a given connection pool
     *
     * @param resourceInfo     JNDI name of the resource to be created
     * @param poolInfo     PoolName to which the connector resource belongs.
     * @param resourceType Resource type Unused.
     * @throws ConnectorRuntimeException If the resouce creation fails.
     */
    public void createConnectorResource(ResourceInfo resourceInfo, PoolInfo poolInfo,
                                        String resourceType) throws ConnectorRuntimeException {

        try {
            ConnectorConnectionPool ccp = null;
            String jndiNameForPool = ConnectorAdminServiceUtils.
                    getReservePrefixedJNDINameForPool(poolInfo);
            try {
                ccp = (ConnectorConnectionPool) namingService.lookup(poolInfo, jndiNameForPool);
            } catch (NamingException ne) {
                //Probably the pool is not yet initialized (lazy-loading), try doing a lookup
                try {
                    checkAndLoadPool(poolInfo);
                    ccp = (ConnectorConnectionPool) namingService.lookup(poolInfo, jndiNameForPool);
                } catch (NamingException e) {
                    Object params[] = new Object[]{poolInfo, e};
                    _logger.log(Level.SEVERE, "unable.to.lookup.pool", params);
                }
            }

            if(ccp == null){
                ccp = (ConnectorConnectionPool) namingService.lookup(poolInfo, jndiNameForPool);
            }
            ConnectorDescriptorInfo cdi = ccp.getConnectorDescriptorInfo();

            javax.naming.Reference ref=new  javax.naming.Reference(
                   cdi.getConnectionFactoryClass(), 
                   "com.sun.enterprise.resource.naming.ConnectorObjectFactory",
                   null);
            RefAddr addr = new SerializableObjectRefAddr(PoolInfo.class.getName(), poolInfo);
            ref.add(addr);
            addr = new StringRefAddr("rarName", cdi.getRarName() );
            ref.add(addr);
            RefAddr resAddr = new SerializableObjectRefAddr(ResourceInfo.class.getName(), resourceInfo);
            ref.add(resAddr);

            try{
                namingService.publishObject(resourceInfo, ref, true);
                _registry.addResourceInfo(resourceInfo);
            }catch(NamingException ne){
                ConnectorRuntimeException cre = new ConnectorRuntimeException(ne.getMessage());
                cre.initCause(ne);
                Object params[] = new Object[]{resourceInfo, cre};
                _logger.log(Level.SEVERE, "rardeployment.resource_jndi_bind_failure", params);
                throw cre;
            }

/*

            ConnectorObjectFactory cof = new ConnectorObjectFactory(jndiName, ccp.getConnectorDescriptorInfo().
                    getConnectionFactoryClass(), cdi.getRarName(), poolName);

            _runtime.getNamingManager().publishObject(jndiName, cof, true);
*/

            //To notify that a connector resource rebind has happened.
            ConnectorResourceNamingEventNotifier.getInstance().
                    notifyListeners(new ConnectorNamingEvent(resourceInfo.toString(),
                            ConnectorNamingEvent.EVENT_OBJECT_REBIND));

        } catch (NamingException ne) {
            ConnectorRuntimeException cre = new ConnectorRuntimeException(ne.getMessage());
            cre.initCause(ne);
            Object params[] = new Object[]{resourceInfo, cre};
            _logger.log(Level.SEVERE, "rardeployment.jndi_lookup_failed", params);
            throw cre;
        }
    }

    /**
     * Deletes the connector resource.
     *
     * @param resourceInfo JNDI name of the resource to delete.
     * @throws ConnectorRuntimeException if connector resource deletion fails.
     */
    public void deleteConnectorResource(ResourceInfo resourceInfo)
            throws ConnectorRuntimeException {

        try {
            namingService.unpublishObject(resourceInfo, resourceInfo.getName());
        } catch (NamingException ne) {
            /* TODO for System RAR (not needed as proxy will always be present ?)
            ResourcesUtil resUtil = ResourcesUtil.createInstance();
            if (resUtil.resourceBelongsToSystemRar(jndiName)) {
                return;
            }
            */
            if (ne instanceof NameNotFoundException) {
                if(_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "rardeployment.connectorresource_removal_from_jndi_error", resourceInfo);
                    _logger.log(Level.FINE, "", ne);
                }
                return;
            }
            ConnectorRuntimeException cre = new ConnectorRuntimeException
                    ("Failed to delete connector resource from jndi");
            cre.initCause(ne);
            _logger.log(Level.SEVERE, "rardeployment.connectorresource_removal_from_jndi_error", resourceInfo);
            _logger.log(Level.SEVERE, "", cre);
            throw cre;
        }finally{
            _registry.removeResourceInfo(resourceInfo);
        }
    }

    /**
     * Gets Connector Resource Rebind Event notifier.
     *
     * @return ConnectorNamingEventNotifier
     */
    public ConnectorNamingEventNotifier getResourceRebindEventNotifier() {
        return ConnectorResourceNamingEventNotifier.getInstance();
    }


    /**
     * Look up the JNDI name with appropriate suffix.
     * Suffix can be either __pm or __nontx.
     *
     * @param resourceInfo resource-name
     * @return Object - from jndi
     * @throws NamingException - when unable to get the object form jndi
     */
    public Object lookup(ResourceInfo resourceInfo) throws NamingException {

        Hashtable env = null;
        String jndiName = resourceInfo.getName();
        String suffix = ConnectorsUtil.getValidSuffix(jndiName);

        //To pass suffix that will be used by connector runtime during lookup
        if(suffix != null){
            env = new Hashtable();
            env.put(ConnectorConstants.JNDI_SUFFIX_PROPERTY, suffix);
            jndiName = jndiName.substring(0, jndiName.lastIndexOf(suffix));
        }
        ResourceInfo actualResourceInfo = new ResourceInfo(jndiName, resourceInfo.getApplicationName(),
                resourceInfo.getModuleName());
        return namingService.lookup(actualResourceInfo, actualResourceInfo.getName(), env);
    }

}
