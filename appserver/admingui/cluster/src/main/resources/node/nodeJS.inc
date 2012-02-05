<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.

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

<f:verbatim>
    <script type="text/javascript">
    function showSection(type, sshSection, dcomSection, sshDirId, configDirId, installProp, archProp){
         var sshSheet = document.getElementById(sshSection);
         var dcomSheet = document.getElementById(dcomSection);
         if (type == 'CONFIG'){
             sshSheet.style.display = 'none';
             dcomSheet.style.display = 'none';
             webui.suntheme.common.setVisible(sshDirId, false);
             webui.suntheme.common.setVisible(configDirId, true);

             if (installProp != null){
                 webui.suntheme.common.setVisible(installProp, false);
                 webui.suntheme.common.setVisible(archProp, false);
             }
         }
         if (type == 'SSH'){
             sshSheet.style.display = 'block';
             dcomSheet.style.display = 'none';
             webui.suntheme.common.setVisible(sshDirId, true);
             webui.suntheme.common.setVisible(configDirId, false);
             if(installProp != null){
                 webui.suntheme.common.setVisible(installProp, true);
                 webui.suntheme.common.setVisible(archProp, true);
             }
         }
         if (type == 'DCOM'){
             sshSheet.style.display = 'none';
             dcomSheet.style.display = 'block';
             webui.suntheme.common.setVisible(sshDirId, true);
             webui.suntheme.common.setVisible(configDirId, false);
             if(installProp != null){
                 webui.suntheme.common.setVisible(installProp, true);
                 webui.suntheme.common.setVisible(archProp, true);
             }
         }
    }

    function showSshPswdSection(type, ps1Id, pswdAId){
        if (type == '1'){
            hide(ps1Id);
            hide(pswdAId);
            show('#{pageSession.setupSshPropId}');
            show('#{pageSession.keyfilePropId}');
            showSetupSSH();
            return;
        }
        if (type == '2'){
            show(ps1Id);
            hide(pswdAId);
            hide('#{pageSession.setupSshPropId}');
            hide('#{pageSession.keyfilePropId}');
            hide('#{pageSession.generateId}');
            hide('#{pageSession.setupPswdPropId}');
            hide('#{pageSession.publicKeyId}');
            return;
        }
        if (type == '3'){
            hide(ps1Id);
            show(pswdAId);
            hide('#{pageSession.setupSshPropId}');
            hide('#{pageSession.keyfilePropId}');
            hide('#{pageSession.generateId}');
            hide('#{pageSession.setupPswdPropId}');
            hide('#{pageSession.publicKeyId}');
            return;
        }
    }

    function showPswdSection(type, ps1Id, pswdAId){
        if (type == '1'){
            hide(ps1Id);
            hide(pswdAId);
            return;
        }
        if (type == '2'){
            show(ps1Id);
            hide(pswdAId);
            return;
        }
        if (type == '3'){
            hide(ps1Id);
            show(pswdAId);
            return;
        }
    }

    function showSetupSSH(){
        var checked = document.getElementById('#{pageSession.setupSshId}').checked;
        if (checked){
            show('#{pageSession.generateId}');
            show('#{pageSession.setupPswdPropId}');
            show('#{pageSession.publicKeyId}');
        }else{
            hide('#{pageSession.generateId}');
            hide('#{pageSession.setupPswdPropId}');
            hide('#{pageSession.publicKeyId}');
        }

    }
    function checkSSHPswd(typeId, nodeHostId, psSelectId, ps1Id, setupSshId, setupPswdId, winPsSelectId, winPs1Id){
        if (! guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')){
	    return false;
	}
        var atype=document.getElementById(typeId).value;
        if ( atype == 'CONFIG'){
            return true;
        }

        if (!checkRequired(nodeHostId, '#{reqMsg}')){
            return false;
        }
        if (atype == 'SSH'){
            if ( document.getElementById(psSelectId).value == '2'){
                if (!checkRequired(ps1Id, '#{reqMsg}')){
                    return false;
                }else{
                    //ensure don't call setup-ssh'
                    document.getElementById(setupSshId).checked=false;
                    return true;
                }
            }
            if ( document.getElementById(psSelectId).value == '3'){
                //must have pswd alias specified
                var alcomp = document.getElementById('#{pageSession.pswdAliasId}');
                if (alcomp.value=='') {
                    return showAlert('$resource{i18ncs.msg.JS.noPasswordAlias}');
                }else{
                    //ensure don't call setup-ssh'
                    document.getElementById(setupSshId).checked=false;
                    return true;
                }
            }
            if ( (document.getElementById(psSelectId).value == '1') &&
                 (document.getElementById(setupSshId).checked ) &&
                 (document.getElementById(setupPswdId).value == '' ) ){
                    return checkRequired(setupPswdId, '#{reqMsg}');
                 }
            return true;
        }

        if (atype == 'DCOM'){
            if ( document.getElementById(winPsSelectId).value == '2'){
                //empty password is not allowed.
                return checkRequired(winPs1Id, '#{reqMsg}');
            }

            if ( document.getElementById(winPsSelectId).value == '3'){
                //must have pswd alias specified
                var aliascomp = document.getElementById('#{pageSession.winPswdAliasId}');
                if (aliascomp.value=='') {
                    return showAlert('$resource{i18ncs.msg.JS.noPasswordAlias}');
                }
            }
        }
        return true;
    }


function confirmEmptyPswd(ps1Id, emptyMsg) {
    var psComp = document.getElementById(ps1Id);
    var pswd = psComp.value;
    if (pswd == null || pswd == ''){
        psComp.select();
        psComp.focus();
        if ( getConfirm(this, emptyMsg) ){
            return true;
        }else{
            return false;
        }
    }
    return true;
}

    
    function disableTextComponent(textId) {
        var comp = document.getElementById(textId);
        if (comp != null){
            var textVal = comp.value;
            disableComponent(textId, 'text');
            comp.value=textVal;
        }
    }

    function  showTestDir(validateId, testdirId ){
        var ss = document.getElementById(validateId).checked;
        if (ss){
             webui.suntheme.common.setVisible(testdirId, true);
        }else{
            webui.suntheme.common.setVisible(testdirId, false);
        }
    }

    function show(compId){
        webui.suntheme.common.setVisible(compId, true);
    }

    function hide(compId){
        webui.suntheme.common.setVisible(compId, false);
    }

    </script>
</f:verbatim>
