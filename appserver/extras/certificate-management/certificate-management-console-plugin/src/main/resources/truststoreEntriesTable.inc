<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.

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
<sun:table id="truststoreEntriesTable" title="$resource{i18ncm.certificateManagement.truststoreEntriesTable.title}"
           style="padding: 10pt"
           deselectMultipleButton="$boolean{true}"
           deselectMultipleButtonOnClick="setTimeout('admingui.table.changeThreeTableButtons(\\\\\'#{pageSession.trustEntriesTopActionGroup}\\\\\', \\\\\'#{pageSession.trustEntriesTableId}\\\\\');', 0)"
           selectMultipleButton="$boolean{true}"
           selectMultipleButtonOnClick="setTimeout('admingui.table.changeThreeTableButtons(\\\\\'#{pageSession.trustEntriesTopActionGroup}\\\\\', \\\\\'#{pageSession.trustEntriesTableId}\\\\\');', 0)">
    <!afterCreate
        getClientId(component="$this{component}" clientId=>$page{trustEntriesTableId});
    />

    <!facet actionsTop>
        <sun:panelGroup id="topActionsGroup1">
            <!afterCreate
                getClientId(component="$this{component}" clientId=>$page{trustEntriesTopActionGroup});
            />
            <sun:button id="addButton" text="$resource{i18ncm.certificateManagement.button.add}" >
                <!command gf.redirect(page="#{pageSession.addTruststoreEntryUrl}") />
            </sun:button>
            <!insert name="listTableDeleteButton">
                <sun:button id="button1" text="$resource{i18n.button.Delete}" disabled="#{true}" primary="#{false}"
                            onClick="if (getConfirm(this,'$resource{i18ncm.certificateManagement.truststoreEntriesTable.deleteMsg}')) {
                                        return submitAndDisable(this, '$resource{i18n.button.Processing}');
                                    } else {
                                        return false;
                                    }">
                    <!command
                        getUIComponent(clientId="$pageSession{trustEntriesTableRowGroupId}", component=>$attribute{trustEntriesTableRowGroup});
                        getAllSingleMapRows(TableRowGroup="${trustEntriesTableRowGroup}" Rows=>$attribute{trustEntriesSelectedRows});

                        foreach(var="row" list="${trustEntriesSelectedRows}"){
                            py.removeEntries(endpoint="#{sessionScope.REST_URL}/servers/server/#{pageSession.encodedInstanceName}/remove-truststore-entry"
                                    entry="#{requestScope.row}" response="#{pageSession.commandResponse}");

                            if("#{pageSession.commandResponse.data['exit_code']} = FAILURE") {
                                prepareAlertMsg(type="error", detail="#{pageSession.commandResponse.data.message}",
                                        summary="An error has occurred");
                                gf.redirect(page="#{request.selfUrl}?&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
                            }
                        }

                        py.prepareSuccessfulCommandMsg();
                        gf.redirect(page="#{pageSession.selfUrl}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
                    />
                </sun:button>
            </insert>
        </sun:panelGroup>
    </facet>
    <sun:tableRowGroup id="truststoreEntriesRowGroup" data={"$attribute{trustEntries}"} sourceVar="td"
                       emptyDataMsg="$resource{i18ncm.certificateManagement.truststoreEntriesTable.emptyDataMsg}">
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{trustEntriesTableRowGroupId});
        />
        <sun:tableColumn headerText="$resource{i18ncm.certificateManagement.select}"
                         selectId="select" rowHeader="$boolean{false}" id="col0">
            <sun:checkbox id="select" selectedValue="$boolean{true}"
                          selected="#{td.value.selected}"
                          onClick="setTimeout('admingui.table.changeOneTableButton(\\\\\'#{pageSession.trustEntriesTopActionGroup}\\\\\', \\\\\'#{pageSession.trustEntriesTableId}\\\\\'); admingui.table.initAllRows(\\\\\'#{pageSession.trustEntriesTableId}\\\\\');', 0);"/>
        </sun:tableColumn>
        <sun:tableColumn headerText="$resource{i18ncm.certificateManagement.alias}" sort="alias"
                         rowHeader="$boolean{true}" id="col1">
            <staticText id="alias" value="#{td.value.alias}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="$resource{i18ncm.certificateManagement.truststore}"
                         rowHeader="$boolean{true}" id="col2">
            <staticText id="truststore" value="#{td.value.store}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="$resource{i18ncm.certificateManagement.usedBy}"
                         rowHeader="$boolean{true}" id="col3">
            <event>
                <!beforeEncode
                    py.convertUsedByCsvToList(usedByString="#{td.value.usedBy}" result="#{usedByList}");
                />
            </event>
            "<table border="1">
                <foreach key="usedBy" list="#{requestScope.usedByList}">
                    <event>
                        <!beforeEncode
                            py.getUsedByLink(usedBy="#{usedBy}"
                                    usedByLinks="#{requestScope.trustEntriesUsedByLinks}"
                                    usedByLink="#{usedByLink}");
                        />
                    </event>
                    <sun:hyperlink id="usedByLinkId" url="#{usedByLink}" text="#{usedBy}" />
                    "<br />
                </foreach>
                "</table>
        </sun:tableColumn>
        <sun:tableColumn headerText="$resource{i18ncm.certificateManagement.entry}"
                         rowHeader="$boolean{true}" id="col4">
            <staticText id="entry" value="#{td.value.entry}" />
        </sun:tableColumn>
    </sun:tableRowGroup>
</sun:table>