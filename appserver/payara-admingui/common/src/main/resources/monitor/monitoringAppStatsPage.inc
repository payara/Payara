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

<!-- monitor/monitoringAppStatsPage.inc -->
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
                gf.getChildrenNamesList(endpoint="#{sessionScope.REST_URL}/applications/application" result="#{pageSession.tmpList}");
                removeListElement(list="#{pageSession.tmpList}" name="list-sub-components" finalList="#{pageSession.tmpList}");
                gf.getChildrenNamesList(endpoint="#{sessionScope.REST_URL}/configs/config/#{pageSession.configName}/http-service/virtual-server" result="#{pageSession.vsList}");
                addToMonitorList(oldList="#{pageSession.loginList}", newList="#{pageSession.tmpList}"  result="#{pageSession.appList}");
                populateApplicationsMonitorDropDown(monitorURL="#{pageSession.monitorURL}" AppsList="#{appList}", MonitorList=>$attribute{monitorList} FirstItem=>$attribute{firstVal});
                if(!#{viewVal}) {
                    setAttribute(key="viewVal" value="#{firstVal}");
                }
                if(#{viewVal}) {
                    setPageSessionAttribute(key="viewVal" value="#{viewVal}");
                }
                if(#{cVal}) {
                    setPageSessionAttribute(key="cVal" value="#{cVal}");
                }
                getAppName(endpoint="#{sessionScope.REST_URL}/applications/application" name="#{viewVal}", appName=>$attribute{appValue}, appFullName=>$attribute{appFullName});
                //To get the sub components of a module
                if(#{viewVal}) {
                    setAttribute(key="isAppScopedresourcesComp" value="$boolean{false}");
                    if(#{cVal}) {
                        startsWith(testStr="#{cVal}" pattern="resources/" result=>$attribute{isAppScopedresourcesComp});
                    }
                    if(!#{isAppScopedresourcesComp}) {
                        createMap(result="#{requestScope.attrsMap}");
                        mapPut(map="#{requestScope.attrsMap}"  key="id" value="#{viewVal}");
                        mapPut(map="#{requestScope.attrsMap}"  key="appname"  value="#{appValue}");
                        gf.restRequest(
                            endpoint="#{sessionScope.REST_URL}/applications/application/#{appValue}/list-sub-components"
                            attrs="#{requestScope.attrsMap}"
                            method="GET"
                            result="#{requestScope.result}" );
                        setPageSessionAttribute(key="propsMap" value="#{requestScope.result.data.properties}");
                    }
                }

                getWebStatsUrl(app="#{appFullName}",
                    compVal="#{cVal}",
                    monitorURL="#{pageSession.monitorURL}",
                    vsList="#{pageSession.vsList}",
                    moduleProps="#{pageSession.propsMap}",
                    webServletUrl=>$attribute{webServletUrl},
                    webUrl=>$attribute{webUrl},
                    webServletType=>$attribute{webServletType},
                    webType=>$attribute{webType});
                getStatsUrl(app="#{appFullName}",
                    compVal="#{cVal}",
                    monitorURL="#{pageSession.monitorURL}",
                    moduleProps="#{pageSession.propsMap}",
                    statUrl=>$attribute{statUrl},
                    statType=>$attribute{statType});
            }
            if("#{pageSession.isMonitoringOff}") {
                setPageSessionAttribute(key="displayPropSection" value="#{false}");
            }
        }
        if("!#{requestScope.isInstanceRunning}"){
            setPageSessionAttribute(key="displayPropSection" value="#{false}");
            setAttribute(key="instanceRunningAlert" value="#{true}");
        }
        calculateHelpUrl(pluginId="#{pluginId}", helpKey: "$resource{help_web.monitorApps}", url="#{olhLink}");
    />
</event>    

<sun:title id="propertyContentPage" title="$resource{i18nc.appMonitor}"
    helpText="$resource{i18nc.monitoring.webContainer.PageHelp}">
        <!facet pageButtonsTop>
            <sun:panelGroup id="topButtons">
                <sun:button id="refreshButton"
                            text="$resource{i18n.button.Refresh}">
                    <!command
                        setAttribute(key="viewVal" value="#{viewVal}");
                        setAttribute(key="monitorList" value="#{monitorList}");
                        setAttribute(key="cVal" value="#{cVal}");
                        setAttribute(key="componentList" value="#{componentList}");
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
            <sun:property id="ApplicationProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}">
                
                <sun:dropDown id="View" label="$resource{i18nc.monitoring.Application} : " items="$attribute{monitorList}" selected="#{viewVal}" submitForm="#{true}" immediate="#{true}" >
                                <!command
                                    setAttribute(key="click" value="$this{component}");
                                    setAttribute(key="viewVal" value="#{click.selected}");
                                    setAttribute(key="componentList" value="#{componentList}");
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
                "&nbsp;&nbsp;&nbsp;&nbsp;
                <sun:dropDown id="ComponentView" label="$resource{i18nc.monitoring.Component} : " items="$attribute{componentList}" selected="#{cVal}"  submitForm="#{true}" immediate="#{true}" >
                                <!beforeCreate
                                   populateComponentDropDown(AppName="#{appValue}", VSList="#{vsList}", ModuleName="#{viewVal}", MonitorURL="#{pageSession.monitorURL}", ComponentList=>$attribute{componentList});
                                 />
                                <!command
                                    setAttribute(key="click" value="$this{component}");
                                    setAttribute(key="cVal" value="#{click.selected}");
                                    setAttribute(key="viewVal" value="#{viewVal}");
                                    setAttribute(key="componentList" value="#{componentList}");
                                    setAttribute(key="monitorList" value="#{monitorList}");
                                    if ("$pageSession{encodedInstanceName}") {
                                        setAttribute(key="instanceName" value="$pageSession{encodedInstanceName}");
                                    }
                                    if ("$pageSession{encodedClusterName}") {
                                        setAttribute(key="clusterName" value="$pageSession{encodedClusterName}");
                                    }
                                    gf.navigate(page="#{pageSession.selfPage}");
                                />
                                <!afterCreate
                                    getClientId(component="$this{component}" clientId=>$page{dropDownId});
                                />
                            </sun:dropDown>
            </sun:property>
       </sun:propertySheetSection>
                </sun:propertySheet>   
                   
<!-- Table .... -->
<sun:table id="appsTable" itemsText="$resource{i18nc.monitoring.statistics}"   style="padding: 10pt" title="$resource{i18nc.monitoring}"  >
         <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{tableId});
        />
    <sun:tableRowGroup id="jerseyStats" rendered="#{hasStatsJersey}" data={"$attribute{jerseyStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.jAppStatistics} : #{viewVal}"
        groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
        <!beforeCreate
            getStats(endpoint="#{sessionScope.MONITOR_URL}/server/applications/#{viewVal}/jersey/resources/resource-0", statType="jersey", result=>$attribute{jerseyStatsList}  hasStats=>$attribute{hasStatsJersey});
        />

#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup>
    <sun:tableRowGroup id="mdbAppStats" rendered="#{hasStatsMDBApp}" data={"$attribute{MDBAppStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.MDB} : #{viewVal}/#{cVal} "
        groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
        <!beforeCreate
            getStats(endpoint="#{statUrl}", statType="#{statType}", type="MessageDrivenBean", result=>$attribute{MDBAppStatsList}  hasStats=>$attribute{hasStatsMDBApp});
         />

#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup>
    <sun:tableRowGroup id="entityBeanAppStats" rendered="#{hasStatsEntityBean}" data={"$attribute{EntityBeanStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.EntityBean} : #{viewVal}/#{cVal} "
        groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
        <!beforeCreate
            getStats(endpoint="#{statUrl}", statType="#{statType}", type="EntityBean", result=>$attribute{EntityBeanStatsList}  hasStats=>$attribute{hasStatsEntityBean});
         />

#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup>
    <sun:tableRowGroup id="singletonBeanAppStats" rendered="#{hasStatsSingletonBean}" data={"$attribute{SingletonBeanStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.SingletonBean} : #{viewVal}/#{cVal} "
        groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
        <!beforeCreate
            getStats(endpoint="#{statUrl}", statType="#{statType}", type="SingletonSessionBean", result=>$attribute{SingletonBeanStatsList}  hasStats=>$attribute{hasStatsSingletonBean});
         />

#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup>    
    <sun:tableRowGroup id="sfullStats" rendered="#{hasStatsSfull}" data={"$attribute{SfullStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.Sfull} : #{viewVal}/#{cVal} "
        groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
        <!beforeCreate
            getStats(endpoint="#{statUrl}", statType="#{statType}", type="StatefulSessionBean", result=>$attribute{SfullStatsList}  hasStats=>$attribute{hasStatsSfull});
         />

#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup>
    <sun:tableRowGroup id="slessStats" rendered="#{hasStatsSless}" data={"$attribute{SlessStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.Sless} : #{viewVal}/#{cVal} "
        groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
        <!beforeCreate
            getStats(endpoint="#{statUrl}", statType="#{statType}", type="StatelessSessionBean", result=>$attribute{SlessStatsList}  hasStats=>$attribute{hasStatsSless});
         />

#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup>
    <sun:tableRowGroup id="ejbTimerStats" rendered="#{hasStatsEJBTimer}" data={"$attribute{EJBTimerStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.EJBTimer} : #{viewVal}/#{cVal} "
        groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
        <!beforeCreate
            getStats(endpoint="#{statUrl}", statType="#{statType}", type="Timers", result=>$attribute{EJBTimerStatsList}  hasStats=>$attribute{hasStatsEJBTimer});
         />

#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup>
     <sun:tableRowGroup id="ejbMethodStats" rendered="#{hasStatsEjbMethod}" data={"$attribute{EjbMethodStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.EJBMethod} : #{viewVal}/#{cVal} "
        groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
        <!beforeCreate
            getStats(endpoint="#{statUrl}", statType="#{statType}", type="BeanMethods", result=>$attribute{EjbMethodStatsList}  hasStats=>$attribute{hasStatsEjbMethod});
         />

#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup>
        <sun:tableRowGroup id="ejbCacheStats" rendered="#{hasStatsEjbCache}" data={"$attribute{EjbCacheStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.EJBCache} : #{viewVal}/#{cVal} "
        groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
        <!beforeCreate
           getStats(endpoint="#{statUrl}", statType="#{statType}", type="BeanCache", result=>$attribute{EjbCacheStatsList}  hasStats=>$attribute{hasStatsEjbCache});
         />

#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup>
         <sun:tableRowGroup id="ejbPoolStats" rendered="#{hasStatsEjbPool}" data={"$attribute{EjbPoolStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.EJBPool} : #{viewVal}/#{cVal} "
        groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
        <!beforeCreate
            getStats(endpoint="#{statUrl}", statType="#{statType}", type="BeanPool", result=>$attribute{EjbPoolStatsList}  hasStats=>$attribute{hasStatsEjbPool});
         />

#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup>
         <sun:tableRowGroup id="appScopedResStats" rendered="#{hasStatsAppScopedRes}" data={"$attribute{AppScopedResStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.AppScopedResource} : #{viewVal}/#{cVal} "
        groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
        <!beforeCreate
            getStats(endpoint="#{statUrl}", statType="#{statType}", type="AppScopedResource", result=>$attribute{AppScopedResStatsList}  hasStats=>$attribute{hasStatsAppScopedRes});
         />

#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup>    
    <sun:tableRowGroup id="wmStats" rendered="#{hasStatsWMS}" data={"$attribute{wmStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.WorkManagementStats} : #{viewVal}"
        groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
        <!beforeCreate
            getStats(endpoint="#{pageSession.monitorURL}/connector-service/#{viewVal}/work-management", result=>$attribute{wmStatsList}  hasStats=>$attribute{hasStatsWMS});
        
         />  
                        
#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup>
    //Refer the issue #13647 for login stats.
    // <sun:tableRowGroup id="loginStats" rendered="#{hasStatsLogin}" data={"$attribute{loginStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.LoginStats} : #{viewVal}"
    //    groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
    //    <!beforeCreate
    //        getStatsbyTypeName(type="login-mon" name="webintegration/#{viewVal}", result=>$attribute{loginStatsList}  hasStats=>$attribute{hasStatsLogin});
    //     />

<!--#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup> -->

    <sun:tableRowGroup id="servletsInstance" rendered="#{hasStatsSI}" data={"$attribute{servletInstanceList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.servletInstanceStatistics} : #{viewVal}/#{cVal}"
        groupToggleButton="$boolean{true}" collapsed="#{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
        <!beforeCreate
            getStats(endpoint="#{webServletUrl}", statType="#{webServletType}", type="ServletInstance", result=>$attribute{servletInstanceList}  hasStats=>$attribute{hasStatsSI});

         />
#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup>
    <sun:tableRowGroup id="servlets" rendered="#{hasStatsSS}" data={"$attribute{servletList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.servletStatisticsWeb} : #{viewVal}"
        groupToggleButton="$boolean{true}" collapsed="#{collapseServlets}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
        <!beforeCreate
            setAttribute(key="collapseServlets" value="#{false}");
            if(#{hasStatsSI} || #{hasStatsJersey}) {
                setAttribute(key="collapseServlets" value="#{true}");
            }
            getStats(endpoint="#{webUrl}", statType="#{webType}", type="Web", result=>$attribute{servletList}  hasStats=>$attribute{hasStatsSS});
            filterWebStats(webStats="$attribute{servletList}", statType="Servlet", stats=>$attribute{servletList});
         />
#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup>

    <sun:tableRowGroup id="session" rendered="#{hasStatsSSP}" data={"$attribute{sessionList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.sessionStatisticsWeb} : #{viewVal}"
        groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
        <!beforeCreate
            getStats(endpoint="#{webUrl}", statType="#{webType}", type="Web", result=>$attribute{sessionList}  hasStats=>$attribute{hasStatsSSP});
            filterWebStats(webStats="$attribute{sessionList}", statType="Session", stats=>$attribute{sessionList});
         />

#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup>
    <sun:tableRowGroup id="requestStats" rendered="#{hasStatsWR}" data={"$attribute{requestStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.requestStatisticsWeb} : #{viewVal}"
        groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
        <!beforeCreate
            getStats(endpoint="#{webUrl}", statType="#{webType}", type="Web", result=>$attribute{requestStatsList}  hasStats=>$attribute{hasStatsWR});
            filterWebStats(webStats="$attribute{requestStatsList}", statType="Request", stats=>$attribute{requestStatsList});
         />

#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup>
    <sun:tableRowGroup id="jspStats" rendered="#{hasStatsJSP}" data={"$attribute{jspStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.jspStatisticsWeb} : #{viewVal} "
        groupToggleButton="$boolean{true}" collapsed="$boolean{true}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
        <!beforeCreate
            getStats(endpoint="#{webUrl}", statType="#{webType}", type="Web", result=>$attribute{jspStatsList}  hasStats=>$attribute{hasStatsJSP});
            filterWebStats(webStats="$attribute{jspStatsList}", statType="Jsp", stats=>$attribute{jspStatsList});
         />

#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup>
    
</sun:table>
</sun:title>