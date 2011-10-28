package org.glassfish.elasticity.config.serverbeans;

import org.glassfish.api.Param;
import org.jvnet.hk2.config.Attribute;
import java.beans.PropertyVetoException;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 10/26/11
 */
public interface URLMetricGatherer extends MetricGatherer {
    /**
     * Sets the metric URL
     * @param value URL
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="URL", primary = true)
    public void setUrl(String value) throws PropertyVetoException;

    @Attribute
    public String getUrl();

}
