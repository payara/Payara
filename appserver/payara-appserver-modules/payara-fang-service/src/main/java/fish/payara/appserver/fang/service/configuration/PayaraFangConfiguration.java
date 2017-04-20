/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.appserver.fang.service.configuration;

import java.beans.PropertyVetoException;
import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

/**
 *
 * @author Andrew Pielage
 */
@Configured
public interface PayaraFangConfiguration extends ConfigBeanProxy, ConfigExtension {
    
    /**
     * Checks if Payara Fang is enabled or not
     * @return true if enabled
     */
    @Attribute(defaultValue="false", dataType = Boolean.class)
    String getEnabled();
    void setEnabled(String value) throws PropertyVetoException;

    @Attribute(defaultValue="/fang")
    String getContextRoot();
    void setContextRoot(String contextRoot) throws PropertyVetoException;
    
    @Attribute(defaultValue="__fang")
    String getApplicationName();
    void setApplicationName(String name) throws PropertyVetoException;
}
