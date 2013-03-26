/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.web.WebComponentDecorator;
import com.sun.enterprise.web.WebModule;

import java.util.Collection;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

import org.glassfish.api.deployment.DeploymentContext;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jvnet.hk2.annotations.Service;

/**
 * This is a decorator which calls Weld implemetation to
 * do necessary injection of a web component. It is called by
 * {@link com.sun.web.server.J2EEInstanceListener}
 * before a web component is put into service.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 * @author Roger.Kitain@Sun.COM
 */
@Service
public class WebComponentInjectionManager<T> implements WebComponentDecorator<T> {
    @SuppressWarnings("unchecked")
    public void decorate(T webComponent, WebModule wm) {
        if (wm.getWebBundleDescriptor().hasExtensionProperty(WeldDeployer.WELD_EXTENSION)) {
            DeploymentContext deploymentContext = wm.getWebModuleConfig().getDeploymentContext();
            WeldBootstrap weldBootstrap = deploymentContext.getTransientAppMetaData(
                WeldDeployer.WELD_BOOTSTRAP, org.jboss.weld.bootstrap.WeldBootstrap.class);

            DeploymentImpl deploymentImpl = deploymentContext.getTransientAppMetaData(
                WeldDeployer.WELD_DEPLOYMENT, DeploymentImpl.class); 
            Collection<BeanDeploymentArchive> deployments = deploymentImpl.getBeanDeploymentArchives();
            BeanDeploymentArchive beanDeploymentArchive = deployments.iterator().next();
            BeanManager beanManager = weldBootstrap.getManager(beanDeploymentArchive);
            // PENDING : Not available in this Web Beans Release
            CreationalContext<T> ccontext = beanManager.createCreationalContext(null);
            @SuppressWarnings("rawtypes")
            Class<T> clazz = (Class<T>) webComponent.getClass();
            AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(clazz);
            InjectionTarget<T> injectionTarget = beanManager.createInjectionTarget(annotatedType);
            injectionTarget.inject(webComponent, ccontext);
        }
    }
}
