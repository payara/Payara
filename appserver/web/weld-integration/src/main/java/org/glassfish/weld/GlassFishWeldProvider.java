/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.weld;

import org.jboss.weld.Container;
import org.jboss.weld.SimpleCDI;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.manager.BeanManagerImpl;

import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.CDIProvider;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:j.j.snyder@oracle.com">JJ Snyder</a>
 */
public class GlassFishWeldProvider implements CDIProvider {
    private static class GlassFishEnhancedWeld extends SimpleCDI {

        @Override
        protected BeanManagerImpl unsatisfiedBeanManager(String callerClassName) {
            /*
             * In certain scenarios we use flat deployment model (weld-se, weld-servlet). In that case
             * we return the only BeanManager we have.
             */
            if (Container.instance().beanDeploymentArchives().values().size() == 1) {
                return Container.instance().beanDeploymentArchives().values().iterator().next();
            }

            // To get the correct bean manager we need to determine the class loader of the calling class.
            // unfortunately we only have the class name so we need to find the root bda that has a class loader
            // that can successfully load the class.  This should give us the correct BDA which then can be used
            // to get the correct bean manager
            Map<BeanDeploymentArchive, BeanManagerImpl> beanDeploymentArchives =
                Container.instance().beanDeploymentArchives();
            Set<java.util.Map.Entry<BeanDeploymentArchive,BeanManagerImpl>> entries = beanDeploymentArchives.entrySet();
            for (Map.Entry<BeanDeploymentArchive, BeanManagerImpl> entry : entries) {
              BeanDeploymentArchive beanDeploymentArchive = entry.getKey();
              if ( beanDeploymentArchive instanceof RootBeanDeploymentArchive ) {
                RootBeanDeploymentArchive rootBeanDeploymentArchive = ( RootBeanDeploymentArchive ) beanDeploymentArchive;
                ClassLoader moduleClassLoaderForBDA = rootBeanDeploymentArchive.getModuleClassLoaderForBDA();
                try {
                  Class.forName( callerClassName, false, moduleClassLoaderForBDA );
                  // successful so this is the bda we want.
                  return entry.getValue();
                } catch ( Exception ignore ) {}
              }
            }

            return super.unsatisfiedBeanManager(callerClassName);
        }
    }

    @Override
    public CDI<Object> getCDI() {
      try {
        return new GlassFishEnhancedWeld();
      } catch ( Throwable throwable ) {
        Throwable cause = throwable.getCause();
        if ( cause instanceof IllegalStateException ) {
          return null;
        }
        throw throwable;
      }
    }

}
