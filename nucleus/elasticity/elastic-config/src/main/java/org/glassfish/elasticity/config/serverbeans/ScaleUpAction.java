package org.glassfish.elasticity.config.serverbeans;

import com.sun.enterprise.config.serverbeans.customvalidators.NotDuplicateTargetName;
import com.sun.enterprise.config.serverbeans.customvalidators.NotTargetKeyword;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.config.Named;
import org.glassfish.api.admin.config.ReferenceContainer;
import org.glassfish.config.support.Create;
import org.glassfish.config.support.TypeResolver;
import org.jvnet.hk2.component.Injectable;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
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
public interface ScaleUpAction extends ConfigBeanProxy {

    /**
   * Sets the action name
   * @param value action name
   * @throws PropertyVetoException if a listener vetoes the change
   */
  @Param(name="name", primary = true, defaultValue = "scale-up-action")
  public void setName(String value) throws PropertyVetoException;

  @Attribute (defaultValue = "scale-up-action")
  public String getName();


}
