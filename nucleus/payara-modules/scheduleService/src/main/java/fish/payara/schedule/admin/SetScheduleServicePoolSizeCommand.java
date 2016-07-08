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
import java.util.logging.Logger;
import java.util.logging.Level;
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
@Service(name="set-schedule-thread-pool")
@PerLookup
public class SetScheduleServicePoolSizeCommand implements AdminCommand{

    private static final Logger log = Logger.getLogger(SetScheduleServicePoolSizeCommand.class.getCanonicalName());
    
    @Inject
    ScheduleConfig config;
        
    @Inject
    ScheduleService service;
    
    @Param
    String coreSize;
    
    @Param
    String useCoreSize;
    
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        try {
            ConfigSupport.apply(new SingleConfigCode<ScheduleConfig>(){
                public Object run(ScheduleConfig configProxy)
                    throws PropertyVetoException, TransactionFailure{                       
                        configProxy.setCoreSize(coreSize);
                        service.setPoolSize(coreSize);
                        if (useCoreSize.equals("true")|useCoreSize.equals("false")){
                            configProxy.setFixedSize(useCoreSize);
                        }else{
                            actionReport.setMessage("useCoreSize must be 'true' or false'");
                            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        }
                        configProxy.setFixedSize(useCoreSize);                                                                        
                        return null;
                 }
            },config);
        }catch (TransactionFailure ex){
            log.log(Level.INFO ,"The transaction has failed "+ ex);
            actionReport.setMessage("Set core size. See server.log for more details");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }//en transaction
        
    }
    
}
