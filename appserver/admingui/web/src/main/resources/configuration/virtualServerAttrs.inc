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

<!-- web/configuration/virtualServerAttrs.inc -->
#include "/common/applications/applicationHandlers.inc"
<sun:propertySheet id="propertySheet">
#include "/common/shared/configNameSection.inc"
    <sun:propertySheetSection id="propertSectionTextField">
        <sun:property id="IdStaticProp"  rendered="#{edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.vs.Id}" >
            <sun:staticText id="IdStatic" text="#{pageSession.Name}"/>
        </sun:property>

        <sun:property id="IdTextProp"  rendered="#{!edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.vs.Id}">
            <sun:textField id="IdText" styleClass="required" text="#{pageSession.valueMap['id']}" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.vs.Id']}" required="#{true}" />
        </sun:property>

        <sun:property id="hostsProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.vs.Hosts}" helpText="$resource{i18n_web.vs.HostsHelp}">
            <sun:textField id="Hosts" styleClass="required" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.vs.Hosts']}" text="#{pageSession.valueMap['hosts']}" required="#{true}"/>
        </sun:property>

        <sun:property id="stateProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.vs.State}">
            <sun:dropDown id="state"  selected="#{pageSession.valueMap['state']}" labels={"$resource{i18n_web.vs.StateOn}","$resource{i18n_web.vs.StateOff}","$resource{i18n_web.vs.StateDisabled}"} values={"on","off","disabled"} />
        </sun:property>

        <sun:property id="enableLog"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.vs.sso}">
            <sun:dropDown id="state"  selected="#{pageSession.valueMap['ssoEnabled']}" labels={"$resource{i18n_web.vs.controlByHS}","$resource{i18n_web.vs.alwaysEnable}","$resource{i18n_web.vs.alwaysDisable}"} values={"inherit","true","false"} />
        </sun:property>

        <sun:property id="ssoCookieHttpOnly"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.vs.ssoCookieHttpOnly}" helpText="$resource{i18n_web.vs.ssoCookieHttpOnlyHelp}">
            <sun:checkbox label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap['ssoCookieHttpOnly']}"  selectedValue="true" />
        </sun:property>

        <sun:property id="nwProps"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.vs.NetworkListeners}" helpText="$resource{i18n_web.vs.NetworkListenersHelp}">
            <sun:listbox id="nw" immediate="#{true}" multiple="#{true}"  rows="$int{4}" 
                labels="$pageSession{availableNetworkListeners}"
                selected="#{pageSession.selectedNetworkListeners}" >
                <!beforeCreate
                    gf.getChildrenNamesList(endpoint="#{sessionScope.REST_URL}/configs/config/#{pageSession.configName}/network-config/network-listeners/network-listener"
                          result="#{pageSession.networkListeners}");
                    getListBoxOptions(
                        availableList="#{pageSession.networkListeners}",
                        selectedCommaString="#{pageSession.valueMap['networkListeners']}",
                        availableListResult="#{pageSession.availableNetworkListeners}",
                        selectedOptions="#{pageSession.selectedNetworkListeners}"
                        addEmptyFirstChoice="true");
                />
                </sun:listbox>
            </sun:property>

        <sun:property id="webProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.vs.DefaultWeb}" helpText="$resource{i18n_web.vs.DefaultWebHelp}">
            <sun:dropDown id="Web" labels="$pageSession{modules}" selected="#{pageSession.valueMap['defaultWebModule']}" />
            <!beforeCreate
                setPageSessionAttribute(key="snifferType" value="web");
                gfr.getApplicationsBySnifferType();
                setAttribute(key="modList" value="#{pageSession.snifferTypeApplicationsList}");
                addEmptyFirstElement(in="$attribute{modList}"  out="#{pageSession.modules}")
            />
        </sun:property>
        <sun:property id="logFileProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.vs.LogFile}" helpText="$resource{i18n_web.vs.LogFileHelp}">
            <sun:textField id="LogFile" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.vs.LogFile']}" text="#{pageSession.valueMap['logFile']}" />
        </sun:property>

        <sun:property id="docroot"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.vs.docroot}" helpText="$resource{i18n_web.vs.docrootHelp}">
            <sun:textField id="docroot" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.vs.docroot']}" text="#{pageSession.valueMap['docroot']}" />
        </sun:property>

    </sun:propertySheetSection>

    <sun:propertySheetSection id="al" label="$resource{i18n_web.vs.accessLog}">

        <sun:property id="enableLog"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.vs.accessLogging}">
             <sun:dropDown id="log"  selected="#{pageSession.valueMap['accessLoggingEnabled']}" labels={"$resource{i18n_web.vs.controlByHS}","$resource{i18n_web.vs.alwaysEnable}","$resource{i18n_web.vs.alwaysDisable}"} values={"inherit","true","false"} />
         </sun:property>

        <sun:property id="accesslog"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.vs.path}" helpText="$resource{i18n_web.vs.pathHelp}">
            <sun:textField id="accesslog" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.vs.path']}" text="#{pageSession.valueMap['accessLog']}" />
        </sun:property>

    </sun:propertySheetSection>
</sun:propertySheet>
"<br><br>

