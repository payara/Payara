package org.glassfish.elasticity.config.serverbeans;

import org.glassfish.api.Param;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import java.beans.PropertyVetoException;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 10/26/11
 */
@Configured
public interface EmailAction extends ActionConfig{

    @Param(name="address")
    public void setAddress(String value) throws PropertyVetoException;

    @Attribute
    String getAddress();

    @Param(name="cc")
    public void setCC(String value) throws PropertyVetoException;

    @Attribute
    String getCC();

}
