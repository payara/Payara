package org.glassfish.elasticity.config.serverbeans;

import com.sun.enterprise.config.serverbeans.customvalidators.NotDuplicateTargetName;
import com.sun.enterprise.config.serverbeans.customvalidators.NotTargetKeyword;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;

import javax.validation.Payload;
import javax.validation.constraints.Pattern;
import java.beans.PropertyVetoException;

import static org.glassfish.config.support.Constants.NAME_SERVER_REGEX;
/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 8/26/11
 */
@Configured
public interface ScaleUpAction extends ActionConfig {

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
