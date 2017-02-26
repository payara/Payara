/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.nucleus.requesttracing;

import com.sun.enterprise.config.serverbeans.Server;
import fish.payara.nucleus.notification.NotificationService;
import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.domain.EventSource;
import fish.payara.nucleus.notification.domain.NotifierExecutionOptionsFactoryStore;
import fish.payara.nucleus.notification.service.NotificationEventFactoryStore;
import fish.payara.nucleus.notification.log.LogNotificationEventFactory;
import fish.payara.nucleus.notification.log.LogNotificationEvent;
import fish.payara.nucleus.notification.log.LogNotifierExecutionOptions;
import fish.payara.nucleus.requesttracing.domain.execoptions.RequestTracingExecutionOptions;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.ServiceLocator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    NotificationEventFactoryStore eventFactoryStore;

    @Mock
    NotifierExecutionOptionsFactoryStore execOptionsFactoryStore;


    @InjectMocks
    RequestTracingServiceMock requestTracingService = new RequestTracingServiceMock();

    LogNotificationEventFactoryMock logNotificationEventFactoryMock = new LogNotificationEventFactoryMock();

    @Test
    public void endingTraceInvokesNotificationServiceSuccessfullyWhenThresholdExceeded() {
        long elapsedTime = 1001L;
        when(eventStore.getElapsedTime()).thenReturn(elapsedTime);
        when(eventFactoryStore.get(NotifierType.LOG)).thenReturn(logNotificationEventFactoryMock);

        requestTracingService.endTrace();

        verify(notificationService).notify(any(EventSource.class), any(LogNotificationEvent.class));
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

class LogNotificationEventFactoryMock extends LogNotificationEventFactory {

    @Override
    public NotificationEventFactoryStore getStore() {
        return mock(NotificationEventFactoryStore.class);
    }


    @Override
    protected LogNotificationEvent initializeEvent(LogNotificationEvent logNotificationEvent) {
        return new LogNotificationEvent();
    }
}