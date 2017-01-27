!--

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.

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
<sun:tabSet id="notificationConfigTabs" immediate="true" selected="#{sessionScope.notificationConfigTab}" 
         text="Notification" toolTip="Notification Service Tabs">
    <!beforeCreate
        setResourceBundle(key="i18nn" bundle="fish.payara.admingui.extras.Strings");
        setResourceBundle(key="i18nc" bundle="org.glassfish.common.admingui.Strings");
    />
    
    <sun:tab id="notificationGeneralTab" immediate="true" text="General" toolTip="General Notification Config">
	<!command
            setSessionAttribute(key="notificationConfigTab" value="notificationGeneral");
            gf.redirect(page="#{request.contextPath}/payaraExtras/notification/notification.jsf?configName=#{configName}");
	/>
    </sun:tab>
</sun:tabSet>
<event>
    <!afterCreate
        includeIntegrations(type="fish.payara.admingui:notifierTab");
    />
</event>


