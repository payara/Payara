package org.glassfish.elasticity.config.serverbeans;

import org.glassfish.api.Param;
import org.glassfish.paas.tenantmanager.api.TenantScoped;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;

import java.beans.PropertyVetoException;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 4/10/12
 */
@TenantScoped
@Configured
public interface ElasticAlerts extends ConfigBeanProxy {

    /**
     * Sets the alert name
     * @param value alert name
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="name", primary = true)
    public void setName(String value);

    @Attribute
    public String getName();

    @Param(name="type")
    public void setType(String val);

    @Attribute
    public String getType();
    /**
     * Sets the alert schedule
     * @param value alert schedule
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="schedule", optional=true, defaultValue="30s")
    public void setSchedule(String value);

    @Attribute (defaultValue = "10s")
    public String getSchedule();

    /**
     * Sets the alert sample interval in minutes
     * @param value alert interval
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="sample-interval", optional=true, defaultValue="5")
    public void setSampleInterval(int value);

    @Attribute (defaultValue = "5")
    public int getSampleInterval();

    /**
     * Sets the alert sample interval in minutes
     * @param value alert interval
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="enabled", optional=true, defaultValue="true")
    public void setEnabled(boolean value);

    @Attribute  (defaultValue = "true")
    public boolean getEnabled();

   /**
     * Sets the alert service
     * @param value alert service
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="environment")
    public void setEnvironment(String value);

    @Attribute
    public String getEnvironment();

}
