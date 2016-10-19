<sun:table id="allInstancesTable" title="All Cluster Members" style="padding: 10pt"
           rendered="#{pageSession.hazelcastConfiguration['enabled'] == true}">
<sun:tableRowGroup id="allInstancesRowGroup" data={"$attribute{allInstances}"} sourceVar="td" 
                   emptyDataMsg="No instances in cluster">
    <sun:tableColumn headerText="Instance Name" sort="instanceName" rowHeader="$boolean{true}" id="col1">
        <staticText id="instanceName" value="#{td.value.instanceName}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="Instance Type" sort="instanceType" rowHeader="$boolean{true}" id="col2">
        <staticText id="instanceType" value="#{td.value.instanceType}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="Host Name" sort="hostName" rowHeader="$boolean{false}" id="col3">
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
    <sun:tableColumn headerText="Hazelcast Port" sort="hazelcastPort" rowHeader="$boolean{false}" id="col7">
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