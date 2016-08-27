<!--

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation and/or its affiliates. 
 All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
-->
<sun:tabSet id="notificationTabs" immediate="true" selected="#{sessionScope.notificationTabs}" >
    <sun:tab id="notificationConfigure" immediate="true" text="$resource{i18nn.notification.notificationConfigure}" toolTip="$resource{i18nn.notification.notificationConfigureTab}">
	<!command
           setSessionAttribute(key="notificationTabs" value="notificationConfigure");
           gf.redirect(page="#{request.contextPath}/payaraExtras/notification/notification.jsf?configName=#{pageSession.configName}");
	/>
    </sun:tab>

    <sun:tab id="notificationConfigureNotifier" immediate="true" text="$resource{i18nn.notification.notificationConfigureNotifier}" toolTip="$resource{i18nn.notification.notificationConfigureNotifierTab}">
	<!command
            setSessionAttribute(key="notificationTabs" value="notificationConfigureNotifier");
            gf.redirect(page="#{request.contextPath}/payaraExtras/notification/notificationNotifier.jsf?configName=#{pageSession.configName}");
	/>
    </sun:tab>
</sun:tabSet>
