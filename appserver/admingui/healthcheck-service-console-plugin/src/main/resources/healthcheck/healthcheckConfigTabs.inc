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
<sun:tabSet id="healthcheckConfigTabs" immediate="true" selected="#{sessionScope.healthcheckConfigTab}" 
         text="$resource{i18nhx.healthcheck.configuration.tabSetTitle}" 
         toolTip="$resource{i18nhx.healthcheck.configuration.tabSetTitleToolTip}">
    <!beforeCreate
        setResourceBundle(key="i18nhx" bundle="fish.payara.admingui.healthcheck.Strings");
        setResourceBundle(key="i18nc" bundle="org.glassfish.common.admingui.Strings");
    />
    
    <sun:tab id="healthcheckGeneralTab" immediate="true" text="$resource{i18nhx.healthcheck.configuration.generalTabTitle}"
             toolTip="$resource{i18nhx.healthcheck.configuration.generalTabTitleToolTip}">
	<!command
            setSessionAttribute(key="healthcheckConfigTab" value="healthcheckGeneralTab");
            gf.redirect(page="#{request.contextPath}/healthcheck/healthcheck/healthcheckConfiguration.jsf?configName=#{configName}");
	/>
    </sun:tab>
    
    <sun:tab id="healthcheckCpuUsageCheckerTab" immediate="true" 
             text="$resource{i18nhx.healthcheck.configuration.cpuUsageCheckerTabTitle}"
             toolTip="$resource{i18nhx.healthcheck.configuration.cpuUsageCheckerTabTitleToolTip}">
	<!command
            setSessionAttribute(key="healthcheckConfigTab" value="healthcheckCpuUsageCheckerTab");
            gf.redirect(page="#{request.contextPath}/healthcheck/healthcheck/checkers/cpuUsageCheckerConfiguration.jsf?configName=#{configName}");
	/>
    </sun:tab>
    
    <sun:tab id="healthcheckConnectionPoolCheckerTab" immediate="true" 
             text="$resource{i18nhx.healthcheck.configuration.connectionPoolCheckerTabTitle}"
             toolTip="$resource{i18nhx.healthcheck.configuration.connectionPoolCheckerTabTitleToolTip}">
	<!command
            setSessionAttribute(key="healthcheckConfigTab" value="healthcheckConnectionPoolCheckerTab");
            gf.redirect(page="#{request.contextPath}/healthcheck/healthcheck/checkers/connectionPoolCheckerConfiguration.jsf?configName=#{configName}");
	/>
    </sun:tab>
    
    <sun:tab id="healthcheckHeapMemoryUsageCheckerTab" immediate="true" 
             text="$resource{i18nhx.healthcheck.configuration.heapMemoryUsageCheckerTabTitle}"
             toolTip="$resource{i18nhx.healthcheck.configuration.heapMemoryUsageCheckerTabTitleToolTip}">
	<!command
            setSessionAttribute(key="healthcheckConfigTab" value="healthcheckHeapMemoryUsageCheckerTab");
            gf.redirect(page="#{request.contextPath}/healthcheck/healthcheck/checkers/heapMemoryUsageCheckerConfiguration.jsf?configName=#{configName}");
	/>
    </sun:tab>
    
    <sun:tab id="healthcheckMachineMemoryUsageCheckerTab" immediate="true" 
             text="$resource{i18nhx.healthcheck.configuration.machineMemoryUsageCheckerTabTitle}"
             toolTip="$resource{i18nhx.healthcheck.configuration.machineMemoryUsageCheckerTabTitleToolTip}">
	<!command
            setSessionAttribute(key="healthcheckConfigTab" value="healthcheckMachineMemoryUsageCheckerTab");
            gf.redirect(page="#{request.contextPath}/healthcheck/healthcheck/checkers/machineMemoryUsageCheckerConfiguration.jsf?configName=#{configName}");
	/>
    </sun:tab>
    
    <sun:tab id="healthcheckHoggingThreadsCheckerTab" immediate="true" 
             text="$resource{i18nhx.healthcheck.configuration.hoggingThreadsCheckerTabTitle}"
             toolTip="$resource{i18nhx.healthcheck.configuration.hoggingThreadsCheckerTabTitleToolTip}">
	<!command
            setSessionAttribute(key="healthcheckConfigTab" value="healthcheckHoggingThreadsCheckerTab");
            gf.redirect(page="#{request.contextPath}/healthcheck/healthcheck/checkers/hoggingThreadsCheckerConfiguration.jsf?configName=#{configName}");
	/>
    </sun:tab>
    
    <sun:tab id="healthcheckGarbageCollectorCheckerTab" immediate="true" 
             text="$resource{i18nhx.healthcheck.configuration.garbageCollectorCheckerTabTitle}"
             toolTip="$resource{i18nhx.healthcheck.configuration.garbageCollectorCheckerTabTitleToolTip}">
	<!command
            setSessionAttribute(key="healthcheckConfigTab" value="healthcheckGarbageCollectorCheckerTab");
            gf.redirect(page="#{request.contextPath}/healthcheck/healthcheck/checkers/garbageCollectorCheckerConfiguration.jsf?configName=#{configName}");
	/>
    </sun:tab>
</sun:tabSet>

