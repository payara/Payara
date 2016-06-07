/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.schedule.admin;

import fish.payara.schedule.service.ScheduleConfig;
import javax.inject.Inject;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Daniel
 */
@Service(name="foo-bar")
@PerLookup
public class CreateSchedule implements AdminCommand{

    @Inject
    ScheduleConfig config;
    
    @Param(name="time",optional=false)
    String time;
    
    @Override
    public void execute(AdminCommandContext context) {
        System.out.println("I AM FOO");
    }
    
}
