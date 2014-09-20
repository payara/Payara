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

<!-- grizzly/networkListenerAttr.inc -->


<sun:propertySheet id="propertySheet">
#include "/common/shared/configNameSection.inc"
    <!-- Text Field section -->
    <sun:propertySheetSection id="propertSectionTextField">

        <sun:property id="nameNew"   labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.common.name}" helpText="$resource{i18n_web.common.nameHelp}">
            <sun:textField id="name"  required="#{true}" columns="$int{50}" maxLength="#{sessionScope.fieldLengths['maxLength.common.Name']}" text="#{pageSession.valueMap['name']}"
                onChange="setProtocolName()" >
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{listenerId});
                />
            </sun:textField>
        </sun:property>


    <sun:property id="prop1" labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18n_web.grizzly.protocol}">
    <sun:radioButton id="rdBtn" name="rdBtn" selected="#{pageSession.rdBtn}" selectedValue="create" onClick="radioBtnAction('create');setProtocolName();"  >
      <!beforeCreate
            setPageSessionAttribute(key="rdBtn", value="create");
      />
   </sun:radioButton>
   <sun:staticText text="$resource{i18n_web.grizzly.networkListener.createNewProtocol}" />
   <sun:textField id="pName" required="#{false}" text="#{pageSession.valueMap['newProtocolName']}" submitForm="false" style="margin-left: 4pt" columns="$int{50}" maxLength="#{sessionScope.fieldLengths['maxLength.grizzly.networkListener.newProtocolName']}"  >
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{pNameId});
        />
    </sun:textField>

        "<br/><br/>
        <sun:staticText text="$resource{i18n_web.http.defVirtualServerLabel}" style="margin-left: 12pt" />
        <sun:dropDown id="vs" selected="#{pageSession.valueMap['defaultVirtualServer']}" labels="$pageSession{vsList}"  values="$pageSession{vsList}" style="margin-left: 4pt" >
        <!beforeCreate
            gf.getChildrenNamesList(endpoint="#{sessionScope.REST_URL}/configs/config/#{pageSession.configName}/http-service/virtual-server" result="#{pageSession.vsList}");
            mapPut(map="#{pageSession.valueMap}" key="defaultVirtualServer"  value="server")
        />
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{newVsId});
        />
       </sun:dropDown>

   "<br/><br/><br/>

    <sun:radioButton id="existingRdBtn" name="rdBtn"
        selected="#{pageSession.rdBtn}"  selectedValue="existing"  onClick="radioBtnAction('existing')"/>

        <sun:staticText text="$resource{i18n_web.grizzly.networkListener.useExistingProtocol}" />
        <sun:dropDown id="protocoldw" selected="#{pageSession.valueMap['existingProtocolName']}"  disabled="true" style="margin-left: 4pt" labels="$pageSession{protocolListLabel}" >
            <!beforeCreate
                gf.getChildrenNamesList(endpoint="#{sessionScope.REST_URL}/configs/config/#{pageSession.configName}/network-config/protocols/protocol" result="#{pageSession.protocolListLabel}");
            />
            <!afterCreate
                getClientId(component="$this{component}" clientId=>$page{PdropDownId});
            />
        </sun:dropDown>

        <sun:textField  id="protocolChoice" visible="$boolean{false}" text="#{pageSession.valueMap['protocolChoice']}">
            <!beforeCreate
                mapPut(map="#{pageSession.valueMap}" value="create"  key="protocolChoice")
            />
            <!afterCreate
                getClientId(component="$this{component}" clientId=>$page{protocolChoiceId});
            />
        </sun:textField>
        "<br/><br/><br/>
        </sun:property>

        <sun:property id="port"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.grizzly.networkListener.port}" helpText="$resource{i18n_web.grizzly.networkListener.portHelp}">
            <sun:textField id="port" columns="$int{40}" maxLength="#{sessionScope.fieldLengths['maxLength.grizzly.networkListener.port']}" required="#{true}" styleClass="required port" text="#{pageSession.valueMap['port']}"  />
        </sun:property>

        <sun:property id="addr"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.grizzly.networkListener.address}" helpText="$resource{i18n_web.grizzly.networkListener.addressHelp}">
            <sun:textField id="addr" columns="$int{40}" maxLength="#{sessionScope.fieldLengths['maxLength.grizzly.networkListener.address']}" text="#{pageSession.valueMap['address']}"  />
        </sun:property>

        <sun:property id="status"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.common.status}"  helpText="$resource{i18n_web.grizzly.networkListener.statusHelp}" >
            <sun:checkbox label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap['enabled']}" selectedValue="true" />
        </sun:property>

        <sun:property id="security"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.grizzly.networkListener.security}" helpText="$resource{i18n_web.grizzly.networkListener.securityHelp}" >
            <sun:checkbox label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap['securityEnabled']}" selectedValue="true" />
        </sun:property>

        <sun:property id="JkEnabled"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.grizzly.networkListener.JkEnabled}" helpText="$resource{i18n_web.grizzly.networkListener.JkEnabledHelp}">
            <sun:checkbox label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap['jkenabled']}" selectedValue="true" />
        </sun:property>

        <sun:property id="threadpool"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.grizzly.networkListener.threadPool}" helpText="$resource{i18n_web.grizzly.networkListener.threadPoolHelp}">
            <sun:dropDown id="threadpool" selected="#{pageSession.valueMap['threadpool']}" labels="$pageSession{threadPoolList}"  values="$pageSession{threadPoolList}" >
            <!beforeCreate
                gf.getChildrenNamesList(endpoint="#{sessionScope.REST_URL}/configs/config/#{pageSession.configName}/thread-pools/thread-pool" result="#{pageSession.threadPoolList}");
            />
           </sun:dropDown>
        </sun:property>

        <sun:property id="transport"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.grizzly.networkListener.transport}" helpText="$resource{i18n_web.grizzly.networkListener.transportHelp}">
            <sun:dropDown id="transport" selected="#{pageSession.valueMap['transport']}" labels="$pageSession{transportList}"  values="$pageSession{transportList}"  >
            <!beforeCreate
                gf.getChildrenNamesList(endpoint="#{sessionScope.REST_URL}/configs/config/#{pageSession.configName}/network-config/transports/transport" result="#{pageSession.transportList}");
            />
           </sun:dropDown>
        </sun:property>

<!--
        <sun:property id="protocol"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n_web.grizzly.networkListener.protocol}" helpText="$resource{i18n_web.grizzly.networkListener.protocolHelp}">
            <sun:dropDown id="protocolChoice" selected="#{pageSession.valueMap['protocol']}"
                labels="$pageSession{protocolListLabel}"  values="$pageSession{protocolListValue}"
                onChange="javascript: disableProtocolNameField();" >
            <!beforeCreate
                gf.getChildrenNamesList(endpoint="#{sessionScope.REST_URL}/configs/config/#{pageSession.configName}/network-config/protocols/protocol" result="#{pageSession.protocolListLabel}");
                gf.getChildrenNamesList(endpoint="#{sessionScope.REST_URL}/configs/config/#{pageSession.configName}/network-config/protocols/protocol" result="#{pageSession.protocolListValue}");
                listAdd(list="#{pageSession.protocolListLabel}" value="$resource{i18n_web.grizzly.createNewProtocol}" index="0")
                listAdd(list="#{pageSession.protocolListValue}" value="__create__" index="0")
            />
            <!afterCreate
                getClientId(component="$this{component}" clientId="#{pageSession.protocolChoice}");
            />
           </sun:dropDown>
        </sun:property>

        <sun:property id="protocolNm"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18_web.grizzly.protocol.protocolName}" helpText="$resource{i18n_web.grizzly.protocol.protocolNameHelp}">
            <sun:textField id="protocolNm"  columns="$int{55}" text="#{pageSession.valueMap['newProtocolName']}" >
            <!afterCreate
                getClientId(component="$this{component}" clientId="#{pageSession.protocolNmField}");
            />
            </sun:textField>
        </sun:property>
-->

    </sun:propertySheetSection>
</sun:propertySheet>
