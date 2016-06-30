/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.schedule.service;

import java.util.List;
import org.glassfish.api.admin.config.ConfigExtension;
import org.glassfish.api.admin.config.Container;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;



/**
 *
 * @author Daniel
 */
@Configured
public interface ScheduleConfig extends ConfigBeanProxy, ConfigExtension, Container{
    @Attribute(defaultValue="false")
    public Boolean getEnabled();
    public void setEnabled(String enabled);
    
    @Attribute(defaultValue="1")
    public int getCoreSize();
    public void setCoreSize(String size);
    
    @Attribute(defaultValue="false")
    public Boolean getFixedSize();
    public void setFixedSize(String sizeEnabled);
    
    @Element
    public void setJobs(List<String> args);
    public List<String> getJobs();
    

}
