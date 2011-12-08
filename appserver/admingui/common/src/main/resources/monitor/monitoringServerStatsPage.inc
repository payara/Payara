<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.

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

<!-- monitor/monitoringServerStatsPage.inc -->
#include "/common/shared/commonHandlers.inc"
<event>
    <!beforeCreate
         gfr.getListOfRunningInstances();
        setPageSessionAttribute(key="displayPropSection" value="#{true}");
        gf.containedIn(list="#{pageSession.runningInstances}" testStr="#{pageSession.instanceName}" contain="#{requestScope.isInstanceRunning}");
        setPageSessionAttribute(key="isMonitoringOff" value="#{false}");
        if("#{requestScope.isInstanceRunning}"){
            setAttribute(key="instanceRunningAlert" value="#{false}");
            gfr.isMonitoringOff();
            if("!#{pageSession.isMonitoringOff}") {
                gf.getChildrenNamesList(endpoint="#{sessionScope.REST_URL}/configs/config/#{pageSession.configName}/http-service/virtual-server" result="#{pageSession.vsList}");
                gf.getChildrenNamesList(endpoint="#{sessionScope.REST_URL}/configs/config/#{pageSession.configName}/network-config/network-listeners/network-listener" result="#{pageSession.nlList}");
                gf.getChildrenNamesList(endpoint="#{pageSession.monitorURL}/jvm/thread-system", result="#{pageSession.threadList}");
                gf.getChildrenNamesList(endpoint="#{pageSession.monitorURL}/jvm/garbage-collectors", result="#{pageSession.gcList}");
                populateServerMonitorDropDown(VSList="#{vsList}", ThreadSystemList="#{threadList}", GCList="#{gcList}", NLList="#{nlList}", MonitorList=>$attribute{monitorList});
                 }
            if("#{pageSession.isMonitoringOff}") {
                setPageSessionAttribute(key="displayPropSection" value="#{false}");
            }
        }
        if("!#{requestScope.isInstanceRunning}"){
            setPageSessionAttribute(key="displayPropSection" value="#{false}");
            setAttribute(key="instanceRunningAlert" value="#{true}");
        }
        if(!#{viewVal}){
        getStats(endpoint="#{pageSession.monitorURL}/security/ejb", result=>$attribute{ejbStatsList} hasStats=>$attribute{hasStatsEJB});
        getStats(endpoint="#{pageSession.monitorURL}/web/jsp", result=>$attribute{jspWebStatsList}  hasStats=>$attribute{hasStatsJsp});
        getStats(endpoint="#{pageSession.monitorURL}/web/request", result=>$attribute{requestWebStatsList}  hasStats=>$attribute{hasStatsWRS});
        getStats(endpoint="#{pageSession.monitorURL}/web/session", result=>$attribute{sessionWebList}  hasStats=>$attribute{hasStatsWSSP});
        getStats(endpoint="#{pageSession.monitorURL}/web/servlet", result=>$attribute{servletWebList}  hasStats=>$attribute{hasStatsWSS});
        getStats(endpoint="#{pageSession.monitorURL}/jvm/runtime", result=>$attribute{runtimeStatsList}  hasStats=>$attribute{hasStatsJVMR});
        getStats(endpoint="#{pageSession.monitorURL}/jvm/compilation-system",  result=>$attribute{comStatsList}  hasStats=>$attribute{hasStatsJVMCOM});
        getStats(endpoint="#{pageSession.monitorURL}/jvm/class-loading-system", result=>$attribute{clStatsList}  hasStats=>$attribute{hasStatsJVMCL});
        getStats(endpoint="#{pageSession.monitorURL}/jvm/operating-system", result=>$attribute{osStatsList}  hasStats=>$attribute{hasStatsOS});
        getStats(endpoint="#{pageSession.monitorURL}/jvm/memory", result=>$attribute{jvmMemStatsList}  hasStats=>$attribute{hasStatsJVMM});
        getStats(endpoint="#{pageSession.monitorURL}/realm", result=>$attribute{realmStatsList} hasStats=>$attribute{hasStatsRealm});
        getStats(endpoint="#{pageSession.monitorURL}/transaction-service", result=>$attribute{txnServiceStatsList} hasStats=>$attribute{hasStatsTXN});
        getStats(endpoint="#{pageSession.monitorURL}/security/web", result=>$attribute{webSecurityStatsList}  hasStats=>$attribute{hasStatsWSD});
        getStats(endpoint="#{pageSession.monitorURL}/webservices", result=>$attribute{webServicesStatsList}  hasStats=>$attribute{hasStatsWS});
        getStats(endpoint="#{pageSession.monitorURL}/jvm/thread-system", result=>$attribute{jvmThreadStatsList}  hasStats=>$attribute{hasStatsJVMThread});
        getStats(endpoint="#{pageSession.monitorURL}/orb/transport/connectioncache/inbound", result=>$attribute{ORBInStatsList}  hasStats=>$attribute{hasStatsORBIn});
        getStats(endpoint="#{pageSession.monitorURL}/orb/transport/connectioncache/outbound", result=>$attribute{ORBOutStatsList}  hasStats=>$attribute{hasStatsORBOut});
        getStats(endpoint="#{pageSession.monitorURL}/jms-service/work-management", result=>$attribute{JMSStatsList}  hasStats=>$attribute{hasStatsJMS});
        getStats(endpoint="#{pageSession.monitorURL}/deployment/lifecycle", result=>$attribute{deployLifeCycleStatsList}  hasStats=>$attribute{hasStatsDeployLifeCycle});
        }
        if(#{viewVal}){
            setAttribute(key="hasStatsJVMThread" value="#{false}");
            setAttribute(key="hasStatsTXN" value="#{false}");
            setAttribute(key="hasStatsEJB" value="#{false}");
            setAttribute(key="hasStatsJsp" value="#{false}");
            setAttribute(key="hasStatsWRS" value="#{false}");
            setAttribute(key="hasStatsWSSP" value="#{false}");
            setAttribute(key="hasStatsWSS" value="#{false}");
            setAttribute(key="hasStatsJVMR" value="#{false}");
            setAttribute(key="hasStatsJVMCOM" value="#{false}");
            setAttribute(key="hasStatsJVMCL" value="#{false}");
            setAttribute(key="hasStatsOS" value="#{false}");
            setAttribute(key="hasStatsJVMM" value="#{false}");
            setAttribute(key="hasStatsRealm" value="#{false}");
            setAttribute(key="hasStatsWSD" value="#{false}");
            setAttribute(key="hasStatsWS" value="#{false}");
            setAttribute(key="hasStatsCQ" value="#{false}");
            setAttribute(key="hasStatsORBIn" value="#{false}");
            setAttribute(key="hasStatsORBOut" value="#{false}");
            setAttribute(key="hasStatsJMS" value="#{false}");
            setAttribute(key="hasStatsDeployLifeCycle" value="#{false}");
        }
        calculateHelpUrl(pluginId="#{pluginId}", helpKey: "$resource{help_web.monitorRuntime}", url="#{olhLink}");
    />
</event>
            
<sun:title id="propertyContentPage" title="$resource{i18nc.serverMonitor}"
    helpText="$resource{i18nc.monitoring.webContainer.PageHelp}">
    <!facet pageButtonsTop>
        <sun:panelGroup id="topButtons">
            <sun:button id="refreshButton"
                        text="$resource{i18n.button.Refresh}">
            <!command
                setAttribute(key="viewVal" value="#{viewVal}");
                setAttribute(key="monitorList" value="#{monitorList}");
                if ("$pageSession{encodedInstanceName}") {
                    setAttribute(key="instanceName" value="$pageSession{encodedInstanceName}");
                }
                if ("$pageSession{encodedClusterName}") {
                    setAttribute(key="clusterName" value="$pageSession{encodedClusterName}");
                }
                gf.navigate(page="#{pageSession.selfPage}");
            />
            </sun:button>
        </sun:panelGroup>
    </facet>
#include "/common/shared/nameSection.inc"
    <sun:propertySheet id="infoSheet">
        <sun:propertySheetSection id="infoSheetSection">
            <sun:property id="instNotRunningProp" rendered="#{instanceRunningAlert}"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" >
                <sun:staticText id="instNotRunning"text="$resource{i18nc.monitoring.webContainer.instanceNotRunningMsg}" />
            </sun:property>
            <sun:property id="monDisabledProp" rendered="#{isMonitoringOff}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" >
                <sun:staticText id="monDisabled"  text="$resource{i18nc.monitoring.webContainer.disableMonitoringMsg}" />
            </sun:property>
        </sun:propertySheetSection>
    </sun:propertySheet>
    <sun:propertySheet id="propertySheet">
        <sun:propertySheetSection id="viewPropertySection" rendered="#{displayPropSection}">
            <sun:property id="VsProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}">
                <sun:dropDown id="View" label="$resource{i18nc.monitoring.View} : " selected="#{viewVal}" items="$attribute{monitorList}" submitForm="#{true}" immediate="#{true}" >
                    <!command
                        setAttribute(key="click" value="$this{component}");
                        setAttribute(key="viewVal" value="#{click.selected}");
                        setAttribute(key="monitorList" value="#{monitorList}");
                        if ("$pageSession{encodedInstanceName}") {
                            setAttribute(key="instanceName" value="$pageSession{encodedInstanceName}");
                        }
                        if ("$pageSession{encodedClusterName}") {
                            setAttribute(key="clusterName" value="$pageSession{encodedClusterName}");
                        }
                        gf.navigate(page="#{pageSession.selfPage}");
                    />
                </sun:dropDown>
            </sun:property>
        </sun:propertySheetSection>
    </sun:propertySheet>

    <!-- Table .... -->
    <sun:table id="basicTable"  itemsText="$resource{i18nc.monitoring.statistics}" style="padding: 10pt" title="$resource{i18nc.monitoring}"  >
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{tableId});
        />
        <sun:tableRowGroup id="virtualServerStats" rendered="#{hasStatsVS}" data={"$attribute{virtualServerStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.virtualServerStatistics} : #{viewVal}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
            <!beforeCreate
                getStats(endpoint="#{pageSession.monitorURL}/http-service/#{viewVal}", result=>$attribute{virtualServerStatsList} hasStats=>$attribute{hasStatsVS});
            />

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="httpServiceStats" rendered="#{hasStatsHTTP}" data={"$attribute{httpServiceStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.requestStatisticsHttp} : #{viewVal}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
            <!beforeCreate
                getStats(endpoint="#{pageSession.monitorURL}/http-service/#{viewVal}/request", result=>$attribute{httpServiceStatsList} hasStats=>$attribute{hasStatsHTTP});
            />

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="fileCacheStats" rendered="#{hasStatsFC}" data={"$attribute{fileCacheStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.FileCacheStats} : #{viewVal}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
            <!beforeCreate
                getStats(endpoint="#{pageSession.monitorURL}/network/#{viewVal}/file-cache", result=>$attribute{fileCacheStatsList} hasStats=>$attribute{hasStatsFC});
            />

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="keepAliveStats" rendered="#{hasStatsKA}" data={"$attribute{keepAliveStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.KeepAliveStats} : #{viewVal}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
            <!beforeCreate
                getStats(endpoint="#{pageSession.monitorURL}/network/#{viewVal}/keep-alive", result=>$attribute{keepAliveStatsList} hasStats=>$attribute{hasStatsKA});
             />

#include "/common/monitor/monitoringTableRows.inc"
             "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="connectionQueueStats" rendered="#{hasStatsCQ}" data={"$attribute{connectionQueueStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.ConnectionQueueStats} : #{viewVal}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
            <!beforeCreate
                getStats(endpoint="#{pageSession.monitorURL}/network/#{viewVal}/connection-queue", result=>$attribute{connectionQueueStatsList} hasStats=>$attribute{hasStatsCQ});
             />

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="connectionsStats" rendered="#{hasStatsConnections}" data={"$attribute{connectionStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.ConnectionsStats} : #{viewVal}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
            <!beforeCreate
                getStats(endpoint="#{pageSession.monitorURL}/network/#{viewVal}/connections", result=>$attribute{connectionStatsList} hasStats=>$attribute{hasStatsConnections});
             />

#include "/common/monitor/monitoringTableRows.inc"
             "<br/>
         </sun:tableRowGroup>
         <sun:tableRowGroup id="threadPoolStats" rendered="#{hasStatsThreadPool}" data={"$attribute{threadPoolStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.ThreadPoolStatistics} : #{viewVal}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
            <!beforeCreate
                getStats(endpoint="#{pageSession.monitorURL}/network/#{viewVal}/thread-pool", result=>$attribute{threadPoolStatsList} hasStats=>$attribute{hasStatsThreadPool});
             />

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
         </sun:tableRowGroup>

        <sun:tableRowGroup id="jvmThreadInfoStats" rendered="#{hasStatsJVMThreadInfo}" data={"$attribute{jvmThreadInfoStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.ThreadInfo}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}"  extraHeaderHtml="colspan='6'">
            <!beforeCreate
                getStats(endpoint="#{pageSession.monitorURL}/jvm/thread-system/#{viewVal}", result=>$attribute{jvmThreadInfoStatsList}  hasStats=>$attribute{hasStatsJVMThreadInfo});
             />

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="gcStats" rendered="#{hasStatsGC}" data={"$attribute{gcStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.jvm.garbageCollectorsStats} : #{viewVal}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}"  extraHeaderHtml="colspan='6'">
            <!beforeCreate
                getStats(endpoint="#{pageSession.monitorURL}/jvm/garbage-collectors/#{viewVal}", result=>$attribute{gcStatsList}  hasStats=>$attribute{hasStatsGC});
            />

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="txnServiceStats" rendered="#{hasStatsTXN}" data={"$attribute{txnServiceStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.transaction}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="ejbContainerStats" rendered="#{hasStatsEJB}" data={"$attribute{ejbStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.ejb}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="securityRealm" rendered="#{hasStatsRealm}" data={"$attribute{realmStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.realm}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="jvmMemStats" rendered="#{hasStatsJVMM}" data={"$attribute{jvmMemStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.jvm.memoryJvm}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}"  extraHeaderHtml="colspan='6'">

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="osStats" rendered="#{hasStatsOS}" data={"$attribute{osStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.jvm.operatingSystemJvm}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}"  extraHeaderHtml="colspan='6'">

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="clStats" rendered="#{hasStatsJVMCL}" data={"$attribute{clStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.jvm.classLoadingJvm}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}"  extraHeaderHtml="colspan='6'">
#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="comStats" rendered="#{hasStatsJVMCOM}" data={"$attribute{comStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.jvm.compilationJvm}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="runtimeStats" rendered="#{hasStatsJVMR}" data={"$attribute{runtimeStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.jvm.runtimeJvm}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="jvmThreadStats" rendered="#{hasStatsJVMThread}" data={"$attribute{jvmThreadStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.ThreadSystem}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}"  extraHeaderHtml="colspan='6'">

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="servletsWeb" rendered="#{hasStatsWSS}" data={"$attribute{servletWebList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.servletStatisticsWeb} "
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>

        <sun:tableRowGroup id="sessionWeb" rendered="#{hasStatsWSSP}" data={"$attribute{sessionWebList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.sessionStatisticsWeb} "
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="requestWebStats" rendered="#{hasStatsWRS}" data={"$attribute{requestWebStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.requestStatisticsWeb}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="jspWebStats" rendered="#{hasStatsJsp}" data={"$attribute{jspWebStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.jspStatisticsWeb}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">

#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
         <sun:tableRowGroup id="webSecurity" rendered="#{hasStatsWSD}" data={"$attribute{webSecurityStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.webSecurityStats}"
             groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="jms" rendered="#{hasStatsJMS}" data={"$attribute{JMSStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.jmsStats}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
#include "/common/monitor/monitoringTableRows.inc"
                "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="orbInStats" rendered="#{hasStatsORBIn}" data={"$attribute{ORBInStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.ORBInStatistics}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="orbOutStats" rendered="#{hasStatsORBOut}" data={"$attribute{ORBOutStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.ORBOutStatistics}"
            groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="webServices" rendered="#{hasStatsWS}" data={"$attribute{webServicesStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.webServicesStats}"
                groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
        <sun:tableRowGroup id="deployLifeCycle" rendered="#{hasStatsDeployLifeCycle}" data={"$attribute{deployLifeCycleStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.deployLifeCycleStats}"
                groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
#include "/common/monitor/monitoringTableRows.inc"
            "<br/>
        </sun:tableRowGroup>
    </sun:table>
</sun:title>
