<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.

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

<!-- configuration/providerConfig.inc -->    
<!-- Provider Configuration section -->               
<sun:propertySheetSection id="providerConfSection" label="#{pageSession.providerSectionLabel}" >

    <sun:property id="AuthLayerProp"  rendered="#{edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.msgSecurity.authLayerLabel}" >
        <sun:staticText id="AuthLayer" text="#{pageSession.msgSecurityName}" />
    </sun:property>

    <sun:property id="ProviderIdProp"  rendered="#{edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.msgSecProvider.ProviderIdLabel}" >
        <sun:staticText id="ProviderId" text="#{pageSession.valueMap['Name']}" />
    </sun:property>
    <sun:property id="ProviderIdTextProp"  rendered="#{!edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.msgSecProvider.ProviderIdLabel}">
        <sun:textField id="ProviderIdText" styleClass="required" text="#{pageSession.valueMap['Name']}" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.msgSecProvider.ProviderId']}" required="#{true}" />
        <sun:staticText text=""/>
    </sun:property>

    <sun:property >
        <sun:staticText text=" " />
    </sun:property>
    <!--  the defaultProvider attr is not in the <provider-config> element, User should set default provider
          in the msg security page itself during edit.
          During creation, the create command takes this option, so we can pass that in.
    -->
    <sun:property id="DefaultProviderProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.msgSecProvider.defaultProviderLabel}"
                   helpText="$resource{i18nc.msgSecProvider.defaultProviderLabeHelp}">
        <sun:checkbox id="def" selected="#{pageSession.valueMap['defaultProvider']}" selectedValue="true" />
    </sun:property>

    <sun:property id="ProviderTypeProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.msgSecProvider.providerTypeLabel}" helpText="$resource{i18nc.msgSecProvider.providerTypeHelp}">
        <sun:dropDown id="ProviderType" selected="#{pageSession.valueMap['ProviderType']}" labels={"client", "server", "client-server"}  required="#{true}" />
    </sun:property>                          
    
    <sun:property id="ClassNameProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.msgSecProvider.classNameLabel}" helpText="$resource{i18nc.msgSecProvider.classNameHelp}">
        <sun:textField id="ClassName" styleClass="required" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.msgSecProvider.className']}" text="#{pageSession.valueMap['ClassName']}" required="#{true}" />
    </sun:property>

</sun:propertySheetSection>                            


