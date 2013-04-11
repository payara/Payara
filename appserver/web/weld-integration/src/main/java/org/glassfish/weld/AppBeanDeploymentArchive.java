package org.glassfish.weld;

import com.sun.enterprise.deployment.EjbDescriptor;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.weld.connector.WeldUtils;
import org.jboss.weld.bootstrap.spi.BeansXml;

import java.net.URL;
import java.util.Collections;

/**
 * @author <a href="mailto:j.j.snyder@oracle.com">JJ Snyder</a>
 */
public class AppBeanDeploymentArchive extends BeanDeploymentArchiveImpl {
    public AppBeanDeploymentArchive(String id, DeploymentContext deploymentContext) {
        super(id,
              Collections.<Class<?>>emptyList(),
              Collections.<URL>emptyList(),
              Collections.<EjbDescriptor>emptyList(),
              deploymentContext );
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
}
