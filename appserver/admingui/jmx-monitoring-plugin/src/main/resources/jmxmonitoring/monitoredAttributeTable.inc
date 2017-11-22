<!-- jmxmonitoring/monitoredAttributeTable.inc -->

<!-- Table .... -->
<sun:table id="basicTable"  style="padding: 10pt" title="$resource{i18n.common.AdditionalProperties}" 
           deselectMultipleButton="$boolean{true}"
           deselectMultipleButtonOnClick="setTimeout('admingui.table.changeOneTableButton(\\\\\'#{pageSession.topActionGroup}\\\\\', \\\\\'#{pageSession.tableId}\\\\\');', 0)"
           selectMultipleButton="$boolean{true}"
           selectMultipleButtonOnClick="setTimeout('admingui.table.changeOneTableButton(\\\\\'#{pageSession.topActionGroup}\\\\\', \\\\\'#{pageSession.tableId}\\\\\');', 0)" >
    <!afterCreate
        getClientId(component="$this{component}" clientId=>$page{tableId});
    />
    <!-- Actions (Top) -->
    <!facet actionsTop>
    <sun:panelGroup id="topActionsGroup1">
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{topActionGroup});
        />
        <sun:button id="addSharedTableButton" disabled="#{false}" text="$resource{i18n.button.AddProperty}" >   
            <!command
                getUIComponent(clientId="$pageSession{propertyTableRowGroupId}", component=>$attribute{tableRowGroup});
                addRowToTable(TableRowGroup="$attribute{tableRowGroup}", NameList={"objectName", "attributeName", "description"});
            />                                 
        </sun:button>
        <sun:button id="button1" text="$resource{i18n.button.DeleteProperties}" disabled="#{true}" primary="#{false}">
            <!command
                getUIComponent(clientId="$pageSession{propertyTableRowGroupId}", component=>$attribute{trg});   
                getSelectedTableRowKeys(tableRowGroup="${trg}" rowKeys=>$attribute{rowKeys});
                deleteTableRows(tableRowGroup="${trg}" rowKeys="${rowKeys}");
                commitTableRowGroup(tableRowGroup="${trg}");
            />                                
        </sun:button>
    </sun:panelGroup>
</facet>       
<sun:tableRowGroup id="rowGroup1" selected="#{td.value.selected}" data={"$pageSession{tableList}", "$pageSession{tableList2}"}  sourceVar="td">
    <ui:event type="beforeCreate">
        // Ensure all required fields exist
        foreach (var="item" list="#{pageSession.tableList}") {
            if ("!(#{item.objectName}=#{null})") {
                if ("#{item.attributeName}=#{null}") {
                    mapPut(map="#{item}" key="attributeName" value="");
                }
                if ("#{item.description}=#{null}") {
                    mapPut(map="#{item}" key="description" value="");
                }
            }
        }

        // Add extra table properties...
        createList(size="0", result="#{pageSession.tableList2}");
        foreach(var="row" list="#{pageSession.tableList}") {
            selectiveEncode(value="#{row.objectName}" result="#{requestScope.tlEncObjName}");
            gf.createAttributeMap(
                keys={"selected", "encodedName", "description"},
                values={"$boolean{false}", "$attribute{tlEncObjName}", ""},
                map="#{requestScope.tlMap}");
            listAdd(list="#{pageSession.tableList2}", value="#{requestScope.tlMap}");
        }
    </ui:event>
    <ui:event type="afterCreate">
        getClientId(component="$this{component}" clientId=>$page{propertyTableRowGroupId});
    </ui:event>
    <sun:tableColumn headerText="$resource{i18n.common.SelectHeader}" selectId="select" rowHeader="$boolean{false}" id="col1">
        <sun:checkbox id="select"
                      toolTip="$resource{i18n.common.select}" 
                      selected="#{td.value.selected}" 
                      selectedValue="$boolean{true}" 
                      onClick="setTimeout('admingui.table.changeOneTableButton(\\\\\'#{pageSession.topActionGroup}\\\\\', \\\\\'#{pageSession.tableId}\\\\\'); admingui.table.initAllRows(\\\\\'#{pageSession.tableId}\\\\\');', 0);"
                      />
    </sun:tableColumn>                    
    <sun:tableColumn headerText="$resource{i18n.common.PropertyName}" sort="name" rowHeader="$boolean{true}" id="col2">
        <sun:textField columns="$int{40}" maxLength="#{sessionScope.fieldLengths['maxLength.common.PropertyName']}" id="col1St" value="#{td.value.objectName}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="$resource{i18n.common.PropertyValue}" sort="value" rowHeader="$boolean{false}" id="col3">
        <sun:textField columns="$int{40}" maxLength="#{sessionScope.fieldLengths['maxLength.common.PropertyValue']}" id="col1St" value="#{td.value.attributeName}" />
    </sun:tableColumn>
    <sun:tableColumn headerText="$resource{i18n.common.description.header}" sort="value" rowHeader="$boolean{false}" id="col4" rendered="#{pageSession.showDescCol != 'false'}">
        <sun:textField columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.common.description']}"  id="col1St" value="#{td.value.description}" />
    </sun:tableColumn>
    "<br/>
</sun:tableRowGroup>
</sun:table>
