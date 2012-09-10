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

package com.sun.enterprise.connectors.inbound;

import com.sun.enterprise.connectors.*;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.EjbMessageBeanDescriptor;
import com.sun.enterprise.deployment.runtime.BeanPoolDescriptor;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.appserv.connectors.internal.api.ConnectorConstants;

import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ActivationSpec;
import java.util.Hashtable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;

/**
 * Represents the active (runtime) inbound resource-adapter
 */
@Service(name= ConnectorConstants.AIRA)
@PerLookup
public class ActiveInboundResourceAdapterImpl extends ActiveOutboundResourceAdapter
        implements ActiveInboundResourceAdapter {


    //beanID -> endpoint factory and its activation spec
    private Hashtable<String, MessageEndpointFactoryInfo> factories_;


    /**
     * Creates an active inbound resource adapter. Sets all RA java bean
     * properties and issues a start.
     *
     * @param ra         <code>ResourceAdapter<code> java bean.
     * @param desc       <code>ConnectorDescriptor</code> object.
     * @param moduleName Resource adapter module name.
     * @param jcl        <code>ClassLoader</code> instance.
     * @throws com.sun.appserv.connectors.internal.api.ConnectorRuntimeException
     *          If there is a failure in loading
     *          or starting the resource adapter.
     */
    public void init(ResourceAdapter ra, ConnectorDescriptor desc, String moduleName, ClassLoader jcl)
            throws ConnectorRuntimeException {
        super.init(ra, desc, moduleName, jcl);
        this.factories_ = new Hashtable<String, MessageEndpointFactoryInfo>();
    }

    public ActiveInboundResourceAdapterImpl() {
    }

    /**
     * Destroys default pools and resources. Stops the Resource adapter
     * java bean.
     */
    public void destroy() {
        deactivateEndPoints();
        super.destroy();
    }

    private void deactivateEndPoints() {
        if (resourceadapter_ != null) {
            //deactivateEndpoints as well!
            Iterator<MessageEndpointFactoryInfo> iter = getAllEndpointFactories().iterator();
            while (iter.hasNext()) {
                MessageEndpointFactoryInfo element = iter.next();
                try {
                    this.resourceadapter_.endpointDeactivation(
                            element.getEndpointFactory(), element.getActivationSpec());
                } catch (RuntimeException e) {
                    _logger.warning(e.getMessage());
                    _logger.log(Level.FINE, "Error during endpointDeactivation ", e);
                }
            }
        }
    }

    /**
     * Retrieves the information about all endpoint factories.
     *
     * @return a <code>Collection</code> of <code>MessageEndpointFactory</code>
     *         objects.
     */
    public Collection<MessageEndpointFactoryInfo> getAllEndpointFactories() {
        return factories_.values();
    }


    /**
     * Returns information about endpoint factory.
     *
     * @param id Id of the endpoint factory.
     * @return <code>MessageEndpointFactoryIndo</code> object.
     */
    public MessageEndpointFactoryInfo getEndpointFactoryInfo(String id) {
        return factories_.get(id);
    }

    public void updateMDBRuntimeInfo(EjbMessageBeanDescriptor descriptor_, BeanPoolDescriptor poolDescriptor)
            throws ConnectorRuntimeException {
        //do nothing
    }

    public void validateActivationSpec(ActivationSpec spec) {
        //do nothing
    }

    /*
     * @return A set of Map.Entry that has the bean ID as the key
     *         and the MessageEndpointFactoryInfo as value
     *         A shallow copy only to avoid concurrency issues.
     */
    public Set getAllEndpointFactoryInfo() {
        Hashtable infos = (Hashtable<String, MessageEndpointFactoryInfo>) factories_.clone();
        return infos.entrySet();
    }

    /**
     * {@inheritDoc}
     */
    public boolean handles(ConnectorDescriptor cd, String moduleName) {
        return (cd.getInBoundDefined() && !ConnectorsUtil.isJMSRA(moduleName));
     }


    /**
     * Adds endpoint factory information.
     *
     * @param id   Unique identifier of the endpoint factory.
     * @param info <code>MessageEndpointFactoryInfo</code> object.
     */
    public void addEndpointFactoryInfo(
            String id, MessageEndpointFactoryInfo info) {
        factories_.put(id, info);
    }

    /**
     * Removes information about an endpoint factory
     *
     * @param id Unique identifier of the endpoint factory to be
     *           removed.
     */
    public void removeEndpointFactoryInfo(String id) {
        factories_.remove(id);
    }

}
