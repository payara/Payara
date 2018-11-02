<!--

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

  Copyright (c) [2016-2018] Payara Foundation and/or its affiliates. 
  All rights reserved.

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
  The Payara Foundation designates this particular file as subject to the 
  "Classpath" exception as provided by the Payara Foundation in the GPL 
  Version 2 section of the License file that accompanied this code.

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
<sun:table id="microInstancesTable" title="$resource{i18nhc.hazelcast.microInstancesTableTitle}" style="padding: 10pt"
           selectMultipleButton="$boolean{true}" 
           selectMultipleButtonOnClick="setTimeout('admingui.table.changeOneTableButton(\\\\\'#{pageSession.topActionGroup}\\\\\', \\\\\'#{pageSession.tableId}\\\\\'); admingui.table.initAllRows(\\\\\'#{pageSession.tableId}\\\\\');', 0);"
           deselectMultipleButton="$boolean{true}"
           deselectMultipleButtonOnClick="setTimeout('admingui.table.changeOneTableButton(\\\\\'#{pageSession.topActionGroup}\\\\\', \\\\\'#{pageSession.tableId}\\\\\'); admingui.table.initAllRows(\\\\\'#{pageSession.tableId}\\\\\');', 0);"
           rendered="#{pageSession.hazelcastConfiguration['enabled'] == true}">
    <!afterCreate
        getClientId(component="$this{component}" clientId=>$page{tableId});
    />
    <!facet actionsTop>
        <sun:panelGroup id="topActionsGroup1">
            <!afterCreate
                getClientId(component="$this{component}" clientId=>$page{topActionGroup});
            />
            <sun:textField id="asadminCommandField" label="$resource{i18nhc.hazelcast.microInstancesTable.sendAdminCommand.label}" columns="$int{40}" maxLength="250"
                           text="#{pageSession.command}"/>
            <sun:button id="button1" text="$resource{i18nhc.hazelcast.microInstancesTable.sendAdminCommand.button}" disabled="#{true}" primary="#{false}"
                        onClick="if (getConfirm(this,'Send Asadmin command?')) {
                            return submitAndDisable(this, '$resource{i18n.button.Processing}');
                        } else {
                            return false;
                        }" 
                        onKeyPress="javascript: return true;">
                <!command
                    getUIComponent(clientId="$pageSession{tableRowGroupId}", component=>$attribute{tableRowGroup});
                    
                    getAllSingleMapRows(TableRowGroup="${tableRowGroup}" Rows="#{requestScope.rows}");
                    py.sendAsadminCommandToSelectedInstances(parentEndpoint="#{pageSession.parentUrl}", 
                            rows="#{requestScope.rows}", command="#{pageSession.command}", 
                            response="#{pageSession.commandResponse}");
                            
                    if("#{pageSession.commandResponse.data['exit_code']} = FAILURE") {
                        prepareAlertMsg(type="error", detail="#{pageSession.commandResponse.data.message}", 
                                summary="An error has occurred");
                        gf.redirect(page="#{request.contextPath}/hazelcast/hazelcastMembersDomain.jsf?&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
                    }
                            
                    py.prepareSuccessfulCommandMsg();
                    gf.redirect(page="#{request.contextPath}/hazelcast/hazelcastMembersDomain.jsf?&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
                />
            </sun:button>
        </sun:panelGroup>
    </facet>
    <sun:tableRowGroup id="microInstancesRowGroup" data={"$attribute{microInstances}"} sourceVar="td" 
                       emptyDataMsg="$resource{i18nhc.hazelcast.microInstancesTable.emptyDataMsg}">
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{tableRowGroupId});
        />
        <sun:tableColumn headerText="$resource{i18nhc.hazelcast.microInstancesTable.select}" selectId="select" rowHeader="$boolean{false}" id="col0">
            <sun:checkbox id="select" selectedValue="$boolean{true}"
                          selected="#{td.value.selected}"
                          onClick="setTimeout('admingui.table.changeOneTableButton(\\\\\'#{pageSession.topActionGroup}\\\\\', \\\\\'#{pageSession.tableId}\\\\\'); admingui.table.initAllRows(\\\\\'#{pageSession.tableId}\\\\\');', 0);"/>
        </sun:tableColumn>
        <sun:tableColumn headerText="$resource{i18nhc.hazelcast.microInstancesTable.instanceName}" sort="instanceName" rowHeader="$boolean{true}" id="col1">
            <staticText id="instanceName" value="#{td.value.instanceName}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="$resource{i18nhc.hazelcast.microInstancesTable.instanceGroup}" sort="instanceGroup" rowHeader="$boolean{false}" id="col2">
            <staticText id="instanceGroup" value="#{td.value.instanceGroup}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="$resource{i18nhc.hazelcast.microInstancesTable.hostName}" sort="hostName" rowHeader="$boolean{true}" id="col3">
            <staticText id="hostName" value="#{td.value.hostName}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="$resource{i18nhc.hazelcast.microInstancesTable.httpPorts}" sort="httpPorts" rowHeader="$boolean{false}" id="col4">
            <staticText id="httpPorts" value="#{td.value.httpPorts}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="$resource{i18nhc.hazelcast.microInstancesTable.httpsPorts}" sort="httpsPorts" rowHeader="$boolean{false}" id="col5">
            <staticText id="httpsPorts" value="#{td.value.httpsPorts}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="$resource{i18nhc.hazelcast.microInstancesTable.adminPort}" sort="adminPort" rowHeader="$boolean{false}" id="col6">
            <staticText id="adminPort" value="#{td.value.adminPort}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="$resource{i18nhc.hazelcast.microInstancesTable.hazelcastPort}" sort="hazelcastPort" rowHeader="$boolean{true}" id="col7">
            <staticText id="hazelcastPort" value="#{td.value.hazelcastPort}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="$resource{i18nhc.hazelcast.microInstancesTable.liteMember}" sort="liteMember" rowHeader="$boolean{false}" id="col8">
            <staticText id="liteMember" value="#{td.value.liteMember}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="$resource{i18nhc.hazelcast.microInstancesTable.applications}" sort="applications" rowHeader="$boolean{false}" id="col9">
            <staticText id="applications" value="#{td.value.applications}" />
        </sun:tableColumn>
    </sun:tableRowGroup>
</sun:table>