<!--

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 C2B2 Consulting Limited and/or its affiliates. 
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
<sun:tabSet id="requestTracingTabs" immediate="true" selected="#{sessionScope.requestTracingTabs}" >
    <sun:tab id="requestTracingConfigure" immediate="true" text="$resource{i18nrt.requestTracing.requestTracingConfigure}" toolTip="$resource{i18nrt.requestTracing.requestTracingConfigureTab}">
	<!command
           setSessionAttribute(key="requestTracingTabs" value="requestTracingConfigure");
           gf.redirect(page="#{request.contextPath}/payaraExtras/requestTracing/requestTracing.jsf?configName=#{pageSession.configName}");
	/>
    </sun:tab>

    <sun:tab id="requestTracingConfigureNotifier" immediate="true" text="$resource{i18nrt.requestTracing.requestTracingConfigureNotifier}" toolTip="$resource{i18nrt.requestTracing.requestTracingConfigureNotifierTab}">
	<!command
            setSessionAttribute(key="requestTracingTabs" value="requestTracingConfigureNotifier");
            gf.redirect(page="#{request.contextPath}/payaraExtras/requestTracing/requestTracingNotifier.jsf?configName=#{pageSession.configName}");
	/>
    </sun:tab>
</sun:tabSet>