package org.glassfish.weld;

import org.jboss.weld.Container;
import org.jboss.weld.Weld;
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

    private static class WeldSingleton {
        private static final Weld WELD_INSTANCE = new GlassFishEnhancedWeld();
    }

    private static class GlassFishEnhancedWeld extends Weld {

        @Override
        protected BeanManagerImpl unsatisfiedBeanManager(String callerClassName) {
            /*
             * In certain scenarios we use flat deployment model (weld-se, weld-servlet). In that case
             * we return the only BeanManager we have.
             */
            if (Container.instance().beanDeploymentArchives().values().size() == 1) {
                return Container.instance().beanDeploymentArchives().values().iterator().next();
            }

            Map<BeanDeploymentArchive, BeanManagerImpl> beanDeploymentArchives =
                Container.instance().beanDeploymentArchives();
            Set<BeanDeploymentArchive> bdas = beanDeploymentArchives.keySet();
            for ( BeanDeploymentArchive oneBda : bdas ) {
                if ( oneBda instanceof AppBeanDeploymentArchive ) {
                    return beanDeploymentArchives.get( oneBda );
                }
            }
            return super.unsatisfiedBeanManager(callerClassName);
        }
    }

    @Override
    public CDI<Object> getCDI() {
        return WeldSingleton.WELD_INSTANCE;
    }
}
