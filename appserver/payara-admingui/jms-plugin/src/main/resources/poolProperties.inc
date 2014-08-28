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

<sun:propertySheet id="propertySheet">
    <sun:propertySheetSection id="generalPropertySheet" label="$resource{i18n.common.GeneralSettings}"   >
        <sun:property id="jndiProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.jndiName}"  rendered="#{!edit}">
            <sun:textField id="jndiProp" styleClass="required" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.common.poolName']}" text="#{pageSession.valueMap['name']}" required="#{true}" rendered="#{!edit}" >
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{resCompId});
                />
            </sun:textField>
        </sun:property>
                
        <sun:property id="jndiProp2"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.jndiName}" rendered="#{edit}">
            <sun:staticText text="#{pageSession.Name}"  rendered="#{edit}"/>
        </sun:property>
                
                
        <sun:property id="logic"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.logicalJndiName}"  rendered="#{edit and renderLogic}">
            <sun:staticText  id="logic" text="#{pageSession.logicalJndiName}" />
        </sun:property>
                
                
        <sun:property id="resTyped"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.resType}">
            <sun:dropDown id="resType" selected="#{pageSession.valueMap['connectionDefinitionName']}" rendered="#{!edit}"
                labels={"javax.jms.TopicConnectionFactory","javax.jms.QueueConnectionFactory","javax.jms.ConnectionFactory"} />
            <sun:staticText text="#{pageSession.valueMap['connectionDefinitionName']}" rendered="#{edit}"/>
        </sun:property>
                
                
        <sun:property id="descProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.description}">
            <sun:textField id="descProp" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.common.description']}" text="#{pageSession.valueMap['description']}" />
        </sun:property>
        <sun:property id="statusProp" rendered="#{useCheckBox}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.status}">
            <sun:checkbox label="$resource{i18n.common.Enabled}" selected="#{pageSession.resourceRef['enabled']}" selectedValue="true"/>
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
    <sun:propertySheetSection id="poolPropertySheet" label="$resource{i18njms.connectionFactory.poolSettings}">
        <sun:property id="initSizeProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njms.connectionFactory.initPoolSize}" helpText="$resource{i18njms.connectionFactory.initPoolSizeHelp}">
            <sun:textField id="ds" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.connectionFactory.initPoolSize']}" text="#{pageSession.valueMap['steadyPoolSize']}" />
            <sun:staticText id="con1" style="padding: 8pt" text="$resource{i18n.common.Connections}"/>
        </sun:property>
        <sun:property id="maxProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njms.connectionFactory.maxPoolSize}" helpText="$resource{i18njms.connectionFactory.maxPoolSizeHelp}">
            <sun:textField id="ds" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.connectionFactory.maxPoolSize']}" text="#{pageSession.valueMap['maxPoolSize']}" />
            <sun:staticText id="con" style="padding: 8pt" text="$resource{i18n.common.Connections}"/>
        </sun:property>
        <sun:property id="resizeProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njms.connectionFactory.poolResize}" helpText="$resource{i18njms.connectionFactory.poolResizeHelp}">
            <sun:textField id="ds" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.connectionFactory.poolResize']}" text="#{pageSession.valueMap['poolResizeQuantity']}" />
            <sun:staticText id="con" style="padding: 8pt" text="$resource{i18n.common.Connections}"/>
        </sun:property>
        <sun:property id="idleProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njms.connectionFactory.idleTimeout}" helpText="$resource{i18njms.connectionFactory.idleTimeoutHelp}">
            <sun:textField id="ds" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.connectionFactory.idleTimeout']}" text="#{pageSession.valueMap['idleTimeoutInSeconds']}" />
            <sun:staticText id="sec" style="padding: 8pt" text="$resource{i18n.common.Seconds}"/>
        </sun:property>
        <sun:property id="maxWaitProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njms.connectionFactory.maxWait}" helpText="$resource{i18njms.connectionFactory.maxWaitHelp}">
            <sun:textField id="ds" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.connectionFactory.maxWait']}" text="#{pageSession.valueMap['maxWaitTimeInMillis']}" />
            <sun:staticText id="mil" style="padding: 8pt" text="$resource{i18n.common.Milliseconds}"/>
        </sun:property>


        <sun:property id="failProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njms.connectionFactory.onFail}" helpText="$resource{i18njms.connectionFactory.onFailHelp}">
            <sun:checkbox label="$resource{i18njms.connectionFactory.closeAll}" selected="#{pageSession.valueMap['failAllConnections']}" selectedValue="true"/>
        </sun:property>
        <sun:property id="transprop"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njms.connectionFactory.transSupport}" helpText="$resource{i18njms.connectionFactory.transSupportHelp}" >
            <sun:dropDown id="trans" selected="#{pageSession.valueMap['transactionSupport']}" labels={"", "XATransaction", "LocalTransaction", "NoTransaction"} />
        </sun:property>
        <sun:property id="conValidationProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njms.connectionFactory.conValidation}" helpText="$resource{i18njms.connectionFactory.conValidationHelp}">
            <sun:checkbox label="$resource{i18n.common.Required}" selected="#{pageSession.valueMap['isConnectionValidationRequired']}" selectedValue="true" />
        </sun:property>

    </sun:propertySheetSection>
</sun:propertySheet>
