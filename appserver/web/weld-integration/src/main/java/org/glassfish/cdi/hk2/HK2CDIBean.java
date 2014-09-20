/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.cdi.hk2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * This is a CDI bean that is backed by an HK2 service
 * 
 * @author jwells
 *
 */
public class HK2CDIBean<T> implements Bean<T> {
    private final ServiceLocator locator;
    private final ActiveDescriptor<T> descriptor;
    
    /* package */
    HK2CDIBean(ServiceLocator serviceLocator, ActiveDescriptor<T> descriptor) {
        this.locator = serviceLocator;
        this.descriptor = descriptor;
    }

    @Override
    public T create(CreationalContext<T> arg0) {
        ServiceHandle<T> serviceHandle = locator.getServiceHandle(descriptor);
        return serviceHandle.getService();
    }

    @Override
    public void destroy(T arg0, CreationalContext<T> arg1) {
        descriptor.dispose(arg0);
    }

    @Override
    public Class<?> getBeanClass() {
        return descriptor.getImplementationClass();
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public String getName() {
        return descriptor.getName();
    }

    @Override
    public Set<Annotation> getQualifiers() {
        if (descriptor.getQualifierAnnotations().isEmpty()) {
            HashSet<Annotation> retVal = new HashSet<Annotation>();
            
            retVal.add(new DefaultImpl());
            
            return retVal;
        }
        
        return descriptor.getQualifierAnnotations();
    }

    @Override
    public Class<? extends Annotation> getScope() {
        Class<? extends Annotation> scope = descriptor.getScopeAnnotation();
        if (scope == null || scope.equals(PerLookup.class)) {
            scope = Dependent.class;
        }
        
        return scope;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<Type> getTypes() {
        return descriptor.getContractTypes();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public boolean isNullable() {
        // TODO, some scoped DO support a null return
        return false;
    }
    
    public ActiveDescriptor<T> getHK2Descriptor() {
        return descriptor;
    }
    
    @Override
    public String toString() {
        return "HK2CDIBean(" + descriptor + "," + System.identityHashCode(this) + ")";
    }
}
