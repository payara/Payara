<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.

    The contents of this file are subject to the terms of either the GNU
    General Public License Version 2 only ("GPL") or the Common Development
    and Distribution License("CDDL") (collectively, the "License").  You
    may not use this file except in compliance with the License.  You can
    obtain a copy of the License at
    https://github.com/payara/Payara/blob/main/LICENSE.txt
    See the License for the specific
    language governing permissions and limitations under the License.

    When distributing the software, include this License Header Notice in each
    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.

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
                    // GlassFish 4 seemingly does not support DELETE and POST for the user-group or principal,
                    // so use update-connector-security-map instead of the separate requests from GlassFish 3.
                    // The "add" and "remove" lists cannot contain the same items for this endpoint.

                    // Get the old user groups and principals.
                    gf.restRequest(endpoint="#{pageSession.selfUrl}/user-group" method="GET" result="#{requestScope.resultU}");
                    setAttribute(key="oldGroupsList" value="#{requestScope.resultU.data.extraProperties.leafList}");
                    gf.restRequest(endpoint="#{pageSession.selfUrl}/principal" method="GET" result="#{requestScope.resultP}");
                    setAttribute(key="oldPrincipalsList" value="#{requestScope.resultP.data.extraProperties.leafList}");

                    // Create the request attributes, which has either user groups or principals (not both).
                    createMap(result="#{requestScope.attrsMap}");
                    mapPut(map="#{requestScope.attrsMap}" key="poolname" value="$pageSession{Name}");
                    mapPut(map="#{requestScope.attrsMap}" key="mapname" value="$pageSession{mapName}");

                    if("#{requestScope.usersOptionU}=users"){
                        // Remove common elements from the old and new "user groups" lists.
                        convertStringtoList(str="#{pageSession.valueMap.userGroupCommaStr}" result="#{requestScope.newGroupsList}");
                        gf.listCombine(list="#{requestScope.oldGroupsCopy}" list2="#{requestScope.oldGroupsList}" result="#{requestScope.oldGroupsCopy}")
                        foreach(var="oneGroup" list="#{requestScope.oldGroupsCopy}"){
                            gf.containedIn(list="#{requestScope.newGroupsList}" testStr="#{requestScope.oneGroup}" contain="containsGroup");
                            if("#{requestScope.containsGroup}"){
                                listRemove(list="#{requestScope.oldGroupsList}" value="#{requestScope.oneGroup}" result="#{requestScope.oldGroupsList}");
                                listRemove(list="#{requestScope.newGroupsList}" value="#{requestScope.oneGroup}" result="#{requestScope.newGroupsList}");
                            }
                        }
                        convertListToCommaString(list="#{requestScope.newGroupsList}" commaString="#{requestScope.newGroupsCommaStr}");
                        mapPut(map="#{requestScope.attrsMap}" key="addusergroups" value="$attribute{newGroupsCommaStr}");
                    }
                    if("#{requestScope.usersOptionP}=principals"){
                        // Remove common elements from the old and new "principals" lists.
                        convertStringtoList(str="#{pageSession.valueMap.principalCommaStr}" result="#{requestScope.newPrincipalsList}");
                        gf.listCombine(list="#{requestScope.oldPrincipalsCopy}" list2="#{requestScope.oldPrincipalsList}" result="#{requestScope.oldPrincipalsCopy}")
                        foreach(var="onePrincipal" list="#{requestScope.oldPrincipalsCopy}"){
                            gf.containedIn(list="#{requestScope.newPrincipalsList}" testStr="#{requestScope.onePrincipal}" contain="containsPrincipal");
                            if("#{requestScope.containsPrincipal}"){
                                listRemove(list="#{requestScope.oldPrincipalsList}" value="#{requestScope.onePrincipal}" result="#{requestScope.oldPrincipalsList}");
                                listRemove(list="#{requestScope.newPrincipalsList}" value="#{requestScope.onePrincipal}" result="#{requestScope.newPrincipalsList}");
                            }
                        }
                        convertListToCommaString(list="#{requestScope.newPrincipalsList}" commaString="#{requestScope.newPrincipalsCommaStr}");
                        mapPut(map="#{requestScope.attrsMap}" key="addprincipals" value="$attribute{newPrincipalsCommaStr}");
                    }

                    convertListToCommaString(list="#{requestScope.oldPrincipalsList}" commaString="#{requestScope.oldPrincipalsCommaStr}");
                    mapPut(map="#{requestScope.attrsMap}" key="removeprincipals" value="$attribute{oldPrincipalsCommaStr}");
                    convertListToCommaString(list="#{requestScope.oldGroupsList}" commaString="#{requestScope.oldGroupsCommaStr}");
                    mapPut(map="#{requestScope.attrsMap}" key="removeusergroups" value="$attribute{oldGroupsCommaStr}");

                    mapPut(map="#{requestScope.attrsMap}" key="mappedpassword" value="#{pageSession.valueMap['mappedPassword']}");

                    gf.restRequest(endpoint="#{sessionScope.REST_URL}/resources/update-connector-security-map" attrs="#{requestScope.attrsMap}" method="POST");

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
