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
<sun:table id="serverInstancesTable" title="Payara Server Instances" style="padding: 10pt"
           rendered="#{pageSession.hazelcastConfiguration['enabled'] == true}">
<sun:tableRowGroup id="serverInstancesRowGroup" data={"$attribute{serverInstances}"} sourceVar="td" 
                   emptyDataMsg="No Payara Server instances in cluster">
    <sun:tableColumn headerText="Instance Name" sort="instanceName" rowHeader="$boolean{true}" id="col1">
        <staticText id="instanceName" value="#{td.value.instanceName}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="Host Name" sort="hostName" rowHeader="$boolean{true}" id="col3">
        <staticText id="hostName" value="#{td.value.hostName}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="HTTP Ports" sort="httpPorts" rowHeader="$boolean{false}" id="col4">
        <staticText id="httpPorts" value="#{td.value.httpPorts}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="HTTPS Ports" sort="httpsPorts" rowHeader="$boolean{false}" id="col5">
        <staticText id="httpsPorts" value="#{td.value.httpsPorts}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="Admin Port" sort="adminPort" rowHeader="$boolean{false}" id="col6">
        <staticText id="adminPort" value="#{td.value.adminPort}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="Hazelcast Port" sort="hazelcastPort" rowHeader="$boolean{true}" id="col7">
        <staticText id="hazelcastPort" value="#{td.value.hazelcastPort}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="Lite Member" sort="liteMember" rowHeader="$boolean{false}" id="col8">
        <staticText id="liteMember" value="#{td.value.liteMember}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="Applications" sort="applications" rowHeader="$boolean{false}" id="col9">
        <staticText id="applications" value="#{td.value.applications}" />
    </sun:tableColumn>
</sun:tableRowGroup>
</sun:table>