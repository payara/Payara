<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.

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

<!-- shared/instanceActionButtons.inc -->

<!facet actionsTop>
    <sun:panelGroup id="topActionsGroup1">
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{topActionGroup});
        />

        <sun:button id="newButton" text="$resource{i18n.button.New}" >
            <!command redirect(page="#{newPageLink}"); />
        </sun:button>

        <sun:button id="button1" text="$resource{i18n.button.Delete}" disabled="#{true}" primary="#{false}"
            onClick="if (getConfirm(this, '$resource{i18n.msg.JS.confirmDeleteInstance}') )
                { return submitAndDisable(this, '$resource{i18n.button.Processing}');} 
                else {return false;} " >
            <!command 
            getUIComponent(clientId="$pageSession{instancesTableRowGroupId}", component=>$attribute{tableRowGroup});      
            getSelectedSingleMapRows(TableRowGroup="${tableRowGroup}" selectedRows=>$attribute{selectedRows});
            if ($pageSession{clusterList}){
                deleteSelectedInstances(selectedRows="${selectedRows}");
                redirect(page="clusterInstances.jsf?clusterName=#{clusterName}");
            }
            if (!$pageSession{clusterList}){
                deleteStandaloneInstance(selectedRows="${selectedRows}");
                redirect(page="standaloneInstances.jsf");
            }
            />            
        </sun:button>
       
        <sun:button id="button2" text="$resource{i18n.button.Start}" disabled="#{true}" primary="#{false}" 
            onClick="if (getConfirm(this, '$resource{i18n.msg.JS.confirmStartInstances}') )
                { return submitAndDisable(this, '$resource{i18n.button.Processing}');} 
                else {return false;} " >
            
            <!command 
            getUIComponent(clientId="$pageSession{instancesTableRowGroupId}", component=>$attribute{tableRowGroup});      
            getSelectedSingleMapRows(TableRowGroup="${tableRowGroup}" selectedRows=>$attribute{selectedRows});
            if ($pageSession{clusterList}){
                clusterInstanceAction(clusterName="#{clusterName}", start="$boolean{true}", selectedRows="${selectedRows}");
                setAttribute(key="selectedRows" value="${selectedRows}");
                redirect(page="clusterInstances.jsf?clusterName=#{clusterName}");
            }
            if (!$pageSession{clusterList}){
                startSelectedInstance(selectedRows="${selectedRows}");
                setAttribute(key="selectedRows" value="${selectedRows}");
                redirect(page="standaloneInstances.jsf");
            }
            />            
        </sun:button>
        <sun:button id="button3" text="$resource{i18n.button.Stop}" disabled="#{true}" primary="#{false}" 
            onClick="if (getConfirm(this, '$resource{i18n.msg.JS.confirmStopInstances}') )
                { return submitAndDisable(this, '$resource{i18n.button.Processing}');} 
                else {return false;} " >
            <!command 
            getUIComponent(clientId="$pageSession{instancesTableRowGroupId}", component=>$attribute{tableRowGroup});      
            getSelectedSingleMapRows(TableRowGroup="${tableRowGroup}" selectedRows=>$attribute{selectedRows});
            
            if ($pageSession{clusterList}){
                clusterInstanceAction(clusterName="#{clusterName}", start="$boolean{false}", selectedRows="${selectedRows}");
                setAttribute(key="selectedRows" value="${selectedRows}");
                redirect(page="clusterInstances.jsf?clusterName=#{clusterName}");
            }
            if (!$pageSession{clusterList}){
                stopSelectedInstance(selectedRows="${selectedRows}");
                setAttribute(key="selectedRows" value="${selectedRows}");
                redirect(page="standaloneInstances.jsf");
            }
            />            
        </sun:button>

        <!--
            <sun:dropDown rendered="false" id="dropdown1" submitForm="#{true}"  disabled="#{true}"
                labels="$attribute{actionLabels}" values="$attribute{actionValues}" >
                <!beforeCreate
                    setAttribute(key="actionLabels" value={ "$resource{i18n.common.LBActionDropDown}"
                        "$resource{i18n.cluster.startLB}"  "$resource{i18n.cluster.quiesce}" });
                    setAttribute(key="actionValues" value={"" "lb-enable" "lb-disable" });
                />
                <!command
                    getUIComponent(clientId="$pageSession{instancesTableRowGroupId}", component=>$attribute{tableRowGroup});   
                    getSelectedSingleMapRows(TableRowGroup="$attribute{tableRowGroup}" selectedRows=>$attribute{selectedRows});
                    setAttribute(key="click" value="$this{component}");
                    setAttribute(key="actionValue" value="#{click.selected}")
                    if( ${actionValue}=lb-enable){
                        changeServerRefLB(clusterName="#{clusterName}" selectedRows="${selectedRows}" enabled="$boolean{true}" );
                    }
                    if( ${actionValue}=lb-disable){
                        changeServerRefLB(clusterName="#{clusterName}" selectedRows="${selectedRows}" enabled="$boolean{false}");
                    }
                    setAttribute(key="selectedRows" value="${selectedRows}");
                    redirect(page="clusterInstances.jsf?clusterName=#{clusterName}");
                />
            </sun:dropDown>
            -->

    </sun:panelGroup>
</facet>
