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
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.JustInTimeInjectionResolver;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

/**
 * @author jwells
 *
 */
@Singleton
public class CDISecondChanceResolver implements JustInTimeInjectionResolver {
    private final ServiceLocator locator;
    
    @Inject
    private CDISecondChanceResolver(ServiceLocator locator) {
        this.locator = locator;
    }
    
    /**
     * Gets the currently scoped BeanManager
     * @return The currently scoped BeanManager, or null if a bean manager cannot be found
     */
    private BeanManager getCurrentBeanManager() {
        try {
            Context jndiContext = new InitialContext();
            return (BeanManager) jndiContext.lookup("java:comp/BeanManager");
        }
        catch (NamingException ne) {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.JustInTimeInjectionResolver#justInTimeResolution(org.glassfish.hk2.api.Injectee)
     */
    @SuppressWarnings({ "unchecked" })
    @Override
    public boolean justInTimeResolution(Injectee failedInjectionPoint) {
        Type requiredType = failedInjectionPoint.getRequiredType();
        
        Set<Annotation> setQualifiers = failedInjectionPoint.getRequiredQualifiers();
        
        Annotation qualifiers[] = setQualifiers.toArray(new Annotation[setQualifiers.size()]);
        
        BeanManager manager = getCurrentBeanManager();
        if (manager == null) return false;
        
        Set<Bean<?>> beans = manager.getBeans(requiredType, qualifiers);
        if (beans == null || beans.isEmpty()) {
            return false;
        }
        
        DynamicConfiguration config = ServiceLocatorUtilities.createDynamicConfiguration(locator);
        for (Bean<?> bean : beans) {
            // Add a bean to the service locator
            CDIHK2Descriptor<Object> descriptor = new CDIHK2Descriptor<Object>(manager, (Bean<Object>) bean, requiredType);
            config.addActiveDescriptor(descriptor);
        }
        
        config.commit();
        
        return true;
    }

}
