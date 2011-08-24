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
  public List<MetricGatherer> getMetricGatherer();

  /**
   * Return the metric gatherer with the given type, or null if no such alert exists.
   *
   * @param   name    the type of the metric gatherer
   * @return          the MetricGatherer object, or null if no such type
   */

  @DuckTyped
  public MetricGatherer  getMetricGatherer(String type);


  class Duck {
      public static MetricGatherer getMetricGatherer(MetricGatherers instance, String type) {
          for (MetricGatherer mg : instance.getMetricGatherer()) {
              if (mg.getName().equals(type)) {
                  return mg;
              }
          }
          return null;
      }

  }

}
