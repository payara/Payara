<!--

 Copyright (c) 2018 Payara Foundation and/or its affiliates. All rights reserved.

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

<!initPage
    setResourceBundle(key="i18nhx" bundle="fish.payara.admingui.healthcheck.Strings");
    setResourceBundle(key="help_full" bundle="org.glassfish.full.admingui.Helplinks");
/>
<!composition template="/templates/default.layout"  
        guiTitle="$resource{i18nhx.healthcheck.checker.configuration.microprofileHealthcheckChecker.pageTitle}">

<!define name="content">

<event>
    <!beforeCreate
        getRequestValue(key="tabSetName" value=>$page{tabSetName} default="mpHealthcheckTab");
        urlencode(value="#{pageSession.tabSetName}" encoding="UTF-8" result="#{pageSession.encodedTabSetName}");       
        getRequestValue(key="configName" value=>$page{configName} default="server-config");
        urlencode(value="#{pageSession.configName}" encoding="UTF-8" result="#{pageSession.encodedConfigName}");
        getRequestValue(key="target" value="#{pageSession.target}");
 
        setPageSessionAttribute(key="HEALTHCHECK_CONFIG_URL", 
                value="#{sessionScope.REST_URL}/configs/config/#{pageSession.encodedConfigName}/health-check-service-configuration");
        gf.restRequest(endpoint="#{pageSession.HEALTHCHECK_CONFIG_URL}/get-healthcheck-configuration?target=#{pageSession.encodedConfigName}"  
                method="GET" result="#{requestScope.resp}");      
        setPageSessionAttribute(key="valueMap", 
                value="#{requestScope.resp.data.subReports.get(4).extraProperties.mpHealth}");
        mapPut(map="#{pageSession.valueMap}" key="target" value="#{pageSession.encodedConfigName}");
                
        setPageSessionAttribute(key="convertToFalseList", value={"enabled", "dynamic"});
        if (#{pageSession.valueMap['enabled']}=true) {
            setPageSessionAttribute(key="enabledSelected", value="true");
        }
        
        setPageSessionAttribute(key="dynamic", value="true");
    />
</event>

<sun:form id="propertyForm">
#include "/common/shared/alertMsg_1.inc"

