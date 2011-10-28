package org.glassfish.elasticity.config.serverbeans;


import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.jvnet.hk2.component.Injectable;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

import java.beans.PropertyVetoException;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 9/21/11
 */
@Configured
public interface ScaleDownAction extends ActionConfig {
    /**
   * Sets the service name where the scale up action will be executed.  If null then the current service is assumed
   * @param value service name
   * @throws PropertyVetoException if a listener vetoes the change
   */
  @Param(name="service-name")
  public void setServiceName(String value) throws PropertyVetoException;

  @Attribute
  public String getServiceName();

}
