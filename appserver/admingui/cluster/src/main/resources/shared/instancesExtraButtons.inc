<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.

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

<!-- cluster/shared/instancesExtraButtons.inc -->

<!define name="listTableDeleteButton">
    <sun:button id="button1" text="$resource{i18n.button.Delete}" disabled="#{true}" primary="#{false}"
            onClick="if ( getConfirm(this,'$pageSession{confirmDeleteMsg}') )
                { return submitAndDisable(this, '$resource{i18n.button.Processing}');}  else {return false;} " >
        <!command
            getUIComponent(clientId="$pageSession{tableRowGroupId}", component="#{requestScope.tableRowGroup}");
            getSelectedSingleMapRows(TableRowGroup="${tableRowGroup}" selectedRows="#{requestScope.selectedRows}");
            gf.instanceAction(rows="#{requestScope.selectedRows}", action="delete-instance" );
            gf.redirect(page="#{pageSession.listLink}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
        />
    </sun:button>
</define>

<!define name="extraButton2">
   <sun:button id="button2" text="$resource{i18n.button.Start}" disabled="#{true}" primary="#{false}"
        onClick="if ( getConfirm(this,'$resource{i18ncs.msg.JS.confirmStartInstances}') )
            { return submitAndDisable(this, '$resource{i18n.button.Processing}');}  else {return false;} " >
        <!command
            getUIComponent(clientId="$pageSession{tableRowGroupId}", component="#{requestScope.tableRowGroup}");
            getSelectedSingleMapRows(TableRowGroup="${tableRowGroup}" selectedRows="#{requestScope.selectedRows}");
            gf.instanceAction(rows="#{requestScope.selectedRows}", action="start-instance");
            gf.redirect(page="#{pageSession.listLink}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
        />
    </sun:button>
</define>

<!define name="extraButton3">
    <sun:button id="button3" text="$resource{i18n.button.Stop}" disabled="#{true}" primary="#{false}"
            onClick="if ( getConfirm(this,'$resource{i18ncs.msg.JS.confirmStopInstances}') )
                    { return submitAndDisable(this, '$resource{i18n.button.Processing}');}  else {return false;} " >
        <!command
            getUIComponent(clientId="$pageSession{tableRowGroupId}", component="#{requestScope.tableRowGroup}");
            getSelectedSingleMapRows(TableRowGroup="${tableRowGroup}" selectedRows="#{requestScope.selectedRows}");
            gf.instanceAction(rows="#{requestScope.selectedRows}", action="stop-instance");
            gf.redirect(page="#{pageSession.listLink}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
         />
    </sun:button>
</define>

<!define name="extraDropDown">
    <sun:dropDown id="dropdown1" rendered="#{pageSession.clusterInstancesPage}" submitForm="#{true}"  disabled="#{false}" toolTip="$resource{i18n.common.moreActionDropDown}"
            labels={ "$resource{i18n.common.moreActionDropDown}"
                    "$resource{i18n.button.Enable}"  "$resource{i18n.button.Disable}"
                    "$resource{i18n.targetListing.table.lbEnable}"  "$resource{i18n.targetListing.table.lbDisable}"
                 }
            values={"" "enable" "disable" "lb-enable" "lb-disable" } >
            <!command
                getUIComponent(clientId="#{tableRowGroupId}", component=>$attribute{tableRowGroup});
                getSelectedSingleMapRows(TableRowGroup="$attribute{tableRowGroup}" selectedRows=>$attribute{selectedRows});
                setAttribute(key="click" value="$this{component}");
                setAttribute(key="actionValue" value="#{click.selected}")

                if( ${actionValue}=enable){
                    gf.changeClusterStatus(selectedRows="${selectedRows}"  clusterName="#{pageSession.clusterName}" Enabled="#{true}" forLB="$boolean{false}")
                }
                if( ${actionValue}=disable){
                    gf.changeClusterStatus(selectedRows="${selectedRows}"  clusterName="#{pageSession.clusterName}" Enabled="#{false}" forLB="$boolean{false}")
                }
                if( ${actionValue}=lb-enable){
                    gf.changeClusterStatus(selectedRows="${selectedRows}" clusterName="#{pageSession.clusterName}" Enabled="#{true}" forLB="$boolean{true}")
                }
                if( ${actionValue}=lb-disable){
                    gf.changeClusterStatus(selectedRows="${selectedRows}"  clusterName="#{pageSession.clusterName}" Enabled="#{false}" forLB="$boolean{true}")
                }
                gf.redirect(page="#{listLink}");
            />
    </sun:dropDown>
</define>
