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

<!-- full/adminObjectAttr.inc -->
<!-- used by adminObjectNew.jsf and adminObjectEdit.jsf -->
   

<sun:propertySheet id="propertySheet">
    <!-- Text Field section -->
    <sun:propertySheetSection id="propertSectionTextField">
        <sun:property id="name"  rendered="#{edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.jndiName}">
            <sun:staticText id="name" text="#{pageSession.name}"  rendered="#{Edit}" />
        </sun:property>
        <sun:property id="nameNew"  rendered="#{!edit}"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.jndiName}">
            <sun:textField id="name" styleClass="required"  required="#{true}" columns="$int{30}" maxLength="#{sessionScope.fieldLengths['maxLength.common.jndiName']}" text="#{pageSession.valueMap['name']}" >
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{resCompId});
                />
            </sun:textField>
        </sun:property>
        
        <sun:property id="hostProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njmail.javaMail.mailHost}" helpText="$resource{i18njmail.javaMail.mailHostHelp}">
            <sun:textField id="host" styleClass="required" text="#{pageSession.valueMap['host']}" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.javaMail.mailHost']}" required="#{true}" /> 
        </sun:property>   

        <sun:property id="userProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njmail.javaMail.user}" helpText="$resource{i18njmail.javaMail.userHelp}">
            <sun:textField id="user" styleClass="required" text="#{pageSession.valueMap['user']}" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.javaMail.user']}" required="#{true}" /> 
        </sun:property>   

        <sun:property id="fromProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njmail.javaMail.returnAddr}" helpText="$resource{i18njmail.javaMail.returnAddrHelp}">
            <sun:textField id="from" styleClass="required" text="#{pageSession.valueMap['from']}" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.javaMail.returnAddr']}" required="#{true}" /> 
        </sun:property>   
        <sun:property id="deploymentOrder" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}"  rendered="#{edit}" label="$resource{i18n.common.resource.deploymentOrder}"  helpText="$resource{i18n.common.resource.deploymentOrderHelp}">
            <sun:textField id="deploymentOrder" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.common.deploymentOrder']}" text="#{pageSession.valueMap['deploymentOrder']}" />
        </sun:property>

        <sun:property id="descProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.description}" helpText="$resource{i18njmail.javaMail.descHelp}">
            <sun:textField id="desc" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.common.description']}" text="#{pageSession.valueMap['description']}" />      
        </sun:property>

        <sun:property id="statusProp" rendered="#{useCheckBox}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.status}">
            <sun:checkbox label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap['enabled']}" selectedValue="true" visible="#{false}" />
            <sun:checkbox id="enabled" label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap2['enabled']}" selectedValue="true" />
       </sun:property>
       <sun:property id="statusProp2" rendered="#{useString}"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}"
            label="$resource{i18n.common.status}" helpText="$resource{i18n.application.EnableTargetHelp}">
            <sun:staticText  id="enabledStr" text="#{requestScope.enabledString}" >
                <!beforeCreate
                    gf.getTargetEnableInfo(appName="#{pageSession.encodedName}" isApp="#{false}" status="#{requestScope.enabledString}");
                />
           </sun:staticText>
       </sun:property>
   
        "<br /><br />
    </sun:propertySheetSection>

    <sun:propertySheetSection id="section2" label="$resource{i18n.common.Advanced}" >
        <sun:property id="storeProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njmail.javaMail.storeProtocol}" helpText="$resource{i18njmail.javaMail.storeProtocolHelp}">
            <sun:textField id="storeP" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.javaMail.storeProtocol']}" text="#{pageSession.valueMap['storeProtocol']}" />      
        </sun:property>

        <sun:property id="storeClassProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njmail.javaMail.storeClass}" helpText="$resource{i18njmail.javaMail.storeClassHelp}">
            <sun:textField id="storeClass" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.javaMail.storeClass']}" text="#{pageSession.valueMap['storeProtocolClass']}" />      
        </sun:property>

        <sun:property id="transPProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njmail.javaMail.transportProtocol}" helpText="$resource{i18njmail.javaMail.transportProtocolHelp}">
            <sun:textField id="transP" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.javaMail.transportProtocol']}" text="#{pageSession.valueMap['transportProtocol']}" />      
        </sun:property>

        <sun:property id="transClassProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njmail.javaMail.transProtocolClass}" helpText="$resource{i18njmail.javaMail.transProtocolClassHelp}">
            <sun:textField id="transPClass" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.javaMail.transProtocolClass']}" text="#{pageSession.valueMap['transportProtocolClass']}" />      
        </sun:property>

        <sun:property id="debugProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njmail.javaMail.Debug}"  helpText="$resource{i18njmail.javaMail.DebugHelp}">
            <sun:checkbox selected="#{pageSession.valueMap['debug']}"  label="$resource{i18n.common.Enabled}" selectedValue="true" />
       </sun:property>   
        "<br /><br />
    </sun:propertySheetSection>
</sun:propertySheet> 
