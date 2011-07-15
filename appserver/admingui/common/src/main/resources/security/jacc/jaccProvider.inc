<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.

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
		 
        <sun:propertySheetSection id="propertSectionTextField">
            <sun:property id="IdStaticProp"  rendered="#{edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.Name}" >
                <sun:staticText id="IdStatic" text="#{pageSession.valueMap['name']}"/>
            </sun:property> 
            <sun:property id="IdTextProp"  rendered="#{!edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.Name}" helpText="$resource{i18nc.jacc.NameHelp}">
                <sun:textField id="IdText" text="#{pageSession.valueMap['name']}" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.common.Name']}" styleClass="required" required="#{true}" />
            </sun:property> 
            <sun:property id="policyConfigProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.jacc.PolicyConfig}" helpText="$resource{i18nc.jacc.PolicyConfigHelp}">
                <sun:textField id="PolicyConfig" columns="$int{75}" maxLength="#{sessionScope.fieldLengths['maxLength.jacc.PolicyConfig']}" text="#{pageSession.valueMap['policyConfigurationFactoryProvider']}" styleClass="required" required="#{true}"/>
            </sun:property>      
            <sun:property id="policyProviderProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.jacc.PolicyProvider}" helpText="$resource{i18nc.jacc.PolicyProviderHelp}">
                <sun:textField id="PolicyProvider" columns="$int{75}" maxLength="#{sessionScope.fieldLengths['maxLength.jacc.PolicyProvider']}" text="#{pageSession.valueMap['policyProvider']}" styleClass="required" required="#{true}"/>
            </sun:property>      
        </sun:propertySheetSection>
</sun:propertySheet>    

