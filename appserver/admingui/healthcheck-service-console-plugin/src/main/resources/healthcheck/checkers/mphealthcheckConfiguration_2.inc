<!--

 Copyright (c) 2018 Payara Foundation and/or its affiliates. All rights reserved.

 The contents of this file are subject to the terms of either the GNU
 General Public License Version 2 only ("GPL") or the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://github.com/payara/Payara/blob/master/LICENSE.txt
 See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at glassfish/legal/LICENSE.txt.

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
 <sun:title id="propertyContentPage" title="$resource{i18nhx.healthcheck.checker.configuration.microprofileHealthcheckChecker.pageTitle}" 
               helpText="$resource{i18nhx.healthcheck.checker.configuration.microprofileHealthcheckChecker.pageTitleHelpText}" >
        <!facet pageButtonsTop>
        <sun:panelGroup id="topButtons">
            <sun:button id="saveButton"  text="$resource{i18n.button.Save}"
                        onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')) submitAndDisable(this, '$resource{i18n.button.Processing}'); return false;" >
                <!command
                    mapPut(map="#{pageSession.valueMap}" key="enabled" value="#{pageSession.enabledSelected}");
                    mapPut(map="#{pageSession.valueMap}" key="dynamic" value="#{pageSession.dynamic}");
                    prepareSuccessfulMsg();
                    gf.updateEntity(endpoint="#{pageSession.HEALTHCHECK_CONFIG_URL}/healthcheck-mp-configure"
                            attrs="#{pageSession.valueMap}" convertToFalse="#{pageSession.convertToFalseList}");
                    />
            </sun:button>
        </sun:panelGroup>
    </facet>
</sun:title>
    <sun:propertySheet id="nameSheet">
        <!beforeCreate
            setPageSessionAttribute(key="showInstanceName" value="#{false}");
            if("!(#{pageSession.target}=${null})"){
               setPageSessionAttribute(key="showInstanceName", value="#{true}");
            }

            setPageSessionAttribute(key="showConfigName" value="#{false}");
            if("#{pageSession.target}=${null}"){
                setPageSessionAttribute(key="showConfigName", value="#{true}");
            }
        />
        <!-- Text Field section -->
        <sun:propertySheetSection id="propertSectionTextField">
            <sun:property id="configNameProp" 
                          rendered="#{pageSession.showConfigName}"  
                          labelAlign="left" 
                          noWrap="#{true}" 
                          overlapLabel="#{false}" 
                          label="$resource{i18n.common.ConfigurationName}" >
                <sun:staticText id="configName"text="#{pageSession.encodedConfigName}" />
            </sun:property>
            <sun:property id="instanceName" 
                          rendered="#{pageSession.showInstanceName}"  
                          labelAlign="left"
                          noWrap="#{true}" 
                          overlapLabel="#{false}" 
                          label="$resource{i18n.common.instanceName}" >
                <sun:staticText id="instanceName" text="#{pageSession.target}" />
            </sun:property>
        </sun:propertySheetSection>
    </sun:propertySheet>
    "<br/>

    <sun:propertySheetSection id="mpHealthcheckCheckerProps">
        <sun:property id="enabledProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18nhx.healthcheck.configuration.enabledLabel}"  
                      helpText="$resource{i18nhx.healthcheck.configuration.enabledLabelHelpText}">
            <sun:checkbox id="enabledBox" selected="#{pageSession.enabledSelected}" selectedValue="true" />
        </sun:property>
        <sun:property id="dynamicProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18nhx.healthcheck.configuration.dynamicLabel}"  
                      helpText="$resource{i18nhx.healthcheck.configuration.dynamicLabelHelpText}">
            <sun:checkbox id="dynamicBox" selected="#{pageSession.dynamic}" selectedValue="true" />
        </sun:property>
        <sun:property id="nameProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18nhx.healthcheck.checker.configuration.nameLabel}"  
                      helpText="$resource{i18nhx.healthcheck.checker.configuration.nameLabelHelpText}">
            <sun:textField id="nameField" columns="$int{40}" maxLength="250" text="#{pageSession.valueMap['checkerName']}"/>
        </sun:property>
        <sun:property id="timeProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18nhx.healthcheck.checker.configuration.timeLabel}"  
                      helpText="$resource{i18nhx.healthcheck.checker.configuration.timeLabelHelpText}">
            <sun:textField id="timeField" columns="$int{40}" maxLength="250" styleClass="integer"
                           text="#{pageSession.valueMap['time']}"/>
        </sun:property>
        <sun:property id="unitProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18nhx.healthcheck.checker.configuration.unitLabel}"  
                      helpText="$resource{i18nhx.healthcheck.checker.configuration.unitLabelHelpText}">
            <sun:dropDown id="unitDropdown" selected="#{pageSession.valueMap['unit']}" 
                          labels={"NANOSECONDS", "MICROSECONDS", "MILLISECONDS", "SECONDS", "MINUTES", "HOURS", "DAYS"} />
        </sun:property>
        <sun:property id="thresholdTimeProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" 
                      label="$resource{i18nhx.healthcheck.checker.configuration.microprofileHealthcheckChecker.timeoutLabel}"  
                      helpText="$resource{i18nhx.healthcheck.checker.configuration.microprofileHealthcheckChecker.timeoutLabelHelpText}">
            <sun:textField id="timeField" columns="$int{40}" maxLength="250" styleClass="integer"
                           text="#{pageSession.valueMap['timeout']}"/>
        </sun:property>
    </sun:propertySheetSection>
  </sun:form>
 </define>
</composition>