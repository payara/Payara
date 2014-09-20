<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.

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

<!-- security/realms/realmAttrs.inc -->

    <sun:propertySheet id="propertySheet" requiredFields="true">
#include "/common/shared/configNameSection.inc"
        <sun:propertySheetSection id="propertySectionTextField">
            <sun:property id="NameProp"  rendered="#{edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.realmName}" >
                <sun:staticText id="NameStatic" text="#{pageSession.Name}"/>
            </sun:property>
            <sun:property id="classname"  rendered="#{edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.Classname}" >
                <sun:staticText id="classname" text="#{attrMap.classname}"/>
            </sun:property>
            <sun:property id="NameTextProp"  rendered="#{!edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.Name}">
                <sun:textField id="NameText" styleClass="required"  text="#{attrMap.Name}" columns="$int{55}" maxLength="#{sessionScope.fieldLengths['maxLength.common.Name']}" required="#{true}" />
            </sun:property> 

            <sun:property id="cp"  visible="#{!edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.Classname}" helpText="$resource{i18nc.realm.ClassnameHelp}<br /><br />">
                <sun:radioButton id="optA"  name="classnameOption" selected="#{classnameOption}" selectedValue="predefine"
                    onClick="enableClassnameFields('predefine'); "
                />                              
                <sun:dropDown id="Classname" labels="$pageSession{realmClasses}" selected="#{attrMap.classname}" 
                    disabled="#{!attrMap.predefinedClassname}" 
                    onChange=" showDisplay(''); ">
                    <!afterCreate
                        getClientId(component="$this{component}" clientId=>$page{classnameDropdownId});
                    />
                </sun:dropDown>
                "<br /><br />

                <sun:radioButton id="optB"  name="classnameOption" selected="#{classnameOption}" selectedValue="input"
                    onClick="javascript: enableClassnameFields('input');"
                />                                 
                <sun:textField id="ClassnameText" text="#{attrMap.classnameInput}" columns="$int{57}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.Classname']}">
                    <!afterCreate
                        getClientId(component="$this{component}" clientId=>$page{classnameTextId});
                    />
                </sun:textField>
                <!afterCreate
                    getClientId(component="$this{component}" clientId=>$page{rbPropId});
                />
            </sun:property> 
        </sun:propertySheetSection>
    </sun:propertySheet>
"<br><br>
    <sun:propertySheetSection id="fileSection" visible="#{false}" label="$resource{i18nc.realm.specificProps}" >
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{fileSectionId});
        />
        <sun:property id="jaax" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.jaax}" helpText="$resource{i18nc.realm.jaaxHelp}" >
            <sun:textField id="jaax" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.jaax']}" text="#{attrMap.fileJaax}" required="#{true}" />
        </sun:property>
        <sun:property id="keyFile" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.keyFile}" helpText="$resource{i18nc.realm.keyFileHelp}" >
            <sun:textField id="keyFile" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.keyFile']}" text="#{attrMap.file}" required="#{true}" />
        </sun:property>
        <sun:property id="asgroup" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.asgroup}" helpText="$resource{i18nc.realm.asgroupHelp}" >
            <sun:textField id="asgroups" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.asgroup']}" text="#{attrMap.fileAsGroups}"  />
        </sun:property> 
    </sun:propertySheetSection>

    <sun:propertySheetSection id="certSection" visible="#{false}" label="$resource{i18nc.realm.specificProps}">
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{certSectionId});
        />  
        <sun:property id="asgroup" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.asgroup}" helpText="$resource{i18nc.realm.asgroupHelp}" >
            <sun:textField id="asgroup" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.asgroup']}" text="#{attrMap.certAsGroups}"  />
        </sun:property> 
    </sun:propertySheetSection>

    <sun:propertySheetSection id="ldapSection" visible="#{false}" label="$resource{i18nc.realm.specificProps}">
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{ldapSectionId});
        />
         <sun:property id="jaax" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.jaax}" helpText="$resource{i18nc.realm.jaaxHelp}" >
            <sun:textField id="jaax" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.jaax']}" text="#{attrMap.ldapJaax}" required="#{true}" />
        </sun:property>
        <sun:property id="directory" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.directory}" helpText="$resource{i18nc.realm.directoryHelp}" >
            <sun:textField id="directory" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.directory']}" text="#{attrMap.directory}" required="#{true}" />
        </sun:property>
        <sun:property id="baseDn" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.baseDn}" helpText="$resource{i18nc.realm.baseDnHelp}" >
            <sun:textField id="baseDn" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.baseDn']}" text="#{attrMap.baseDn}" required="#{true}" />
        </sun:property> 
        <sun:property id="asgroup" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.asgroup}" helpText="$resource{i18nc.realm.asgroupHelp}" >
            <sun:textField id="asgroup" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.asgroup']}" text="#{attrMap.ldapAsGroups}"  />
        </sun:property> 
    </sun:propertySheetSection>

    <sun:propertySheetSection id="solarisSection" visible="#{false}" label="$resource{i18nc.realm.specificProps}">
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{solarisSectionId});
        />  
        <sun:property id="jaax" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.jaax}" helpText="$resource{i18nc.realm.jaaxHelp}" >
            <sun:textField id="jaax" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.jaax']}" text="#{attrMap.solarisJaax}" required="#{true}" />
        </sun:property> 
        <sun:property id="asgroup" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.asgroup}" helpText="$resource{i18nc.realm.asgroupHelp}" >
            <sun:textField id="asgroup" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.asgroup']}" text="#{attrMap.solarisAsGroups}"  />
        </sun:property> 
    </sun:propertySheetSection>

    <sun:propertySheetSection id="pamSection" visible="#{false}" label="$resource{i18nc.realm.specificProps}">
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{pamSectionId});
        />
        <sun:property id="jaax" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.jaax}" helpText="$resource{i18nc.realm.jaaxHelp}" >
            <sun:textField id="jaax" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.jaax']}" text="#{attrMap.pamJaax}" required="#{true}" />
        </sun:property>
    </sun:propertySheetSection>

    <sun:propertySheetSection id="jdbcSection" visible="#{false}" label="$resource{i18nc.realm.specificProps}">
        <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{jdbcSectionId});
        />  
        <sun:property id="jaax" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.jaax}" helpText="$resource{i18nc.realm.jaaxHelp}" >
            <sun:textField id="jaax" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.jaax']}" text="#{attrMap.jdbcJaax}" required="#{true}" />
        </sun:property> 
        <sun:property id="jndi" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.jndi}" helpText="$resource{i18nc.realm.jndiHelp}" >
            <sun:textField id="jndi" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.jndi']}" text="#{attrMap.datasourceJndi}" required="#{true}" />
        </sun:property>
        <sun:property id="userTable" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.userTable}" helpText="$resource{i18nc.realm.userTableHelp}" >
            <sun:textField id="userTable" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.userTable']}" text="#{attrMap.userTable}" required="#{true}" />
        </sun:property>
        <sun:property id="userName" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.userName}" helpText="$resource{i18nc.realm.userNameHelp}" >
            <sun:textField id="userName" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.userName']}" text="#{attrMap.userNameColumn}" required="#{true}" />
        </sun:property>
        <sun:property id="pswd" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.pswd}" helpText="$resource{i18nc.realm.pswdHelp}" >
            <sun:textField id="pswd" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.pswd']}" text="#{attrMap.passwordColumn}" required="#{true}" />
        </sun:property>
        <sun:property id="groupTable" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.groupTable}" helpText="$resource{i18nc.realm.groupTableHelp}" >
            <sun:textField id="groupTable" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.groupTable']}" text="#{attrMap.groupTable}" required="#{true}" />
        </sun:property>
        <sun:property id="groupTableUserName" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.groupTableUserName}" helpText="$resource{i18nc.realm.groupTableUserNameHelp}" >
            <sun:textField id="groupTable" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.userName']}" text="#{attrMap.groupTableUserName}" />
        </sun:property>
        <sun:property id="groupName" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.groupName}" helpText="$resource{i18nc.realm.groupNameHelp}" >
            <sun:textField id="groupName" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.groupName']}" text="#{attrMap.groupNameColumn}" required="#{true}" />
        </sun:property>
        <sun:property id="pswdEnc" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.pswdEnc}" helpText="$resource{i18nc.realm.pswdEncHelp}" >
            <sun:textField id="pswdEnc" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.pswdEncAlgorithm']}" text="#{attrMap.pswdEncAlgorithm}"  required="#{true}" />
        </sun:property>
        <sun:property id="asgroup" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.asgroup}" helpText="$resource{i18nc.realm.asgroupHelp}" >
            <sun:textField id="asgroup" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.asgroup']}" text="#{attrMap.jdbcAsGroups}"  />
        </sun:property> 
        <sun:property id="dbUser" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.dbUser}" helpText="$resource{i18nc.realm.dbUserHelp}" >
            <sun:textField id="dbUser" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.dbUser']}" text="#{attrMap.dbUser}"  />
        </sun:property> 
        <sun:property id="dbPswd" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.dbPswd}" helpText="$resource{i18nc.realm.dbPswdHelp}" >
            <sun:textField id="dbPswd" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.dbPswd']}" text="#{attrMap.dbPassword}"  />
        </sun:property> 
        <sun:property id="digest" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.digest}" helpText="$resource{i18nc.realm.digestHelp}" >
            <sun:textField id="digest" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.digest']}" text="#{attrMap.digestAlgorithm}"  />
        </sun:property>
        <sun:property id="encoding" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.encoding}" helpText="$resource{i18nc.realm.encodingHelp}" >
            <sun:textField id="encoding" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.encoding']}" text="#{attrMap.encoding}"  />
        </sun:property> 
        <sun:property id="charset" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18nc.realm.charset}" helpText="$resource{i18nc.realm.charsetHelp}" >
            <sun:textField id="charset" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.realm.charset']}" text="#{attrMap.charset}"  />
        </sun:property> 
    </sun:propertySheetSection>
