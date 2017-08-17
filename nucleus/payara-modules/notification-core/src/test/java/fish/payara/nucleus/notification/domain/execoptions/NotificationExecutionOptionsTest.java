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
package fish.payara.nucleus.notification.domain.execoptions;

import static fish.payara.nucleus.notification.configuration.NotifierType.LOG;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.domain.NotificationExecutionOptions;
import fish.payara.nucleus.notification.log.LogNotifierConfigurationExecutionOptions;

/**
 * @author mertcaliskan
 */
public class NotificationExecutionOptionsTest {

    NotificationExecutionOptions executionOptions;

    @Before
    public void setup() {
        executionOptions = new NotificationExecutionOptions();
    }

    @Test
    public void logNotifierConfigurationExecutionOptionAddedSuccessfully() {
        executionOptions.addNotifierConfigurationExecutionOption(new LogNotifierConfigurationExecutionOptions());

        assertNotNull(executionOptions.getNotifierConfigurationExecutionOptionsList());
        
        assertThat(
        		executionOptions.getNotifierConfigurationExecutionOptionsList().size(), 
        		is(1));
        
        assertThat(
        		executionOptions.getNotifierConfigurationExecutionOptionsList().get(LOG),
        		is(instanceOf(LogNotifierConfigurationExecutionOptions.class)));
    }

    @Test
    public void logNotifierConfigurationExecutionOptionRemovedSuccessfully() {
        LogNotifierConfigurationExecutionOptions logExecOptions = new LogNotifierConfigurationExecutionOptions();
        executionOptions.addNotifierConfigurationExecutionOption(logExecOptions);
        executionOptions.removeNotifierConfigurationExecutionOption(logExecOptions);

        assertNotNull(executionOptions.getNotifierConfigurationExecutionOptionsList());
        assertThat(executionOptions.getNotifierConfigurationExecutionOptionsList().size(), is(0));
    }

    @Test
    public void logNotifierConfigurationExecutionOptionEnabledSuccessfully() {
        NotificationExecutionOptions executionOptions = new NotificationExecutionOptions();

        LogNotifierConfigurationExecutionOptions logExecOptions = new LogNotifierConfigurationExecutionOptions();
        logExecOptions.setEnabled(true);
        executionOptions.addNotifierConfigurationExecutionOption(logExecOptions);

        assertNotNull(executionOptions.getNotifierConfigurationExecutionOptionsList());
        assertThat(executionOptions.isNotifierServiceEnabled(LOG), is(true));
    }
}
