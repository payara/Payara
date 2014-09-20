/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deployment.EjbDescriptor;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.weld.connector.WeldUtils;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.junit.Test;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static org.easymock.EasyMock.*;

import org.easymock.EasyMockSupport;

import java.net.URI;
import java.net.URL;
import java.util.*;
import org.junit.Ignore;

/**
 * @author <a href="mailto:j.j.snyder@oracle.com">JJ Snyder</a>
 */
public class RootBeanDeploymentArchiveTest {

    @Test
    public void testConstructor() throws Exception {
        String archiveName = "an";
        String webInfLib1 = "WEB-INF/lib/lib1.jar";
        String webInfLib2 = "WEB-INF/lib/lib2.jar";
        String subArchive11Name = "sa1";
        String subArchive12Name = "sa2";
        URI webInfLib1URI = URI.create(webInfLib1);
        URI webInfLib2URI = URI.create(webInfLib2);
        ArrayList<String> lib1ClassNames = new ArrayList<>();
        lib1ClassNames.add(Lib1Class1.class.getName() + ".class");
        lib1ClassNames.add(Lib1Class2.class.getName() + ".class");
        ArrayList<String> lib2ClassNames = new ArrayList<>();
        lib2ClassNames.add(Lib2Class1.class.getName() + ".class");
        lib2ClassNames.add(Lib2Class2.class.getName() + ".class");
        WeldUtils.BDAType bdaType = WeldUtils.BDAType.WAR;
        ArrayList<String> webInfLibEntries = new ArrayList<>();
        webInfLibEntries.add(webInfLib1);
        webInfLibEntries.add(webInfLib2);

        EasyMockSupport mockSupport = new EasyMockSupport();
        BeansXml beansXML = mockSupport.createMock(BeansXml.class);
        WeldBootstrap wb = mockSupport.createMock(WeldBootstrap.class);
        ReadableArchive readableArchive = mockSupport.createMock(ReadableArchive.class);
        ReadableArchive subArchive1 = mockSupport.createMock(ReadableArchive.class);
        ReadableArchive subArchive2 = mockSupport.createMock(ReadableArchive.class);

        Collection<EjbDescriptor> ejbs = Collections.emptyList();
        DeploymentContext deploymentContext = mockSupport.createMock(DeploymentContext.class);
        expect(deploymentContext.getClassLoader()).andReturn(null).anyTimes();

        expect(readableArchive.getName()).andReturn(archiveName).anyTimes();
        expect(readableArchive.exists(WeldUtils.WEB_INF_BEANS_XML)).andReturn(true).anyTimes();
        expect(readableArchive.exists(WeldUtils.WEB_INF_CLASSES_META_INF_BEANS_XML)).andReturn(false).anyTimes();

        // in BeanDeploymentArchiveImpl.populate
        expect(deploymentContext.getTransientAppMetadata()).andReturn(null).anyTimes();
        expect(deploymentContext.getTransientAppMetaData(WeldDeployer.WELD_BOOTSTRAP, WeldBootstrap.class)).andReturn(wb).anyTimes();
        expect(wb.parse(anyObject(URL.class))).andReturn(beansXML).anyTimes();

        expect(readableArchive.getURI()).andReturn(URI.create("an.war")).anyTimes();
        expect(subArchive1.getURI()).andReturn(webInfLib1URI).anyTimes();
        expect(subArchive2.getURI()).andReturn(webInfLib2URI).anyTimes();
        expect(beansXML.getBeanDiscoveryMode()).andReturn(BeanDiscoveryMode.ALL).anyTimes();

        expect(readableArchive.entries()).andReturn(Collections.<String>emptyEnumeration());
        readableArchive.close();

        expect(readableArchive.exists(WeldUtils.WEB_INF_LIB)).andReturn(true).anyTimes();
        expect(readableArchive.entries(WeldUtils.WEB_INF_LIB)).andReturn(Collections.enumeration(webInfLibEntries));

        expect(readableArchive.getSubArchive(webInfLib1)).andReturn(subArchive1);
        expect(subArchive1.exists(WeldUtils.META_INF_BEANS_XML)).andReturn(true);

        expect(readableArchive.getSubArchive(webInfLib2)).andReturn(subArchive2);
        expect(subArchive2.exists(WeldUtils.META_INF_BEANS_XML)).andReturn(true);

        // build new BeanDeploymentArchiveImpl for lib1 and lib2
        setupMocksForWebInfLibBda(subArchive1, subArchive11Name, lib1ClassNames);
        setupMocksForWebInfLibBda(subArchive2, subArchive12Name, lib2ClassNames);
        readableArchive.close();
        mockSupport.replayAll();

        RootBeanDeploymentArchive rootBeanDeploymentArchive = new RootBeanDeploymentArchive(readableArchive,
                                                                                            ejbs,
                                                                                            deploymentContext);

        assertEquals("root_" + archiveName, rootBeanDeploymentArchive.getId());
        assertEquals(WeldUtils.BDAType.UNKNOWN, rootBeanDeploymentArchive.getBDAType());
        assertEquals(0, rootBeanDeploymentArchive.getBeanClasses().size());
        assertEquals(0, rootBeanDeploymentArchive.getBeanClassObjects().size());
        assertNull(rootBeanDeploymentArchive.getBeansXml());

        BeanDeploymentArchiveImpl moduleBda = (BeanDeploymentArchiveImpl) rootBeanDeploymentArchive.getModuleBda();
        assertNotNull(moduleBda);
        assertEquals(WeldUtils.BDAType.WAR, moduleBda.getBDAType());

        assertEquals(3, rootBeanDeploymentArchive.getBeanDeploymentArchives().size());
        assertTrue(rootBeanDeploymentArchive.getBeanDeploymentArchives().contains(moduleBda));

        assertEquals(3, moduleBda.getBeanDeploymentArchives().size());
        assertTrue(moduleBda.getBeanDeploymentArchives().contains(rootBeanDeploymentArchive));

        assertEquals(0, rootBeanDeploymentArchive.getModuleBeanClasses().size());
        assertEquals(0, rootBeanDeploymentArchive.getModuleBeanClassObjects().size());
        assertSame(rootBeanDeploymentArchive.getModuleClassLoaderForBDA(), moduleBda.getModuleClassLoaderForBDA());

        mockSupport.verifyAll();
        mockSupport.resetAll();
    }

    private void setupMocksForWebInfLibBda(ReadableArchive libJarArchive,
                                           String libJarArchiveName,
                                           ArrayList<String> archiveClassNames) throws Exception {
        expect(libJarArchive.getName()).andReturn(libJarArchiveName).anyTimes();
        expect(libJarArchive.exists(WeldUtils.WEB_INF_BEANS_XML)).andReturn(false).anyTimes();
        expect(libJarArchive.exists(WeldUtils.WEB_INF_CLASSES_META_INF_BEANS_XML)).andReturn(false).anyTimes();
        expect(libJarArchive.exists(WeldUtils.WEB_INF_CLASSES)).andReturn(false).anyTimes();
        expect(libJarArchive.exists(WeldUtils.WEB_INF_LIB)).andReturn(false).anyTimes();
        expect(libJarArchive.exists(WeldUtils.META_INF_BEANS_XML)).andReturn(true).anyTimes();
        expect(libJarArchive.entries()).andReturn(Collections.enumeration(archiveClassNames));
        libJarArchive.close();
    }

    public class Lib1Class1 {
    }

    public class Lib1Class2 {
    }

    public class Lib2Class1 {
    }

    public class Lib2Class2 {
    }
}
