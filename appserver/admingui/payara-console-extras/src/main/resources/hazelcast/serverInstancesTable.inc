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
<sun:table id="serverInstancesTable" title="$resource{i18nhc.hazelcast.serverInstancesTableTitle}" style="padding: 10pt"
           rendered="#{pageSession.hazelcastConfiguration['enabled'] == true}">
<sun:tableRowGroup id="serverInstancesRowGroup" data={"$attribute{serverInstances}"} sourceVar="td" 
                   emptyDataMsg="$resource{i18nhc.hazelcast.serverInstancesTable.emptyDataMsg}">
    <sun:tableColumn headerText="$resource{i18nhc.hazelcast.serverInstancesTable.instanceName}" sort="instanceName" rowHeader="$boolean{true}" id="col1">
        <staticText id="instanceName" value="#{td.value.instanceName}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="$resource{i18nhc.hazelcast.serverInstancesTable.instanceGroup}" sort="instanceGroup" rowHeader="$boolean{false}" id="col2">
        <staticText id="instanceGroup" value="#{td.value.instanceGroup}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="$resource{i18nhc.hazelcast.serverInstancesTable.hostName}" sort="hostName" rowHeader="$boolean{true}" id="col3">
        <staticText id="hostName" value="#{td.value.hostName}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="$resource{i18nhc.hazelcast.serverInstancesTable.httpPorts}" sort="httpPorts" rowHeader="$boolean{false}" id="col4">
        <staticText id="httpPorts" value="#{td.value.httpPorts}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="$resource{i18nhc.hazelcast.serverInstancesTable.httpsPorts}" sort="httpsPorts" rowHeader="$boolean{false}" id="col5">
        <staticText id="httpsPorts" value="#{td.value.httpsPorts}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="$resource{i18nhc.hazelcast.serverInstancesTable.adminPort}" sort="adminPort" rowHeader="$boolean{false}" id="col6">
        <staticText id="adminPort" value="#{td.value.adminPort}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="Hazelcast Port" sort="$resource{i18nhc.hazelcast.serverInstancesTable.hazelcastPort}" rowHeader="$boolean{true}" id="col7">
        <staticText id="hazelcastPort" value="#{td.value.hazelcastPort}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="$resource{i18nhc.hazelcast.serverInstancesTable.liteMember}" sort="liteMember" rowHeader="$boolean{false}" id="col8">
        <staticText id="liteMember" value="#{td.value.liteMember}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="$resource{i18nhc.hazelcast.serverInstancesTable.applications}" sort="applications" rowHeader="$boolean{false}" id="col9">
        <staticText id="applications" value="#{td.value.applications}" />
    </sun:tableColumn>
</sun:tableRowGroup>
</sun:table>