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

package com.sun.enterprise.connectors.inbound;

import com.sun.enterprise.connectors.ActiveResourceAdapter;
import com.sun.enterprise.deployment.EjbMessageBeanDescriptor;
import com.sun.enterprise.deployment.runtime.BeanPoolDescriptor;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;

import javax.resource.spi.ActivationSpec;


public interface ActiveInboundResourceAdapter extends ActiveResourceAdapter {

    /**
     * Adds endpoint factory information.
     *
     * @param id   Unique identifier of the endpoint factory.
     * @param info <code>MessageEndpointFactoryInfo</code> object.
     */
    public void addEndpointFactoryInfo(String id, MessageEndpointFactoryInfo info) ;

    /**
     * Removes information about an endpoint factory
     *
     * @param id Unique identifier of the endpoint factory to be
     *           removed.
     */
    public void removeEndpointFactoryInfo(String id);

    /**
     * Returns information about endpoint factory.
     *
     * @param id Id of the endpoint factory.
     * @return <code>MessageEndpointFactoryIndo</code> object.
     */
    MessageEndpointFactoryInfo getEndpointFactoryInfo(String id);


    /**
     * update MDB container runtime properties
     * @param descriptor_ MessageBean Descriptor
     * @param poolDescriptor Bean pool descriptor
     * @throws ConnectorRuntimeException
     */
    public void updateMDBRuntimeInfo(EjbMessageBeanDescriptor descriptor_,
           BeanPoolDescriptor poolDescriptor) throws ConnectorRuntimeException ;


    /**
     * validate the activation-spec
     * @param spec activation-spec
     */
    public void validateActivationSpec(ActivationSpec spec);
}
