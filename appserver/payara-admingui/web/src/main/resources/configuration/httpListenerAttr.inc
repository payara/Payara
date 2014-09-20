<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.

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

<sun:propertySheet id="propertySheet">
#include "/common/shared/configNameSection.inc"
    <!-- Text Field section -->
    <sun:propertySheetSection id="propertSectionTextField">

        <sun:property id="nameNew"  rendered="#{!edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.common.name}" helpText="$resource{i18n_web.common.nameHelp}">
            <sun:textField id="name"  required="#{true}" columns="$int{50}" maxLength="#{sessionScope.fieldLengths['maxLength.common.Name']}" text="#{pageSession.valueMap['name']}" />
        </sun:property>

        <sun:property id="name"  rendered="#{edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.common.name}">
            <sun:staticText id="name" text="#{pageSession.Name}"  rendered="#{Edit}" />
        </sun:property>
        
        <sun:property id="port"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.grizzly.networkListener.port}" helpText="$resource{i18n_web.grizzly.networkListener.portHelp}">
            <sun:textField id="port" columns="$int{40}" maxLength="#{sessionScope.fieldLengths['maxLength.grizzly.networkListener.port']}" required="#{true}" styleClass="required port" text="#{pageSession.valueMap['port']}"  />
        </sun:property>

        <sun:property id="status"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.common.status}"  helpText="$resource{i18n_web.grizzly.networkListener.statusHelp}" >
            <sun:checkbox label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap['enabled']}" selectedValue="true" />
        </sun:property>

        <sun:property id="security"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.grizzly.networkListener.security}" helpText="#{pageSession.secHelpText}" >
            <sun:checkbox disabled="#{readOnly}"  label="$resource{i18n.common.Enabled}" selected="#{pageSession.protocolMap['securityEnabled']}" selectedValue="true" />
        </sun:property>

        <sun:property id="JkEnabled"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.grizzly.networkListener.JkEnabled}" helpText="$resource{i18n_web.grizzly.networkListener.JkEnabledHelp}">
            <sun:checkbox label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap['jkEnabled']}" selectedValue="true" />
        </sun:property>
        
        <sun:property id="addr"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.grizzly.networkListener.address}" helpText="$resource{i18n_web.grizzly.networkListener.addressHelp}">
            <sun:textField id="addr" columns="$int{40}" maxLength="#{sessionScope.fieldLengths['maxLength.grizzly.networkListener.address']}" text="#{pageSession.valueMap['address']}"  />
        </sun:property>

        <sun:property id="vs"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.defVirtualServerLabel}" helpText="$resource{i18n_web.http.defVirtualServerLabelHelp}">
            <sun:dropDown id="vs" selected="#{pageSession.httpMap['defaultVirtualServer']}" labels="$pageSession{vsList}"  values="$pageSession{vsList}" >
            <!beforeCreate
                gf.getChildrenNamesList(endpoint="#{sessionScope.REST_URL}/configs/config/#{pageSession.configName}/http-service/virtual-server" result="#{pageSession.vsList}");

            />
           </sun:dropDown>
        </sun:property>
        <sun:property id="threadpool"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.grizzly.networkListener.threadPool}" helpText="$resource{i18n_web.grizzly.networkListener.threadPoolHelp}">
            <sun:dropDown id="threadpool" selected="#{pageSession.valueMap['threadPool']}" labels="$pageSession{threadPoolList}"  values="$pageSession{threadPoolList}" >
            <!beforeCreate
                gf.getChildrenNamesList(endpoint="#{sessionScope.REST_URL}/configs/config/#{pageSession.configName}/thread-pools/thread-pool" result="#{pageSession.threadPoolList}");
            />
           </sun:dropDown>
        </sun:property>

        <sun:property id="ServerNameProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.http.serverNameLabel}" helpText="$resource{i18n_web.http.serverNameHelp}" >
            <sun:textField id="ServerName" columns="$int{50}" maxLength="#{sessionScope.fieldLengths['maxLength.http.serverName']}" text="#{pageSession.httpMap['serverName']}" />
        </sun:property>

    </sun:propertySheetSection>
</sun:propertySheet>
