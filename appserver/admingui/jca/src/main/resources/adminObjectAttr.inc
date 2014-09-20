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

<!-- jca/adminObjectAttr.inc -->
<!-- used by adminObjectNew.jsf and adminObjectEdit.jsf -->
   
#include "/common/applications/applicationHandlers.inc"
<sun:propertySheet id="propertySheet">
    <!-- Text Field section -->
    <sun:propertySheetSection id="propertSectionTextField">
        <sun:property id="name"  rendered="#{edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.jndiName}">
            <sun:staticText id="name" text="#{pageSession.Name}"  rendered="#{edit}" >
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{nameId});
                />
            </sun:staticText>
        </sun:property>

        <sun:property id="nameNew"  rendered="#{!edit}"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.jndiName}">
            <sun:textField id="name" styleClass="required"  required="#{true}" columns="$int{30}" maxLength="#{sessionScope.fieldLengths['maxLength.common.jndiName']}" text="#{pageSession.valueMap['name']}" >
            <!afterCreate
                getClientId(component="$this{component}" clientId=>$page{resCompId});
            />
            </sun:textField>
        </sun:property>


        <sun:property id="resAdaptorProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.resourceAdapter}" helpText="$resource{i18njca.adminObjectResource.resourceAdapterHelp}">
            <sun:dropDown id="resAdaptorProp" selected="#{pageSession.valueMap['resAdapter']}" labels="$pageSession{connectorList}"  values="$pageSession{connectorList}" required="#{true}" submitForm="#{true}" immediate="#{true}">
                <!beforeCreate
                     setPageSessionAttribute(key="snifferType" value="connector");
                     gfr.getApplicationsBySnifferType();
                     setAttribute(key="connectorList" value="#{pageSession.snifferTypeApplicationsList}");
                     if ("#{sessionScope._jms_exist}"){
                        listAdd(list="#{requestScope.connectorList}" value="jmsra")
                        setPageSessionAttribute(key="connectorList" value="#{requestScope.connectorList}")
                    }
                    if ("!(#{sessionScope._jms_exist})"){
                        addEmptyFirstElement(in="#{requestScope.connectorList}"  out="#{pageSession.connectorList}")
                    }
                />
                <!afterCreate
                      getClientId(component="$this{component}" clientId=>$page{resAdaptorPropId});
                />
                <!command
                    setAttribute(key="click" value="$this{component}");
                    setAttribute(key="resAdapter" value="#{click.selected}");
                    setAttribute(key="reload" value="#{true}" );
                    if(#{statusId}) {
                        getUIComponent(clientId="#{statusId}", component=>$attribute{statusComponent});
                        getUIComponentProperty(component="$attribute{statusComponent}", name="submittedValue", value=>$attribute{status});
                    }
                    getUIComponent(clientId="#{descId}", component=>$attribute{descComponent});
                    getUIComponentProperty(component="$attribute{descComponent}", name="submittedValue", value=>$attribute{desc});
                    //gf.navigate works only for new page & not for edit page
                    //conversely gf.redirect works only for edit page & not new page
                    //needs more investigation - same scenario in component below too
                    if(!#{edit}){
                        getUIComponent(clientId="#{resCompId}", component=>$attribute{nameComponent});
                        getUIComponentProperty(component="$attribute{nameComponent}", name="submittedValue", value=>$attribute{nameChosen});
                        gf.navigate(page="#{selfPage}");
                    }
                    if(#{edit}){
                        getUIComponent(clientId="#{nameId}", component=>$attribute{nameComponent});
                        getUIComponentProperty(component="$attribute{nameComponent}", name="value", value=>$attribute{nameChosen});
                        if(#{statusId}) {
                            gf.redirect(page="#{selfPage}&reload=#{reload}&name=$attribute{nameChosen}&resAdapter=#{resAdapter}&description=#{desc}&status=#{status[0]}");
                        }
                        if(!#{statusId}) {
                            gf.redirect(page="#{selfPage}&reload=#{reload}&name=$attribute{nameChosen}&resAdapter=#{resAdapter}&description=#{desc}");
                        }
                    }
                 />
           </sun:dropDown>
        </sun:property>

        <sun:property id="resTypeProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.resType}"  helpText="$resource{i18njca.adminObjectResource.resTypeHelp}">
            <sun:dropDown id="db" required="#{true}" styleClass="required" selected="#{pageSession.valueMap['resType']}"  labels="$attribute{resourceTypes}" submitForm="#{true}" immediate="#{true}">
            <!afterCreate
                      getClientId(component="$this{component}" clientId=>$page{resTypePropId});
             />
            <!command
                    setAttribute(key="click" value="$this{component}");
                    setAttribute(key="resType" value="#{click.selected}")
                    setAttribute(key="reload" value="#{true}" );
                    getUIComponent(clientId="#{resAdaptorPropId}", component=>$attribute{resAdaptorComponent});
                    getUIComponentProperty(component="$attribute{resAdaptorComponent}", name="selected", value=>$attribute{resAdapter});
                    if(#{statusId}) {
                        getUIComponent(clientId="#{statusId}", component=>$attribute{statusComponent});
                        getUIComponentProperty(component="$attribute{statusComponent}", name="submittedValue", value=>$attribute{status});
                    }
                    getUIComponent(clientId="#{descId}", component=>$attribute{descComponent});
                    getUIComponentProperty(component="$attribute{descComponent}", name="submittedValue", value=>$attribute{desc});
                    if(!#{edit}){
                        getUIComponent(clientId="#{resCompId}", component=>$attribute{nameComponent});
                        getUIComponentProperty(component="$attribute{nameComponent}", name="submittedValue", value=>$attribute{nameChosen});
                        gf.navigate(page="#{selfPage}");
                    }
                    if(#{edit}){
                        getUIComponent(clientId="#{nameId}", component=>$attribute{nameComponent});
                        getUIComponentProperty(component="$attribute{nameComponent}", name="value", value=>$attribute{nameChosen});
                        if(#{statusId}) {
                            gf.redirect(page="#{selfPage}&reload=#{reload}&name=$attribute{nameChosen}&resType=#{resType}&resAdapter=$attribute{resAdapter}&description=#{desc}&status=#{status[0]}");
                        }
                        if(!#{statusId}) {
                            gf.redirect(page="#{selfPage}&reload=#{reload}&name=$attribute{nameChosen}&resType=#{resType}&resAdapter=$attribute{resAdapter}&description=#{desc}");
                        }
                    }
                />
            </sun:dropDown>
        </sun:property>  

        <sun:property id="className"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njca.adminObjectResource.classname}" helpText="$resource{i18njca.adminObjectResource.classnameHelp}">
            <sun:dropDown id="db" required="#{true}" styleClass="required" selected="#{pageSession.valueMap['className']}"  labels="$attribute{classNames}" values="$attribute{classNames}" submitForm="#{true}" immediate="#{true}" >
                <!command
                    setAttribute(key="click" value="$this{component}");
                    setAttribute(key="className" value="#{click.selected}")
                    setAttribute(key="reload" value="#{true}" );
                    getUIComponent(clientId="#{resAdaptorPropId}", component=>$attribute{resAdaptorComponent});
                    getUIComponentProperty(component="$attribute{resAdaptorComponent}", name="selected", value=>$attribute{resAdapter});
                    getUIComponent(clientId="#{resTypePropId}", component=>$attribute{resTypeComponent});
                    getUIComponentProperty(component="$attribute{resTypeComponent}", name="selected", value=>$attribute{resType});
                    if(#{statusId}) {
                        getUIComponent(clientId="#{statusId}", component=>$attribute{statusComponent});
                        getUIComponentProperty(component="$attribute{statusComponent}", name="submittedValue", value=>$attribute{status});
                    }
                    getUIComponent(clientId="#{descId}", component=>$attribute{descComponent});
                    getUIComponentProperty(component="$attribute{descComponent}", name="submittedValue", value=>$attribute{desc});
                    if(!#{edit}){
                        getUIComponent(clientId="#{resCompId}", component=>$attribute{nameComponent});
                        getUIComponentProperty(component="$attribute{nameComponent}", name="submittedValue", value=>$attribute{nameChosen});
                        gf.navigate(page="#{selfPage}");
                    }
                />
            </sun:dropDown>
        </sun:property>
        <sun:property id="deploymentOrder" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}"  rendered="#{edit}" label="$resource{i18n.common.resource.deploymentOrder}"  helpText="$resource{i18n.common.resource.deploymentOrderHelp}">
            <sun:textField id="deploymentOrder" styleClass="integer" columns="$int{10}" maxLength="#{sessionScope.fieldLengths['maxLength.common.deploymentOrder']}" text="#{pageSession.valueMap['deploymentOrder']}" />
        </sun:property>
        <sun:property id="descriptionProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.description}">
            <sun:textField id="descAdaptor" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.common.description']}" text="#{pageSession.valueMap['description']}" >
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{descId});
                />
            </sun:textField>
        </sun:property>
        <sun:property id="statusProp" rendered="#{useCheckBox}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.status}" >
            <sun:checkbox label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap['enabled']}" selectedValue="true" visible="#{false}" />
            <sun:checkbox id="enabled" label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap2['enabled']}" selectedValue="true" >
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{statusId});                    
                />
            </sun:checkbox>
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
</sun:propertySheet> 


