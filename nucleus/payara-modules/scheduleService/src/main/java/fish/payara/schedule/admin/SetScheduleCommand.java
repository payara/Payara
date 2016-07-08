/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2016 C2B2 Consulting Limited and/or its affiliates.
 * All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.schedule.admin;

import fish.payara.schedule.service.ScheduleConfig;
import fish.payara.schedule.service.ScheduleService;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Daniel
 */
@Service(name="set-schedule")
@PerLookup
public class SetScheduleCommand implements AdminCommand{
    
    private static final Logger log = Logger.getLogger(SetScheduleCommand.class.getCanonicalName());
    
    @Inject
    ScheduleConfig config;
    
    @Param(name="name",optional=false)
    String name;
    
    @Param(name="cron",optional=false)
    String cron;
    
    @Param(name="filePath", optional=false)
    String filePath;
    
    @Inject
    ScheduleService service;
    
    String newJob;
    
    String job;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        try {
            ConfigSupport.apply(new SingleConfigCode<ScheduleConfig>(){
                public Object run(ScheduleConfig configProxy)
                    throws PropertyVetoException, TransactionFailure{
                    if (name != null){
                        Boolean found = false;

                        List<String> jobs = new ArrayList();
                        for (int i =0; i < configProxy.getJobs().size();i++){
                            job = configProxy.getJobs().get(i);
                            String[] configInfo = job.split(",");
                            String[] nameInfo = configInfo[0].split("=");

                            if (nameInfo[1].equals(name) ){ 
                                newJob = "name="+nameInfo[1]+",cron="+service.buildCron(cron)+",filePath="+filePath;
                                if (service.validateSchedule(newJob,false)){                                      
                                    jobs.add(newJob);
                                    found=true;
                                }                              
                            } else{
                                jobs.add(configProxy.getJobs().get(i));
                                log.log(Level.INFO, "Edited schedule "+ name);
                            }
                            
                        }
                                            
                        if (found.equals(false)){
                            log.log(Level.WARNING,"The schedule name was not found, please check that schedule exists");
                            actionReport.setMessage("Could not edit schedule. See server.log for more details");
                            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        }else {
                            HashMap scheduledTasks = service.getFuturesList();                            
                            Future job = (Future)scheduledTasks.get(name);
                            job.cancel(true);
                            configProxy.setJobs(jobs);
                            service.validateSchedule(newJob, true);
                            
                        }
                    }
                    return null;
                }
            },config);
        }catch (TransactionFailure ex){
            log.log(Level.WARNING,"The transaction has failed "+ ex);
            ex.printStackTrace();
            actionReport.setMessage("Could not edit schedule. See server.log for more details");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
//end transaction
    }
    
    
    
}
