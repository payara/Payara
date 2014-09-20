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
    <!-- Text Field section -->
    <sun:propertySheetSection id="propertSectionTextField">

    <sun:property id="IdStaticProp"  rendered="#{edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.Name}" >
        <sun:staticText id="IdStatic" text="#{pageSession.valueMap['name']}"/>
    </sun:property>

    <sun:property id="IdTextProp"  rendered="#{!edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.Name}">
        <sun:textField id="IdText" styleClass="required" text="#{pageSession.valueMap['name']}" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.common.Name']}" required="#{true}" />
    </sun:property>

    <sun:property id="classNameProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.lifecycleModule.classname}" helpText="$resource{i18nc.lifecycleModule.classnameHelp}" >
        <sun:textField id="classname" styleClass="required" columns="$int{70}" maxLength="#{sessionScope.fieldLengths['maxLength.lifecycleModule.classname']}" text="#{pageSession.valueMap['classname']}"  required="#{true}" />
    </sun:property> 

    <sun:property id="pathProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}"  label="$resource{i18nc.lifecycleModule.classPath}" helpText="$resource{i18nc.lifecycleModule.classPathHelp}" >
        <sun:textField id="classPath" columns="$int{70}" maxLength="#{sessionScope.fieldLengths['maxLength.lifecycleModule.classPath']}" text="#{pageSession.valueMap['classpath']}" />
    </sun:property> 

    <sun:property id="loadOrderProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.lifecycleModule.loadOrder}"  helpText="$resource{i18nc.lifecycleModule.loadOrderHelp}" >
        <sun:textField id="loadOrder" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.lifecycleModule.loadOrder']}" text="#{pageSession.valueMap['loadorder']}" />
    </sun:property> 

    <sun:property id="descProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.description}">
        <sun:textField id="desc" columns="$int{70}" maxLength="#{sessionScope.fieldLengths['maxLength.common.description']}" text="#{pageSession.valueMap['description']}" />
    </sun:property>

    <!-- Virtual Server doesn't apply to lifecycle module. refer to issue# 13620 -->

    <sun:property id="statusNew" rendered="#{!edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.status}">
        <sun:checkbox id="status" label="$resource{i18n.common.Enabled}"  selected="#{pageSession.valueMap['enabled']}" selectedValue="true" />
    </sun:property>

    <sun:property id="statusEdit"  rendered="#{edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.status}">
        <sun:checkbox id="status" rendered="#{pageSession.onlyDASExist}" label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap['enabled']}" selectedValue="true" />
        <sun:staticText id="statusText" rendered="#{!pageSession.onlyDASExist}" text="#{pageSession.status}" />
    </sun:property>

    <sun:property id="failProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.lifecycleModule.onLoadFailure}" helpText="$resource{i18nc.lifecycleModule.onLoadFailureHelp}" >
        <sun:checkbox label="$resource{i18nc.lifecycleModule.preventStart}" selected="#{pageSession.valueMap['failurefatal']}"  selectedValue="true" />
   </sun:property>

    </sun:propertySheetSection>
</sun:propertySheet>

    "<br /><br />

