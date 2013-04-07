package org.glassfish.weld;

import com.sun.enterprise.deployment.EjbDescriptor;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.weld.connector.WeldUtils;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A root BDA represents the root of a module where a module is a war, ejb, rar, ear lib
 * A root BDA of each module follows accessibility of the module (can only see BDAs, including root ones,
 * in accessible modules).  A root BDA contains no bean classes.  All bdas of the module are visible to the root bda.
 * And the root bda is visible to all bdas of the module.
 *
 * (Alternatively creating one root BDA per deployment has the disadvantage that you need to be careful about accessibility rules.
 * If you allow every BDA to see the root BDA - return it from BDA.getBeanDeploymentArchives() - and allow the root BDA
 * to see all other BDAs - return all other BDAs from root BDA.getDeployemtArchive(). Due to transitivity you make
 * any BDA accessible to any other BDA and break the accessibility rules.  One way is to only allow the root BDA to
 * see all the other BDAs (but not vice versa). This may work for the InjectionTarget case but may be a
 * limitation elsewhere.)
 *
 * @author <a href="mailto:j.j.snyder@oracle.com">JJ Snyder</a>
 */
public class RootBeanDeploymentArchive extends BeanDeploymentArchiveImpl {
    private BeanDeploymentArchiveImpl moduleBda;
    private List<BeanDeploymentArchive> dependentBdas = new ArrayList<>();

    public RootBeanDeploymentArchive(ReadableArchive archive,
                                     Collection<EjbDescriptor> ejbs,
                                     DeploymentContext deploymentContext) {
        this(archive, ejbs, deploymentContext, null);
    }

    public RootBeanDeploymentArchive(ReadableArchive archive,
                                     Collection<EjbDescriptor> ejbs,
                                     DeploymentContext deploymentContext,
                                     String moduleBdaID) {
        super("root_" + archive.getName(),
              Collections.<Class<?>>emptyList(),
              Collections.<URL>emptyList(),
              Collections.<EjbDescriptor>emptyList(),
              deploymentContext);
        createModuleBda(archive, ejbs, deploymentContext, moduleBdaID);
    }

    private void createModuleBda(ReadableArchive archive,
                                 Collection<EjbDescriptor> ejbs,
                                 DeploymentContext deploymentContext,
                                 String bdaId) {
        moduleBda = new BeanDeploymentArchiveImpl( archive, ejbs, deploymentContext, bdaId );

        // set the bda visibility for the root
        Collection<BeanDeploymentArchive> bdas = moduleBda.getBeanDeploymentArchives();
        for ( BeanDeploymentArchive oneBda : bdas ) {
            oneBda.getBeanDeploymentArchives().add( this );
            addDependentBda( oneBda );
        }

        moduleBda.getBeanDeploymentArchives().add( this );
        addDependentBda(moduleBda);
    }

    private void addDependentBda( BeanDeploymentArchive beanDeploymentArchive ) {
        dependentBdas.add( beanDeploymentArchive );
    }

//    public RootBeanDeploymentArchive(String id, List<Class<?>> wClasses, List<URL> beansXmlUrls, Collection<EjbDescriptor> ejbs, DeploymentContext ctx) {
//        super(id, wClasses, beansXmlUrls, ejbs, ctx);
//    }
//    public RootBeanDeploymentArchive(ReadableArchive archive, DeploymentContext ctx, String bdaID) {
//        super(archive, Collections.<EjbDescriptor>emptyList(), ctx, bdaID);
//    }


    @Override
    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return dependentBdas;
    }

    @Override
    public Collection<String> getBeanClasses() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Class<?>> getBeanClassObjects() {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getModuleBeanClasses() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Class<?>> getModuleBeanClassObjects() {
        return Collections.emptyList();
    }

    @Override
    public BeansXml getBeansXml() {
        return null;
    }

    @Override
    public WeldUtils.BDAType getBDAType() {
        //todo: this should return a root type
        return WeldUtils.BDAType.UNKNOWN;
    }

    @Override
    public ClassLoader getModuleClassLoaderForBDA() {
        return moduleBda.getModuleClassLoaderForBDA();
    }

    public BeanDeploymentArchive getModuleBda() {
        return moduleBda;
    }

    public WeldUtils.BDAType getModuleBDAType() {
        return moduleBda.getBDAType();
    }
}
