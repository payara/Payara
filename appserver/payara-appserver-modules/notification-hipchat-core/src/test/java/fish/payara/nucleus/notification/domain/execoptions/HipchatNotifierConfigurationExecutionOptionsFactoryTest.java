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

import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.notification.hipchat.HipchatNotifierConfiguration;
import fish.payara.notification.hipchat.HipchatNotifierConfigurationExecutionOptions;
import fish.payara.notification.hipchat.HipchatNotifierConfigurationExecutionOptionsFactory;
import fish.payara.nucleus.notification.service.NotifierConfigurationExecutionOptionsFactoryStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.UnsupportedEncodingException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author mertcaliskan
 */
@RunWith(MockitoJUnitRunner.class)
public class HipchatNotifierConfigurationExecutionOptionsFactoryTest {

    @InjectMocks
    HipchatNotifierConfigurationExecutionOptionsFactoryMock factory =
            new HipchatNotifierConfigurationExecutionOptionsFactoryMock();

    @Test
    public void notifierConfigurationExecutionOptionBuildsSuccessfullyForHipchat() throws UnsupportedEncodingException {
        HipchatNotifierConfiguration hipchatNotifierConfiguration = mock(HipchatNotifierConfiguration.class);
        when(hipchatNotifierConfiguration.getEnabled()).thenReturn("true");
        when(hipchatNotifierConfiguration.getRoomName()).thenReturn("room1");
        when(hipchatNotifierConfiguration.getToken()).thenReturn("token1");

        HipchatNotifierConfigurationExecutionOptions executionOptions = factory.build(hipchatNotifierConfiguration);

        assertThat(executionOptions.getNotifierType(), is(NotifierType.HIPCHAT));
        assertThat(executionOptions.isEnabled(), is(true));
        assertThat(executionOptions.getRoomName(), is("room1"));
        assertThat(executionOptions.getToken(), is("token1"));
    }
}

class HipchatNotifierConfigurationExecutionOptionsFactoryMock extends HipchatNotifierConfigurationExecutionOptionsFactory {

    public NotifierConfigurationExecutionOptionsFactoryStore getStore() {
        return mock(NotifierConfigurationExecutionOptionsFactoryStore.class);
    }
}