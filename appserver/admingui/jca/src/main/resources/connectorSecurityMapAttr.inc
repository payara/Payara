<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.

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

<!-- jca/connectorSecurityMapAttr.inc -->

<!-- PropertySheet .... -->
<sun:propertySheet id="propertySheet">
    <sun:propertySheetSection id="propertSectionTextField">
        <!afterCreate
        getClientId(component="$this{component}" clientId=>$page{sheetId});
        />
        <sun:property id="poolName" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njca.connectorSecurityMap.poolName}">
            <sun:staticText id="poolName" text="#{pageSession.Name}"/>
        </sun:property>

        <sun:property id="mapName" rendered="#{edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njca.connectorSecurityMap.securityMapName}">
            <sun:staticText id="mapName" text="#{pageSession.mapName}"/>
        </sun:property>

        <sun:property id="mapNameNew" rendered="#{!edit}"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njca.connectorSecurityMap.securityMapName}" >
            <sun:textField id="mapName"   maxLength="#{sessionScope.fieldLengths['maxLength.connectorSecurityMap.securityMapName']}" styleClass="required" text="#{pageSession.valueMap['name']}" required="#{true}">
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{resCompId});
                />
            </sun:textField>
        </sun:property>

        <sun:property id="groupProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njca.connectorSecurityMap.userGroups}"  helpText="$resource{i18njca.connectorSecurityMap.userGroupsHelp}">
            <sun:radioButton id="optA"  name="usersOption" selected="#{requestScope.usersOptionU}" selectedValue="users"
                onClick="enableSecurityMapFields('users'); "
            />
            <sun:textField id="group" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.connectorSecurityMap.userGroups']}" text="#{pageSession.valueMap['userGroupCommaStr']}" >
                    <!afterCreate
                        getClientId(component="$this{component}" clientId=>$page{userGroupsId});
                    />
            </sun:textField>
            <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{grpPropId});
                />
        </sun:property>

        <sun:property id="principalsProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njca.connectorSecurityMap.principals}"  helpText="$resource{i18njca.connectorSecurityMap.principalsHelp}">
                   <sun:radioButton id="optB"  name="usersOption" selected="#{requestScope.usersOptionP}" selectedValue="principals"
                    onClick="javascript: enableSecurityMapFields('principals');"
                />
            <sun:textField id="principals" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.connectorSecurityMap.principals']}" text="#{pageSession.valueMap['principalCommaStr']}" >
                    <!afterCreate
                        getClientId(component="$this{component}" clientId=>$page{principalsId});
                    />
            </sun:textField>
            <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{pPropId});
                />
        </sun:property>
        "<br /><br />
    </sun:propertySheetSection>

    <sun:propertySheetSection id="propertSectionTextField2" label="$resource{i18njca.connectorSecurityMap.backendPrincipal}">
        <sun:property id="userNameEdit" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njca.connectorSecurityMap.userName}">
            <sun:textField id="userNameEdit" disabled="#{edit}" styleClass="required" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.connectorSecurityMap.userName']}" text="#{pageSession.valueMap['mappedUserName']}" required="#{true}" />
        </sun:property>
        <sun:property id="passwordEdit" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18njca.connectorSecurityMap.password}">
            <sun:passwordField id="passwordEdit" columns="$int{55}" text="#{pageSession.valueMap['mappedPassword']}"/>
        </sun:property>
    </sun:propertySheetSection>

</sun:propertySheet>
