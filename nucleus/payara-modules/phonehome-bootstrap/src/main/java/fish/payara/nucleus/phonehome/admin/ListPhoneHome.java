/*
 * 
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) 2016 Payara Foundation and/or its affiliates.
 *  All rights reserved.
 * 
 *  The contents of this file are subject to the terms of the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 *  or packager/legal/LICENSE.txt.  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at packager/legal/LICENSE.txt.
 * 
 */
package fish.payara.nucleus.phonehome.admin;

import fish.payara.nucleus.phonehome.PhoneHomeRuntimeConfiguration;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 * Admin command to disable Phone Home service.
 *
 * @author David Weaver
 */
@Service(name = "list-phone-home")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list-phone-home")
@ExecuteOn(RuntimeType.INSTANCE)
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.DEPLOYMENT_GROUP})
public class ListPhoneHome implements AdminCommand {
    
    @Inject
    PhoneHomeRuntimeConfiguration configuration;

    @Override
    public void execute(AdminCommandContext context) {
        
        final ActionReport report = context.getActionReport();
        
        if (Boolean.valueOf(configuration.getEnabled())) {
            report.setMessage("Phone Home Service is enabled");
        } else {
            report.setMessage("Phone Home Service is disabled");
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}