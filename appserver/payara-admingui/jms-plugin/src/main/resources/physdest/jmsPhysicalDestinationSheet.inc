<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.

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
<sun:title id="propertyContentPage" title="#{pageTitle}" helpText="#{pageTitleHelp}">
    <sun:button id="def" rendered="#{edit}" primary="#{false}"  text="$resource{i18n.button.LoadDefaults}" >
        <!command
            getDefaultPhysicalDestinationValues( orig="#{pageSession.valueMap}"  map="#{pageSession.valueMap}" )
        />
    </sun:button>
    <!facet pageButtonsTop>
    <sun:panelGroup id="topButtons">
     <sun:button id="newButton" rendered="#{!edit}" text="$resource{i18n.button.OK}"
            onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
        <!command
            createMap(result="#{requestScope.attrsMap}");
            if ("#{type} = topic") {
                mapPut(map="#{pageSession.valueMap}", key="MaxNumActiveConsumers", value="#{null}");
                mapPut(map="#{pageSession.valueMap}", key="MaxNumBackupConsumers", value="#{null}");
                mapPut(map="#{pageSession.valueMap}", key="LocalDeliveryPreferred", value="#{null}");
            }
            mapJoin(map="#{pageSession.valueMap}", sep=":", result="#{requestScope.properties}");

            mapPut(map="#{requestScope.attrsMap}" key="property" value="#{requestScope.properties}");
            mapPut(map="#{requestScope.attrsMap}", key="desttype", value="#{type}");
            mapPut(map="#{requestScope.attrsMap}", key="target", value="#{pageSession.target}");
            mapPut(map="#{requestScope.attrsMap}", key="id", value="#{name}");

            gf.restRequest(endpoint="#{baseUrl}/create-jmsdest", method="post", attrs="#{requestScope.attrsMap}", result="#{requestScope.result}");
            gf.redirect(page="#{request.contextPath}/#{pageSession.parentPage}");
        />
    </sun:button>
     <sun:button id="updateButton" rendered="#{edit}" text="$resource{i18n.button.Save}"
            onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
        <!command
            createMap(result="#{requestScope.attrsMap}");
            mapJoin(map="#{pageSession.valueMap}", sep=":", result="#{requestScope.properties}");

            mapPut(map="#{requestScope.attrsMap}" key="property" value="#{requestScope.properties}");
            mapPut(map="#{requestScope.attrsMap}", key="desttype", value="#{pageSession.destType}");
            mapPut(map="#{requestScope.attrsMap}", key="target", value="#{pageSession.target}");
            mapPut(map="#{requestScope.attrsMap}", key="id", value="#{pageSession.valueMap.Name}");
            prepareSuccessfulMsg();
            gf.restRequest(endpoint="#{baseUrl}/__update-jmsdest", method="post", attrs="#{requestScope.attrsMap}", result="#{requestScope.result}");

//            updatePhysicalDestination(name="#{pageSession.destName}", type="#{pageSession.destType}", attributes="#{pageSession.valueMap}");
            gf.redirect(page="#{pageSession.selfPage}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}")
        />
    </sun:button>
    <sun:button id="cancelButton" immediate="#{true}" text="$resource{i18n.button.Cancel}" primary="#{false}">
        <!command
            gf.redirect(page="#{request.contextPath}/#{pageSession.parentPage}");
        />
  </sun:button>

</sun:panelGroup>
</facet>
</sun:title>
<!-- PropertySheet .... -->
<sun:propertySheet id="propertySheet">
    <sun:propertySheetSection id="propertSectionTextField">
        <sun:property id="NameTextProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.Name}">
            <h:outputText id="NameStatic" rendered="#{edit}" value="#{pageSession.destName}"/>
            <h:inputHidden rendered="#{edit}" value="#{pageSession.destName}" />
            <sun:textField id="NameText" rendered="#{!edit}" styleClass="required" text="#{name}" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.common.Name']}" required="#{true}" />
        </sun:property>
        <sun:property id="typeProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njms.jmsPhysDestinations.type}">
            <h:outputText id="TypeStatic" rendered="#{edit}" value="#{pageSession.destType}"/>
            <h:inputHidden rendered="#{edit}" value="#{pageSession.destType}" />
            <sun:dropDown id="type" rendered="#{!edit}" labels={"javax.jms.Topic" "javax.jms.Queue"} values={"topic" "queue"} selected="#{type}"/>
        </sun:property>

        <sun:property id="maxNumMsgsProp" labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njms.jmsPhysDestinations.MaxNumMsgs}" helpText="$resource{i18njms.jmsPhysDestinations.MaxNumMsgsHelp}" >
            <sun:textField id="maxNumMsgs" text="#{pageSession.valueMap['MaxNumMsgs']}" columns="$int{6}" maxLength="#{sessionScope.fieldLengths['maxLength.jmsPhysDestinations.MaxNumMsgs']}" />
        </sun:property>
        <sun:property id="maxBytesPerMsgProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njms.jmsPhysDestinations.MaxBytesPerMsg}" helpText="$resource{i18njms.jmsPhysDestinations.MaxBytesPerMsgHelp}" >
            <sun:textField id="maxBytesPerMsg" text="#{pageSession.valueMap['MaxBytesPerMsg']}" columns="$int{6}" maxLength="#{sessionScope.fieldLengths['maxLength.jmsPhysDestinations.MaxBytesPerMsg']}" />
        </sun:property>
        <sun:property id="maxTotalMsgBytesProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njms.jmsPhysDestinations.MaxTotalMsgBytes}" helpText="$resource{i18njms.jmsPhysDestinations.MaxTotalMsgBytesHelp}" >
            <sun:textField id="maxTotalMsgBytes" text="#{pageSession.valueMap['MaxTotalMsgBytes']}" columns="$int{6}" maxLength="#{sessionScope.fieldLengths['maxLength.jmsPhysDestinations.MaxTotalMsgBytes']}" />
        </sun:property>
        <sun:property id="limitBehaviorProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njms.jmsPhysDestinations.LimitBehavior}" helpText="$resource{i18njms.jmsPhysDestinations.LimitBehaviorHelp}" >
            <sun:dropDown id="Type" values={"REJECT_NEWEST", "FLOW_CONTROL","REMOVE_OLDEST","REMOVE_LOW_PRIORITY"}
                          labels={"$resource{i18njms.jmsPhysDestinations.REJECT_NEWEST}", "$resource{i18njms.jmsPhysDestinations.FLOW_CONTROL}", 
                            "$resource{i18njms.jmsPhysDestinations.REMOVE_OLDEST}", "$resource{i18njms.jmsPhysDestinations.REMOVE_LOW_PRIORITY}"}
                          selected="#{pageSession.valueMap['LimitBehavior']}"/>
        </sun:property>
        <sun:property id="maxNumProducersProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njms.jmsPhysDestinations.MaxNumProducers}" helpText="$resource{i18njms.jmsPhysDestinations.MaxNumProducersHelp}" >
            <sun:textField id="maxNumProducers" text="#{pageSession.valueMap['MaxNumProducers']}" columns="$int{6}" maxLength="#{sessionScope.fieldLengths['maxLength.jmsPhysDestinations.MaxNumProducers']}" />
        </sun:property>
        <sun:property id="maxNumActiveConsumersProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" rendered="#{pageSession.destType != 'topic'}" label="$resource{i18njms.jmsPhysDestinations.MaxNumActiveConsumers}" helpText="$resource{i18njms.jmsPhysDestinations.MaxNumActiveConsumersHelp}" >
            <sun:textField id="maxNumActiveConsumers" text="#{pageSession.valueMap['MaxNumActiveConsumers']}" columns="$int{6}" maxLength="#{sessionScope.fieldLengths['maxLength.jmsPhysDestinations.MaxNumActiveConsumers']}" />
        </sun:property>
        <sun:property id="maxNumBackupConsumersProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" rendered="#{pageSession.destType != 'topic'}" label="$resource{i18njms.jmsPhysDestinations.MaxNumBackupConsumers}" helpText="$resource{i18njms.jmsPhysDestinations.MaxNumBackupConsumersHelp}" >
            <sun:textField id="maxNumBackupConsumers" text="#{pageSession.valueMap['MaxNumBackupConsumers']}" columns="$int{6}" maxLength="#{sessionScope.fieldLengths['maxLength.jmsPhysDestinations.MaxNumBackupConsumers']}" />
        </sun:property>
        <sun:property id="consumerFlowLimitProp" labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" label="$resource{i18njms.jmsPhysDestinations.ConsumerFlowLimit}" helpText="$resource{i18njms.jmsPhysDestinations.ConsumerFlowLimitHelp}" >
            <sun:textField id="consumerFlowLimit" text="#{pageSession.valueMap['ConsumerFlowLimit']}" columns="$int{6}" maxLength="#{sessionScope.fieldLengths['maxLength.jmsPhysDestinations.ConsumerFlowLimit']}" />
        </sun:property>
        <sun:property id="localDeliveryPreferredProp" labelAlign="left" noWrap="#{false}" overlapLabel="#{false}" rendered="#{pageSession.destType != 'topic'}" label="$resource{i18njms.jmsPhysDestinations.LocalDeliveryPreferred}" helpText="$resource{i18njms.jmsPhysDestinations.LocalDeliveryPreferredHelp}" >
            <sun:dropDown id="localDeliveryPreferred" values={"true", "false"} labels={"$resource{i18n.common.true}", "$resource{i18n.common.false}"} selected="#{pageSession.valueMap['LocalDeliveryPreferred']}" selectedValue="true"/>
        </sun:property>
        <sun:property id="useDmqProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njms.jmsPhysDestinations.UseDMQ}" helpText="$resource{i18njms.jmsPhysDestinations.UseDMQHelp}" >
            <sun:dropDown id="useDmq" values={"true", "false"} labels={"$resource{i18n.common.true}", "$resource{i18n.common.false}"} selected="#{pageSession.valueMap['UseDMQ']}" selectedValue="true"/>
        </sun:property>
        <sun:property id="validateSchemaProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njms.jmsPhysDestinations.ValidateXMLSchemaEnabled}" helpText="$resource{i18njms.jmsPhysDestinations.ValidateXMLSchemaEnabledHelp}" >
            <sun:dropDown id="validateXMLSchemaEnabled" values={"true", "false"} labels={"$resource{i18n.common.true}", "$resource{i18n.common.false}"} selected="#{pageSession.valueMap['ValidateXMLSchemaEnabled']}" selectedValue="true"/>
        </sun:property>
        <sun:property id="xmlSchemeUrlProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njms.jmsPhysDestinations.XMLSchemaURIList}" helpText="$resource{i18njms.jmsPhysDestinations.XMLSchemaURIListHelp}" >
            <sun:textField id="xmlSchemaURIList" text="#{pageSession.valueMap['XmlSchemaURIList']}" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.jmsPhysDestinations.XMLSchemaURIList']}" />
        </sun:property>
    </sun:propertySheetSection>
</sun:propertySheet>
