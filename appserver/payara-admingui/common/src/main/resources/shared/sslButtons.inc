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

<!-- sslButtons.inc -->
   
<!facet pageButtonsTop>
    <sun:panelGroup id="topButtons">
        <sun:button id="saveButton" rendered="#{edit}" text="$resource{i18n.button.Save}"
                 onClick="if (checkSSLRequiredElements() && guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
            <!command
                convertCiphersItemsToStr(common="#{pageSession.selectedCommon}" ephemeral="#{pageSession.selectedEphemeral}" other="#{pageSession.selectedOther}" ecc="#{pageSession.selectedEcc}" ciphers="#{requestScope.ciphers}")
                mapPut(map="#{pageSession.valueMap}" key="ssl3TlsCiphers" value="#{requestScope.ciphers}")
                gf.createEntity(endpoint="#{pageSession.selfUrl}"
                            attrs="#{pageSession.valueMap}"
                            skipAttrs="#{pageSession.skipAttrsList}"
                            convertToFalse="#{pageSession.convertToFalseList}"
                );
                prepareSuccessfulMsg();
                gf.checkIfEndPointExist(endpoint="#{pageSession.selfUrl}" exists="#{requestScope.exist}")

                gf.redirect(page="#{selfPage}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
            />
        </sun:button>

        <sun:button id="newButton" rendered="#{!edit}" text="$resource{i18n.button.OK}"
                    onClick="if (checkSSLRequiredElements()  && guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}') ) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
            <!command
                convertCiphersItemsToStr(common="#{pageSession.selectedCommon}" ephemeral="#{pageSession.selectedEphemeral}" other="#{pageSession.selectedOther}" ecc="#{pageSession.selectedEcc}" ciphers="#{requestScope.ciphers}")
                mapPut(map="#{pageSession.valueMap}" key="ssl3TlsCiphers" value="#{requestScope.ciphers}")

                <!-- since create-ssl doesn't take all the options, we create it first just with cert name, and then edit it with all the attributes -->
                mapPut(map="#{pageSession.createMap}", key="certNickname",  value="#{pageSession.valueMap['certNickname']}")
                gf.createEntity(endpoint="#{pageSession.createSslUrl}" attrs="#{pageSession.createMap}" skipAttrs="#{pageSession.skipAttrsList}" convertToFalse="#{pageSession.convertToFalseList}");
                gf.createEntity(endpoint="#{pageSession.selfUrl}" attrs="#{pageSession.valueMap}" skipAttrs="#{pageSession.skipAttrsList}" convertToFalse="#{pageSession.convertToFalseList}");
                prepareSuccessfulMsg();
                gf.redirect(page="#{selfPage}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
            />
        </sun:button>
        <sun:button id="cancelButton" immediate="#{true}" primary="#{false}" rendered="#{pageSession.showCancelButton}" text="$resource{i18n.button.Cancel}" >
            <!command
                gf.redirect(page="#{parentPage}?configName=#{configName}");
            />
        </sun:button>
    </sun:panelGroup>
</facet>
