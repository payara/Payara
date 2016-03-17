package fish.payara.asadmin.recorder;

import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Andrew Pielage
 */
@Configured
public interface AsadminRecorderConfiguration extends ConfigBeanProxy, ConfigExtension
{
    @Attribute(defaultValue = "false", dataType = Boolean.class)
    public Boolean isEnabled();
    public void setEnabled(Boolean enabled);
    
    @Attribute(defaultValue = "true", dataType = Boolean.class)
    public Boolean filterCommands();
    public void setFilterCommands(Boolean filterCommands);
    
    @Attribute(defaultValue = 
            "${com.sun.aas.instanceRoot}/asadmin-commands.txt")
    public String getOutputLocation();
    public void setOutputLocation(String outputLocation);
    
    @Attribute(defaultValue = "version,_(.*),list(.*),get(.*),uptime")
    public String getFilteredCommands();
    public void setFilteredCommands(String filteredCommands);
}
