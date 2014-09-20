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

<!-- applications/depoyTableButtons.inc -->

<!facet actionsTop>
     <sun:panelGroup id="topActionsGroup1">
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{topActionGroup});
        />
        <sun:button id="deployButton" text="$resource{i18n.button.Deploy}" primary="#{true}">
                <!command
		    gf.redirect(page="#{pageSession.nextPageLink}") />
        </sun:button>
        <sun:button id="button1" text="$resource{i18n.button.Undeploy}" disabled="#{true}" primary="#{false}"
                onClick="if ( getConfirm(this,'$pageSession{confirmDeleteMsg}') )
                    { return submitAndDisable(this, '$resource{i18n.button.Processing}');}
                    else {return false;} " >
            <!command
                getUIComponent(clientId="$pageSession{tableRowGroupId}", component=>$attribute{tableRowGroup});
                getSelectedSingleMapRows(TableRowGroup="$attribute{tableRowGroup}" selectedRows=>$attribute{selectedRows});
                gf.undeploy(selectedRows="${selectedRows}" );
                gf.navigate(page="#{listPageLink}");
            />
         </sun:button>
        <sun:button id="button2" text="$resource{i18n.button.Enable}" rendered="#{pageSession.onlyDASExist}" disabled="#{true}" primary="#{false}"
                onClick="return submitAndDisable(this, '$resource{i18n.button.Processing}');" >
        <!command
                getUIComponent(clientId="$pageSession{tableRowGroupId}", component=>$attribute{tableRowGroup});
                getSelectedSingleMapRows(TableRowGroup="$attribute{tableRowGroup}" selectedRows=>$attribute{selectedRows});
                prepareAlertMsg(type="success" summary="$resource{i18n.msg.enableSuccessfulPE}")
                foreach (var="row" list="#{requestScope.selectedRows}") {
                   urlencode(value="#{row['name']}" result="#{requestScope.encodedName}");
                   createMap(result="#{requestScope.attrMap}");
                   mapPut(map="#{requestScope.attrMap}" key="target" value="server");
                   gf.restRequest(
                        endpoint="#{sessionScope.REST_URL}/applications/application/#{requestScope.encodedName}/enable"
                        attrs="#{requestScope.attrMap}"
                        method="POST");
                }
                gf.navigate(page="#{listPageLink}");
            />
            </sun:button>
        <sun:button id="button3" text="$resource{i18n.button.Disable}" rendered="#{pageSession.onlyDASExist}" disabled="#{true}" primary="#{false}"
            onClick="return submitAndDisable(this, '$resource{i18n.button.Processing}');" >
        <!command
                getUIComponent(clientId="$pageSession{tableRowGroupId}", component=>$attribute{tableRowGroup});
                getSelectedSingleMapRows(TableRowGroup="$attribute{tableRowGroup}" selectedRows=>$attribute{selectedRows});
                prepareAlertMsg(type="success" summary="$resource{i18n.msg.disableSuccessfulPE}")
                foreach (var="row" list="#{requestScope.selectedRows}") {
                   urlencode(value="#{row['name']}" result="#{requestScope.encodedName}");
                   createMap(result="#{requestScope.attrMap}");
                   mapPut(map="#{requestScope.attrMap}" key="target" value="server");
                   gf.restRequest(
                        endpoint="#{sessionScope.REST_URL}/applications/application/#{requestScope.encodedName}/disable"
                        attrs="#{requestScope.attrMap}"
                        method="POST");
                }
                gf.navigate(page="#{listPageLink}");
            />
        </sun:button>

        <sun:image id="actionTopSeparator1" 
           align="top"
           height="$int{18}"
           width="$int{18}"
           url="#{request.contextPath}/theme/com/sun/webui/jsf/suntheme/images/table/actions_separator.gif"
        />

       <sun:dropDown id="filter"  selected="#{filterValue}" label="$resource{i18n.common.filterDropDown}" submitForm="true" immediate="true" values="${filters}" labels="${filters}" onChange="return admingui.woodstock.dropDownChanged(this);">
            <!command
                setAttribute(key="click" value="$this{component}");
                setAttribute(key="filterValue" value="#{click.selected}");
                gf.redirect(page="#{request.contextPath}/#{pageSession.listPageLink}?filterValue=${filterValue}" );
            />
        </sun:dropDown>

     </sun:panelGroup>
 </facet>
