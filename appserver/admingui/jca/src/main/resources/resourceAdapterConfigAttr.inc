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

<!-- jca/resourceAdapterConfigAttr.inc -->
<!-- used by resourceAdapterConfigNew.jsf and resourceAdapterConfigEdit.jsf -->
   
#include "/common/applications/applicationHandlers.inc"
<sun:propertySheet id="propertySheet">
    <!-- Text Field section -->
    <sun:propertySheetSection id="propertSectionTextField">
        <sun:property id="name"  rendered="#{edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njca.resourceAdapterConfig.Name}">
            <sun:staticText id="name" text="#{pageSession.Name}"  rendered="#{Edit}" />
        </sun:property>

        <sun:property id="nameNewDropDown"  rendered="#{!edit}"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njca.resourceAdapterConfig.Name}" helpText="$resource{i18njca.resourceAdapterConfig.NameHelp}" >
            <!afterCreate
                getClientId(component="$this{component}" clientId=>$page{propId});
            />
            <sun:hidden id="option" value="#{pageSession.myOption}">
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{optionId});
                />
            </sun:hidden>
            <sun:radioButton id="optA"  rendered="#{!edit}" name="dropDownOption" onClick="navigateToPage('#{pageSession.nameDropDown}', 'dropdown');"/>
            <sun:dropDown id="resAdaptorProp" selected="#{pageSession.nameDropDown}" labels="$pageSession{connectorList}"
                    submitForm="#{true}" immediate="#{true}" >
                <!beforeCreate
                     setPageSessionAttribute(key="snifferType" value="connector");
                     gfr.getApplicationsBySnifferType();
                     setPageSessionAttribute(key="connectorList" value="#{pageSession.snifferTypeApplicationsList}");
                     filterOutRarExtension(inList="#{pageSession.connectorList}" convertedList="#{pageSession.connectorList}");
                     if (#{sessionScope._jms_exist}){
                        listAdd(list="#{pageSession.connectorList}" value="jmsra" index="$int{0}")
                    }
                />
                <!command
                    setAttribute(key="click" value="$this{component}");
                    setAttribute(key="name" value="#{click.selected}");
                    getUIComponent(clientId="#{pageSession.textId}", component=>$attribute{textIdComp});
                    getUIComponentProperty(component="$attribute{textIdComp}", name="value", value=>$attribute{nameText});
                    setAttribute(key="reload" value="#{true}" );
                    gf.navigate(page="#{pageSession.selfPage}");
                />
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{dropdownTextId});
                />
           </sun:dropDown>
           "<br />
           <sun:radioButton id="optB"  rendered="#{!edit}" name="textOption" onClick="navigateToPage('#{pageSession.nameDropDown}', 'text');" />
            <sun:textField id="nameNewText" rendered="#{!edit}" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.resourceAdapterConfig.Name']}" text="#{pageSession.nameText}" >
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{textId});
                />
            </sun:textField>
        </sun:property>

        <sun:property id="threadPoolsIdProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njca.resourceAdapterConfig.threadPoolId}"
                helpText="$resource{i18njca.resourceAdapterConfig.ThreadPoolIdsHelp}" >
            //<sun:textField id="threadpoolsid" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.resourceAdapterConfig.ThreadPoolId']}" text="#{pageSession.valueMap['threadPoolIds']}" />
            <sun:dropDown id="threadpoolsid" selected="#{pageSession.valueMap['threadPoolIds']}" labels="#{pageSession.threadPoolList}" >
                <!beforeCreate
                    gf.getChildrenNamesList(endpoint="#{sessionScope.REST_URL}/configs/config/server-config/thread-pools/thread-pool"
                        result="#{requestScope.tmpList}");
                    addEmptyFirstElement(in="#{requestScope.tmpList}" out="#{pageSession.threadPoolList}")
                />
                </sun:dropDown>
        </sun:property>

        <sun:property id="deploymentOrder" labelAlign="left" noWrap="#{true}" rendered="#{edit}" overlapLabel="#{false}"  rendered="#{edit}" label="$resource{i18n.common.resource.deploymentOrder}" helpText="$resource{i18n.common.resource.deploymentOrderHelp}">
            <sun:textField id="deploymentOrder" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.common.deploymentOrder']}" text="#{pageSession.valueMap['deploymentOrder']}" />
        </sun:property>

        "<br /><br />
    </sun:propertySheetSection>
</sun:propertySheet> 
