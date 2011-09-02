package com.sun.enterprise.config.serverbeans;
import com.sun.enterprise.config.serverbeans.customvalidators.NotTargetKeyword;
import com.sun.enterprise.config.serverbeans.customvalidators.NotDuplicateTargetName;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.util.net.NetUtils;
import com.sun.enterprise.util.StringUtils;
import com.sun.logging.LogDomains;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.*;
import static org.glassfish.config.support.Constants.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.component.Injectable;
import org.glassfish.api.admin.config.Named;
import org.glassfish.api.admin.config.ReferenceContainer;

import javax.validation.OverridesAttribute;
import javax.validation.Payload;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 8/29/11
 */
@Configured
public interface AlertActions extends ConfigBeanProxy, Injectable {

       /**
   * Return the list of currently configured alert. Alerts can
   * be added or removed by using the returned {@link java.util.List}
   * instance
   *
   * @return the list of configured {@link Alerts}
   */

  @Element
   public List<AlertAction> getAlertAction();

  /**
   * Return the alert with the given name, or null if no such alert exists.
   *
   * @param   state    the state of the alert
   * @return          the Alert object, or null if no such alert
   */
  @DuckTyped
  public AlertAction  getAlertAction(String state);

  class Duck {

      public static   AlertAction getAlertAction(AlertActions instance, String state)     {
          for (AlertAction action: instance.getAlertAction()){
              if (action.getState().equals(state)){
                  return action;
              }
          }
          return null;
      }
  }
}
