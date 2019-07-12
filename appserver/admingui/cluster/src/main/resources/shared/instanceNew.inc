<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.

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

    Portions Copyright [2018] [Payara Foundation and/or its affiliates]

-->

<sun:form id="propertyForm">
#include "/cluster/shared/handlers.inc"
#include "/common/shared/alertMsg.inc"
    <sun:title id="propertyContentPage" title="#{pageSession.pageTitle}" helpText="#{pageSession.pageTitleHelp}">
        <!facet pageButtonsTop>
        <sun:panelGroup id="topButtons">
            <sun:button id="newButton" text="$resource{i18n.button.OK}"
                onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
            <!command
                if (!#{pageSession.autoNameSelected}) {
                    setAttribute(key="nameToCheck" value="#{pageSession.name}");
                    gfj.checkNameExist();
                }

                createMap(result="#{requestScope.attrs}")
                mapPut(map="#{requestScope.attrs}" key="name"  value="#{pageSession.name}" )
                mapPut(map="#{requestScope.attrs}" key="node"  value="#{pageSession.node}" )

                if (#{pageSession.autoNameSelected}) {
                    mapPut(map="#{requestScope.attrs}" key="autoname"  value="true" )
                }

                if (#{forStandalone}){
                    if ('#{configOptionGrp}=copy'){
                        if ("! (#{pageSession.config}=default-config)"){
                            setAttribute(key="nameToCheck" value="#{pageSession.name}-config");
                            gfj.checkNameExist();
                            createMap(result="#{requestScope.copyAttrs}")
                            mapPut(map="#{requestScope.copyAttrs}" key="id"  value={"$pageSession{config}" , "$pageSession{name}-config" } );
                            gf.restRequest(endpoint="#{sessionScope.REST_URL}/configs/copy-config"  attrs="#{requestScope.copyAttrs}" );
                            mapPut(map="#{requestScope.attrs}" key="config"  value="#{pageSession.name}-config" )
                        }
                    }
                    if ("!(#{configOptionGrp}=copy)"){
                        mapPut(map="#{requestScope.attrs}" key="config"  value="#{pageSession.config}" )
                    }
                }
                if (!#{forStandalone}){
                    mapPut(map="#{requestScope.attrs}" key="cluster"  value="#{pageSession.clusterName}" )
                }
                if (#{forDG}){
                    mapPut(map="#{requestScope.attrs}" key="deploymentgroup"  value="#{pageSession.dgName}" )
                }

                gf.restRequest(endpoint="#{sessionScope.REST_URL}/create-instance"  attrs="#{requestScope.attrs}" method="post"  result="#{requestScope.result}");

                gf.getChildrenNamesList(endpoint="#{sessionScope.REST_URL}/deployment-groups/deployment-group", result="#{pageSession.dgList}");
                listAdd(list="#{pageSession.dgList}" value="", index="0", result="#{pageSession.dgList}");
                setAttribute(key="one" value="$int{1}");
                setAttribute(key="deploymentGroupSize" value="#{pageSession.dgList.size()}");
                setPageSessionAttribute(key="isSelectedInstanceEmpty", value="#{empty pageSession.enabledDeploymentGroupsArr}");

                if (#{requestScope.deploymentGroupSize}>#{requestScope.one}){ 
                    if('!#{pageSession.isSelectedInstanceEmpty} = true'){                
                        py.updateDeploymentGroups(endpoint="#{sessionScope.REST_URL}/deployment-groups/add-instance-to-deployment-group" selected="#{pageSession.enabledDeploymentGroupsArr}" deploymentGroups="#{pageSession.allDeploymentGroupsArray}" instance="#{pageSession.name}");
                    }
                }
                gf.redirect(page="#{pageSession.listLink}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
             />
            </sun:button>
            <sun:button id="cancelButton" immediate="#{true}" text="$resource{i18n.button.Cancel}" primary="#{false}" >
                <!command
                    gf.redirect(page="#{pageSession.listLink}");
                />
            </sun:button>

        </sun:panelGroup>
        </facet>
       
    </sun:title>

    <sun:propertySheet id="propertySheet">

        <sun:propertySheetSection id="propertSectionTextField">

            <sun:property id="clusterNameProp" rendered="#{!forStandalone}"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.cluster.ClusterName}" >
                <sun:staticText id="clusterName" text="#{encodedClusterName}" />
            </sun:property>

            <sun:property id="NameTextProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.instanceName}" >
                <sun:textField id="NameText" text="#{pageSession.name}" styleClass="required" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.cluster.instanceName']}" required="#{true}" />
            </sun:property>

            <sun:property id="AutoNameProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.autoName}" >
                <sun:checkbox id="AutoName" selected="#{pageSession.autoNameSelected}" selectedValue="#{true}" />
            </sun:property>

            <sun:property id="avaliableDeploymentGroups" visible="#{pageSession.showDeploymentGroupSection}" labelAlign="left" noWrap="#{true}" overlapLabel="#{true}">
                <sun:addRemove id="instancesAddRemove"
                     selectAll="$boolean{true}"
                     items="#{pageSession.allDeploymentGroup}"
                     selected="#{pageSession.enabledDeploymentGroupsArr}"
                     availableItemsLabel="$resource{i18ncs.dg.AvailableDeploymentGroup}"
                     selectedItemsLabel="$resource{i18ncs.dg.SelectedDeploymentGroup}">
                </sun:addRemove>
            </sun:property>

            <sun:property id="node"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.clusterNew.Node}" helpText="$resource{i18ncs.clusterNew.NodeHelp}">
                <sun:dropDown id="node" labels="#{pageSession.nodesList}" selected="#{pageSession.node}" >
                </sun:dropDown>
            </sun:property>
            <sun:property id="configProp"  rendered="#{pageSession.forStandalone}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.clusterNew.Configuration}" helpText="$resource{i18ncs.clusterNew.ConfigurationHelp}">
                <sun:dropDown id="Config" labels="#{pageSession.configsList}" selected="#{pageSession.config}" >
                </sun:dropDown>
            </sun:property>

            <sun:property id="configOptionProp" rendered="#{pageSession.forStandalone}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}">
                <sun:radioButton id="optC"  name="configOptionGrp" label="$resource{i18ncs.clusterNew.CopyConfig}" selected="#{configOption}" selectedValue="copy" />
                "<br />
                <sun:radioButton id="optR"  name="configOptionGrp" label="$resource{i18ncs.clusterNew.ReferenceConfig}" selected="#{configOption}" selectedValue="ref" />
                "<br />
            </sun:property>
        </sun:propertySheetSection>
    </sun:propertySheet>

    <sun:hidden id="helpKey" value="#{pageSession.helpId}" />
</sun:form>
