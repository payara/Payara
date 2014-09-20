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

<sun:table id="resourcesTable" title="#{tableTitle}"
           deselectMultipleButton="$boolean{true}"
           deselectMultipleButtonOnClick="setTimeout('admingui.table.toggleButtons(\\\\\'#{pageSession.topActionGroup}\\\\\', \\\\\'#{pageSession.tableId}\\\\\');', 0)"
           selectMultipleButton="$boolean{true}"
           selectMultipleButtonOnClick="setTimeout('admingui.table.toggleButtons(\\\\\'#{pageSession.topActionGroup}\\\\\', \\\\\'#{pageSession.tableId}\\\\\');', 0)" >
    <!afterCreate
        getClientId(component="$this{component}" clientId=>$page{tableId});
    />

#include "/common/resourceNode/resourceHandlers.inc"
#include "/jms/shared/tableButtons.inc"

    <sun:tableRowGroup id="rowGroup1" selected="#{td.value.selected}" data={"$pageSession{listOfRows}"} sourceVar="td">
        <!beforeCreate
            if (#{pageSession.isConnPage}) {
                gfr.getJmsConnectionFactories();
                getJmsResourcesInfo( resourcesList="#{requestScope.children}" isConnectionFactory="#{true}" result="#{pageSession.tmpRows}");
                gfr.getResourcesTableData();
            }
            if (!#{pageSession.isConnPage}) {
                gfr.getJmsDestResources();
                getJmsResourcesInfo( resourcesList="#{requestScope.children}" isConnectionFactory="#{false}" result="#{pageSession.tmpRows}");
                gfr.getResourcesTableData();
            }
        />
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{tableRowGroupId});
        />
        <sun:tableColumn id="colSelect" headerText="$resource{i18n.common.SelectHeader}" selectId="select" rowHeader="$boolean{false}">
            <sun:checkbox id="select"
                          toolTip="$resource{i18n.common.select}"
                          selected="#{td.value.selected}"
                          selectedValue="$boolean{true}"
                          onClick="setTimeout('admingui.table.toggleButtons(\\\\\'#{pageSession.topActionGroup}\\\\\', \\\\\'#{pageSession.tableId}\\\\\'); admingui.table.initAllRows(\\\\\'#{pageSession.tableId}\\\\\');', 0);"/>
        </sun:tableColumn>

        <sun:tableColumn id="colName" headerText="$resource{i18njms.resourcesTable.colJndiName}"  sort="name" rowHeader="$boolean{true}">
            <sun:hyperlink id="link" text="#{td.value.name}" url="#{pageSession.editLink}?name=#{td.value.encodedName}" />
        </sun:tableColumn>
        
        <sun:tableColumn id="colLogicalName" headerText="$resource{i18n.common.resourcesTable.colLogicalJndiName}"  sort="logicalJndiName" rendered="#{isConnPage}" rowHeader="$boolean{false}">
            <h:outputText id="lname" value="#{td.value.logicalJndiName}"/>
        </sun:tableColumn>

        <sun:tableColumn rendered="#{!pageSession.onlyDASExist}" headerText="$resource{i18n.deployTable.colStatus}" sort="enabled" rowHeader="$boolean{false}" id="col22">
            <sun:hyperlink rendered="#{!pageSession.onlyDASExist}" id="targetLink" text="#{td.value.enableURL}" url="#{targetPage}?name=#{td.value.encodedName}&generalPage=#{pageSession.generalPage}?name=#{td.value.encodedName}" />
        </sun:tableColumn>

        <sun:tableColumn  rendered="#{pageSession.onlyDASExist}" headerText="$resource{i18n.deployTable.colEnabled}" sort="enabled" rowHeader="$boolean{false}" id="col222">
            <sun:image rendered="#{pageSession.onlyDASExist}" id="enCol" value="#{td.value.enableURL}" />
        </sun:tableColumn>

        <sun:tableColumn id="colResType" headerText="$resource{i18njms.resourcesTable.colResType}" sort="resType"  rowHeader="$boolean{false}">
            <h:outputText id="resType" value="#{td.value.resType}"/>
        </sun:tableColumn>

        <sun:tableColumn id="colTypeDesc" headerText="$resource{i18njms.resourcesTable.colDesc}" sort="description" rowHeader="$boolean{false}">
            <h:outputText id="typeDesc" value="#{td.value.description}" />
        </sun:tableColumn>

    </sun:tableRowGroup>

</sun:table>
