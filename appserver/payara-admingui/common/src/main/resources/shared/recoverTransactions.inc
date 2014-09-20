<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.

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

<sun:form id="propertyForm">
#include "/common/shared/alertMsg.inc"
    <sun:title id="propertyContentPage" title="$resource{i18nc.recoverTransactionsTitle}"   helpText="$resource{i18nc.recoverTransactionsTitleHelp}">
        <!facet pageButtonsTop>
            <sun:panelGroup id="topButtons">
                <sun:button id="saveButton" text="$resource{i18n.button.OK}"
                     onClick=" if (checkTxDir('#{pageSession.targetId}', '#{pageSession.instanceName}', '#{pageSession.logDirId}' , '$resource{i18nc.msg.JS.recoverTransactions.enterLogDir}'))
                                {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;"  >
                    <!command
                        createMap(result="#{requestScope.attrs}");
                        mapPut(map="#{requestScope.attrs}", key="server_name", value="#{pageSession.instanceName}");
                        if("!#{useSelf}"){
                            mapPut(map="#{requestScope.attrs}", key="transactionlogdir", value="#{pageSession.txnLogDir}");
                            mapPut(map="#{requestScope.attrs}", key="target", value="#{pageSession.target}");
                        }
                        gf.restRequest(endpoint="#{pageSession.resourceUrl}/recover-transactions", attrs="#{requestScope.attrs}", method="post", result="#{requestScope.results}");
                        prepareAlertMsg(type="success", summary="$resource{i18nc.msg.recoverTransactionsSuccessful}");
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
    </sun:title>


    <sun:propertySheet id="propertySheet">

        <sun:propertySheetSection id="propertSectionTextField" >
            <sun:property id="selfRecovery" rendered="#{useSelf}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.recoverTransactions.selfRecover}" >
                <sun:staticText id="descTarget" text="#{pageSession.instanceName}" />
            </sun:property>

            <sun:property id="sourceName" rendered="#{!useSelf}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.recoverTransactions.instanceName}" >
                <sun:staticText id="sourceName" text="#{pageSession.instanceName}" />
            </sun:property>

            <sun:property id="instanceDestProp" rendered="#{!useSelf}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.destServer}"
                helpText="$resource{i18nc.destServerHelp}">
                <sun:dropDown id="target" selected="#{pageSession.target}" required="#{true}" labels="#{pageSession.runningInstances}"
                      onChange="javascript:showTxnLogDir(this.value, '#{pageSession.instanceName}', '#{pageSession.logDirId}');"
                 >
                <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.targetId}");
                    setPageSessionAttribute(key='target', value="#{pageSession.runningInstances.get(0)}");
                />
                </sun:dropDown>
            </sun:property>
            <sun:property id="txnLogProp" rendered="#{!useSelf}"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.txnLogDir}"
                    helpText="$resource{i18nc.txnLogDirHelp}">
                <sun:textField id="txnLogDir" text="#{pageSession.txnLogDir}" required="#{true}" styleClass="required"  columns="$int{60}"  maxLength="#{sessionScope.fieldLengths['maxLength.recoverTransaction.transactionlogdir']}" >
                    <!afterCreate
                        getClientId(component="$this{component}" clientId="#{pageSession.logDirId}");
                    />
                </sun:textField>
            </sun:property>
        </sun:propertySheetSection>
    </sun:propertySheet>

 <sun:hidden id="helpKey" value="#{pageSession.helpKey}" />

</sun:form>
<f:verbatim>
    <script type="text/javascript">
    function checkTxDir(targetId, instanceName, logDirId, errorMsg){
        var el = document.getElementById(targetId);
        if (el == null) {
            return true;
        }

        var targetName = el.value;
        var logDir = document.getElementById(logDirId);
        var logDirValue = logDir.value;
        if(targetName == instanceName){
            disableComponent(logDirId, 'text');
            return true;     //logDir is not required.
        }else{
            if (logDirValue==null || logDirValue==''){
                logDir.select();
                logDir.focus();
                alert(errorMsg);
                return false;
            }
            return true;
       }
    }

    function showTxnLogDir(targetName, instanceName, logDirId){
        if (logDirId==null || logDirId==''){
            return;
        }
        if (targetName == instanceName){
            disableComponent(logDirId, 'text');
        }else{
            enableComponent(logDirId, 'text');
        }

    }
    </script>
</f:verbatim>
