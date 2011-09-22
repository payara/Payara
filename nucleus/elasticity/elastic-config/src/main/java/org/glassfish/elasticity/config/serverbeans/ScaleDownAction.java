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
public interface ScaleDownAction extends ConfigBeanProxy {
    /**
   * Sets the action name
   * @param value action name
   * @throws PropertyVetoException if a listener vetoes the change
   */
  @Param(name="name", primary = true, defaultValue = "scale-down-action")
  public void setName(String value) throws PropertyVetoException;

  @Attribute (defaultValue = "scale-down-action")
  public String getName();

}
