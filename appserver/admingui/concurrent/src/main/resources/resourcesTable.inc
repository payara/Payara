<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.

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

<!-- concurrent/resourcesTable.inc -->

<sun:table id="resourcesTable" title="#{tableTitle}" 
            deselectMultipleButton="$boolean{true}"
            deselectMultipleButtonOnClick="setTimeout('admingui.table.changeThreeTableButtons(\\\\\'#{pageSession.topActionGroup}\\\\\', \\\\\'#{pageSession.tableId}\\\\\');', 0)"
            selectMultipleButton="$boolean{true}"
            selectMultipleButtonOnClick="setTimeout('admingui.table.changeThreeTableButtons(\\\\\'#{pageSession.topActionGroup}\\\\\', \\\\\'#{pageSession.tableId}\\\\\');', 0)" >
         <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{tableId});
        />
    
#include "/common/resourceNode/resourceHandlers.inc"
#include "/concurrent/tableButtons.inc"

   <sun:tableRowGroup id="rowGroup1" selected="#{td.value.selected}" data={"$pageSession{listOfRows}"} sourceVar="td">
       <!beforeCreate
           gf.getChildList(parentEndpoint="#{pageSession.parentUrl}", childType="#{pageSession.childType}", id="jndiName", result="#{pageSession.tmpRows}");
           gfr.getResourcesTableData();
           gf.restRequest(endpoint="#{sessionScope.REST_URL}/resources/#{pageSession.listCommand}"  method="GET" result="#{requestScope.tmpJMap}");
           mapGet(Map="#{requestScope.tmpJMap.data.extraProperties}", Key="#{pageSession.logicalJndiMapKey}"  Value="#{requestScope.logicalMapList}")
           gf.getLogicalJndiName(logicalMapList="#{requestScope.logicalMapList}", listRow="#{pageSession.listOfRows}" result="#{pageSession.listOfRows}");
           foreach (var="oneRow" list="#{pageSession.listOfRows}") {
               if (#{oneRow.contextInfoEnabled}=true){
                    mapPut(map="#{requestScope.oneRow}" key="ctxInfoEnableImage" value="/resource/images/enabled.png");
                }
                if (#{oneRow.contextInfoEnabled}=false){
                    mapPut(map="#{requestScope.oneRow}" key="ctxInfoEnableImage" value="/resource/images/disabled.png");
                    mapPut(map="#{requestScope.oneRow}" key="contextInfo" value="" );
               }
           }
        />
     <!afterCreate
         getClientId(component="$this{component}" clientId=>$page{tableRowGroupId});
        />
   <sun:tableColumn headerText="$resource{i18n.common.SelectHeader}" selectId="select" rowHeader="$boolean{false}" id="col0">
            <sun:checkbox id="select"
                toolTip="$resource{i18n.common.select}" 
                selected="#{td.value.selected}" 
                selectedValue="$boolean{true}" 
                onClick="setTimeout('admingui.table.changeThreeTableButtons(\\\\\'#{pageSession.topActionGroup}\\\\\', \\\\\'#{pageSession.tableId}\\\\\'); admingui.table.initAllRows(\\\\\'#{pageSession.tableId}\\\\\');', 0);"
            />
    </sun:tableColumn>    

    <sun:tableColumn headerText="$resource{i18ncon.resourcesTable.colJndiName}" sort="name" rowHeader="$boolean{true}" id="col1">
        <sun:hyperlink id="link" text="#{td.value.name}" url="#{pageSession.editLink}?name=#{td.value.encodedName}&logicalJndiName=#{td.value.encodedLogicalJndiName}" />
    </sun:tableColumn>

    <sun:tableColumn id="colLogicalName" headerText="$resource{i18n.common.resourcesTable.colLogicalJndiName}"  sort="logicalJndiName"  rowHeader="$boolean{false}">
        <h:outputText id="lname" value="#{td.value.logicalJndiName}"/>
    </sun:tableColumn>

    <sun:tableColumn rendered="#{!pageSession.onlyDASExist}" headerText="$resource{i18n.deployTable.colStatus}" sort="enabled" rowHeader="$boolean{false}" id="col22">
        <sun:hyperlink rendered="#{!pageSession.onlyDASExist}" id="targetLink" text="#{td.value.enableURL}" url="#{targetPage}?name=#{td.value.encodedName}&generalPage=#{pageSession.generalPage}?name=#{td.value.encodedName}" />
    </sun:tableColumn>

    <sun:tableColumn  rendered="#{pageSession.onlyDASExist}" headerText="$resource{i18n.deployTable.colEnabled}" sort="enabled" rowHeader="$boolean{false}" id="coldd">
        <sun:image rendered="#{pageSession.onlyDASExist}" id="enCol" value="#{td.value.enableURL}" />
    </sun:tableColumn>

    <sun:tableColumn headerText="$resource{i18ncon.resourcesTable.colContextInfo}" sort="contextInfo"  rowHeader="$boolean{false}" id="coltype">
        <sun:staticText escape="true" id="contextInfo" value="#{td.value.contextInfo}" />
    </sun:tableColumn>

    <sun:tableColumn headerText="$resource{i18ncon.resourcesTable.colThreadPriority}" sort="threadPriority" rendered="#{pageSession.hasThreadPriority}"  rowHeader="$boolean{false}" id="thread">
        <sun:staticText escape="true" id="threadPriority" rendered="#{pageSession.hasThreadPriority}" value="#{td.value.threadPriority}" />
    </sun:tableColumn>
    
    <sun:tableColumn headerText="$resource{i18ncon.resourcesTable.colDesc}" rowHeader="$boolean{false}" id="col4">
        <sun:staticText escape="true" id="typeDesc" value="#{td.value.description}" />
    </sun:tableColumn>
    
  </sun:tableRowGroup>
  
</sun:table>
