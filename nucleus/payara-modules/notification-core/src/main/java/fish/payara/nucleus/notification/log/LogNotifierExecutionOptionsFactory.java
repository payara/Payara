/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2016 Payara Foundation. All rights reserved.
 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.
 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.notification.log;

import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.domain.NotifierExecutionOptions;
import fish.payara.nucleus.notification.domain.NotifierExecutionOptionsFactory;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;

/**
 * @author mertcaliskan
 */
@Service
@RunLevel(StartupRunLevel.VAL)
public class LogNotifierExecutionOptionsFactory extends NotifierExecutionOptionsFactory<LogNotifier> {

    @PostConstruct
    void postConstruct() {
        register(NotifierType.LOG, this);
    }

    public NotifierExecutionOptions build(LogNotifier notifier) {
        LogNotifierExecutionOptions executionOptions = new LogNotifierExecutionOptions();
        executionOptions.setEnabled(Boolean.parseBoolean(notifier.getEnabled()));
        return executionOptions;
    }
}