<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.

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

#include "/cluster/shared/handlers.inc"
#include "/common/shared/commonHandlers.inc"

<handler id="gfr.prepareNodeCmd" >
    if (!#{pageSession.valueMap['force']}){
        mapPut(map="#{pageSession.valueMap}"  key="force" value="false");
    }
    createMap(result="#{pageSession.attrsMap}");
    foreach(var="attr" list="#{requestScope.attrsList}"){
        mapGet(Map="#{pageSession.valueMap}" Key="#{requestScope.attr}" Value="#{requestScope.val}");
        mapPut(map="#{pageSession.attrsMap}", key="#{requestScope.attr}" value="#{requestScope.val}");
    }
</handler>


<handler id="gfr.fixPswd" >
    if ("#{pageSession.psSelected}=2"){
        mapPut(map="#{pageSession.valueMap}" ,key="sshpassword" value="#{pageSession.pswdText}");
    }
    if ("#{pageSession.psSelected}=3"){
        gf.convertToAlias(in="#{pageSession.pswdAlias}" out="#{requestScope.tmpv}");
        mapPut(map="#{pageSession.valueMap}"  key="sshpassword" value="#{requestScope.tmpv}")
    }
</handler>


    <sun:title id="propertyContentPage" title="#{pageSession.title}" helpText="#{pageSession.titleHelp}">
<!facet pageButtonsTop>
        <sun:panelGroup id="topButtons">
            <sun:button id="saveButton" rendered="#{edit}" text="$resource{i18n.button.Save} "
                    onClick="if (checkSSHPswd('#{pageSession.typeId}','#{pageSession.nodeHostId}','#{pageSession.psSelectId}', '#{pageSession.ps1Id}', '#{pageSession.ps2Id}', 
                        '$resource{i18n.msg.JS.manageUser.passwordNotMatch}', '$resource{i18ncs.msg.JS.confirmEmptyPswd}' , '$resource{i18ncs.msg.JS.nodeHostRequired}'))
                    {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" 
		>
                <!command
                    setAttribute(key="includeInstallDir" value="false");
                    if ("#{pageSession.valueMap['type']}=CONFIG"){
                        setAttribute(key="attrsList" value={"nodehost", "nodedir" });
                        setAttribute(key="endpoint" value="nodes/node/#{pageSession.nodeName}/update-node-config");
                        gfr.prepareNodeCmd();
                        mapPut(map="#{pageSession.attrsMap}", key="installdir" value="#{pageSession.valueMap['installdirConfig']}");
                    }
                    if ("#{pageSession.valueMap['type']}=SSH"){
                        gfr.fixPswd();
                        setAttribute(key="attrsList" value={"nodehost", "nodedir", "installdir", "sshport", "sshuser", "sshpassword" "sshkeyfile", "force"});
                        setAttribute(key="endpoint" value="nodes/node/#{pageSession.nodeName}/update-node-ssh");
                        gfr.prepareNodeCmd();
                    }
                    
                    gf.restRequest(endpoint="#{sessionScope.REST_URL}/#{requestScope.endpoint}" method="POST" attrs="#{pageSession.attrsMap}")
                    prepareSuccessfulMsg();
                    gf.redirect(page="#{selfPage}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
                />
            </sun:button>

            <sun:button id="newButton" rendered="#{!edit}" text="$resource{i18n.button.OK}"
                    onClick="if (checkSSHPswd('#{pageSession.typeId}', '#{pageSession.nodeHostId}', '#{pageSession.psSelectId}', '#{pageSession.ps1Id}', '#{pageSession.ps2Id}', 
                         '$resource{i18n.msg.JS.manageUser.passwordNotMatch}', '$resource{i18ncs.msg.JS.confirmEmptyPswd}', '$resource{i18ncs.msg.JS.nodeHostRequired}'))
                    {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" 
	    >
            <!command
                setAttribute(key="nameToCheck" value="#{pageSession.valueMap['id']}");
                gfj.checkNameExist();
                setAttribute(key="includeInstallDir" value="true");
                if ("#{pageSession.valueMap['type']}=CONFIG"){
                    setAttribute(key="attrsList" value={"id", "nodehost", "nodedir"});
                    setAttribute(key="endpoint" value="nodes/create-node-config");
                    gfr.prepareNodeCmd();
                    mapPut(map="#{pageSession.attrsMap}", key="installdir" value="#{pageSession.valueMap['installdirConfig']}");
                }
                if ("#{pageSession.valueMap['type']}=SSH"){
                    gfr.fixPswd();
                    setAttribute(key="attrsList" value={"id", "nodehost", "nodedir", "installdir", "sshport", "sshuser", "sshpassword" "sshkeyfile", "force"});
                    setAttribute(key="endpoint" value="nodes/create-node");
                    gfr.prepareNodeCmd();
                }
                
                gf.restRequest( endpoint="#{sessionScope.REST_URL}/#{requestScope.endpoint}" attrs="#{pageSession.attrsMap}" method="POST" )
                gf.redirect(page="#{request.contextPath}/cluster/node/nodes.jsf?alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
             />
            </sun:button>
            <sun:button id="cancelButton" immediate="#{true}" text="$resource{i18n.button.Cancel}" primary="#{false}" >
                <!command
                    gf.redirect(page="#{request.contextPath}/cluster/node/nodes.jsf");
                />
            </sun:button>

        </sun:panelGroup>
        </facet>

    </sun:title>
     <event>
        <!beforeEncode
            setPageSessionAttribute(key="showSSH" value="#{false}");
            if ("#{pageSession.valueMap['type']}=SSH"){
                   setPageSessionAttribute(key="showSSH" value="#{true}")
                   setPageSessionAttribute(key="showInstalldirSSH" value="#{true}");
                   setPageSessionAttribute(key="showInstalldirConfig" value="#{false}");
            }
            if ("#{pageSession.valueMap['type']}=CONFIG"){
                   setPageSessionAttribute(key="showSSH" value="#{false}")
                   setPageSessionAttribute(key="showInstalldirSSH" value="#{false}");
                   setPageSessionAttribute(key="showInstalldirConfig" value="#{true}");
            }
        />
    </event>
    <sun:propertySheet id="propertySheet">
        <sun:propertySheetSection id="propertSectionTextField">
            <sun:property id="nameProp"  rendered="#{!edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.Name}" helpText="$resource{i18ncs.node.NameHelp}">
                <sun:textField id="name" text="#{pageSession.valueMap['id']}" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.node.Name']}" styleClass="required" required="#{true}" />
            </sun:property>

            <sun:property id="staticnameProp"  rendered="#{edit}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.Name}" >
                <sun:staticText id="IdStatic" text="#{pageSession.nodeName}"/>
            </sun:property>
            <sun:property id="typeProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.type}" helpText="$resource{i18ncs.node.typeHelp}">
                <sun:dropDown id="type" labels={"SSH" "CONFIG"}  selected="#{pageSession.valueMap['type']}"
                              onChange="javascript:showSshSection(this.value, '#{pageSession.sshConnectorSectionId}', '#{pageSession.installdirSshId}', '#{pageSession.installdirConfigId}');">
                <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.typeId}");
                />
                </sun:dropDown>
            </sun:property>

            <sun:property id="NodeHost"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.NodeHost}" helpText="$resource{i18ncs.node.NodeHostHelp}">
                <sun:textField id="NodeHost" text="#{pageSession.valueMap['nodehost']}"  columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.node.NodeHost']}">
                <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.nodeHostId}");
                />
                </sun:textField>
            </sun:property>

            <sun:property id="NodeHome"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.NodeDir}" helpText="$resource{i18ncs.node.NodeDirHelp}">
                <sun:textField id="NodeHome"  text="#{pageSession.valueMap['nodedir']}" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.node.NodeDir']}" />
            </sun:property>


            <sun:property id="installdir-ssh"  visible="#{pageSession.showInstalldirSSH}"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.InstallDir}" helpText="$resource{i18ncs.node.InstallDirHelp}">
                <sun:textField id="installDirssh" text="#{pageSession.valueMap['installdir']}"  columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.node.InstallDir']}"  />
                <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.installdirSshId}");
                />
            </sun:property>
                
            <sun:property id="installdir-config" visible="#{pageSession.showInstalldirConfig}"   labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.InstallDir}" helpText="$resource{i18ncs.node.InstallDirHelp}">
                <sun:textField id="installDirConfig" text="#{pageSession.valueMap['installdirConfig']}"  columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.node.InstallDir']}"  />
                 <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.installdirConfigId}");
                />
                 
            </sun:property>
               
            <sun:property>
                <sun:staticText text=" "/>
            </sun:property>

        </sun:propertySheetSection>

        <sun:propertySheetSection id="sshConnectorSection" visible="#{pageSession.showSSH}" label="$resource{i18ncs.node.sshConnectorSection}" >
            <!afterCreate
                getClientId(component="$this{component}" clientId="#{pageSession.sshConnectorSectionId}");
            />
            <sun:property id="force"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.force}" helpText="$resource{i18ncs.node.forceHelp}">
                <sun:checkbox id="force" label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap['force']}" selectedValue="true" />
            </sun:property>

            <sun:property id="sshport"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.sshPort}" helpText="$resource{i18ncs.node.sshPortHelp}">
                <sun:textField id="sshport" text="#{pageSession.valueMap['sshport']}" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.node.sshPort']}"  />
            </sun:property>
            <sun:property id="UserName"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.UserName}" helpText="$resource{i18ncs.node.UserNameHelp}">
                <sun:textField id="UserName" text="#{pageSession.valueMap['sshuser']}" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.node.UserName']}"  />
            </sun:property>
            <sun:property id="psSelected" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.selectPasswordSelect}" helpText="$resource{i18ncs.node.selectPasswordSelectHelp}">
                <sun:dropDown id="psSelected" labels={"$resource{i18ncs.node.noPassword}", "$resource{i18ncs.node.textPassword}", "$resource{i18ncs.node.usePasswordAlias}" } values={"1", "2", "3"}
                      selected="#{pageSession.psSelected}"
                      onChange="javascript:showPswdSection(this.value, '#{pageSession.ps1Id}', '#{pageSession.ps2Id}', '#{pageSession.pswdAliasId}');"
                 >
                <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.psSelectId}");
                />
                </sun:dropDown>
            </sun:property>
            <sun:property id="newPasswordProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.enterPassword}" helpText="$resource{i18ncs.node.enterPasswordHelp}" >
            <sun:passwordField id="NewPassword" text="#{pageSession.pswdText}" columns="$int{25}" maxLength="#{sessionScope.fieldLengths['maxLength.server.NewPassword']}"  >
                 <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.ps1Id}");
                />
            </sun:passwordField>
            </sun:property>

            <sun:property id="confirmPasswordProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.confirmPassword}" helpText="$resource{i18ncs.node.confirmPasswordHelp}" >
                <sun:passwordField id="ConfirmPassword" text="#{pageSession.confirmPassword}"  columns="$int{25}" maxLength="#{sessionScope.fieldLengths['maxLength.server.ConfirmNewPassword']}"   >
                    <!afterCreate
                        getClientId(component="$this{component}" clientId="#{pageSession.ps2Id}");
                    />
                </sun:passwordField>
            </sun:property>

            <sun:property id="pswdAlias" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.passwordAlias}" helpText="$resource{i18ncs.node.passwordAliasHelp}">
                <sun:dropDown id="pswdAlias" labels="#{pageSession.listOfAliases}" selected="#{pageSession.pswdAlias}" >
                    <!afterCreate
                        getClientId(component="$this{component}" clientId="#{pageSession.pswdAliasId}");
                    />
                    <!beforeCreate
                        gfr.getPswdAliases();
                    />
                </sun:dropDown>
            </sun:property>
            <sun:property id="Keyfile"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.Keyfile}" helpText="$resource{i18ncs.node.KeyfileHelp}">
                <sun:textField id="Keyfile" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.node.Keyfile']}" text="#{pageSession.valueMap['sshkeyfile']}" />
            </sun:property>

        </sun:propertySheetSection>
    </sun:propertySheet>
         "<br /><br /><br />
