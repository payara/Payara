<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.

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

<!-- jdbc/jdbcConnectionPoolNew2Buttons.inc -->
<!facet pageButtonsTop>
  <sun:panelGroup id="topButtons">
    <sun:button id="previousButton" primary="#{false}"
        text="$resource{i18n.button.Previous}"
        onClick="document.getElementById('form2:validate').value='false'; return submitAndDisable(this, '$resource{i18n.button.Processing}');" >
        <!command
            getUIComponent(clientId="$pageSession{propertyTableRowGroupId}", component=>$attribute{tableRowGroup});
            getAllSingleMapRows(TableRowGroup="$attribute{tableRowGroup}",  Rows=>$attribute{newList});
            setSessionAttribute(key="wizardPoolProperties", value="$attribute{newList}");
            setAttribute(key="fromStep2" value="#{true}")
            gf.navigate(page="#{request.contextPath}/jdbc/#{step1Page}");
        />
    </sun:button>

    <sun:button id="finishButton" text="$resource{i18n.button.Finish}"
        onClick="document.getElementById('form2:validate').value='true';
            if (isClassNamePresent('#{wizardPoolExtra.datasourceClassname}', '#{wizardPoolExtra.driverClassname}', '#{datasourceClassNameFieldId}', '#{driverClassNameFieldId}', '$resource{i18njdbc.msg.Error.classNameCannotBeEmpty}')
            && guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
       <!command
            updateJdbcConnectionPoolWizardStep2();
            setAttribute(key="pingValue" value="#{wizardMap['ping']}");
            mapPut(map="#{wizardMap}" key="ping" value="false");
            gf.createEntity(endpoint="#{pageSession.parentUrl}/#{pageSession.childType}",
                    attrs="#{wizardMap}"
                    convertToFalse="#{pageSession.convertToFalseList}"
                    result => $attribute{resultEndPoint});
            if (#{pageSession.hasPropertyTable}){
                getUIComponent(clientId="$pageSession{propertyTableRowGroupId}", component=>$attribute{tableRowGroup});
                getAllSingleMapRows(TableRowGroup="$attribute{tableRowGroup}",  Rows=>$attribute{newList});
                removeEmptyProps(props="#{newList}" modifiedProps="#{newList}");
                javaToJSON(obj="#{newList}" json="#{requestScope.tmpJSON}");
                urlencode(value="#{wizardMap['name']}" encoding="UTF-8" result="#{pageSession.encodeName}");
                gf.restRequest(endpoint="#{pageSession.parentUrl}/#{pageSession.childType}/#{pageSession.encodeName}/property.json"
                               method="POST"
                               data="#{requestScope.tmpJSON}"
                               result="#{requestScope.restResponse}");
            }
            if ("#{pingValue}") {
                urlencode(value="#{wizardMap['name']}" encoding="UTF-8" result="#{pageSession.encodedName}");
                gf.createAttributeMap(keys={"id"} values={"$pageSession{encodedName}"} map="#{requestScope.attrMap}");
                gf.restRequest(endpoint="#{sessionScope.REST_URL}/resources/ping-connection-pool.json"
                               attrs="#{requestScope.attrMap}"
                               method="get"
                               throwException="#{false}"
                               result="#{requestScope.result}");
                if ("#{requestScope.result.data['exit_code']} = SUCCESS") {
                     prepareAlertMsg(summary="$resource{i18n.msg.PingSucceed}", type="success");
                }
                if ("!(#{requestScope.result.data['exit_code']} = SUCCESS)") {
                     prepareAlertMsg(summary="$resource{i18njdbc.msg.warning.poolCreatedPingFailed}", detail="#{requestScope.result.data.message}", type="warning");
                }
                setSessionAttribute(key="wizardMap" value="#{null}")
                setSessionAttribute(key="wizardPoolExtra" value="#{null}")
                setSessionAttribute(key="wizardPoolProperties" value="#{null}")
                gf.redirect(page="#{parentPage}?alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
            }
            if ("!#{pingValue}") {
                setSessionAttribute(key="wizardMap" value="#{null}")
                setSessionAttribute(key="wizardPoolExtra" value="#{null}")
                setSessionAttribute(key="wizardPoolProperties" value="#{null}")
                gf.redirect(page="#{parentPage}");
            }
        />    
    </sun:button>
    <sun:button id="cancelButton" immediate="#{true}" primary="#{false}" text="$resource{i18n.button.Cancel}" > 
       <!command
            setSessionAttribute(key="wizardMap" value="#{null}")
            setSessionAttribute(key="wizardPoolExtra" value="#{null}")
            setSessionAttribute(key="wizardPoolProperties" value="#{null}")
            gf.redirect(page="#{parentPage}");
        />    
    </sun:button>
  </sun:panelGroup>
</facet>
