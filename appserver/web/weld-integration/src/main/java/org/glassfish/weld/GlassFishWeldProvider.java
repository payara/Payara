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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2024-2025] [Payara Foundation and/or its affiliates]

package org.glassfish.weld;

import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;
import org.jboss.weld.Container;
import org.jboss.weld.SimpleCDI;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.logging.BeanManagerLogger;
import org.jboss.weld.manager.BeanManagerImpl;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.CDIProvider;
import java.lang.StackWalker.StackFrame;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;

/**
 * @author <a href="mailto:j.j.snyder@oracle.com">JJ Snyder</a>
 */
public class GlassFishWeldProvider implements CDIProvider {
    private static final WeldDeployer weldDeployer = Globals.get(WeldDeployer.class);
    private static final InvocationManager invocationManager = Globals.get(InvocationManager.class);
    private static final StackWalker stackWalker = StackWalker.getInstance(RETAIN_CLASS_REFERENCE);

    private static class GlassFishEnhancedWeld extends SimpleCDI {
        private final Set<String> knownClassNames;

        GlassFishEnhancedWeld() {
            knownClassNames = new HashSet<>(super.knownClassNames);
        }

        GlassFishEnhancedWeld(String contextId) {
            super(contextId == null ? Container.instance() : Container.instance(contextId));
            knownClassNames = new HashSet<>(super.knownClassNames);
        }

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

        @Override
        protected String getCallingClassName() {
            boolean outerSubclassReached = false;
            BundleDescriptor bundleDescriptor = getBundleDescriptor();
            for (StackFrame element : stackWalker.walk(sf -> sf.collect(Collectors.toList()))) {
                // the method call that leads to the first invocation of this class or its subclass is considered the caller
                if (!knownClassNames.contains(element.getClassName())) {
                    Class<?> declaringClass = element.getDeclaringClass();
                    if (outerSubclassReached && declaringClass.getClassLoader() != null) {
                        if (bundleDescriptor != null && declaringClass.getClassLoader() == bundleDescriptor.getClassLoader()) {
                            // we are in the same class loader, so this is the caller
                            return declaringClass.getName();
                        } else {
                            return getAnyClassFromBundleDescriptor(declaringClass.getName());
                        }
                    }
                } else {
                    outerSubclassReached = true;
                }
            }
            throw BeanManagerLogger.LOG.unableToIdentifyBeanManager();
        }

        private static String getAnyClassFromBundleDescriptor(String backupClassName) {
            BeanDeploymentArchive bda = weldDeployer.getBeanDeploymentArchiveForBundle(getBundleDescriptor());
            return bda != null ? bda.getBeanClasses().stream().findAny().orElse(backupClassName) : backupClassName;
        }
    }

    @Override
    public CDI<Object> getCDI() {
        try {
            BeanDeploymentArchive bda = weldDeployer.getBeanDeploymentArchiveForBundle(getBundleDescriptor());
            if (bda == null) {
                return new GlassFishEnhancedWeld();
            } else {
                return new GlassFishEnhancedWeld(weldDeployer.getContextIdForArchive(bda));
            }
        } catch ( Throwable throwable ) {
            Throwable cause = throwable.getCause();
            if ( cause instanceof IllegalStateException ) {
                return null;
            }
            throw throwable;
        }
    }

    private static BundleDescriptor getBundleDescriptor() {
        BundleDescriptor bundle = null;
        Object componentEnv = null;
        if (!invocationManager.isInvocationStackEmpty()) {
            componentEnv = invocationManager.getCurrentInvocation().getJNDIEnvironment();
        }
        if( componentEnv instanceof EjbDescriptor) {
            bundle = (BundleDescriptor)
                    ((EjbDescriptor) componentEnv).getEjbBundleDescriptor().
                            getModuleDescriptor().getDescriptor();

        } else if( componentEnv instanceof WebBundleDescriptor) {
            bundle = (BundleDescriptor) componentEnv;
        }
        return bundle;
    }
}
