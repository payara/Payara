<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) [2018-2020] Payara Foundation and/or its affiliates. All rights reserved.

    The contents of this file are subject to the terms of either the GNU
    General Public License Version 2 only ("GPL") or the Common Development
    and Distribution License("CDDL") (collectively, the "License").  You
    may not use this file except in compliance with the License.  You can
    obtain a copy of the License at
    https://github.com/payara/Payara/blob/main/LICENSE.txt
    See the License for the specific
    language governing permissions and limitations under the License.

    When distributing the software, include this License Header Notice in each
    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.

    GPL Classpath Exception:
    The Payara Foundation designates this particular file as subject to the "Classpath"
    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

    <sun:title id="propertyContentPage" title="$resource{i18n_microprofile.microprofile.specs.configuration.config.ordinal.pageTitle}" 
               helpText="$resource{i18n_microprofile.microprofile.specs.configuration.config.ordinal.pageTitleHelpText}" >
        <!facet pageButtonsTop>
        <sun:panelGroup id="topButtons">
            <sun:button id="saveButton"  text="$resource{i18n.button.Save}"
                        onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')) 
                        submitAndDisable(this, '$resource{i18n.button.Processing}'); return false;" >
                      <!command
                        gf.updateEntity(endpoint="#{pageSession.MICROPROFILE_CONFIG_URL}" attrs="#{pageSession.microprofileConfigMap}");
                        prepareSuccessfulMsg();
                        gf.redirect(page="#{request.contextPath}/microprofile/microprofile/specs/configConfiguration.jsf?alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}&configName=#{pageSession.encodedConfigName}");
                    /> 
            </sun:button>
        </sun:panelGroup>
        </facet>
    </sun:title>
   <sun:propertySheet id="propertySheet">
#include "/common/shared/configNameSection.inc"
  <sun:propertySheetSection id="ordinalProps">
        <sun:property id="applicationOrdinalProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18n_microprofile.config.ordinal.configuration.application}">
              <sun:textField id="endPoint" columns="$int{40}" maxLength="250" 
                           text="#{pageSession.microprofileConfigMap['applicationOrdinality']}"/>
        </sun:property>

        <sun:property id="cloudOrdinalProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18n_microprofile.config.ordinal.configuration.cloud}">
              <sun:textField id="endPoint" columns="$int{40}" maxLength="250" 
                           text="#{pageSession.microprofileConfigMap['cloudOrdinality']}"/>
        </sun:property>
      
        <sun:property id="clusterOrdinalProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18n_microprofile.config.ordinal.configuration.cluster}">
              <sun:textField id="endPoint" columns="$int{40}" maxLength="250" 
                           text="#{pageSession.microprofileConfigMap['clusterOrdinality']}"/>
        </sun:property>

        <sun:property id="configOrdinalProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                       label="$resource{i18n_microprofile.config.ordinal.configuration.config}"> 
              <sun:textField id="endPoint" columns="$int{40}" maxLength="250" 
                           text="#{pageSession.microprofileConfigMap['configOrdinality']}"/>
        </sun:property>
       
        <sun:property id="domainOrdinalProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18n_microprofile.config.ordinal.configuration.domain}">
              <sun:textField id="endPoint" columns="$int{40}" maxLength="250" 
                           text="#{pageSession.microprofileConfigMap['domainOrdinality']}"/>
        </sun:property>
    
        <sun:property id="jdbcOrdinalProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18n_microprofile.config.ordinal.configuration.jdbc}">
              <sun:textField id="endPoint" columns="$int{40}" maxLength="250" 
                           text="#{pageSession.microprofileConfigMap['jdbcOrdinality']}"/>
        </sun:property>
      
        <sun:property id="jndiOrdinalProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18n_microprofile.config.ordinal.configuration.jndi}"> 
              <sun:textField id="endPoint" columns="$int{40}" maxLength="250" 
                           text="#{pageSession.microprofileConfigMap['jndiOrdinality']}"/>
        </sun:property>
    
        <sun:property id="moduleOrdinalProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18n_microprofile.config.ordinal.configuration.module}"> 
              <sun:textField id="endPoint" columns="$int{40}" maxLength="250" 
                           text="#{pageSession.microprofileConfigMap['moduleOrdinality']}"/>
        </sun:property>
    
        <sun:property id="passwordOrdinalProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18n_microprofile.config.ordinal.configuration.password}">
              <sun:textField id="endPoint" columns="$int{40}" maxLength="250" 
                           text="#{pageSession.microprofileConfigMap['passwordOrdinality']}"/>
        </sun:property>
    
        <sun:property id="secretDirectoryOrdinalProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18n_microprofile.config.ordinal.configuration.secretDirectory}"> 
              <sun:textField id="endPoint" columns="$int{40}" maxLength="250" 
                           text="#{pageSession.microprofileConfigMap['secretDirOrdinality']}"/>
        </sun:property>

        <sun:property id="tomlOrdinalProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}"
                      label="$resource{i18n_microprofile.config.ordinal.configuration.toml}">
              <sun:textField id="endPoint" columns="$int{40}" maxLength="250"
                           text="#{pageSession.microprofileConfigMap['tomlOrdinality']}"/>
        </sun:property>

        <sun:property id="serverOrdinalProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18n_microprofile.config.ordinal.configuration.server}">
              <sun:textField id="endPoint" columns="$int{40}" maxLength="250" 
                           text="#{pageSession.microprofileConfigMap['serverOrdinality']}"/>
        </sun:property>
      
        <sun:property id="ldapOrdinalProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18n_microprofile.config.ordinal.configuration.ldap}">
            <sun:textField id="endPoint" columns="$int{40}" maxLength="250" 
                           text="#{pageSession.microprofileConfigMap['ldapOrdinality']}"/>
        </sun:property>

    </sun:propertySheetSection>
   </sun:propertySheet>