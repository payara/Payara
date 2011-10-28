package org.glassfish.elasticity.config.serverbeans;

import org.glassfish.api.Param;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import java.beans.PropertyVetoException;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 10/25/11
 */
@Configured
public interface ActionConfig extends ConfigBeanProxy{

    /**
     * Sets the action name
     * @param value action name
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="name", primary = true)
    public void setName(String value) throws PropertyVetoException;

    @Attribute
    public String getName();


    /**
     * Sets the action type
     * @param value action type
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="type", primary = true)
    public void setType(String value) throws PropertyVetoException;

    @Attribute
    public String getType();


}
