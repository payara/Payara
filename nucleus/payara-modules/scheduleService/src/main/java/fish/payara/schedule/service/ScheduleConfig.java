/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.schedule.service;

import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;



/**
 *
 * @author Daniel
 */
@Configured
public interface ScheduleConfig extends ConfigBeanProxy, ConfigExtension{
    @Attribute
    public String getTime();
    public String setTime(String time);
}
