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
    <!-- Text Field section -->
    <sun:propertySheetSection id="propertSectionTextField">
        <sun:property id="jndiProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.Name}">
            <sun:staticText id="name" text="#{pageSession.appName}"/>
        </sun:property>

        

        <sun:property id="statusProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.status}">
            <sun:checkbox id="status" rendered="#{pageSession.onlyDASExist}" label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap2['enabled']}" selectedValue="true" />
            <sun:staticText id="statusText" rendered="#{!pageSession.onlyDASExist}" text="#{pageSession.status}" />
        </sun:property>

        <sun:property id="osgi" rendered="#{pageSession.showOsgiType}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.deploy.osgiType}" >
                <sun:checkbox id="osgi"  label=" "  disabled="#{true}"  selected="#{requestScope.osgiType}"  />
                <!beforeCreate
                    setAttribute(key="osgiType" value="#{false}");
                    if ("#{pageSession.valueMap['type']"){
                           setAttribute(key="osgiType" value="#{true}");
                    }
                />
        </sun:property>
        <sun:property id="dummy" rendered="#{pageSession.showOsgiType}" >
        </sun:property>
 
        <sun:property id="vsProp"  rendered="#{pageSession.finalShowVS}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.deploy.virtualServerLabel}" helpText="$resource{i18n.deploy.vsHelp}">
            <sun:listbox id="vs" immediate="#{true}" multiple="#{true}"  rows="$int{4}"
                labels="$pageSession{availableVS}"
                selected="#{pageSession.selectedVS}" >
                <!beforeCreate
                    getVsForDeployment(result="#{pageSession.vsList}");
                    getListBoxOptions(
                        availableList="#{pageSession.vsList}",
                        selectedCommaString="#{pageSession.valueMap2['virtualServers']}",
                        availableListResult="#{pageSession.availableVS}",
                        selectedOptions="#{pageSession.selectedVS}"
                        addEmptyFirstChoice="true");
                />
                </sun:listbox>
            </sun:property>

        <sun:property id="ctxProp" rendered="#{pageSession.showContextRoot}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.webApp.ContextRoot}" helpText="$resource{i18n.webApp.contextRootHelp}" >
            <sun:textField id="ctx" columns="$int{40}" maxLength="#{sessionScope.fieldLengths['maxLength.webApp.ContextRoot']}" text="#{pageSession.valueMap['contextRoot']}" />
        </sun:property>

        <sun:property id="implicitCdi" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.deploy.implicitCdi}" helpText="$resource{i18n.deploy.implicitCdiHelp}">
            <sun:checkbox id="implicitCdi" disabled="true" label="$resource{i18n.common.Enabled}" selected="#{pageSession.implicitCdiEnabled}" selectedValue="true" />
        </sun:property>

        <sun:property id="availability" rendered="#{pageSession.finalShowAvail}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.deploy.availability}" helpText="$resource{i18n.deploy.availabilityHelp}">
            <sun:checkbox id="availability" label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap['availabilityEnabled']}" selectedValue="true" />
        </sun:property>


        <sun:property id="jw" rendered="#{pageSession.showJavaWebStart}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.deploy.JavaWebStart}" helpText="$resource{i18n.edit.JavaWebStartHelp}">
            <sun:checkbox id="jwc" disabled="true" label="$resource{i18n.common.Enabled}" selected="#{pageSession.javaWebStartEnabled}" selectedValue="true" >
            </sun:checkbox>
        </sun:property>

        
        <sun:property id="locationProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.location}">
            <sun:staticText id="location" text="#{pageSession.valueMap['location']}"/>
        </sun:property>
            
        <sun:property id="deploymentOrderProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.deploy.deploymentOrder}" helpText="$resource{i18n.deploy.deploymentOrderHelp}">
            <sun:textField id="deploymentOrder" columns="$int{40}" maxLength="#{sessionScope.fieldLengths['maxLength.deploy.deploymentOrder']}" text="#{pageSession.valueMap['deploymentOrder']}" />
        </sun:property>

        <sun:property id="librariesProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.Libraries}">
            <sun:staticText id="libraries" text="#{pageSession.valueMap['libraries']}"/>
            
        </sun:property>
            
        <sun:property id="descProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.description}">
            <sun:textField id="desc" columns="$int{40}" maxLength="#{sessionScope.fieldLengths['maxLength.common.description']}" text="#{pageSession.valueMap['description']}" />
           "<br /><br />
        </sun:property>
        <!beforeCreate
            setPageSessionAttribute(key="javaWebStartEnabled" value="true");
            setPageSessionAttribute(key="implicitCdiEnabled" value="true");
            foreach (var="oneProp" list="#{pageSession.tableList}") {
                if (#{requestScope.oneProp.name}=$constant{org.glassfish.deployment.client.DFDeploymentProperties.DEPLOY_OPTION_JAVA_WEB_START_ENABLED}){
                    setPageSessionAttribute(key="javaWebStartEnabled" value="#{requestScope.oneProp.value}");
                }
                if (#{requestScope.oneProp.name}=implicitCdiEnabled){
                    setPageSessionAttribute(key="implicitCdiEnabled" value="#{requestScope.oneProp.value}");
                }
            }
        />
    </sun:propertySheetSection>
</sun:propertySheet>


