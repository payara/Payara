<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.

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

<!-- javaConfig/jvmProfiler_2.inc -->

#include "/common/shared/alertMsg.inc"
                <!-- Page Title -->
                <sun:title id="propertyContentPage" title="$resource{i18nc.jvm.ProfilerPageName}"
                    helpText="$resource{i18nc.jvm.ProfilerPageHelp}">
    <!facet pageButtonsTop>
        <sun:panelGroup id="topButtons">
            <sun:button id="newButton" rendered="#{!edit}" text="$resource{i18nc.button.CreateProfiler}"
                    onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')){
                        disableAllButtons();
                        return submitAndDisable(this, '$resource{i18n.button.Processing}');}
                        else {return false;}" >
                <!command
                    mapPut(map="#{pageSession.valueMap}" key="target" value="#{pageSession.configName}");
                    getUIComponent(clientId="$pageSession{propertyTableRowGroupId}", component=>$attribute{tableRowGroup});
                    getAllSingleMapRows(TableRowGroup="$attribute{tableRowGroup}",  Rows=>$attribute{newList});
                    foreach(var="oneRow" list="#{requestScope.newList}"){
                        startsWith(testStr="#{requestScope.oneRow.value}" pattern="-" result="#{requestScope.valid}")
                        if ("!#{requestScope.valid}"){
                            gf.handleError( detail="$resource{i18nc.msg.jvmOptionInvalid, #{requestScope.oneRow.value}}");
                        }
                    }
                    
                    gf.createEntity(endpoint="#{pageSession.createProfilerUrl}"
                            attrs="#{pageSession.valueMap}"
                            convertToFalse="#{pageSession.convertToFalseList}"
                            onlyUseAttrs="#{pageSession.onlyUseAttrs}"
                            result => $attribute{objStr});
                    prepareAlertMsg(type="success", summary="$resource{i18nc.jvm.ProfilerCreated}");
                    saveJvmOptionValues(endpoint="#{pageSession.profilerUrl}/jvm-options", target="#{pageSession.configName}", options="$attribute{newList}" ,
                        deleteProfileEndpoint="#{pageSession.deleteProfilerUrl}" profiler="true");
                    gf.redirect(page="#{selfPage}?alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}&name=#{pageSession.Name}&configName=#{pageSession.configName}");
                />
            </sun:button>
            <sun:button id="saveButton" rendered="#{edit}" text="$resource{i18n.button.Save}"
                    onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')){
                        disableAllButtons();
                        return submitAndDisable(this, '$resource{i18n.button.Processing}');}
                        else {return false;}" >
                <!command
                    gf.createEntity(endpoint="#{pageSession.profilerUrl}" attrs="#{pageSession.valueMap}"
                            skipAttrs="#{pageSession.skipAttrsList}"
                            convertToFalse="#{pageSession.convertToFalseList}"
                            onlyUseAttrs="#{pageSession.onlyUseAttrs}");
                    getUIComponent(clientId="$pageSession{propertyTableRowGroupId}", component=>$attribute{tableRowGroup});
                    getAllSingleMapRows(TableRowGroup="$attribute{tableRowGroup}",  Rows=>$attribute{newList});
                    saveJvmOptionValues(endpoint="#{pageSession.profilerUrl}/jvm-options", target="#{pageSession.configName}", options="$attribute{newList}" , origList="#{pageSession.origList}" profiler="true");
                    prepareSuccessfulMsg();
                    gf.redirect(page="#{selfPage}?alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}&name=#{pageSession.Name}&configName=#{pageSession.configName}");
                />
            </sun:button>

            <sun:button id="deleteButton" rendered="#{edit}" text="$resource{i18nc.button.DeleteProfiler}"
                onClick="if ( getConfirm(this,'$pageSession{confirmDeleteMsg}') )
                    { disableAllButtons(); return submitAndDisable(this, '$resource{i18n.button.Processing}');}
                    else {return false;} " >
                <!command
                    createMap(result="#{pageSession.attrMap}");
                    mapPut(map="#{pageSession.attrMap}" key="target" value="#{pageSession.configName}");
                    gf.restRequest(endpoint="#{pageSession.deleteProfilerUrl}", attrs="#{pageSession.attrMap}", method="delete");
                    prepareAlertMsg(type="success", summary="$resource{i18nc.jvm.ProfilerDeleted}");
                    gf.redirect(page="#{selfPage}?configName=#{pageSession.configName}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
                />
            </sun:button>

        </sun:panelGroup>
    </facet>
        </sun:title>
        <!-- PropertySheet .... -->
        <sun:propertySheet id="propertySheet">
            <!-- Text Field section -->
#include "/common/shared/configNameSection.inc"

            <sun:propertySheetSection id="propertSectionTextField">
                <sun:property id="profilerNameProp" rendered="#{!edit}"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.jvm.ProfilerName}">
                    <sun:textField id="ProfilerName"  styleClass="required" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.jvm.ProfilerName']}" text="#{pageSession.valueMap['name']}" required="#{true}"/>
                </sun:property>
                <sun:property id="profilerNamePropEdit" rendered="#{edit}"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.jvm.ProfilerName}">
                    <sun:staticText id="ProfilerName2" text="#{pageSession.valueMap['name']}"/>
                </sun:property>
               <sun:property id="profilerEnabledProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.status}">
                    <sun:checkbox label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap['enabled']}" selectedValue="true" />
               </sun:property>
                <sun:property id="ClasspathProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.jvm.Classpath}" helpText="$resource{i18nc.jvm.ClasspathHelp}">
                    <sun:textField id="ClasspathLabel" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.jvm.Classpath']}"  text="#{pageSession.valueMap['classpath']}" />
                </sun:property>
                <sun:property id="NativeLibraryProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.jvm.NativeLibraryPath}" helpText="$resource{i18nc.jvm.NativeLibraryPathHelp}">
                    <sun:textField id="NativeLibrary" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.jvm.NativeLibraryPath']}" text="#{pageSession.valueMap['nativeLibraryPath']}"/>
                </sun:property>
                "<br /><br />
            </sun:propertySheetSection>
        </sun:propertySheet>
"<br/>
#include "/common/javaConfig/jvmOptionsTable.inc"
                
	<sun:hidden id="helpKey" value="$resource{help_common.serverInstProfiler}" />
            </sun:form>
#include "/common/shared/changeButtonsJS.inc"
</define>
</composition>
