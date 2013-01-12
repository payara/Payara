<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.

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

<!facet actionsTop>
    <sun:panelGroup id="topActionsGroup1">
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{topActionGroup});
        />
        <sun:button id="newButton" text="$resource{i18n.button.New}" >
            <!command gf.redirect(page="#{pageSession.createLink}") />
        </sun:button>

        <sun:button id="deleteConnButton" text="$resource{i18n.button.Delete}" disabled="#{true}" primary="#{false}" rendered="#{isConnPage}"
            onClick="if (getConfirm(this,'$pageSession{confirmDeleteMsg}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
            <!command
                getUIComponent(clientId="$pageSession{tableRowGroupId}", component=>$attribute{tableRowGroup});
                getSelectedSingleMapRows(TableRowGroup="${tableRowGroup}" selectedRows=>$attribute{selectedRows});
                gf.onlyDASExist(onlyDAS="#{pageSession.onlyDASExist}");
                setPageSessionAttribute(key="target", value="server");
                foreach (var="row1" list="#{requestScope.selectedRows}") {
                    if ("#{row1['objectType']}=system-all-req") {
                        gf.handleError( detail="$resource{i18njms.msg.jms.cannotDeleteDefault, #{row1['name']}}");
                    }
                }
                foreach (var="row" list="#{requestScope.selectedRows}") {
                    urlencode(value="#{row['name']}" result="#{requestScope.encodedResName}");
                    if (!#{pageSession.onlyDASExist}){
                        setAttribute(key="targetUrlsList" value="#{row['targetUrls']}");
                        foreach (var="targetUrl" list="#{requestScope.targetUrlsList}") {
                            createMap(result="#{requestScope.attrMap}");
                            mapGet(Map="#{row['targetsMap']}" Key="#{requestScope.targetUrl}" Value="#{requestScope.targetVal}");
                            mapPut(map="#{requestScope.attrMap}" key="target" value="#{targetVal}");
                            gf.restRequest(
                                endpoint="#{requestScope.targetUrl}/resource-ref/#{requestScope.encodedResName}"
                                attrs="#{requestScope.attrMap}"
                                method="DELETE"
                                result="#{requestScope.foo}");
                        }
                        setPageSessionAttribute(key="target", value="domain");
                    }
                    createMap(result="#{requestScope.delMap}");
                    mapPut(map="#{requestScope.delMap}" key="id" value="#{row['name']}");
                    mapPut(map="#{requestScope.delMap}" key="target" value="#{pageSession.target}");
                    gf.restRequest(
                        endpoint="#{sessionScope.REST_URL}/resources/delete-jms-resource"
                        attrs="#{requestScope.delMap}"
                        method="DELETE"
                        result="#{requestScope.foo}");
                    
                }
                setAttribute(key="selectedRows" value="${selectedRows}");
                gf.redirect(page="#{pageSession.listLink}");
            />
        </sun:button>

        <sun:button id="deleteDestButton" text="$resource{i18n.button.Delete}" disabled="#{true}" primary="#{false}" rendered="#{!isConnPage}"
            onClick="if (getConfirm(this,'$pageSession{confirmDeleteMsg}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
            <!command
                getUIComponent(clientId="$pageSession{tableRowGroupId}", component=>$attribute{tableRowGroup});
                getSelectedSingleMapRows(TableRowGroup="${tableRowGroup}" selectedRows=>$pageSession{selectedRows});
                gfr.deleteSelectedResources();
                gf.redirect(page="#{pageSession.listLink}");
            />
        </sun:button>

        <sun:button id="enableButton" text="$resource{i18n.button.Enable}" disabled="#{true}" primary="#{false}">
            <!command
                getUIComponent(clientId="$pageSession{tableRowGroupId}", component=>$attribute{tableRowGroup});
                getSelectedSingleMapRows(TableRowGroup="${tableRowGroup}" selectedRows=>$pageSession{selectedRows});
                gfr.enableSelectedResources();
                setAttribute(key="selectedRows" value="$pageSession{selectedRows}");
                prepareAlertMsg(summary="$resource{i18n.msg.enableResourceSuccessful}", type="success");
                gf.redirect(page="#{pageSession.listLink}?alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
            />
        </sun:button>

        <sun:button id="disableButton" text="$resource{i18n.button.Disable}" disabled="#{true}" primary="#{false}">
            <!command
                getUIComponent(clientId="$pageSession{tableRowGroupId}", component=>$attribute{tableRowGroup});
                getSelectedSingleMapRows(TableRowGroup="${tableRowGroup}" selectedRows=>$pageSession{selectedRows});
                gfr.disableSelectedResources();
                setAttribute(key="selectedRows" value="$pageSession{selectedRows}");
                prepareAlertMsg(summary="$resource{i18n.msg.disableResourceSuccessful}", type="success");
                gf.redirect(page="#{pageSession.listLink}?alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
            />
        </sun:button>
     </sun:panelGroup>
 </facet>
