<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.

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

<!-- applications/uploadButtons.inc -->

<sun:button id="uploadButton" text="$resource{i18n.button.OK}"
    onClick="javascript:
        if(getTextElement('#{pageSession.radioChoosenId}').value=='client') {
            result = (admingui.deploy.checkFileInputRequired('#{fileuploadId}', '#{reqMsg}'));
        } else {
            result = (checkRequired('#{dirPathId}', '#{reqMsg}'));
        }
        if (result == true) {
	    if (admingui.deploy.checkType('#{pageSession.typeId}', '$resource{i18n.msg.JS.deploy.selectType}' )){
                if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')){
                    if (admingui.deploy.checkTarget('#{pageSession.onlyDASExist}', '#{pageSession.commonAddRemoveId}'+'_list_value', '$resource{i18n.msg.JS.deploy.confirmNoTarget}')){
                        disableAllButtons();
                        return submitAndDisable(this, '$resource{i18n.button.Processing}', '_self');
                    }
                }
	    }
        }
        return false;"
     >
    <!command
        compare(obj1="#{uploadRdBtn}" obj2="client" objEqual=>$attribute{needUpload});
        if (${needUpload}){
            gf.logger(logString="GUI deployment: uploadToTempfile", level="INFO");
            uploadFileToTempDir(file="#{uploadedFile}" uploadedTempFile="#{requestScope.filePath}", origPath="#{requestScope.origPath}");
            gf.logger(logString="filePath=#{requestScope.filePath} origPath=#{requestScope.origPath}", level="FINE");
        }
        if (! ${needUpload}){
            setAttribute(key="origPath", value="#{dirPath}");
            setAttribute(key="filePath", value="#{dirPath}");
            gf.logger(logString="No upload needed. origPath=#{requestScope.origPath}, filePath=#{requestScope.filePath} ",level="FINE");

        }
        deploy(filePath="$attribute{filePath}"
              origPath="$attribute{origPath}"
              allMaps = "#{pageSession.deployMap}"
              appType = "#{pageSession.appType}"
              propertyList="$attribute{newList}"
              targets = "#{pageSession.selectedTargets}"
        );
	deleteFileFromTempDir(deleteTempFile="$attribute{filePath}");
        gf.redirect(page="#{request.contextPath}/common/removeFrame.jsf?#{pageSession.listPageLink}&#{extraArgs}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}&bare=true");
    />
</sun:button>

<sun:button id="cancelButton" text="$resource{i18n.button.Cancel}" primary="#{false}" immediate="true"
    onClick="top.admingui.ajax.loadPage({url:'#{request.contextPath}/#{pageSession.cancelPage}?#{pageSession.extraArgs}'}); return false;">
</sun:button>
