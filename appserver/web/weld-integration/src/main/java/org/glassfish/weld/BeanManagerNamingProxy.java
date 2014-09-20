/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2014 Oracle and/or its affiliates. All rights reserved.
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

import javax.naming.NamingException;

import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.naming.NamespacePrefixes;
import org.glassfish.api.naming.NamedNamingObjectProxy;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.enterprise.deployment.WebBundleDescriptor;

/**
 * Proxy for java:comp/BeanManager lookups
 *
 *
 * @author Ken Saks
 */
@Service
@NamespacePrefixes(value = BeanManagerNamingProxy.BEAN_MANAGER_CONTEXT)
public class BeanManagerNamingProxy implements NamedNamingObjectProxy {

    @Inject
    private ComponentEnvManager compEnvManager;

    @Inject
    private InvocationManager invocationManager;

    @Inject
    private WeldDeployer weldDeployer;

    static final String BEAN_MANAGER_CONTEXT
            = "java:comp/BeanManager";
 

    public Object handle(String name) throws NamingException {

        Object beanManager = null;

        if (BEAN_MANAGER_CONTEXT.equals(name)) {
            try {

                // Use invocation context to find applicable BeanDeploymentArchive.
                ComponentInvocation inv = invocationManager.getCurrentInvocation();

                if( inv != null ) {

                    JndiNameEnvironment componentEnv = compEnvManager.getJndiNameEnvironment(inv.getComponentId());

                    if( componentEnv != null ) {

                        BundleDescriptor bundle = null;

                        if( componentEnv instanceof EjbDescriptor ) {
                            bundle = (BundleDescriptor)
                                    ((EjbDescriptor) componentEnv).getEjbBundleDescriptor().
                                            getModuleDescriptor().getDescriptor();

                        } else if( componentEnv instanceof WebBundleDescriptor ) {
                            bundle = (BundleDescriptor) componentEnv;

                        }

                        if( bundle != null ) {
                            BeanDeploymentArchive bda = weldDeployer.getBeanDeploymentArchiveForBundle(bundle);
                            if( bda != null ) {
                                WeldBootstrap bootstrap = weldDeployer.getBootstrapForApp(bundle.getApplication());
                                //System.out.println("BeanManagerNamingProxy:: getting BeanManagerImpl for" + bda);
                                beanManager = bootstrap.getManager(bda);
                            }
                        }

                        if( beanManager == null) {
                            throw new IllegalStateException("Cannot resolve bean manager");
                        }


                    } else {
                        throw new IllegalStateException("No invocation context found");
                    }
                }

            } catch(Throwable t) {
                NamingException ne = new NamingException("Error retrieving java:comp/BeanManager");
                ne.initCause(t);
                throw ne;
            }
        }

        return beanManager;
    }


}
