<!--

 Copyright (c) 2017-2018 Payara Foundation and/or its affiliates. All rights reserved.

 The contents of this file are subject to the terms of either the GNU
 General Public License Version 2 only ("GPL") or the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://github.com/payara/Payara/blob/master/LICENSE.txt
 See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at glassfish/legal/LICENSE.txt.

 GPL Classpath Exception:
 The Payara Foundation designates this particular file as subject to the "Classpath"
 exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 file that accompanied this code.

 Modifications:
 If applicable, add the following below the License Header, with the fields
 enclosed by brackets [] replaced by your own identifying information:
 "Portions Copyright [year] [name of copyright owner]"

 Contributor(s):
 If you wish your version of this file to be governed by only the CDDL or
 only the GPL Version 2, indicate your decision by adding "[Contributor]
 elects to include this software in this distribution under the [CDDL or GPL
 Version 2] license."  If you don't indicate a single choice of license, a
 recipient has the option to distribute your version of this file under
 either the CDDL, the GPL Version 2 or to extend the choice of license to
 its licensees as provided above.  However, if you add GPL Version 2 code
 and therefore, elected the GPL Version 2 license, then the option applies
 only if the new code is made subject to such option by the copyright
 holder.
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
        
    <sun:tab id="healthcheckStuckThreadsCheckerTab" immediate="true" 
             text="$resource{i18nhx.healthcheck.configuration.stuckThreadsCheckerTabTitle}"
             toolTip="$resource{i18nhx.healthcheck.configuration.stuckThreadsCheckerTabTitleToolTip}">
	<!command
            setSessionAttribute(key="healthcheckConfigTab" value="healthcheckStuckThreadsCheckerTab");
            gf.redirect(page="#{request.contextPath}/healthcheck/healthcheck/checkers/stuckThreadsConfiguration.jsf?configName=#{configName}");
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
        
    <sun:tab id="healthcheckMicroprofileHealthcheckCheckerTab" immediate="true" 
             text="$resource{i18nhx.healthcheck.configuration.microprofileHealthcheckCheckerTabTitle}"
             toolTip="$resource{i18nhx.healthcheck.configuration.microprofileHealthcheckCheckerTabTitleToolTip}"
             rendered="#{pageSession.configName == 'server-config'}">
	<!command
            setSessionAttribute(key="healthcheckConfigTab" value="healthcheckMicroprofileHealthcheckCheckerTab");
            gf.redirect(page="#{request.contextPath}/healthcheck/healthcheck/checkers/mphealthcheckConfiguration.jsf?configName=#{configName}");
	/>
    </sun:tab>
</sun:tabSet>

