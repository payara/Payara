<!--

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 -->
<sun:table id="microInstancesTable" title="Payara Micro Instances" style="padding: 10pt" 
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
            <sun:textField id="asadminCommandField" label="Command:" columns="$int{40}" maxLength="250" 
                           text="#{pageSession.command}"/>
            <sun:button id="button1" text="Send Asadmin Command" disabled="#{true}" primary="#{false}"
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
                       emptyDataMsg="No Micro instances in the cluster">
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{tableRowGroupId});
        />
        <sun:tableColumn headerText="Select" selectId="select" rowHeader="$boolean{false}" id="col0">
            <sun:checkbox id="select" selectedValue="$boolean{true}"
                          selected="#{td.value.selected}"
                          onClick="setTimeout('admingui.table.changeOneTableButton(\\\\\'#{pageSession.topActionGroup}\\\\\', \\\\\'#{pageSession.tableId}\\\\\'); admingui.table.initAllRows(\\\\\'#{pageSession.tableId}\\\\\');', 0);"/>
        </sun:tableColumn>
        <sun:tableColumn headerText="Instance Name" sort="instanceName" rowHeader="$boolean{true}" id="col1">
            <staticText id="instanceName" value="#{td.value.instanceName}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="Host Name" sort="hostName" rowHeader="$boolean{true}" id="col2">
            <staticText id="hostName" value="#{td.value.hostName}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="HTTP Ports" sort="httpPorts" rowHeader="$boolean{false}" id="col3">
            <staticText id="httpPorts" value="#{td.value.httpPorts}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="HTTPS Ports" sort="httpsPorts" rowHeader="$boolean{false}" id="col4">
            <staticText id="httpsPorts" value="#{td.value.httpsPorts}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="Admin Port" sort="adminPort" rowHeader="$boolean{false}" id="col5">
            <staticText id="adminPort" value="#{td.value.adminPort}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="Hazelcast Port" sort="hazelcastPort" rowHeader="$boolean{true}" id="col6">
            <staticText id="hazelcastPort" value="#{td.value.hazelcastPort}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="Lite Member" sort="liteMember" rowHeader="$boolean{false}" id="col7">
            <staticText id="liteMember" value="#{td.value.liteMember}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="Applications" sort="applications" rowHeader="$boolean{false}" id="col8">
            <staticText id="applications" value="#{td.value.applications}" />
        </sun:tableColumn>
    </sun:tableRowGroup>
</sun:table>