/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.glassfish.bootstrap;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Context;
import org.glassfish.hk2.api.ServiceHandle;
import org.jvnet.hk2.annotations.Service;

/**
 * This context is associated with a GlassFish server instance.  All services associated with this context
 * will have their life cycle tied to the life cycle of a GlassFish server.
 *
 * @author j.j.snyder
 *
 */
@Service
public class AppServerContext implements Context<AppServer> {
  final private Map<ActiveDescriptor<?>, Object> serviceMap = new HashMap<ActiveDescriptor<?>, Object>();

  /* (non-Javadoc)
  * @see org.glassfish.hk2.api.Context#getScope()
  */
  @Override
  public Class<? extends Annotation> getScope() {
    return AppServer.class;
  }

  /* (non-Javadoc)
  * @see org.glassfish.hk2.api.Context#findOrCreate(org.glassfish.hk2.api.ActiveDescriptor, org.glassfish.hk2.api.ServiceHandle)
  */
  @Override
  public <T> T findOrCreate(ActiveDescriptor<T> activeDescriptor, ServiceHandle<?> root) {
    Object service = serviceMap.get( activeDescriptor );
    if ( service == null ) {
      service = activeDescriptor.create(root);
      serviceMap.put( activeDescriptor, service );
    }

    return (T) service;
  }

  /* (non-Javadoc)
  * @see org.glassfish.hk2.api.Context#find(org.glassfish.hk2.api.Descriptor)
  */
  @Override
  public boolean containsKey(ActiveDescriptor<?> descriptor) {
    return serviceMap.containsKey( descriptor );
  }

  /* (non-Javadoc)
   * @see org.glassfish.hk2.api.Context#isActive()
   */
    @Override
    public boolean isActive() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.Context#supportsNullCreation()
     */
    @Override
    public boolean supportsNullCreation() {
        return false;
    }

    /* (non-Javadoc)
    * @see org.glassfish.hk2.api.Context#supportsNullCreation()
    */
    @Override
    public void shutdown() {
    }

    /**
     * Called when the server goes down so that the services are cleared from this context.
     */
    public void serverStopping() {
        for ( ActiveDescriptor descriptor : serviceMap.keySet()) {
            descriptor.dispose( serviceMap.get( descriptor ) );
        }
        serviceMap.clear();
    }

    @Override
    public void destroyOne(ActiveDescriptor<?> descriptor) {
    
    }
}
