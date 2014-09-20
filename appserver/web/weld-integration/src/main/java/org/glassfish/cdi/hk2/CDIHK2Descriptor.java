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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Singleton;

import org.glassfish.hk2.api.DescriptorType;
import org.glassfish.hk2.api.DescriptorVisibility;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;

/**
 * This is an HK2 Descriptor that is backed by a CDI bean
 * 
 * @author jwells
 *
 */
@SuppressWarnings("serial")
public class CDIHK2Descriptor<T> extends AbstractActiveDescriptor<T> {
    private transient BeanManager manager = null;
    private transient Bean<T> bean = null;
    private transient Type requiredType = null;
    
    public CDIHK2Descriptor() {
        super();
    }
    
    private static Set<Annotation> fixQualifiers(Bean<?> bean) {
        Set<Annotation> fromBean = bean.getQualifiers();
        Set<Annotation> retVal = new HashSet<Annotation>();
        
        for (Annotation beanQ : fromBean) {
            if (Any.class.equals(beanQ.annotationType())) continue;
            
            if (Default.class.equals(beanQ.annotationType())) continue;
            
            retVal.add(beanQ);
        }
        
        return retVal;
    }
    
    private static Class<? extends Annotation> fixScope(Bean<?> bean) {
        if (bean.getScope() == null || Dependent.class.equals(bean.getScope())) {
            return PerLookup.class;
        }
        
        if (Singleton.class.equals(bean.getScope())) {
            return Singleton.class;
        }
        
        return CDIScope.class;
    }
    
    // @SuppressWarnings("unchecked")
    public CDIHK2Descriptor(BeanManager manager, Bean<T> bean, Type requiredType) {
        super(bean.getTypes(),
                fixScope(bean),
                bean.getName(),
                fixQualifiers(bean),
                DescriptorType.CLASS,
                DescriptorVisibility.NORMAL,
                0,
                null,
                null,
                null,
                new HashMap<String, List<String>>());
                
        this.manager = manager;
        this.bean = bean;
        this.requiredType = requiredType;
    }
    
    @Override
    public String getImplementation() {
        return bean.getBeanClass().getName();
    }

    @Override
    public Class<?> getImplementationClass() {
        return bean.getBeanClass();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T create(ServiceHandle<?> root) {
        CreationalContext<T> cc = manager.createCreationalContext(bean);
        
        return (T) manager.getReference(bean, requiredType, cc);
    }

}
