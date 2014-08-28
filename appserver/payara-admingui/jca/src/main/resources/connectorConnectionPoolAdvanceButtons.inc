<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.

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

<!-- jca/connectorConnectionPoolAdvanceButtons.inc -->


<!facet pageButtonsTop>
<sun:panelGroup id="topButtons">
    <sun:button id="saveButton"  text="$resource{i18n.button.Save}"
            onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
        <!command
            if ("#{pageSession.valueMap['lazyConnectionAssociation']}") {
                setValueExpression(expression="#{pageSession.valueMap['lazyConnectionEnlistment']}", value="true");
            }
            
            //Issue 15618 : To save lazyConnectionEnlistment before lazyConnectionAssociation.
            createMap(result=>$attribute{lazyConnectionMap});
            mapPut(map="#{lazyConnectionMap}", key="lazyConnectionEnlistment", value="#{pageSession.valueMap['lazyConnectionEnlistment']}");
            gf.updateEntity(endpoint="#{pageSession.selfUrl}"
                        attrs="#{lazyConnectionMap}"
                        convertToFalse={"lazyConnectionEnlistment"});
            gf.filterMap(map="#{pageSession.valueMap}" attrNames={"lazyConnectionEnlistment"} keep="#{false}" resultMap="#{pageSession.valueMap}");
            
            gf.updateEntity(endpoint="#{pageSession.selfUrl}"  attrs="#{pageSession.valueMap}"
                skipAttrs="#{pageSession.skipAttrsList}"
                convertToFalse="#{pageSession.convertToFalseList}"
                onlyUseAttrs="#{pageSession.onlyUseAttrs}"
            );
            prepareSuccessfulMsg();
            if ("#{valueMap['ping']}") {
                urlencode(value="#{valueMap['name']}" encoding="UTF-8" result="#{pageSession.encodeName}");
                gf.createAttributeMap(keys={"id"} values={"$pageSession{encodeName}"} map="#{requestScope.attrMap}");
                if(#{pageSession.isAppScopedRes}){
                    mapPut(map="#{requestScope.attrMap}" key="appname" value="#{pageSession.encodedAppName}");
                    if(#{pageSession.moduleName}){
                        mapPut(map="#{requestScope.attrMap}" key="modulename" value="#{pageSession.encodedModuleName}");
                    }
                }
                gf.restRequest(endpoint="#{sessionScope.REST_URL}/resources/ping-connection-pool"
                               attrs="#{requestScope.attrMap}"
                               method="GET"
                               result="#{requestScope.result}");
                if ("#{requestScope.result.data['exit_code']} = SUCCESS") {
                     prepareAlertMsg(summary="$resource{i18n.msg.PingSucceed}", type="success");
                }
                if ("!(#{requestScope.result.data['exit_code']} = SUCCESS)") {
                     prepareAlertMsg(summary="$resource{i18njca.msg.warning.poolSavedPingFailed}", type="error");
                }                
            }
            gf.redirect(page="#{selfPage}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
        />
    </sun:button>

    <sun:button id="cancelButton" immediate="#{true}" primary="#{false}" text="$resource{i18n.button.Cancel}" >
        <!command
            gf.redirect(page="#{parentPage}");
        />
    </sun:button>
</sun:panelGroup>
</facet>
