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

<!-- applications/depoyTableButtons.inc -->

<!facet actionsTop>
     <sun:panelGroup id="topActionsGroup1">
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{topActionGroup});
        />
        <sun:button id="button1" text="$resource{i18n.button.Enable}" disabled="#{true}" primary="#{false}" >
            <!command
                getUIComponent(clientId="$pageSession{tableRowGroupId}", component=>$attribute{tableRowGroup});
                getSelectedSingleMapRows(TableRowGroup="${tableRowGroup}" selectedRows=>$attribute{selectedRows});
                // enable the resource for a target
                foreach (var="row" list="#{requestScope.selectedRows}") {
                    gf.createAttributeMap(keys={"enabled"} values={"true"} map="#{requestScope.attrMap}");
                    urlencode(value="#{row['Name']}" result="#{requestScope.encodedResName}");
                    gf.restRequest(
                        endpoint="#{pageSession.restURL}/#{requestScope.encodedResName}"
                        method="GET"
                        result="#{requestScope.result}");
                    if ("#{requestScope.result.responseCode}=200") {
                        gf.restRequest(
                            endpoint="#{pageSession.restURL}/#{requestScope.encodedResName}"
                            attrs="#{requestScope.attrMap}"
                            method="POST"
                            result="#{requestScope.foo}");
                    }
                }
                setAttribute(key="selectedRows" value="${selectedRows}");
                gf.redirect(page="#{pageSession.listLink}&filterValue=#{pageSession.filterValue}");
            />
        </sun:button>
        <sun:button id="button2" text="$resource{i18n.button.Disable}" disabled="#{true}" primary="#{false}" >
            <!command
                getUIComponent(clientId="$pageSession{tableRowGroupId}", component=>$attribute{tableRowGroup});
                getSelectedSingleMapRows(TableRowGroup="${tableRowGroup}" selectedRows=>$attribute{selectedRows});
                // disable the resource for a target
                foreach (var="row" list="#{requestScope.selectedRows}") {
                    gf.createAttributeMap(keys={"enabled"} values={"false"} map="#{requestScope.attrMap}");
                    urlencode(value="#{row['Name']}" result="#{requestScope.encodedResName}");
                    gf.restRequest(
                        endpoint="#{pageSession.restURL}/#{requestScope.encodedResName}"
                        method="GET"
                        result="#{requestScope.result}");
                    if ("#{requestScope.result.responseCode}=200") {
                        gf.restRequest(
                            endpoint="#{pageSession.restURL}/#{requestScope.encodedResName}"
                            attrs="#{requestScope.attrMap}"
                            method="POST"
                            result="#{requestScope.foo}");
                    }
                }
                setAttribute(key="selectedRows" value="${selectedRows}");
                gf.redirect(page="#{pageSession.listLink}&filterValue=#{pageSession.filterValue}");
            />
        </sun:button>
        
        <sun:image id="actionTopSeparator1" 
           align="top"
           height="$int{18}"
           width="$int{18}"
           url="#{request.contextPath}/theme/com/sun/webui/jsf/suntheme/images/table/actions_separator.gif"
        />
        <sun:dropDown id="actions" selected="#{actionValue}" submitForm="#{true}" immediate="#{true}" toolTip="$resource{i18n.common.newDropDown}"
                      labels="$attribute{labels}" values="$attribute{values}"
                >
              <!beforeCreate
                    getContentOfIntegrationPoints(type="org.glassfish.admingui:resourceTypeDropdown" labels="#{requestScope.labels}" values="#{requestScope.values}" );
                    listAdd(list="#{requestScope.labels}" value="$resource{i18n.common.newDropDown}",  index="0");
                    listAdd(list="#{requestScope.values}" value=""  index="0" );
                />
                <!command
                    setAttribute(key="click" value="$this{component}");
                    setAttribute(key="actionValue" value="#{click.selected}");
                    gf.redirect(page="#{request.contextPath}/#{click.selected}?target=#{pageSession.target}" );
                />
            </sun:dropDown>

            <sun:dropDown id="filter" selected="$pageSession{filterValue}" label="$resource{i18n.common.filterDropDown}" submitForm="#{true}" immediate="#{true}"
                labels="$attribute{filterLabels}" values="$attribute{filterValues}">
                <!beforeCreate
                    getContentOfIntegrationPoints(type="org.glassfish.admingui:resFilterDropdown" labels="#{requestScope.filterLabels}" values="#{requestScope.filterValues}" );
                    listAdd(list="#{requestScope.filterLabels}" value="$resource{i18n.common.showAll}",  index="0", sort="true");
                    listAdd(list="#{requestScope.filterValues}" value=""  index="0" );
                />
                <!command
                    setAttribute(key="click" value="$this{component}");
                    setAttribute(key="filterValue" value="#{click.selected}");
                    gf.redirect(page="$pageSession{listLink}&filterValue=${filterValue}" );
                />
            </sun:dropDown>
       </sun:panelGroup>
 </facet>
