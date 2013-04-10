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

<sun:table id="deployTable" title="$resource{i18n.deployTable.applications}" sortPanelToggleButton="#{false}"
       deselectMultipleButton="$boolean{true}"
        deselectMultipleButtonOnClick="setTimeout('admingui.table.changeThreeTableButtons(\\\\\'#{pageSession.topActionGroup}\\\\\', \\\\\'#{pageSession.tableId}\\\\\');', 0)"
        selectMultipleButton="$boolean{true}"
        selectMultipleButtonOnClick="setTimeout('admingui.table.changeThreeTableButtons(\\\\\'#{pageSession.topActionGroup}\\\\\', \\\\\'#{pageSession.tableId}\\\\\');', 0)" >
         <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{tableId});
        />


    <!facet actionsTop>
         <sun:panelGroup id="topActionsGroup1">
            <!afterCreate
                getClientId(component="$this{component}" clientId=>$page{topActionGroup});
            />
            <sun:button id="deployButton" text="$resource{i18n.button.Deploy}" primary="#{true}">
                <!command
		    gf.redirect(page="#{pageSession.nextPageLink}") />
            </sun:button>
            <sun:button id="button1" text="$resource{i18n.button.Undeploy}" disabled="#{true}" primary="#{false}"
                    onClick="if ( getConfirm(this,'$pageSession{confirmDeleteMsg}') )
                        { return submitAndDisable(this, '$resource{i18n.button.Processing}');}
                        else {return false;} " >
                <!command
                    getUIComponent(clientId="$pageSession{tableRowGroupId}", component=>$attribute{tableRowGroup});
                    getSelectedSingleMapRows(TableRowGroup="$attribute{tableRowGroup}" selectedRows=>$attribute{selectedRows});
                    gf.undeploy(selectedRows="${selectedRows}" );
                    gf.redirect(page="#{listPageLink}?#{extraArgs}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
                />
             </sun:button>s

            <sun:button id="button2" text="$resource{i18n.button.Remove}" disabled="#{true}" primary="#{false}"
                    onClick="if (!getConfirm(this,'$resource{i18n.msg.JS.confirmRemoveTarget}'))
                               return false;
                    return submitAndDisable(this, '$resource{i18n.button.Processing}');"  >
                <!command
                    getUIComponent(clientId="$pageSession{tableRowGroupId}", component=>$attribute{tableRowGroup});
                    getSelectedSingleMapRows(TableRowGroup="$attribute{tableRowGroup}" selectedRows=>$attribute{selectedRows});
                    foreach (var="row" list="#{requestScope.selectedRows}") {
                       urlencode(value="#{row['name']}" result="#{requestScope.encodedName}");
                       createMap(result="#{requestScope.attrMap}");
                       mapPut(map="#{requestScope.attrMap}" key="target" value="#{row['targetName']}");
                       gf.restRequest(
                        endpoint="#{sessionScope.REST_URL}#{pageSession.appRefEndpoint}#{requestScope.encodedName}"
                        attrs="#{requestScope.attrMap}"
                        method="DELETE");
                    }
                    gf.redirect(page="#{listPageLink}?#{extraArgs}");
                />
             </sun:button>

            <sun:dropDown id="dropdown1" submitForm="#{true}"  disabled="#{false}"
                labels={ "$resource{i18n.common.moreActionDropDown}"
                        "$resource{i18n.button.Enable}"  "$resource{i18n.button.Disable}"
                        "$resource{i18n.targetListing.table.lbEnable}"  "$resource{i18n.targetListing.table.lbDisable}"
                     }
                values={"" "enable" "disable" "lb-enable" "lb-disable" } >
                <!command
                    getUIComponent(clientId="#{tableRowGroupId}", component=>$attribute{tableRowGroup});
                    getSelectedSingleMapRows(TableRowGroup="$attribute{tableRowGroup}" selectedRows=>$attribute{selectedRows});
                    setAttribute(key="click" value="$this{component}");
                    setAttribute(key="actionValue" value="#{click.selected}")

                    if( ${actionValue}=enable){
                        gf.changeTargetStatus(selectedRows="${selectedRows}"  Enabled="#{true}" forLB="$boolean{false}")
                    }
                    if( ${actionValue}=disable){
                        gf.changeTargetStatus(selectedRows="${selectedRows}"  Enabled="#{false}" forLB="$boolean{false}")
                    }
                    if( ${actionValue}=lb-enable){
                        gf.changeTargetStatus(selectedRows="${selectedRows}" Enabled="#{true}" forLB="$boolean{true}")
                    }
                    if( ${actionValue}=lb-disable){
                        gf.changeTargetStatus(selectedRows="${selectedRows}"  Enabled="#{false}" forLB="$boolean{true}")
                    }
                    gf.redirect(page="#{listPageLink}?#{extraArgs}");
                />
            </sun:dropDown>
         </sun:panelGroup>
     </facet>

   <sun:tableRowGroup id="rowGroup1" selected="#{td.value.selected}" data={"$attribute{listOfRows}"} sourceVar="td">
    <!afterCreate
         getClientId(component="$this{component}" clientId=>$page{tableRowGroupId});
    />

   <sun:tableColumn headerText="$resource{i18n.common.SelectHeader}" selectId="select" rowHeader="$boolean{false}" id="col0">
            <sun:checkbox id="select" 
                selected="#{td.value.selected}" 
                selectedValue="$boolean{true}" 
                onClick="admingui.table.changeThreeTableButtons('#{pageSession.topActionGroup}', '#{pageSession.tableId}'); admingui.table.initAllRows('#{pageSession.tableId}');"
            />
    </sun:tableColumn>    
        
    <sun:tableColumn headerText="$resource{i18n.deployTable.colName}" sort="name" rowHeader="$boolean{true}" id="col1">
        <sun:hyperlink id="link" text="#{td.value.name}" url="#{editLink}?appName=#{td.value.name}&listPageLink=#{pageSession.listPageLink}" />
    </sun:tableColumn>

    <sun:tableColumn  headerText="$resource{i18n.deployTable.colEnabled}" rowHeader="$boolean{true}" id="col22">
        <sun:image id="enablecol" value="#{td.value.enabled}" />
    </sun:tableColumn>

    <sun:tableColumn  headerText="$resource{i18n.targetListing.LBEnabled}"  rowHeader="$boolean{true}" id="col33">
        <sun:image id="lbEnablecol" value="#{td.value.lbEnabled}" />
    </sun:tableColumn>

    <sun:tableColumn headerText="$resource{i18n.deployTable.ColEngines}"  sort="sniffers"  rowHeader="$boolean{true}" id="colsnif">
        <staticText id="sniffer" value="#{td.value.sniffers}" />
    </sun:tableColumn>
  </sun:tableRowGroup>
  
</sun:table>
