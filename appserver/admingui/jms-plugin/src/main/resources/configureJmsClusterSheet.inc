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
<!initPage
    setResourceBundle(key="i18njms" bundle="org.glassfish.jms.admingui.Strings");
    setResourceBundle(key="helpjms" bundle="org.glassfish.jms.admingui.Helplinks")
    setPageSessionAttribute(key="clusterType" value="conventional");
    if (!#{pageSession.jmsTypeOptionGrp}) {
        setPageSessionAttribute(key="jmsTypeOptionGrp" value="EMBEDDED");
    }
    if (!#{pageSession.clusterTypeOptionGrp}) {
        setPageSessionAttribute(key="clusterTypeOptionGrp" value="conventional");
    }
/>
#include "/common/shared/commonHandlers.inc"
<f:verbatim>
<style type="text/css">
    .ConTblCl1Div_sun4 {
        min-width: 228px;
    }
</style>
</f:verbatim>
<sun:propertySheet id="jmsTypePropertySheet" visible="false">
    <sun:propertySheetSection id="jmsTypeSection">
        <sun:property id="jmsTypeProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
            label="$resource{i18ncs.cluster.jms.Type}" >
            <sun:hidden id="jmsType" value="#{pageSession.jmsType}"/>
            <sun:radioButton id="optEmbedded"  name="jmsTypeOptionGrp" style="display: block"
                label="$resource{i18ncs.cluster.jms.jmsIntegrationTypeEmbedded}"
                selected="#{jmsTypeOptionGrp}" selectedValue="EMBEDDED" onChange="updateUI();"/>
            <sun:radioButton id="optLocal"  name="jmsTypeOptionGrp" style="display: block"
                label="$resource{i18ncs.cluster.jms.jmsIntegrationTypeLocal}"
                selected="#{jmsTypeOptionGrp}" selectedValue="LOCAL" onChange="updateUI();"/>
            <sun:radioButton id="optRemote"  name="jmsTypeOptionGrp" style="display: block"
                label="$resource{i18ncs.cluster.jms.jmsIntegrationTypeRemote}"
                selected="#{jmsTypeOptionGrp}" selectedValue="REMOTE" onChange="updateUI();"/>
        </sun:property>
    </sun:propertySheetSection>
</sun:propertySheet>

<sun:propertySheet id="jmsPropertySheet" visible="false">
    <sun:propertySheetSection id="configureJmsClusterSection">
        <sun:property id="ClusterTypeProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                label="$resource{i18ncs.cluster.jms.ClusterTypeName}">

            <sun:hidden id="clusterType" value="#{pageSession.clusterType}"/>

            <sun:radioButton id="optConventional"  name="clusterTypeOptionGrp" style="display: block"
                label="$resource{i18ncs.cluster.jms.mqClusterTypeConventional}"
                selected="#{clusterTypeOptionGrp}" selectedValue="conventional" onChange="updateUI();"/>

            <sun:radioButton id="optEnhanced"  name="clusterTypeOptionGrp" style="display: block"
                label="$resource{i18ncs.cluster.jms.mqClusterTypeEnhanced}"
                selected="#{clusterTypeOptionGrp}" selectedValue="enhanced" onChange="updateUI();"/>
        </sun:property>

        <sun:property id="ConfigStoreTypeProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}"
                label="$resource{i18ncs.cluster.jms.configStoreType}" helpText="$resource{i18ncs.cluster.jms.configStoreTypeHelp}">
            <sun:dropDown id="configStoreType" selected="#{pageSession.configStoreType}" 
                labels={"$resource{i18ncs.cluster.jms.configStoreTypeMasterBroker}" "$resource{i18ncs.cluster.jms.configStoreTypeSharedDb}"}
                values={"masterbroker" "shareddb"} onChange="updateUI();"/>
        </sun:property>

        <sun:property id="MessageStoreTypeProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}"
                label="$resource{i18ncs.cluster.jms.messageStoreType}" helpText="$resource{i18ncs.cluster.jms.messageStoreTypeHelp}">
            <sun:dropDown id="messageStoreType" selected="#{pageSession.messageStoreType}" 
                labels={"$resource{i18ncs.cluster.jms.messageStoreTypeFile}" "$resource{i18ncs.cluster.jms.messageStoreTypeJdbc}"}
                values={"file" "jdbc"} onChange="updateUI();"/>
        </sun:property>

        <sun:property id="DbVendorProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                label="$resource{i18ncs.cluster.jms.dbvendor}" styleClass="__database">
            <sun:textField id="dbVendor" text="#{pageSession.dbVendor}" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.cluster.ClusterName']}"/>
        </sun:property>

        <sun:property id="DbUrlProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                label="$resource{i18ncs.cluster.jms.dbUrl}" styleClass="__database">
            <sun:textField id="dbUrl" text="#{pageSession.dbUrl}" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.cluster.ClusterName']}"/>
        </sun:property>

        <sun:property id="DbUserProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                label="$resource{i18ncs.cluster.jms.dbUser}" styleClass="__database">
            <sun:textField id="dbUser" text="#{pageSession.dbUser}" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.cluster.ClusterName']}"/>
        </sun:property>

        <sun:property id="PswdSelProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" styleClass="__database"
            label="$resource{i18ncs.cluster.jms.dbUserPasswordSelect}" helpText="$resource{i18ncs.cluster.jms.dbUserPasswordSelectHelp}">
            <sun:dropDown id="pwdSel" labels={"$resource{i18ncs.node.textPassword}", "$resource{i18ncs.node.usePasswordAlias}" } values={"password", "alias"}
                    selected="#{pageSession.authType}"
                    onChange="updateUI();">
            </sun:dropDown>
        </sun:property>

        <sun:property id="PswdTextProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" styleClass="__database"
            label="$resource{i18ncs.cluster.jms.dbUserPassword}" helpText="$resource{i18ncs.cluster.jms.dbUserPasswordHelp}" >
            <sun:passwordField id="NewPassword" text="#{pageSession.pswdText}" columns="$int{25}" 
                maxLength="#{sessionScope.fieldLengths['maxLength.server.NewPassword']}" />
        </sun:property>

        <sun:property id="PswdAliasProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" styleClass="__database"
            label="$resource{i18ncs.cluster.jms.dbUserPasswordAlias}" helpText="$resource{i18ncs.cluster.jms.dbUserPasswordAliasHelp}">
            <sun:dropDown id="pswdAlias" labels="#{pageSession.listOfAliases}" selected="#{pageSession.pswdAlias}" >
                <!beforeCreate
                    setAttribute(key="doEscape" value="true");
                    gfr.getPswdAliases();
                />
            </sun:dropDown>
        </sun:property>

        <sun:property id="PropertiesProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}"
                label="$resource{i18ncs.cluster.jms.properties}" helpText="$resource{i18ncs.cluster.jms.propertiesHelp}"
                rendered="#{pageSession.needPropField}">
            <sun:textField id="properties" text="#{pageSession.properties}" columns="$int{55}" />
        </sun:property>
    </sun:propertySheetSection>
</sun:propertySheet>
<f:verbatim>
    <script type="text/javascript">
        try {
            updateUI();
        } catch (e) {
            
        }
    </script>
</f:verbatim>
