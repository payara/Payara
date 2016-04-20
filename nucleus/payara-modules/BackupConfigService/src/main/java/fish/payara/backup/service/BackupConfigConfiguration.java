/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.backup.service;

import javax.validation.constraints.Pattern;
import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

/**
 *
 * @author Daniel
 */
@Configured
public interface BackupConfigConfiguration extends ConfigBeanProxy, ConfigExtension{
    @Attribute(defaultValue="5")
    @Pattern(regexp="[1-9]")
    public String getMinutes();
    public String setMinutes();
    
    
    
    
}
