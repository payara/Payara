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
package org.glassfish.security.services.common;

import java.lang.annotation.Annotation;
import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Context;
import org.glassfish.hk2.api.ServiceHandle;
import org.jvnet.hk2.annotations.Service;

/**
 * The security context used to enable the scoping of security service instances.
 */
@Service
@Singleton
public class SecurityScopeContext implements Context<SecurityScope> {
    private final HashMap<String, HashMap<ActiveDescriptor<?>, Object>> contexts =
        new HashMap<String, HashMap<ActiveDescriptor<?>, Object>>();
    
    @Inject
    private StateManager manager;

    @Override
    public Class<? extends Annotation> getScope() {
        return SecurityScope.class;
    }

    @SuppressWarnings("unchecked")
	@Override
    public <T> T findOrCreate(ActiveDescriptor<T> activeDescriptor, ServiceHandle<?> root) {
        HashMap<ActiveDescriptor<?>, Object> mappings = getCurrentContext();
        
        Object retVal = mappings.get(activeDescriptor);
        if (retVal == null) {
            retVal = activeDescriptor.create(root);
            
            mappings.put(activeDescriptor, retVal);
        }
        return (T) retVal;
    }

    @SuppressWarnings("unchecked")
	@Override
    public boolean containsKey(ActiveDescriptor<?> descriptor) {
        HashMap<ActiveDescriptor<?>, Object> mappings = getCurrentContext();
        
        return mappings.containsKey(descriptor);
    }

    @Override
    public boolean isActive() {
        return manager.getCurrent() != null;
    }
    
    private HashMap<ActiveDescriptor<?>, Object> getCurrentContext() {
        if (manager.getCurrent() == null) throw new IllegalStateException("Not In Active State");
        
        HashMap<ActiveDescriptor<?>, Object> retVal = contexts.get(manager.getCurrent());
        if (retVal == null) {
            retVal = new HashMap<ActiveDescriptor<?>, Object>();
            
            contexts.put(manager.getCurrent(), retVal);
        }
        
        return retVal;
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.Context#supportsNullCreation()
     */
    @Override
    public boolean supportsNullCreation() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.Context#shutdown()
     */
    @Override
    public void shutdown() {
        // Do nothing
        
    }

    @Override
    public void destroyOne(ActiveDescriptor<?> descriptor) {
        // TODO Auto-generated method stub
        
    }
}
