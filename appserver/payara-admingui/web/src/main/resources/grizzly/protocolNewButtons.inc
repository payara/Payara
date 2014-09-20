<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.

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

<!-- grizzly/protocolNewButtons.jsf -->

<!facet pageButtonsTop>
    <sun:panelGroup id="topButtons">
        <sun:button id="newButton" rendered="#{!edit}" text="$resource{i18n.button.OK}"
                onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
            <!command
                urlencode(value="#{pageSession.valueMap['Name']}" result="#{requestScope.encodedProtocolToCheck}");
                gf.checkIfEndPointExist(endpoint="#{pageSession.selfUrl}/#{requestScope.encodedProtocolToCheck}", exists="#{requestScope.pExist}");
                if (#{requestScope.pExist}){
                    gf.handleError(detail="$resource{i18n_web.grizzly.protocol.alreadyExist, #{requestScope.encodedProtocolToCheck}}");
                }
                gf.createEntity(endpoint="#{pageSession.selfUrl}"
                        attrs="#{pageSession.valueMap}"
                        convertToFalse={"securityEnabled"},
                        result => $attribute{newProxy});

                setAttribute(key="emptyStr" value="");
                if("#{pageSession.skipAttrsList}=#{null}") {
                    createList(size="0", result="#{pageSession.skipAttrsList}");
                }
                if ("#{pageSession.httpMap['redirectPort']} = #{emptyStr}") {
                    listAdd(list="#{pageSession.skipAttrsList}" value="redirectPort");
                }
                if ("#{pageSession.httpMap['redirectPort']} = #{null}") {
                    listAdd(list="#{pageSession.skipAttrsList}" value="redirectPort");
                }
                if ("#{pageSession.httpMap['noCompressionUserAgents']} = #{null}") {
                    listAdd(list="#{pageSession.skipAttrsList}" value="noCompressionUserAgents");
                }
                mapPut(map="#{pageSession.httpMap}" key="target"  value="#{pageSession.valueMap['target']}")
                gf.createEntity(endpoint="${newProxy}/#{pageSession.valueMap['Name']}/create-http"
                        attrs="#{pageSession.httpMap}"
                        onlyUseAttrs={"target", "defaultVirtualServer"});
                listAdd(list="#{pageSession.skipAttrsList}" value="target");
                gf.createEntity(endpoint="${newProxy}/#{pageSession.valueMap['Name']}/http"
                        attrs="#{pageSession.httpMap}"
                        convertToFalse={"uploadTimeoutEnabled", "cometSupportEnabled", "dnsLookupEnabled", "rcmSupportEnabled", "traceEnabled", "authPassThroughEnabled", 
                                "chunkingEnabled",  "encodedSlashEnabled", "websocketsSupport", "xpoweredBy"},
                        skipAttrs="#{pageSession.skipAttrsList}"
                        result => $attribute{newHttp});

                gf.createEntity(endpoint="${newHttp}/#{pageSession.childType3}"
                        attrs="#{pageSession.fileMap}"
                        convertToFalse={"enabled"},
                );

                gf.redirect(page="#{parentPage}?configName=#{configName}")
            />
        </sun:button>
        <sun:button id="cancelButton" immediate="#{true}" primary="#{false}" text="$resource{i18n.button.Cancel}" >
            <!command
                gf.redirect(page="#{parentPage}?configName=#{configName}");
            />
        </sun:button>
    </sun:panelGroup>
</facet>
