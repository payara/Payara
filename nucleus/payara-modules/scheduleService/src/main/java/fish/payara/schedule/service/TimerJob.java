/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.schedule.service;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import org.glassfish.common.util.timer.TimerSchedule;

/**
 *
 * @author Daniel
 */
public class TimerJob extends TimerTask{

    @Override
    public void run() {
        System.out.println("THE JOB WAS RUN");
            Timer timer = new Timer();
            TimerJob job = new TimerJob();
            
            
            
            TimerSchedule schedule = new TimerSchedule("*/5 # * # * # * # * # * # * # null # null # null");
            Calendar date = schedule.getNextTimeout();
            Date endDate = date.getTime();
            timer.schedule(job, endDate);
    }
    
}
