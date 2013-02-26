<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.

    The contents of this file are subject to the terms of either the GNU
    General Public License Version 2 only ("GPL") or the Common Development
    and Distribution License("CDDL") (collectively, the "License").  You
    may not use this file except in compliance with the License.  You can
    obtain a copy of the License at
    https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
    or packager/legal/LICENSE.txt.  See the License for the specific
    language governing permissions and limitations under the License.

    When distributing the software, include this License Header Notice in each
    file and include the License file at packager/legal/LICENSE.txt.

    GPL Classpath Exception:
    Oracle designates this particular file as subject to the "Classpath"
    exception as provided by Oracle in the GPL Version 2 section of the License
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

<!--
     This handler gets a list of password aliases
     Input:  #{requestScope.doEscape}
     Output: #{pageSession.listOfAliases}
-->
<handler id="gfr.getPswdAliases">
    gf.restRequest(
        endpoint="#{sessionScope.REST_URL}/list-password-aliases"
        method="GET"
        result="#{requestScope.result}");
    setAttribute(key="_listOfMap" value="#{requestScope.result.data.children}");
    setPageSessionAttribute(key="listOfAliases" value={});
    if( "!(#{requestScope._listOfMap} = #{null})" ){
      foreach(var="oneChildMap" list="#{requestScope._listOfMap}"){
        if ("#{requestScope.doEscape}"){
            htmlEscape(value="#{requestScope.oneChildMap.message}" result="#{requestScope.encodedAliasName}");
            listAdd(list="#{pageSession.listOfAliases}" value="#{requestScope.encodedAliasName}" );
        }
        if ("!#{requestScope.doEscape}"){
         listAdd(list="#{pageSession.listOfAliases}" value="#{requestScope.oneChildMap.message}" );
      }
      }
      gf.listSort(list="#{pageSession.listOfAliases}");
    }
</handler>



<!--
   get a list of running instance.
   output = #{pageSession.runningInstances}
-->
<handler id="gfr.getListOfRunningInstances">
    gf.restRequest(endpoint="#{sessionScope.REST_URL}/list-instances",  method="get", result="#{requestScope.results}");
    listAdd(value="server", result="#{pageSession.runningInstances}");
    foreach (var="instance", list="#{requestScope.results.data.extraProperties.instanceList}") {
        if ('!(#{instance.status} = NOT_RUNNING)') {
            listAdd(list="#{pageSession.runningInstances}", value="#{instance.name}");
        }
    }
</handler>


<!-- get a list of instances based on attrMap
   input:   #{requestScope.listInstanceAttrMap}
   output:  #{pageSession.instanceList}
 -->
<handler id="gfr.getListOfInstances">
    gf.restRequest(endpoint="#{sessionScope.REST_URL}/list-instances",  attrs="#{requestScope.listInstanceAttrMap}" method="get", result="#{requestScope.result}");
    setPageSessionAttribute(key="instanceList" value={});
    foreach (var="instance", list="#{requestScope.result.data.extraProperties.instanceList}") {
        listAdd(list="#{pageSession.instanceList}", value="#{instance.name}");
    }
</handler>

 <!-- To test whether the monitoring is off or not.
   input:   #{pageSession.configName}
   output:  #{pageSession.isMonitoringOff}
 -->
<handler id="gfr.isMonitoringOff">
    urlencode(value="#{configName}" encoding="UTF-8" result="#{encodedConfigName}");
    setPageSessionAttribute(key="isMonitoringOff" value="#{true}");
    gf.getEntityAttrs(endpoint="#{sessionScope.REST_URL}/configs/config/#{pageSession.configName}/monitoring-service" valueMap="#{requestScope.monitorResult}");
    if ("#{requestScope.monitorResult['monitoringEnabled']}=true") {
        gf.getMonitorLevels(endpoint="#{sessionScope.REST_URL}/configs/config/#{pageSession.configName}" monitorCompList="#{requestScope.tableList}")
        setPageSessionAttribute(key="monitorDisable" value="monitorLevels");
        foreach(var="oneRow" list="#{requestScope.tableList}") {
            if("!(#{oneRow.level}=OFF)") {
                setPageSessionAttribute(key="isMonitoringOff" value="#{false}");
            }
        }
    }
</handler>
