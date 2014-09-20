<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.

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

   <!facet pageButtonsTop>
        <sun:panelGroup id="topButtons">
            <sun:button id="saveButton" rendered="#{edit}" text="$resource{i18n.button.Save}"
                onClick="if (isSecurityMappingPresent('$resource{i18n.msg.JS.Error.securityMappingCannotBeEmpty}') && guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
                <!command
                    mapPut(map="#{pageSession.valueMap}" key="resourceAdapterName" value="#{pageSession.valueMap['raname']}");
                    gf.createEntity(endpoint="#{pageSession.selfUrl}"
                        attrs="#{pageSession.valueMap}"
                        skipAttrs="#{pageSession.skipAttrsList}"
                        convertToFalse="#{pageSession.convertToFalseList}");
                    if ('#{usersOptionG}=users') {
                        convertStringToMap(str="#{pageSession.valueMap2['groupMapping']}" result="#{requestScope.groupsMap}");
                        gf.getMapKeys(Map="#{requestScope.groupsMap}" Keys="#{requestScope.groupsList}");
                        gf.getChildList(parentEndpoint="#{pageSession.selfUrl}", childType="group-map", result="#{requestScope.oldGroupsInfo}", id="eisGroup");
                        gf.getMessageProps(messageListProps="#{requestScope.oldGroupsInfo}"
                           id="eisGroup"
                           keyList="#{pageSession.oldGroupsList}");
                        setPageSessionAttribute(key="groupsToAddList" value={});
                        foreach(var="group" list="#{requestScope.groupsList}") {
                            //verify whether this group already existing or not.
                            //If it is already there, update the entity or add to the new groups list.
                            gf.containedIn(list="#{pageSession.oldGroupsList}" testStr="#{group}" contain="#{requestScope.isExists}");
                            if (#{requestScope.isExists}) {
                                createMap(result="#{requestScope.attrsMap}");
                                mapGet(Map="#{requestScope.groupsMap}" Key="#{group}" Value="#{requestScope.val}");
                                mapPut(map="#{requestScope.attrsMap}" key="mappedGroup" value="#{val}");
                                gf.restRequest(endpoint="#{pageSession.selfUrl}/group-map/#{group}" attrs="#{requestScope.attrsMap}" method="POST");
                                removeListElement(list="#{pageSession.oldGroupsList}" name="#{group}" finalList="#{pageSession.oldGroupsList}");
                            }
                            if (!#{requestScope.isExists}) {
                                mapGet(Map="#{requestScope.groupsMap}" Key="#{group}" Value="#{requestScope.val}");
                                listAdd(list="#{pageSession.groupsToAddList}" value="#{group}=#{val}" result="#{pageSession.groupsToAddList}");                                
                            }                            
                        }
                        if(!#{pageSession.isAppScopedRes}){
                            convertListToCommaString(list="#{pageSession.groupsToAddList}" commaString=>$attribute{groupsToAddStr});
                            convertListToCommaString(list="#{pageSession.oldGroupsList}" commaString=>$attribute{oldGroupsStr});
                            createMap(result="#{requestScope.attrsMap}");
                            mapPut(map="#{requestScope.attrsMap}" key="raname" value="#{pageSession.valueMap['resourceAdapterName']}");
                            mapPut(map="#{requestScope.attrsMap}" key="addgroups" value="#{groupsToAddStr}");
                            mapPut(map="#{requestScope.attrsMap}" key="removegroups" value="#{oldGroupsStr}");
                            gf.restRequest(endpoint="#{pageSession.selfUrl}/update-connector-work-security-map" attrs="#{requestScope.attrsMap}" method="POST");
                        }
                    }
                    if ('#{usersOptionP}=principals') {
                        convertStringToMap(str="#{pageSession.valueMap2['principalMapping']}" result="#{requestScope.principalsMap}");
                        gf.getMapKeys(Map="#{requestScope.principalsMap}" Keys="#{requestScope.principalsList}");
                        gf.getChildList(parentEndpoint="#{pageSession.selfUrl}", childType="principal-map", result="#{requestScope.oldPrincipalsInfo}", id="eisPrincipal");
                        gf.getMessageProps(messageListProps="#{requestScope.oldPrincipalsInfo}"
                           id="eisPrincipal"
                           keyList="#{pageSession.oldPrincipalsList}");
                        setPageSessionAttribute(key="principalsToAddList" value={});
                        foreach(var="principal" list="#{requestScope.principalsList}") {
                            //verify whether this principal already existing or not.
                            //If it is already there, update the entity or add to the new principals list.
                            gf.containedIn(list="#{pageSession.oldPrincipalsList}" testStr="#{principal}" contain="#{requestScope.isExists}");
                            if (#{requestScope.isExists}) {
                                createMap(result="#{requestScope.attrsMap}");
                                mapGet(Map="#{requestScope.principalsMap}" Key="#{principal}" Value="#{requestScope.val}");
                                mapPut(map="#{requestScope.attrsMap}" key="mappedPrincipal" value="#{val}");
                                gf.restRequest(endpoint="#{pageSession.selfUrl}/principal-map/#{principal}" attrs="#{requestScope.attrsMap}" method="POST");
                                removeListElement(list="#{pageSession.oldPrincipalsList}" name="#{principal}" finalList="#{pageSession.oldPrincipalsList}");
                            }
                            if (!#{requestScope.isExists}) {
                                mapGet(Map="#{requestScope.principalsMap}" Key="#{principal}" Value="#{requestScope.val}");
                                listAdd(list="#{pageSession.principalsToAddList}" value="#{principal}=#{val}" result="#{pageSession.principalsToAddList}");
                            }
                        }
                        if(!#{pageSession.isAppScopedRes}){
                            convertListToCommaString(list="#{pageSession.principalsToAddList}" commaString=>$attribute{principalsToAddStr});
                            convertListToCommaString(list="#{pageSession.oldPrincipalsList}" commaString=>$attribute{oldPrincipalsStr});
                            createMap(result="#{requestScope.attrsMap}");
                            mapPut(map="#{requestScope.attrsMap}" key="raname" value="#{pageSession.valueMap['resourceAdapterName']}");
                            mapPut(map="#{requestScope.attrsMap}" key="addprincipals" value="#{principalsToAddStr}");
                            mapPut(map="#{requestScope.attrsMap}" key="removeprincipals" value="#{oldPrincipalsStr}");
                            gf.restRequest(endpoint="#{pageSession.selfUrl}/update-connector-work-security-map" attrs="#{requestScope.attrsMap}" method="POST");
                        }

                    }                   
                    prepareSuccessfulMsg();
                    gf.redirect(page="#{selfPage}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
                />
            </sun:button>
            <sun:button id="newButton" rendered="#{!edit}" text="$resource{i18n.button.OK}"
                onClick="if (isSecurityMappingPresent('$resource{i18n.msg.JS.Error.securityMappingCannotBeEmpty}') && guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}') && checkForBackslash('#{resCompId}', '$resource{i18n.msg.JS.resources.resName}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
                <!command
                    if ('#{usersOptionG}=users') {                        
                        mapPut(map="#{pageSession.valueMap}" key="groupsmap" value="#{pageSession.valueMap2['groupMapping']}");
                    }
                    if ('#{usersOptionP}=principals') {
                        mapPut(map="#{pageSession.valueMap}" key="principalsmap" value="#{pageSession.valueMap2['principalMapping']}");
                    }
                    gf.createEntity(endpoint="#{pageSession.parentUrl}/#{pageSession.childType}"
                        attrs="#{pageSession.valueMap}");
                    
                    gf.redirect(page="#{pageSession.parentPage}")
                />
            </sun:button>
            <sun:button id="cancelButton" immediate="#{true}" primary="#{false}"  text="$resource{i18n.button.Cancel}" >
                <!command
                    gf.redirect(page="#{parentPage}");
                />
            </sun:button>
        </sun:panelGroup>
    </facet>
