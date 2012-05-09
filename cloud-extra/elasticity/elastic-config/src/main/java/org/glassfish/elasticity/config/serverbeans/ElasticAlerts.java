package org.glassfish.elasticity.config.serverbeans;

import org.glassfish.api.Param;
import org.glassfish.paas.tenantmanager.api.TenantScoped;
import org.jvnet.hk2.config.*;
import java.util.List;

import java.beans.PropertyVetoException;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 4/10/12
 */
@TenantScoped
@Configured
public interface ElasticAlerts extends ConfigBeanProxy {

    @Element
    List <ElasticAlert> getElasticAlert();

    @DuckTyped
   public ElasticAlert  getElasticAlert(String state);

  class Duck {

      public static   ElasticAlert getElasticAlert(ElasticAlerts instance, String name)     {
          for (ElasticAlert alert: instance.getElasticAlert()){
              if (alert.getName().equals(name)){
                  return alert;
              }
          }
          return null;
      }
  }

}
