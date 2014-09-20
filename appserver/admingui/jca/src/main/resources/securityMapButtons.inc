<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.

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
                onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}') && checkClassname()) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
                <!command
                    //Remove the old user groups
                    gf.restRequest(endpoint="#{pageSession.selfUrl}/user-group" method="GET" result="#{requestScope.result}");
                    setAttribute(key="oldGroupsList" value="#{requestScope.result.data.extraProperties.leafList}");
                    convertListToCommaString(list="#{requestScope.oldGroupsList}" commaString="#{requestScope.oldGroupsCommaStr}");
                    gf.createAttributeMap(keys={"poolName", "mapName", "groups"}
                                          values={"$pageSession{encodedName}", "$pageSession{encodedMapName}", "$attribute{oldGroupsCommaStr}"},
                                          map="#{requestScope.attrsMap}");
                    gf.restRequest(endpoint="#{pageSession.selfUrl}/user-group" attrs="#{requestScope.attrsMap}" method="DELETE");
                    //Remove the old principals
                    gf.restRequest(endpoint="#{pageSession.selfUrl}/principal" method="GET" result="#{requestScope.result}");
                    setAttribute(key="oldPrincipalsList" value="#{requestScope.result.data.extraProperties.leafList}");
                    convertListToCommaString(list="#{requestScope.oldPrincipalsList}" commaString="#{requestScope.oldPrincipalsCommaStr}");
                    gf.createAttributeMap(keys={"poolName", "mapName", "principals"}
                                          values={"$pageSession{encodedName}", "$pageSession{encodedMapName}", "$attribute{oldPrincipalsCommaStr}"},
                                          map="#{requestScope.attrsMap}");
                    gf.restRequest(endpoint="#{pageSession.selfUrl}/principal" attrs="#{requestScope.attrsMap}" method="DELETE");
                    if("#{requestScope.usersOptionU}=users"){
                        //Create a user group with the new values.
                        setAttribute(key="groupStr" value="#{pageSession.valueMap.userGroupCommaStr}");
                        gf.createAttributeMap(keys={"poolName", "mapName", "groups"}
                                              values={"$pageSession{encodedName}", "$pageSession{encodedMapName}", "$attribute{groupStr}"},
                                              map="#{requestScope.attrsMap}");
                        gf.restRequest(endpoint="#{pageSession.selfUrl}/user-group" attrs="#{requestScope.attrsMap}" method="POST");
                    }
                    if("#{requestScope.usersOptionP}=principals"){
                        //Create a principal with the new values.
                        setAttribute(key="principalStr" value="#{pageSession.valueMap.principalCommaStr}");
                        gf.createAttributeMap(keys={"poolName", "mapName", "principals"}
                                              values={"$pageSession{encodedName}", "$pageSession{encodedMapName}", "$attribute{principalStr}"},
                                              map="#{requestScope.attrsMap}");
                        gf.restRequest(endpoint="#{pageSession.selfUrl}/principal" attrs="#{requestScope.attrsMap}" method="POST");
                    }
                    //Update the backend-principal
                    createMap(result="#{requestScope.attrsMap}");
                    mapPut(map="#{requestScope.attrsMap}" key="password" value="#{pageSession.valueMap['mappedPassword']}");
                    gf.restRequest(endpoint="#{pageSession.selfUrl}/backend-principal" attrs="#{requestScope.attrsMap}" method="POST");

                    prepareSuccessfulMsg();
                    gf.redirect(page="#{selfPage}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
                />
            </sun:button>
            <sun:button id="newButton" rendered="#{!edit}" text="$resource{i18n.button.OK}"
                onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}') &&  checkClassname() && checkForBackslash('#{resCompId}', '$resource{i18n.msg.JS.resources.resName}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
                <!command
                    if (#{pageSession.valueMap.userGroupCommaStr}) {
                        convertStringtoList(str="#{pageSession.valueMap.userGroupCommaStr}" result=>$attribute{usersSA});
                    }
                    if (#{pageSession.valueMap.principalCommaStr}) {
                        convertStringtoList(str="#{pageSession.valueMap.principalCommaStr}" result=>$attribute{principalsSA});
                    }
                    if($attribute{usersSA}){
                       mapPut(map="#{pageSession.valueMap}" key="userGroups" value="#{pageSession.valueMap.userGroupCommaStr}")
                    }
                    if($attribute{principalsSA}) {
                        mapPut(map="#{pageSession.valueMap}" key="principals" value="#{pageSession.valueMap.principalCommaStr}")
                    }
                    mapPut(map="#{pageSession.valueMap}" key="poolName" value="#{pageSession.Name}");
                    gf.createEntity(endpoint="#{pageSession.parentUrl}/#{pageSession.childType}"
                        attrs="#{pageSession.valueMap}"
                        convertToFalse="#{pageSession.convertToFalseList}"
                        skipAttrs="#{pageSession.skipAttrsList}"
                        result => $attribute{objStr});                    
                    gf.redirect(page="#{pageSession.parentPage}")
                />
            </sun:button>
            <sun:button id="cancelButton" immediate="#{true}" primary="#{false}" rendered="#{pageSession.showCancelButton}" text="$resource{i18n.button.Cancel}" >
                <!command
                    gf.redirect(page="#{parentPage}");
                />
            </sun:button>
        </sun:panelGroup>
    </facet>
