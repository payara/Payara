<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.

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

<!-- shared/propertyDescTable.inc -->

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
                    addRowToTable(TableRowGroup="$attribute{tableRowGroup}", NameList={"name", "value", "description"});
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
                if ("!(#{item.name}=#{null})") {
                    if ("#{item.value}=#{null}") {
                        mapPut(map="#{item}" key="value" value="");
                    }
                    if ("#{item.description}=#{null}") {
                        mapPut(map="#{item}" key="description" value="");
                    }
                }
            }

            // Add extra table properties...
            createList(size="0", result="#{pageSession.tableList2}");
            foreach(var="row" list="#{pageSession.tableList}") {
                selectiveEncode(value="#{row.name}" result="#{requestScope.tlEncName}");
                gf.createAttributeMap(
                    keys={"selected", "encodedName", "description"},
                    values={"$boolean{false}", "$attribute{tlEncName}", ""},
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
            <sun:textField columns="$int{40}" maxLength="#{sessionScope.fieldLengths['maxLength.common.PropertyName']}" id="col1St" value="#{td.value.name}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="$resource{i18n.common.PropertyValue}" sort="value" rowHeader="$boolean{false}" id="col3">
            <sun:textField columns="$int{40}" maxLength="#{sessionScope.fieldLengths['maxLength.common.PropertyValue']}" id="col1St" value="#{td.value.value}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="$resource{i18n.common.description.header}" sort="value" rowHeader="$boolean{false}" id="col4" rendered="#{pageSession.showDescCol != 'false'}">
            <sun:textField columns="$int{20}" maxLength="#{sessionScope.fieldLengths['maxLength.common.description']}"  id="col1St" value="#{td.value.description}" />
        </sun:tableColumn>
        "<br/>
    </sun:tableRowGroup>
</sun:table>
