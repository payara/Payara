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

import fish.payara.nucleus.phonehome.PhoneHomeCore;
import fish.payara.nucleus.phonehome.PhoneHomeRuntimeConfiguration;
import java.beans.PropertyVetoException;
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
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Admin command to enable Phone Home service.
 *
 * @author David Weaver
 */
@Service(name = "enable-phone-home")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("enable-phone-home")
@ExecuteOn(RuntimeType.INSTANCE)
@TargetType(value = {CommandTarget.DAS})
public class EnablePhoneHome implements AdminCommand {
    
    @Inject
    PhoneHomeRuntimeConfiguration configuration;
    
    @Inject
    PhoneHomeCore service;

    @Override
    public void execute(AdminCommandContext context) {
        
        final ActionReport report = context.getActionReport();
        
        try {
            ConfigSupport.apply(new SingleConfigCode<PhoneHomeRuntimeConfiguration>() {
                @Override
                public Object run(PhoneHomeRuntimeConfiguration configurationProxy)
                        throws PropertyVetoException, TransactionFailure {
                    configurationProxy.setEnabled("true");
                    return configurationProxy;
                }
            }, configuration);
        }
        catch(TransactionFailure ex) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
        
        service.start();
        
        report.setMessage("Phone Home Service is enabled");
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
