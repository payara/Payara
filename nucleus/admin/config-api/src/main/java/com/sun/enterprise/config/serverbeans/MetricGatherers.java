package com.sun.enterprise.config.serverbeans;

import org.glassfish.api.I18n;
import org.glassfish.config.support.Create;
import org.glassfish.config.support.Delete;
import org.glassfish.config.support.TypeAndNameResolver;
import org.glassfish.config.support.TypeResolver;
import org.jvnet.hk2.component.Injectable;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 8/18/11
 * Time: 5:32 PM
 */
@Configured
public interface MetricGatherers extends ConfigBeanProxy, Injectable    {
    /**
   * Return the list of currently configured alert. Alerts can
   * be added or removed by using the returned {@link java.util.List}
   * instance
   *
   * @return the list of configured {@link Alerts}
   */
  @Element
//  @Create(value="create-metric-gatherer", resolver= TypeResolver.class, decorator= MetricGatherer.CreateDecorator.class,
   @Create(value="create-metric-gatherer", resolver= TypeResolver.class,   i18n=@I18n("create.metric-gatherer.command"))
  @Delete(value="delete-metric-gatherer", resolver= TypeAndNameResolver.class,   i18n=@I18n("delete.metric.gatherer.command"))
  public List<MetricGatherer> getMetricGatherers();

  /**
   * Return the alert with the given name, or null if no such alert exists.
   *
   * @param   name    the name of the alert
   * @return          the Alert object, or null if no such alert
   */

  @DuckTyped
  public MetricGatherer  getMetricGatherer(String name);


  class Duck {
      public static MetricGatherer getAlert(MetricGatherers instance, String name) {
          for (MetricGatherer mg : instance.getMetricGatherers()) {
              if (mg.getName().equals(name)) {
                  return mg;
              }
          }
          return null;
      }

  }

}
