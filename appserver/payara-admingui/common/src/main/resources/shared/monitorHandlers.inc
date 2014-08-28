<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.

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
     This handler gets the Monitoring Table.
     Output: #{pageSession.listOfRows}
-->
<handler id="gfr.getMonitoringTable">
    setPageSessionAttribute(key="listOfRows" value={});
    gf.listInstances(optionKeys={"standaloneonly"} optionValues={"true"}, instances="#{pageSession.standaloneList}");
    gf.getChildrenNamesList(endpoint="#{sessionScope.REST_URL}/servers/server" result="#{pageSession.instancesList}");
    setAttribute(key="commonLink" value="#{request.contextPath}/common/monitor");
    foreach (var="instance", list="#{pageSession.instancesList}") {
        setAttribute(key="instanceArgs" value="?instanceName=#{instance}");
        gf.containedIn(list="#{pageSession.standaloneList}" testStr="#{instance}" contain=>$attribute{isStandalone});
        setAttribute(key="instanceLink" value="");
        setAttribute(key="clusterLink" value="");
        setAttribute(key="isCluster" value="$boolean{false}");
        if(#{isStandalone}) {
            setAttribute(key="clusterName" value="");            
            setAttribute(key="instanceLink" value="#{request.contextPath}/cluster/standalone/standaloneInstanceGeneral.jsf?instanceName=#{instance}");
            setAttribute(key="applicationsMonitorLink" value="standaloneInstanceMonitoringAppStatsPage.jsf#{instanceArgs}");
            setAttribute(key="serverMonitorLink" value="standaloneInstanceMonitoringServerPage.jsf#{instanceArgs}");
            setAttribute(key="resourcesMonitorLink" value="standaloneInstanceMonitoringResourcePage.jsf#{instanceArgs}");
        }
        if(!#{isStandalone}) {
            if("#{instance}=server") {
                setAttribute(key="clusterName" value="");
                setAttribute(key="instanceLink" value="#{request.contextPath}/common/appServer/serverInstGeneralPe.jsf#{instanceArgs}");
                setAttribute(key="applicationsMonitorLink" value="serverInstMonitoringAppsPage.jsf#{instanceArgs}");
                setAttribute(key="serverMonitorLink" value="serverInstMonitoringServerPage.jsf#{instanceArgs}");
                setAttribute(key="resourcesMonitorLink" value="serverInstMonitoringResourcesPage.jsf#{instanceArgs}");
            }
            if("!(#{instance}=server)") {
                setAttribute(key="isCluster" value="$boolean{true}");
                gf.getClusterNameForInstance(instanceName="#{instance}"  clusterName=>$attribute{clusterName});
                setAttribute(key="instanceLink" value="#{request.contextPath}/cluster/cluster/clusterInstanceEdit.jsf#{instanceArgs}&clusterName=#{clusterName}");
                setAttribute(key="clusterLink" value="#{request.contextPath}/cluster/cluster/clusterGeneral.jsf?clusterName=#{clusterName}");
                setAttribute(key="applicationsMonitorLink" value="clusterInstanceMonitoringAppStatsPage.jsf#{instanceArgs}&clusterName=#{clusterName}");
                setAttribute(key="serverMonitorLink" value="clusterInstanceMonitoringServerPage.jsf#{instanceArgs}&clusterName=#{clusterName}");
                setAttribute(key="resourcesMonitorLink" value="clusterInstanceMonitoringResourcePage.jsf#{instanceArgs}&clusterName=#{clusterName}");
            }
        }
        gf.getConfigName(target="#{instance}" configName=>$attribute{configName});
        setAttribute(key="actionLink" value="#{request.contextPath}/common/monitor/monitoringPage.jsf?configName=#{configName}");
        gf.createAttributeMap(keys={"instanceName", "instanceLink", "clusterName",
                                    "clusterLink", "actionLink", "appMonitor",
                                    "serverMonitor", "resMonitor", "isCluster"}
                              values={"${instance}", "$attribute{instanceLink}",
                                      "$attribute{clusterName}", "$attribute{clusterLink}",
                                      "$attribute{actionLink}",
                                      "$attribute{commonLink}/$attribute{applicationsMonitorLink}",
                                      "$attribute{commonLink}/$attribute{serverMonitorLink}",
                                      "$attribute{commonLink}/$attribute{resourcesMonitorLink}",
                                      "$attribute{isCluster}",}
                                    map="#{requestScope.oneRowMap}");
        listAdd(list="$pageSession{listOfRows}" value="#{requestScope.oneRowMap}");
    }
</handler>
