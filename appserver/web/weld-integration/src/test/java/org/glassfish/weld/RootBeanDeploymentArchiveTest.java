package org.glassfish.weld;

import com.sun.enterprise.deployment.EjbDescriptor;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.weld.connector.WeldUtils;
import org.junit.Test;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static org.easymock.EasyMock.*;

import org.easymock.EasyMockSupport;

import java.util.*;

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
        ArrayList<String> lib1ClassNames = new ArrayList<>();
        lib1ClassNames.add( Lib1Class1.class.getName() + ".class" );
        lib1ClassNames.add( Lib1Class2.class.getName() + ".class" );
        ArrayList<String> lib2ClassNames = new ArrayList<>();
        lib2ClassNames.add( Lib2Class1.class.getName() + ".class" );
        lib2ClassNames.add( Lib2Class2.class.getName() + ".class" );
        WeldUtils.BDAType bdaType = WeldUtils.BDAType.WAR;
        ArrayList<String> webInfLibEntries = new ArrayList<>();
        webInfLibEntries.add(webInfLib1);
        webInfLibEntries.add(webInfLib2);


        EasyMockSupport mockSupport = new EasyMockSupport();
        ReadableArchive readableArchive = mockSupport.createMock(ReadableArchive.class);
        ReadableArchive subArchive1 = mockSupport.createMock(ReadableArchive.class);
        ReadableArchive subArchive2 = mockSupport.createMock(ReadableArchive.class);

        Collection<EjbDescriptor> ejbs = Collections.emptyList();
        DeploymentContext deploymentContext = mockSupport.createMock( DeploymentContext.class );
        expect(deploymentContext.getClassLoader()).andReturn(null).anyTimes();

        expect(readableArchive.getName()).andReturn(archiveName).anyTimes();
        expect(readableArchive.exists(WeldUtils.WEB_INF_BEANS_XML)).andReturn(true).anyTimes();
        expect(readableArchive.exists(WeldUtils.WEB_INF_CLASSES_META_INF_BEANS_XML)).andReturn(false).anyTimes();

        // in BeanDeploymentArchiveImpl.populate
        expect( deploymentContext.getTransientAppMetadata()).andReturn(null).anyTimes();
        expect( readableArchive.entries() ).andReturn( Collections.<String>emptyEnumeration() );
        readableArchive.close();

        expect(readableArchive.exists(WeldUtils.WEB_INF_LIB)).andReturn(true).anyTimes();
        expect( readableArchive.entries(WeldUtils.WEB_INF_LIB) ).andReturn( Collections.enumeration( webInfLibEntries ));

        expect( readableArchive.getSubArchive( webInfLib1 )).andReturn( subArchive1);
        expect(subArchive1.exists(WeldUtils.META_INF_BEANS_XML ) ).andReturn(true);

        expect( readableArchive.getSubArchive( webInfLib2 )).andReturn( subArchive2);
        expect(subArchive2.exists(WeldUtils.META_INF_BEANS_XML ) ).andReturn(true);

        // build new BeanDeploymentArchiveImpl for lib1 and lib2
        setupMocksForWebInfLibBda( subArchive1, subArchive11Name, lib1ClassNames);
        setupMocksForWebInfLibBda( subArchive2, subArchive12Name, lib2ClassNames);
        readableArchive.close();
        mockSupport.replayAll();

        RootBeanDeploymentArchive rootBeanDeploymentArchive = new RootBeanDeploymentArchive(readableArchive,
                                                                                            ejbs,
                                                                                            deploymentContext);

        assertEquals( "root_" + archiveName, rootBeanDeploymentArchive.getId() );
        assertEquals( WeldUtils.BDAType.UNKNOWN, rootBeanDeploymentArchive.getBDAType() );
        assertEquals(0, rootBeanDeploymentArchive.getBeanClasses().size());
        assertEquals(0, rootBeanDeploymentArchive.getBeanClassObjects().size());
        assertNull(rootBeanDeploymentArchive.getBeansXml());

        BeanDeploymentArchiveImpl moduleBda = ( BeanDeploymentArchiveImpl ) rootBeanDeploymentArchive.getModuleBda();
        assertNotNull( moduleBda );
        assertEquals(WeldUtils.BDAType.WAR, moduleBda.getBDAType());

        assertEquals( 3, rootBeanDeploymentArchive.getBeanDeploymentArchives().size() );
        assertTrue(rootBeanDeploymentArchive.getBeanDeploymentArchives().contains(moduleBda));

        assertEquals( 3, moduleBda.getBeanDeploymentArchives().size() );
        assertTrue( moduleBda.getBeanDeploymentArchives().contains( rootBeanDeploymentArchive ));

        assertEquals(0, rootBeanDeploymentArchive.getModuleBeanClasses().size());
        assertEquals(0, rootBeanDeploymentArchive.getModuleBeanClassObjects().size());
        assertSame(rootBeanDeploymentArchive.getModuleClassLoaderForBDA(), moduleBda.getModuleClassLoaderForBDA());

        mockSupport.verifyAll();
        mockSupport.resetAll();
    }

    private void setupMocksForWebInfLibBda( ReadableArchive libJarArchive,
                                            String libJarArchiveName,
                                            ArrayList<String> archiveClassNames) throws Exception {
        expect( libJarArchive.getName() ).andReturn(libJarArchiveName).anyTimes();
        expect(libJarArchive.exists(WeldUtils.WEB_INF_BEANS_XML)).andReturn(false).anyTimes();
        expect(libJarArchive.exists(WeldUtils.WEB_INF_CLASSES_META_INF_BEANS_XML)).andReturn(false).anyTimes();
        expect(libJarArchive.exists(WeldUtils.WEB_INF_CLASSES)).andReturn(false).anyTimes();
        expect(libJarArchive.exists(WeldUtils.WEB_INF_LIB)).andReturn(false).anyTimes();
        expect(libJarArchive.exists(WeldUtils.META_INF_BEANS_XML)).andReturn(true).anyTimes();
        expect(libJarArchive.entries()).andReturn(Collections.enumeration(archiveClassNames));
        libJarArchive.close();
    }

    public class Lib1Class1 {}
    public class Lib1Class2 {}
    public class Lib2Class1 {}
    public class Lib2Class2 {}
}