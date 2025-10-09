/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2021-2025 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package org.glassfish.weld.services;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import org.glassfish.deployment.common.ModuleDescriptor;
import org.glassfish.weld.WeldDeployer;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.*;

public class JCDIServiceImplTest {

    @Mock
    private EjbDescriptor ejbDescriptor;

    @Mock
    private EjbBundleDescriptor ejbBundleDescriptor;

    @Mock
    private ModuleDescriptor moduleDescriptor;

    @Mock
    private BundleDescriptor bundleDescriptor;

    @Mock
    private WeldDeployer weldDeployer;

    @Mock
    private BeanDeploymentArchive beanDeploymentArchive;

    @Mock
    private WeldBootstrap bootstrap;

    @Mock
    private Application application;

    @InjectMocks
    private JCDIServiceImpl jcdiServiceImpl = new JCDIServiceImpl();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void createJCDIInjectionContextWithoutNullPointerException() {
        Map<Class<?>, Object> ejbInfo = Collections.emptyMap();
        Collection<String> emptyListNames = Collections.emptyList();
        Collection<BeanDeploymentArchive> emptyListOfArchives = Collections.emptyList();
        when(ejbDescriptor.getEjbBundleDescriptor()).thenReturn(ejbBundleDescriptor);
        when(ejbBundleDescriptor.getModuleDescriptor()).thenReturn(moduleDescriptor);
        when(moduleDescriptor.getDescriptor()).thenReturn(bundleDescriptor);
        when(ejbDescriptor.getEjbClassName()).thenReturn("EjbName");
        when(weldDeployer.getBeanDeploymentArchiveForBundle(bundleDescriptor)).thenReturn(beanDeploymentArchive);
        when(weldDeployer.getBootstrapForArchive(beanDeploymentArchive)).thenReturn(bootstrap);
        when(beanDeploymentArchive.getBeanClasses()).thenReturn(emptyListNames);
        when(beanDeploymentArchive.getBeanDeploymentArchives()).thenReturn(emptyListOfArchives);

        Object obj = jcdiServiceImpl.createJCDIInjectionContext(ejbDescriptor, ejbInfo);

        Assert.assertNull(obj);
        verify(ejbDescriptor, times(1)).getEjbBundleDescriptor();
        verify(ejbBundleDescriptor, times(1)).getModuleDescriptor();
        verify(moduleDescriptor, times(1)).getDescriptor();
        verify(weldDeployer, times(1)).getBeanDeploymentArchiveForBundle(bundleDescriptor);
        verify(beanDeploymentArchive, times(1)).getBeanClasses();
        verify(beanDeploymentArchive, times(1)).getBeanDeploymentArchives();
        verify(weldDeployer, times(1)).getBootstrapForArchive(beanDeploymentArchive);
        verify(bootstrap, times(1)).getManager(beanDeploymentArchive);
    }
}