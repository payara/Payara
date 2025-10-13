<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.

    The contents of this file are subject to the terms of either the GNU
    General Public License Version 2 only ("GPL") or the Common Development
    and Distribution License("CDDL") (collectively, the "License").  You
    may not use this file except in compliance with the License.  You can
    obtain a copy of the License at
    https://github.com/payara/Payara/blob/master/LICENSE.txt
    See the License for the specific
    language governing permissions and limitations under the License.

    When distributing the software, include this License Header Notice in each
    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.

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

    Portions Copyright [2020] [Payara Foundation and/or its affiliates]
-->

#include "/cluster/shared/handlers.inc"
#include "/common/shared/commonHandlers.inc"

<handler id="gfr.prepareNodeCmd" >
    createMap(result="#{pageSession.attrsMap}");
    foreach(var="attr" list="#{requestScope.attrsList}"){
        mapGet(Map="#{pageSession.valueMap}" Key="#{requestScope.attr}" Value="#{requestScope.val}");
        if ("#{requestScope.val}"){
            mapPut(map="#{pageSession.attrsMap}", key="#{requestScope.attr}" value="#{requestScope.val}");
        }
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

<handler id="gfr.fixWinPswd" >
    if ("#{pageSession.winPsSelected}=2"){
        mapPut(map="#{pageSession.valueMap}" ,key="windowspassword" value="#{pageSession.winPswdText}");
    }
    if ("#{pageSession.winPsSelected}=3"){
        gf.convertToAlias(in="#{pageSession.winPswdAlias}" out="#{requestScope.tmpv}");
        mapPut(map="#{pageSession.valueMap}"  key="windowspassword" value="#{requestScope.tmpv}")
    }
</handler>


<sun:title id="propertyContentPage" title="#{pageSession.title}" helpText="#{pageSession.titleHelp}">
<!facet pageButtonsTop>
        <sun:panelGroup id="topButtons">
#include "/cluster/node/nodesButtons.inc"
        </sun:panelGroup>
</facet>

    </sun:title>
     <event>
        <!beforeEncode
            setPageSessionAttribute(key="showSSH" value="#{false}");
            if ("#{pageSession.valueMap['type']}=SSH"){
                   setPageSessionAttribute(key="showSSH" value="#{true}")
                   setPageSessionAttribute(key="showDCOM" value="#{false}")
                   setPageSessionAttribute(key="showCONFIG" value="#{false}")
                   setPageSessionAttribute(key="showInstalldirSSH" value="#{true}");
                   setPageSessionAttribute(key="showInstalldirConfig" value="#{false}");
                   setPageSessionAttribute(key="showInstalldirDocker" value="#{false}");
                   setPageSessionAttribute(key="showInstallOp" value="#{true}");
                   setPageSessionAttribute(key="showSetupSsh" value="#{true}");
                   setPageSessionAttribute(key="showGenerateKey" value="#{false}");
                   setPageSessionAttribute(key="showPublicKey" value="#{false}");
                   setPageSessionAttribute(key="showSetupPswd" value="#{false}");
                   if ("#{pageSession.valueMap['setupSsh']"){
                       setPageSessionAttribute(key="showGenerateKey" value="#{true}");
                       setPageSessionAttribute(key="showPublicKey" value="#{true}");
                       setPageSessionAttribute(key="showSetupPswd" value="#{true}");
                   }
                   setPageSessionAttribute(key="showKeyFile" value="#{true}");
                   setPageSessionAttribute(key="isTemp" value="#{false}");
            }
            if ("#{pageSession.valueMap['type']}=CONFIG"){
                   setPageSessionAttribute(key="showSSH" value="#{false}")
                   setPageSessionAttribute(key="showDCOM" value="#{false}")
                   setPageSessionAttribute(key="showCONFIG" value="#{true}")
                   setPageSessionAttribute(key="showInstalldirSSH" value="#{false}");
                   setPageSessionAttribute(key="showInstalldirConfig" value="#{true}");
                   setPageSessionAttribute(key="showInstalldirDocker" value="#{false}");
                   setPageSessionAttribute(key="showInstallOp" value="#{false}");
                   setPageSessionAttribute(key="isTemp" value="#{false}");
            }
            if ("#{pageSession.valueMap['type']}=DCOM"){
                   setPageSessionAttribute(key="showSSH" value="#{false}")
                   setPageSessionAttribute(key="showDCOM" value="#{true}")
                   setPageSessionAttribute(key="showCONFIG" value="#{false}")
                   setPageSessionAttribute(key="showInstalldirSSH" value="#{true}");
                   setPageSessionAttribute(key="showInstalldirConfig" value="#{false}");
                   setPageSessionAttribute(key="showInstalldirDocker" value="#{false}");
                   setPageSessionAttribute(key="showInstallOp" value="#{true}");
                   if ("#{pageSession.valueMap['validateDcom']}"){
                        setPageSessionAttribute(key="showTestdir" value="#{true}");
                   }
                   setPageSessionAttribute(key="isTemp" value="#{false}");
            }
            if ("#{pageSession.valueMap['type']}=DOCKER"){
                setPageSessionAttribute(key="showSSH" value="#{false}")
                setPageSessionAttribute(key="showDCOM" value="#{false}")
                setPageSessionAttribute(key="showCONFIG" value="#{false}")
                setPageSessionAttribute(key="showDOCKER" value="#{true}")
                setPageSessionAttribute(key="showInstalldirSSH" value="#{false}");
                setPageSessionAttribute(key="showInstalldirConfig" value="#{false}");
                setPageSessionAttribute(key="showInstalldirDocker" value="#{true}");
                setPageSessionAttribute(key="showInstallOp" value="#{false}");
                setPageSessionAttribute(key="isTemp" value="#{false}");
            }
            if ("#{pageSession.valueMap['type']}=TEMP"){
                setPageSessionAttribute(key="showSSH" value="#{false}")
                setPageSessionAttribute(key="showDCOM" value="#{false}")
                 setPageSessionAttribute(key="showCONFIG" value="#{false}")
                 setPageSessionAttribute(key="showDOCKER" value="#{false}")
                 setPageSessionAttribute(key="showInstalldirSSH" value="#{false}");
                 setPageSessionAttribute(key="showInstalldirConfig" value="#{false}");
                 setPageSessionAttribute(key="showInstalldirDocker" value="#{false}");
                 setPageSessionAttribute(key="showInstallOp" value="#{false}");
                 setPageSessionAttribute(key="isTemp" value="#{true}");
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

            <sun:property id="typePropHidden" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.type}" rendered="#{pageSession.isTemp}" visible="#{pageSession.isTemp}">
                <sun:staticText id="typeStatic" text="TEMP"/>
            </sun:property>

            <sun:property id="typeProp" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.type}" helpText="$resource{i18ncs.node.typeHelp}" rendered="#{!pageSession.isTemp}" visible="#{!pageSession.isTemp}">
                <sun:dropDown id="type" labels={"SSH" "DCOM" "CONFIG" "DOCKER"}  selected="#{pageSession.valueMap['type']}"
                              onChange="javascript:showSection(this.value, '#{pageSession.sshConnectorSectionId}', '#{pageSession.dcomSectionId}', '#{pageSession.dockerSectionId}',
                                        '#{pageSession.installdirSshId}', '#{pageSession.installdirConfigId}', '#{pageSession.installdirDockerId}',
                                        '#{pageSession.installProp}', '#{pageSession.archProp}');">
                <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.typeId}");
                />
                </sun:dropDown>
            </sun:property>

            <sun:property id="NodeHostHidden" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.NodeHost}" rendered="#{pageSession.isTemp}" visible="#{pageSession.isTemp}">
                <sun:staticText id="NodeHostStatic" text="#{pageSession.valueMap['nodehost']}"/>
            </sun:property>

            <sun:property id="NodeHost"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.NodeHost}" helpText="$resource{i18ncs.node.NodeHostHelp}" rendered="#{!pageSession.isTemp}" visible="#{!pageSession.isTemp}">
                <sun:textField id="NodeHost" text="#{pageSession.valueMap['nodehost']}"  columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.node.NodeHost']}">
                <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.nodeHostId}");
                />
                </sun:textField>
            </sun:property>

            <sun:property id="NodeHomeHidden" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.NodeDir}" rendered="#{sessionScope.isTemp}" visible="#{sessionScope.isTemp}">
                <sun:staticText id="NodeHomeStatic" text="#{pageSession.valueMap['nodedir']}"/>
            </sun:property>

            <sun:property id="NodeHome"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.NodeDir}" helpText="$resource{i18ncs.node.NodeDirHelp}" rendered="#{!pageSession.isTemp}" visible="#{!pageSession.isTemp}">
                <sun:textField id="NodeHome"  text="#{pageSession.valueMap['nodedir']}" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.node.NodeDir']}" />
            </sun:property>


            <sun:property id="installdir-ssh"  visible="#{pageSession.showInstalldirSSH}"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.InstallDir}" helpText="$resource{i18ncs.node.InstallDirHelp}" rendered="#{!pageSession.isTemp}">
                <sun:textField id="installDirssh" text="#{pageSession.valueMap['installdir']}"  columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.node.InstallDir']}"  />
                <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.installdirSshId}");
                />
            </sun:property>

            <sun:property id="installdir-hidden" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.InstallDir}" rendered="#{pageSession.isTemp}" visible="#{pageSession.isTemp}">
                <sun:staticText id="installDirHiddenStatic" text="#{pageSession.valueMap['installdirConfig']}"/>
            </sun:property>

            <sun:property id="installdir-config" visible="#{pageSession.showInstalldirConfig}"   labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.InstallDir}" helpText="$resource{i18ncs.node.InstallDirHelp}" rendered="#{!pageSession.isTemp}">
                <sun:textField id="installDirConfig" text="#{pageSession.valueMap['installdirConfig']}"  columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.node.InstallDir']}"  />
                 <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.installdirConfigId}");
                />

            </sun:property>

            <sun:property id="installdir-docker" visible="#{pageSession.showInstalldirDocker}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.InstallDir}" helpText="$resource{i18ncs.node.DockerInstallDirHelp}" rendered="#{!pageSession.isTemp}">
                <sun:textField id="installDirDocker" text="#{pageSession.valueMap['installdirDocker']}"  columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.node.InstallDir']}"  />
                <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.installdirDockerId}");
                />
            </sun:property>

            <sun:property>
                <sun:staticText text=" "/>
            </sun:property>

            <sun:property id="installOption"  rendered="#{!edit}" visible="#{pageSession.showInstallOp}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.installNode}" helpText="$resource{i18ncs.node.installNodeHelp}">
                <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.installProp}");
                />
                <sun:checkbox id="installOption" selected="#{pageSession.valueMap['install']}" selectedValue="true"  label="Enabled" >
                    <!afterCreate
                        getClientId(component="$this{component}" clientId="#{pageSession.installOptionId}");
                    />
                </sun:checkbox>
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
                <sun:dropDown id="psSelected" labels={"$resource{i18ncs.node.useKeyfile}", "$resource{i18ncs.node.textPassword}", "$resource{i18ncs.node.usePasswordAlias}" } values={"1", "2", "3"}
                      selected="#{pageSession.psSelected}"
                      onChange="showSshPswdSection(this.value, '#{pageSession.ps1PropId}',  '#{pageSession.pswdAliasPropId}'); " >
                <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.psSelectId}");
                />
                </sun:dropDown>
            </sun:property>

            <sun:property id="sshPasswordProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.enterPassword}" helpText="$resource{i18ncs.node.enterPasswordHelp}" >
                <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.ps1PropId}");
                />
                <sun:passwordField id="sshPassword" name="sshPassword" text="#{pageSession.pswdText}" columns="$int{25}" maxLength="#{sessionScope.fieldLengths['maxLength.server.NewPassword']}"  >
                 <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.ps1Id}");
                />
            </sun:passwordField>
            </sun:property>


            <sun:property id="pswdAlias" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.passwordAlias}" helpText="$resource{i18ncs.node.passwordAliasHelp}">
                <!afterCreate
                        getClientId(component="$this{component}" clientId="#{pageSession.pswdAliasPropId}");
                    />
                <sun:dropDown id="pswdAlias" labels="#{pageSession.listOfAliases}" selected="#{pageSession.pswdAlias}" >
                    <!beforeCreate
                        setAttribute(key="doEscape" value="true");
                        gfr.getPswdAliases();
                    />
                    <!afterCreate
                        getClientId(component="$this{component}" clientId="#{pageSession.pswdAliasId}");
                    />
                </sun:dropDown>
            </sun:property>

            <sun:property id="setupSsh"  visible="#{pageSession.showSetupSsh}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.runSetupSSH}" helpText="$resource{i18ncs.node.runSetupSSHHelp}">
                    <!afterCreate
                        getClientId(component="$this{component}" clientId="#{pageSession.setupSshPropId}");
                    />
                <sun:checkbox id="setupSsh"  selected="#{pageSession.valueMap['setupSsh']}" label="$resource{i18n.common.Enabled}" selectedValue="true"
                      onChange="showSetupSSH();">
                    <!afterCreate
                        getClientId(component="$this{component}" clientId="#{pageSession.setupSshId}");
                    />
                </sun:checkbox>
            </sun:property>


            <sun:property id="generate"   visible="#{pageSession.showGenerateKey}"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.generateKey}" helpText="$resource{i18ncs.node.generateKeyHelp}">
                <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.generateId}");
                />
                <sun:checkbox id="generate"  selected="#{pageSession.valueMap['generatekey']}" label="$resource{i18n.common.Enabled}" selectedValue="true" />
            </sun:property>

            <sun:property id="setupPswd"   visible="#{pageSession.showSetupPswd}"   labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.setupsshPswd}" helpText="$resource{i18ncs.node.setupsshPswdHelp}">
                <!afterCreate
                        getClientId(component="$this{component}" clientId="#{pageSession.setupPswdPropId}");
                />
                <sun:passwordField id="setupPswd" name="password" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.node.Keyfile']}" text="#{pageSession.setupPswd}" >
                <!afterCreate
                        getClientId(component="$this{component}" clientId="#{pageSession.setupPswdId}");
                />
                </sun:passwordField>
            </sun:property>

            <sun:property id="public"   visible="#{pageSession.showPublicKey}"   labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.publicKeyfile}" helpText="$resource{i18ncs.node.publicKeyfileHelp}">
                <!afterCreate
                        getClientId(component="$this{component}" clientId="#{pageSession.publicKeyId}");
                />
                <sun:textField id="public" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.node.Keyfile']}" text="#{pageSession.valueMap['sshpublickeyfile']}" >
                </sun:textField>
            </sun:property>



            <sun:property id="Keyfile" visible="#{pageSession.showKeyFile}"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.Keyfile}" helpText="$resource{i18ncs.node.KeyfileHelp}">
                <!afterCreate
                        getClientId(component="$this{component}" clientId="#{pageSession.keyfilePropId}");
                    />
                <sun:textField id="Keyfile" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.node.Keyfile']}" text="#{pageSession.valueMap['sshkeyfile']}" />
            </sun:property>

        </sun:propertySheetSection>


        <!-- =============================== DCOM ====================== -->

        <sun:propertySheetSection id="dcomSection" visible="#{pageSession.showDCOM}" label="$resource{i18ncs.node.DcomSection}" >
            <!afterCreate
                getClientId(component="$this{component}" clientId="#{pageSession.dcomSectionId}");
            />
            <sun:property id="force"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.force}" helpText="$resource{i18ncs.node.forceHelp}">
                <sun:checkbox id="force" label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap['winForce']}" selectedValue="true" />
            </sun:property>

            <sun:property id="validate"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.validateDcom}" helpText="$resource{i18ncs.node.validateDcomHelp}">
                <sun:checkbox id="validate"  selected="#{pageSession.valueMap['validateDcom']}" label="$resource{i18n.common.Enabled}" selectedValue="true"
                      onChange="showTestDir('#{pageSession.validateId}', '#{pageSession.testdirId}');">
                    <!afterCreate
                        getClientId(component="$this{component}" clientId="#{pageSession.validateId}");
                    />
                </sun:checkbox>
            </sun:property>

            <sun:property id="testDir"  visible="#{pageSession.showTestdir}" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.remotetestdir}" helpText="$resource{i18ncs.node.remotetestdirHelp}">
                <sun:textField id="testDir" text="#{pageSession.valueMap['remotetestdir']}" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.deploy.dirPath']}"  />
                <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.testdirId}");
                />
            </sun:property>

            <sun:property id="winDomain"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.windowDomain}" helpText="$resource{i18ncs.node.windowDomainHelp}">
                <sun:textField id="winDomain" text="#{pageSession.valueMap['windowsdomain']}" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.node.windowDomain']}"  />
            </sun:property>
            <sun:property id="winUser"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.windowUser}" helpText="$resource{i18ncs.node.windowUserHelp}">
                <sun:textField id="winUser" text="#{pageSession.valueMap['windowsuser']}" columns="$int{60}" maxLength="#{sessionScope.fieldLengths['maxLength.node.windowUser']}"  />
            </sun:property>

            <sun:property id="winPsSelected" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.selectWinPasswordSelect}" helpText="$resource{i18ncs.node.selectWinPasswordSelectHelp}">
                <sun:dropDown id="winPsSelected" labels={"$resource{i18ncs.node.textPassword}", "$resource{i18ncs.node.usePasswordAlias}" } values={"2", "3"}
                      selected="#{pageSession.winPsSelected}"
                      onChange="showPswdSection(this.value, '#{pageSession.winPs1PropId}', '#{pageSession.winPswdAliasPropId}');"
                 >
                <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.winPsSelectId}");
                />
                </sun:dropDown>
            </sun:property>

            <sun:property id="winPswdTextProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.winUserPassword}" helpText="$resource{i18ncs.node.winUserPasswordHelp}" >
                <!afterCreate
                        getClientId(component="$this{component}" clientId="#{pageSession.winPs1PropId}");
                    />
                <sun:passwordField id="winPswdText" name="winPassword" text="#{pageSession.winPswdText}" columns="$int{25}" maxLength="#{sessionScope.fieldLengths['maxLength.server.NewPassword']}"  >
                     <!afterCreate
                        getClientId(component="$this{component}" clientId="#{pageSession.winPs1Id}");
                    />
                </sun:passwordField>
            </sun:property>


            <sun:property id="winPswdAlias" labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.passwordAlias}" helpText="$resource{i18ncs.node.passwordAliasHelp}">
                <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.winPswdAliasPropId}");
                />
                <sun:dropDown id="winPswdAlias" labels="#{pageSession.listOfAliases}" selected="#{pageSession.winPswdAlias}" >
                    <!beforeCreate
                        setAttribute(key="doEscape" value="true");
                        gfr.getPswdAliases();
                    />
                    <!afterCreate
                        getClientId(component="$this{component}" clientId="#{pageSession.winPswdAliasId}");
                    />
                </sun:dropDown>
            </sun:property>

        </sun:propertySheetSection>

        <!-- =============================== Docker ====================== -->

        <sun:propertySheetSection id="dockerSection" visible="#{pageSession.showDOCKER}" label="$resource{i18ncs.node.dockerSection}" >
            <!afterCreate
                getClientId(component="$this{component}" clientId="#{pageSession.dockerSectionId}");
            />
            <sun:property id="dockerImage"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.dockerImage}" helpText="$resource{i18ncs.node.dockerImageHelp}">
                <sun:textField id="dockerImage" text="#{pageSession.valueMap['dockerimage']}" columns="$int{60}" />
            </sun:property>
            <sun:property id="dockerPort"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.dockerPort}" helpText="$resource{i18ncs.node.dockerPortHelp}">
                <sun:textField id="dockerport" text="#{pageSession.valueMap['dockerport']}" columns="$int{60}" />
            </sun:property>
            <sun:property id="dockerPasswordFile"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.dockerPasswordFile}" helpText="$resource{i18ncs.node.dockerPasswordFileHelp}">
                <sun:textField id="dockerPasswordFile" text="#{pageSession.valueMap['dockerpasswordfile']}" columns="$int{60}" />
            </sun:property>
            <sun:property id="useTls"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncs.node.useTls}" helpText="$resource{i18ncs.node.useTlsHelp}">
                <sun:checkbox id="useTls" selected="#{pageSession.usetlsSelected}" label="$resource{i18n.common.Enabled}" selectedValue="true"/>
            </sun:property>

        </sun:propertySheetSection>
    </sun:propertySheet>
         "<br />

    <sun:title>
        <!facet pageButtonsTop>
            <sun:panelGroup id="bottomButtons">
#include "/cluster/node/nodesButtons.inc"
            </sun:panelGroup>
        </facet>
</sun:title>
    "<br />
