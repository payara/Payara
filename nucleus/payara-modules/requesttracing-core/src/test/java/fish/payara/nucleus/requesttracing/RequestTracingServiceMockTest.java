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
package fish.payara.nucleus.requesttracing;

import fish.payara.nucleus.notification.NotificationService;
import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.domain.LogNotificationEvent;
import fish.payara.nucleus.requesttracing.domain.execoptions.LogNotifierExecutionOptions;
import fish.payara.nucleus.requesttracing.domain.execoptions.RequestTracingExecutionOptions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author mertcaliskan
 */
@RunWith(MockitoJUnitRunner.class)
public class RequestTracingServiceMockTest {

    @Mock
    RequestEventStore eventStore;

    @Mock
    NotificationService notificationService;

    @Mock
    RequestTracingNotificationEventFactory eventFactory;

    @InjectMocks
    RequestTracingServiceMock requestTracingService = new RequestTracingServiceMock();

    @Test
    public void endingTraceInvokesNotificationServiceSuccessfullyWhenThresholdExceeded() {
        long elapsedTime = 1001L;
        LogNotificationEvent logNotificationEvent = new LogNotificationEvent();

        when(eventFactory.build(elapsedTime, NotifierType.LOG)).thenReturn(logNotificationEvent);
        when(eventStore.getElapsedTime()).thenReturn(elapsedTime);

        requestTracingService.endTrace();

        verify(notificationService).notify(logNotificationEvent);
    }
}

class RequestTracingServiceMock extends RequestTracingService {

    @Override
    public RequestTracingExecutionOptions getExecutionOptions() {
        RequestTracingExecutionOptions executionOptions = new RequestTracingExecutionOptions();
        executionOptions.setEnabled(true);
        executionOptions.setThresholdValue(1000L);
        executionOptions.setThresholdUnit(TimeUnit.MILLISECONDS);
        LogNotifierExecutionOptions logNotifierExecutionOptions = new LogNotifierExecutionOptions();
        logNotifierExecutionOptions.setEnabled(true);
        executionOptions.getNotifierExecutionOptionsList().put(NotifierType.LOG, logNotifierExecutionOptions);

        return executionOptions;
    }
}