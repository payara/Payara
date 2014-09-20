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

<sun:button id="saveButton" rendered="#{edit}" text="$resource{i18n.button.Save}" 
        onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
    <!command
        gf.updateEntity(endpoint="#{pageSession.selfUrl}", attrs="#{pageSession.valueMap}", skipAttrs="#{pageSession.skipAttrsList}",
            convertToFalse="#{pageSession.convertToFalseList}"
        );
        if(#{pageSession.resourceRefUrl}) {
            gf.updateEntity(endpoint="#{pageSession.resourceRefUrl}", attrs="#{pageSession.resourceRef}", convertToFalse={"enabled"});
        }

        removeEmptyProps(props="#{newList}" modifiedProps="#{newList}");
        javaToJSON(obj="#{pageSession.tableList}" json="#{requestScope.tmpJSON}");
        gf.restRequest(endpoint="#{pageSession.selfUrl}/property.json", method="POST", data="#{requestScope.tmpJSON}", result="#{requestScope.restResponse}");

        prepareSuccessfulMsg();
        gf.redirect(page="#{selfPage}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
    />
</sun:button>
<sun:button id="newButton" rendered="#{!edit}" text="$resource{i18n.button.OK}"
        onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}') && checkForBackslash('#{resCompId}', '$resource{i18n.msg.JS.resources.resName}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
    <!command
        // Create connector-resource
        // Update the connector-resource with the Name specified on the connector-connection-pool
        
        mapPut(map="#{pageSession.valueMap2}", key="poolName", value="#{pageSession.valueMap['name']}-Connection-Pool");
        mapPut(map="#{pageSession.valueMap2}", key="name", value="#{pageSession.valueMap['name']}");
        mapPut(map="#{pageSession.valueMap}", key="name", value="#{pageSession.valueMap['name']}-Connection-Pool");

        // Create connector-connection-pool
        gf.createEntity(endpoint="#{pageSession.parentUrl}/connector-connection-pool",
            attrs="#{pageSession.valueMap}", convertToFalse="#{pageSession.convertToFalseList}", result => $attribute{resultEndPoint});
        // Create connector-resource
        mapPut(map="#{pageSession.valueMap2}" key="target" value="domain");
        gf.createEntity(endpoint="#{pageSession.parentUrl}/#{pageSession.childType2}",
                attrs="#{pageSession.valueMap2}", convertToFalse="#{pageSession.convertToFalseList2}", result => $attribute{resultEndPoint3});

        //The selected Targets will be available as #{pageSession.selectedTargets},  which is an array.
        //We need to create source-ref of the selected targets.
        convertArrayToCommaString(array="#{pageSession.selectedTargets}" commaString="#{requestScope.cs}")
        convertStringtoList(str="#{requestScope.cs}" result="#{requestScope.targetList}");
        //println("===========#{requestScope.targetList}");
        setAttribute(key="nTargets" value="#{requestScope.targetList.size()}");
        dec(number="#{requestScope.nTargets}" value="#{requestScope.endRef}");
        forLoop(end="#{requestScope.endRef}"  varName="nn"){
            setAttribute(key="targetName" value="#{requestScope.targetList[nn]}");
            gf.checkIfEndPointExist(endpoint="#{sessionScope.REST_URL}/servers/server/#{requestScope.targetName}", exists="#{requestScope.isServer}");
            if (#{requestScope.isServer}){
                setAttribute(key="endp" value="#{sessionScope.REST_URL}/servers/server/#{requestScope.targetName}/resource-ref");
            }
            if (!#{requestScope.isServer}){
                setAttribute(key="endp" value="#{sessionScope.REST_URL}/clusters/cluster/#{requestScope.targetName}/resource-ref");
            }
            createMap(result="#{requestScope.refsMap}")
            mapPut(map="#{requestScope.refsMap}" key="id" value="#{pageSession.valueMap2['name']}")
            mapPut(map="#{requestScope.refsMap}" key="enabled" value="#{pageSession.resourceRef['enabled']}")
            mapPut(map="#{requestScope.refsMap}" key="target" value="#{requestScope.targetName}")
            gf.createEntity(endpoint="#{requestScope.endp}" attrs="#{requestScope.refsMap}" convertToFalse={"enabled"});
        }

        getUIComponent(clientId="$pageSession{propertyTableRowGroupId}", component=>$attribute{tableRowGroup});
        getAllSingleMapRows(TableRowGroup="$attribute{tableRowGroup}",  Rows=>$attribute{newList});
        removeEmptyProps(props="#{newList}" modifiedProps="#{newList}");
        javaToJSON(obj="$attribute{newList}" json="#{requestScope.tmpJSON}");
        urlencode(value="#{pageSession.valueMap.name}" encoding="UTF-8" result="#{pageSession.encodedName}");
        gf.restRequest(endpoint="#{pageSession.parentUrl}/connector-connection-pool/#{pageSession.encodedName}/property.json",
            method="POST", data="#{requestScope.tmpJSON}", result="#{requestScope.restResponse}");
        gf.redirect(page="#{parentPage}")
    />
</sun:button>
<sun:button id="cancelButton" immediate="#{true}" primary="#{false}" rendered="#{pageSession.showCancelButton}" text="$resource{i18n.button.Cancel}" >
    <!command
        gf.redirect(page="#{parentPage}");
    />
</sun:button>
