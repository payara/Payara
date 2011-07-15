<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.

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

<!-- configuration/virtualServerButtons.inc -->
#include "/common/shared/commonHandlers.inc"
<!facet pageButtonsTop>
        <sun:panelGroup id="topButtons">
            <sun:button id="saveButton" rendered="#{edit}" text="$resource{i18n.button.Save}"
                onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
                <!command
                    convertArrayToCommaString(array="#{pageSession.selectedNetworkListeners}"  commaString="#{requestScope.tmp}");
                    mapPut(map="#{pageSession.valueMap}" key="networkListeners" value="$attribute{tmp}");
                    
                    gf.createEntity(endpoint="#{pageSession.selfUrl}"
                        attrs="#{pageSession.valueMap}"
                        skipAttrs="#{pageSession.skipAttrsList}"
                        convertToFalse="#{pageSession.convertToFalseList}");
                    createMap(result="#{pageSession.statusAttrs}");
                    mapPut(map="#{pageSession.statusAttrs}" key="DEFAULT" value="#{pageSession.configName}" );
                    gfr.getListOfInstances();
                    if(#{pageSession.configName}=server-config){
                        listAdd(list="#{pageSession.instanceList}" value="server" index="0");
                    }
                    gf.ensureDefaultWebModule(endpoint="#{pageSession.parentUrl}/#{pageSession.childType}",vsName="#{pageSession.valueMap['id']}",instanceList="#{pageSession.instanceList}");
                    getUIComponent(clientId="$pageSession{propertyTableRowGroupId}", component=>$attribute{tableRowGroup});
                    getAllSingleMapRows(TableRowGroup="$attribute{tableRowGroup}",  Rows=>$attribute{newList});
                    removeEmptyProps(props="#{pageSession.tableList}" modifiedProps="#{pageSession.tableList}");
                    javaToJSON(obj="#{pageSession.tableList}" json="#{requestScope.tmpJSON}");
		    gf.restRequest(endpoint="#{pageSession.selfUrl}/property.json" method="POST" data="#{requestScope.tmpJSON}"
                                        result="#{requestScope.restResponse}");
                    prepareSuccessfulMsg();
                    gf.redirect(page="#{selfPage}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
                />
            </sun:button>
            <sun:button id="newButton" rendered="#{!edit}" text="$resource{i18n.button.OK}"
                onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
                <!command
                    convertArrayToCommaString(array="#{pageSession.selectedNetworkListeners}"  commaString="#{requestScope.tmp}");
                    mapPut(map="#{pageSession.valueMap}" key="networkListeners" value="$attribute{tmp}");
                    setPageSessionAttribute(key="onlyUseAttrs" value={"id", "state", "target", "hosts", "logFile", "defaultWebModule", "networkListeners", "logfile"})
                    gf.createEntity(endpoint="#{pageSession.selfUrl}"
                            attrs="#{pageSession.valueMap}"
                            skipAttrs="#{pageSession.skipAttrsList}"
                            convertToFalse="#{pageSession.convertToFalseList}"
                            onlyUseAttrs="#{pageSession.onlyUseAttrs}"
                    );
                    setPageSessionAttribute(key="onlyUseAttrs" value={"ssoEnabled", "ssoCookieHttpOnly", "accessLoggingEnabled", "docroot", "accessLog"})
                    gf.createEntity(endpoint="#{pageSession.selfUrl}/#{pageSession.valueMap['id']}"
                            attrs="#{pageSession.valueMap}"
                            onlyUseAttrs="#{pageSession.onlyUseAttrs}"
                            convertToFalse="#{pageSession.convertToFalseList}"
                    );
                    createMap(result="#{pageSession.statusAttrs}");
                    mapPut(map="#{pageSession.statusAttrs}" key="DEFAULT" value="#{pageSession.configName}" );
                    gfr.getListOfInstances();
                    if(#{pageSession.configName}=server-config){
                        listAdd(list="#{pageSession.instanceList}" value="server" index="0");
                    }
                    gf.ensureDefaultWebModule(endpoint="#{pageSession.selfUrl}",vsName="#{pageSession.valueMap['id']}",instanceList="#{pageSession.instanceList}");
                    getUIComponent(clientId="$pageSession{propertyTableRowGroupId}", component=>$attribute{tableRowGroup});
                    getAllSingleMapRows(TableRowGroup="$attribute{tableRowGroup}",  Rows=>$attribute{newList});
                    removeEmptyProps(props="#{pageSession.tableList}" modifiedProps="#{pageSession.tableList}");
                    javaToJSON(obj="#{pageSession.tableList}" json="#{requestScope.tmpJSON}");
		    gf.restRequest(endpoint="#{pageSession.selfUrl}/#{pageSession.valueMap['id']}/property.json" method="POST" data="#{requestScope.tmpJSON}" result="#{requestScope.restResponse}");
                    gf.redirect(page="#{parentPage}?configName=#{configName}")
                />
            </sun:button>
            <sun:button id="cancelButton" immediate="#{true}" primary="#{false}" rendered="#{pageSession.showCancelButton}" text="$resource{i18n.button.Cancel}" >
                <!command
                    gf.redirect(page="#{parentPage}?configName=#{configName}");
                />
            </sun:button>
        </sun:panelGroup>
    </facet>
