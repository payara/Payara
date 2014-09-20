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

<!-- monitor/monitoringStatsPage.inc -->
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
                gf.getChildrenNamesList(endpoint="#{pageSession.monitorURL}/resources", result="#{pageSession.poolsList}");
                gf.getMonitoringPools(endpoint="#{sessionScope.REST_URL}/resources",
                    poolNames = "#{pageSession.poolsList}",
                    jdbcList="#{pageSession.jdbcList}",
                    firstJdbc="#{pageSession.firstJdbc}",
                    connectorList="#{pageSession.connectorList}",
                    firstConnector="#{pageSession.firstConnector}");
                addToMonitorList(oldList="#{pageSession.jdbcList}", newList="#{pageSession.connectorList}"  result="#{pageSession.resourceList}");
                populateResourceMonitorDropDown(ResourceList="#{resourceList}", MonitorList=>$attribute{monitorList} FirstItem=>$attribute{firstVal});
                if(!#{viewVal}) {
                    setAttribute(key="viewVal" value="#{firstVal}");
                }
                if(!#{appVal}) {
                    setAttribute(key="appVal" value="");
                }
            }
            if("#{pageSession.isMonitoringOff}") {
                setPageSessionAttribute(key="displayPropSection" value="#{false}");
            }
        }
        if("!#{requestScope.isInstanceRunning}"){
            setPageSessionAttribute(key="displayPropSection" value="#{false}");
            setAttribute(key="instanceRunningAlert" value="#{true}");
        }
        calculateHelpUrl(pluginId="#{pluginId}", helpKey: "$resource{help_web.monitorResources}", url="#{olhLink}");
    />
</event>    

 <sun:title id="propertyContentPage" title="$resource{i18nc.resourcesMonitor}"
     helpText="$resource{i18nc.monitoring.webContainer.PageHelp}">
     <!facet pageButtonsTop>
         <sun:panelGroup id="topButtons">
             <sun:button id="refreshButton" 
                         text="$resource{i18n.button.Refresh}">
                  <!command
                      setAttribute(key="viewVal" value="#{viewVal}");
                      setAttribute(key="appVal" value="#{appVal}");
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
                 <sun:dropDown id="View" label="$resource{i18nc.monitoring.resource} : " selected="#{viewVal}" items="$attribute{monitorList}" submitForm="#{true}" immediate="#{true}" >
                     <!command
                         setAttribute(key="click" value="$this{component}");
                         setAttribute(key="viewVal" value="#{click.selected}");
                         if ("$pageSession{encodedInstanceName}") {
                            setAttribute(key="instanceName" value="$pageSession{encodedInstanceName}");
                        }
                        if ("$pageSession{encodedClusterName}") {
                            setAttribute(key="clusterName" value="$pageSession{encodedClusterName}");
                        }
                         gf.navigate(page="#{pageSession.selfPage}");
                     />
                     <!afterCreate
                         getClientId(component="$this{component}" clientId=>$page{poolId});
                     />
                 </sun:dropDown>
                 "&nbsp;&nbsp;&nbsp;&nbsp;
                 //To provide the support for application based pool monitoring.
                 <sun:dropDown id="appView" visible="#{hasResApps}" label="$resource{i18nc.monitoring.application} : " selected="#{appVal}" items="$attribute{resApps}" submitForm="#{true}" immediate="#{true}" >
                     <!beforeCreate
                         setAttribute(key="resAppsList" value={});
                         gf.getChildrenNamesList(endpoint="#{sessionScope.REST_URL}/applications/application", result="#{appsList}");
                         gf.getChildrenNamesList(endpoint="#{pageSession.monitorURL}/resources/#{viewVal}", result="#{poolAppsList}");
                         foreach (var="poolApp" list="#{poolAppsList}") {
                            gf.containedIn(list="#{appsList}" testStr="#{poolApp}" contain=>$attribute{hasApp});
                            if(#{hasApp}) {
                                listAdd(list="#{resAppsList}" value="#{poolApp}" result=>$attribute{resAppsList});
                            }
                         }
                         setAttribute(key="resAppsSize" value="#{resAppsList.size()}");
                         setAttribute(key="zero" value="$int{0}");
                         if(#{resAppsSize} > #{zero}) {
                            setAttribute(key="hasResApps" value="$boolean{true}");                                                        
                         }
                         if(#{resAppsSize} = #{zero}) {
                            setAttribute(key="hasResApps" value="$boolean{false}");
                         }
                         addEmptyFirstElement(in="#{resAppsList}" out="#{resAppsList}");
                         gf.convertListToOptionArray(list="#{resAppsList}" optionArray=>$attribute{resApps});
                                                  
                     />
                     <!command
                         setAttribute(key="click" value="$this{component}");
                         setAttribute(key="appVal" value="#{click.selected}");
                         getUIComponent(clientId="$pageSession{poolId}", component=>$attribute{poolComp});
                         setAttribute(key="viewVal" value="#{poolComp.selected}");
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
     <sun:table id="resourcesTable" itemsText="$resource{i18nc.monitoring.statistics}"   style="padding: 10pt" title="$resource{i18nc.monitoring}"  >
         <!afterCreate
             getClientId(component="$this{component}" clientId=>$page{tableId});
         />
   
         <sun:tableRowGroup id="jdbcStats" rendered="#{hasStatsJdbc}" data={"$attribute{jdbcStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.JDBC} : #{jdbcHeader}"
             groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
             <!beforeCreate
                 isPool(poolName="#{viewVal}", endpoint="#{sessionScope.REST_URL}/resources/jdbc-connection-pool", result=>$attribute{isJdbcPool});
                 if (#{isJdbcPool}) {
                     if(#{appVal}) {
                        setAttribute(key="jdbcHeader" value="#{viewVal}/#{appVal}");
                     }
                     if(!#{appVal}) {
                        setAttribute(key="jdbcHeader" value="#{viewVal}");
                     }
                     getStats(endpoint="#{pageSession.monitorURL}/resources/#{viewVal}/#{appVal}", result=>$attribute{jdbcStatsList}  hasStats=>$attribute{hasStatsJdbc});
                 }
                 if (!#{isJdbcPool}) {
                     setAttribute(key="hasStatsJdbc" value="#{false}");
                 }
             />
#include "/common/monitor/monitoringTableRows.inc"
             "<br/>
         </sun:tableRowGroup>
         <sun:tableRowGroup id="connectionPoolStats" rendered="#{hasStatsConnectionPool}" data={"$attribute{connectionPoolStatsList}"}  sourceVar="td" headerText="$resource{i18nc.monitoring.ConnectorConnectionPool} : #{connectorHeader}"
             groupToggleButton="$boolean{true}" collapsed="$boolean{false}" aboveColumnHeader="$boolean{true}" emptyDataMsg="$resource{i18nc.monitoring.webContainer.NoStats}">
             <!beforeCreate
                 isPool(poolName="#{viewVal}", endpoint="#{sessionScope.REST_URL}/resources/connector-connection-pool", result=>$attribute{isConnectorPool});
                 if(#{appVal}) {
                    setAttribute(key="connectorHeader" value="#{viewVal}/#{appVal}");
                 }
                 if(!#{appVal}) {
                    setAttribute(key="connectorHeader" value="#{viewVal}");
                 }
                 if (#{isConnectorPool}) {
                     getStats(endpoint="#{pageSession.monitorURL}/resources/#{viewVal}/#{appVal}", result=>$attribute{connectionPoolStatsList}  hasStats=>$attribute{hasStatsConnectionPool});
                 }
                 if (!#{isConnectorPool}) {
                    setAttribute(key="hasStatsConnectionPool" value="#{false}");
                 }
             />
#include "/common/monitor/monitoringTableRows.inc"
        "<br/>
    </sun:tableRowGroup>
</sun:table>
</sun:title>