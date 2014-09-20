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

<!-- applications/deployTable.inc -->


<sun:table id="deployTable" title="$resource{i18n.deployTable.applications}" sortPanelToggleButton="#{false}"
       deselectMultipleButton="$boolean{true}"
        deselectMultipleButtonOnClick="setTimeout('admingui.table.changeThreeTableButtons(\\\\\'#{pageSession.topActionGroup}\\\\\', \\\\\'#{pageSession.tableId}\\\\\');', 0)"
        selectMultipleButton="$boolean{true}"
        selectMultipleButtonOnClick="setTimeout('admingui.table.changeThreeTableButtons(\\\\\'#{pageSession.topActionGroup}\\\\\', \\\\\'#{pageSession.tableId}\\\\\');', 0)" >
         <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{tableId});
        />

#include "/common/applications/deployTableButtons.inc"

   <sun:tableRowGroup id="rowGroup1" selected="#{td.value.selected}" data={"$attribute{listOfRows}"} sourceVar="td">
    <!afterCreate
         getClientId(component="$this{component}" clientId=>$page{tableRowGroupId});
    />

   <sun:tableColumn headerText="$resource{i18n.common.SelectHeader}" selectId="select" rowHeader="$boolean{false}" id="col0">
            <sun:checkbox id="select"
                toolTip="$resource{i18n.common.select}" 
                selected="#{td.value.selected}" 
                selectedValue="$boolean{true}" 
                onClick="admingui.table.changeThreeTableButtons('#{pageSession.topActionGroup}', '#{pageSession.tableId}'); admingui.table.initAllRows('#{pageSession.tableId}');"
            />
    </sun:tableColumn>    
        
    <sun:tableColumn headerText="$resource{i18n.deployTable.colName}" sort="name" rowHeader="$boolean{true}" id="col1">
        <sun:hyperlink id="link" text="#{td.value.name}" url="#{editLink}?appName=#{td.value.encodedName}&listPageLink=#{pageSession.listPageLink}" />
    </sun:tableColumn>

    <sun:tableColumn headerText="$resource{i18n.deployTable.colDeploymentOrder}"  sort="deploymentOrder"   id="colorder">
        <staticText id="order" value="#{td.value.deploymentOrder}" />
    </sun:tableColumn>
        
    <sun:tableColumn rendered="#{!pageSession.onlyDASExist}" headerText="$resource{i18n.deployTable.colStatus}" sort="enableURL"  id="col2">
        <sun:hyperlink rendered="#{!pageSession.onlyDASExist}" id="targetLink" text="#{td.value.enableURL}" url="#{targetLink}?appName=#{td.value.encodedName}&listPageLink=#{pageSession.listPageLink}" />
    </sun:tableColumn>

    <sun:tableColumn  rendered="#{pageSession.onlyDASExist}" headerText="$resource{i18n.deployTable.colEnabled}" sort="enableURL"  id="col22">
        <sun:image rendered="#{pageSession.onlyDASExist}" id="enCol" value="#{td.value.enableURL}" />
    </sun:tableColumn>

    
    <sun:tableColumn headerText="$resource{i18n.deployTable.ColEngines}"  sort="sniffers"   id="colsnif">
        <staticText id="sniffer" value="#{td.value.sniffers}" />
    </sun:tableColumn>

    <sun:tableColumn headerText="$resource{i18n.ComponentTable.colAction}" embeddedActions="#{false}"  id="col4">
            <sun:hyperlink id="link" rendered="#{td.value.hasLaunch}" text="$resource{i18n.deployTable.launch}"
                onClick="var win=window.open('#{request.contextPath}/common/applications/webApplicationLinks.jsf?appID=#{td.value.name}&contextRoot=#{td.value.contextRoot}','ViewerWindow','width='+.75*screen.width+',height='+.75*screen.height+',top='+.1*screen.height+',left='+.1*screen.width+',toolbar=yes,status=yes,menubar=yes,scrollbars=yes,resizable=yes,directories=yes,location=yes');win.focus(); return false;" />
        "&#160;|&#160;

        <sun:hyperlink id="redeployLink" text="$resource{i18n.deployTable.redeploy}" 
            url="#{request.contextPath}/common/applications/redeploy.jsf?appName=#{td.value.name}&appType=$pageSession{appType}&nextPage=#{listPageLink}" />

        "&#160;|&#160;

        <sun:hyperlink id="reloadLink" text="$resource{i18n.deployTable.reload}">
            <!command
                gf.reloadApplication(appName="#{td.value.name}");
                gf.redirect(page="#{request.contextPath}/#{pageSession.listPageLink}?alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
            />
        </sun:hyperlink>
        
    </sun:tableColumn>
    
  </sun:tableRowGroup>
  
  
</sun:table>
