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

<!-- editPageButtons_2.inc -->

    <!-- To use this general include files for page buttons,  the following page Session attributes should be set:
        setPageSessionAttribute(key="edit" value="#{false}" );
        setPageSessionAttribute(key="showDefaultButton" value="#{false}" );  //true is default
        setPageSessionAttribute(key="showCancelButton" value="#{false}" );   // true is default
        setPageSessionAttribute(key="hasPropertySheet" value="#{true}" );    //false is default
        setPageSessionAttribute(key="hasPropertyTable" value="#{true}" );   //false is default
        
        setPageSessionAttribute(key="has2Objects" value="#{true}" );        //false is default
        For creating new objects, if #{pageSession.parentObjectNameStr2} is specified, it will be used as the parent for creating the 2nd config.
        otherwise, it means the 2nd config is the sub-element, and the newly created object will be used as the parent to create
        the 2nd object.

    -->

    <!facet pageButtonsTop>
        <sun:panelGroup id="topButtons">
            <sun:button id="saveButton" rendered="#{edit}" text="$resource{i18n.button.Save}"
                    onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
                <!command
                    <!--
                    // no page defines hasRadioButtons yet, so we commen this out.
                    // when need to use this, we have to ensure that we don't refer to #{param , should go through getRequestValue
                    //if(#{pageSession.hasRadioButtons}){
                    //    forLoop(end="#{pageSession.radioButtonCount}" varName="i"){
                    //        setValueExpression(expression="#{pageSession.valueMap[requestScope.attrNames[i]]}"   value="#{param[requestScope.RadioButtonNames[i]]}");
                    //    }
                    //}
                    -->

                   if (#{pageSession['rest-api']}) {
                        if(#{pageSession.hasPropertySheet}){
                            gf.createEntity(endpoint="#{pageSession.selfUrl}"
                                attrs="#{pageSession.valueMap}"
                                skipAttrs="#{pageSession.skipAttrsList}"
                                convertToFalse="#{pageSession.convertToFalseList}"
                                onlyUseAttrs="#{pageSession.onlyUseAttrs}"
                            );
                        if(#{pageSession.has2Objects}){
                                gf.createEntity(endpoint="#{pageSession.selfUrl2}"
                                    attrs="#{pageSession.valueMap2}"
                                    skipAttrs="#{pageSession.skipAttrsList2}"
                                    convertToFalse="#{pageSession.convertToFalseList2}"
                                    onlyUseAttrs="#{pageSession.onlyUseAttrs2}"
                                );
                            }
                        }
                   }

                    // Save properties...
                    if (#{pageSession.hasPropertyTable}) {
                        removeEmptyProps(props="#{pageSession.tableList}" modifiedProps="#{pageSession.tableList}");
                        javaToJSON(obj="#{pageSession.tableList}" json="#{requestScope.tmpJSON}");
                        gf.restRequest(endpoint="#{pageSession.selfUrl}/property.json", method="POST", data="#{requestScope.tmpJSON}", result="#{requestScope.restResponse}");
                    }

                    if (#{pageSession.additionalSaveHandler}) {
                        gf.invokeHandler(handler="#{pageSession.additionalSaveHandler}", args="#{pageSession.additionalSaveHandlerArgs}" );
                    }
                    prepareSuccessfulMsg();
                    gf.redirect(page="#{pageSession.selfPage}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
                />
            </sun:button>


            <sun:button id="newButton" rendered="#{!edit}" text="$resource{i18n.button.OK}"
                    onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
                <!command

                    if (#{pageSession['rest-api']}){
                    gf.createEntity(endpoint="#{pageSession.parentUrl}/#{pageSession.childType}"
                        attrs="#{pageSession.valueMap}"
                        skipAttrs="#{pageSession.skipAttrsList}"
                        convertToFalse="#{pageSession.convertToFalseList}"
                        result => $attribute{objStr});
                    if(#{pageSession.has2Objects}){
                        if (#{pageSession.parentObjectNameStr2}){
                            gf.createEntity(endpoint="#{pageSession.parentUrl2}/#{pageSession.childType2}"
                                attrs="#{pageSession.valueMap2}"
                                skipAttrs="#{pageSession.skipAttrsList2}"
                                convertToFalse="#{pageSession.convertToFalseList2}"
                                result => $attribute{resultEndpoint});
                        }
                        if (!#{pageSession.parentObjectNameStr2}){
                            //create as a sub-element
                            gf.createEntity(endpoint="$attribute{objStr}"
                                attrs="#{pageSession.valueMap2}"
                                convertToFalse="#{pageSession.convertToFalseList2}"
                                result => $attribute{resultEndpoint2});
                        }
                    }
                    }


                    if (#{pageSession.hasPropertyTable}){
                        if (#{pageSession['rest-api']}) {
                            removeEmptyProps(props="#{pageSession.tableList}" modifiedProps="#{pageSession.tableList}");
                            javaToJSON(obj="#{pageSession.tableList}" json="#{requestScope.tmpJSON}");
                            //println("payload -------------------------------> #{requestScope.tmpJSON}");
                            urlencode(value="#{pageSession.valueMap['name']}" encoding="UTF-8" result="#{pageSession.encodeName}");
                            gf.restRequest(endpoint="#{pageSession.parentUrl}/#{pageSession.childType}/#{pageSession.encodeName}/property.json"
                                           method="POST"
                                           data="#{requestScope.tmpJSON}"
                                           result="#{requestScope.restResponse}");
                        }
                    }

                    if (#{pageSession.additionalSaveHandler}){
                        gf.invokeHandler(handler="#{pageSession.additionalSaveHandler}", args="#{pageSession.additionalSaveHandlerArgs}" );
                    }
                    if (#{pageSession.showSuccessfulMsg}){
                        prepareSuccessfulMsg();
                        gf.redirect(page="#{pageSession.selfPage}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
                    }
                    gf.redirect(page="#{pageSession.parentPage}?configName=#{configName}")
                />
            </sun:button>


            <sun:button id="cancelButton" immediate="#{true}" primary="#{false}" rendered="#{pageSession.showCancelButton}" text="$resource{i18n.button.Cancel}" >
                <!command
                    gf.redirect(page="#{parentPage}?configName=#{configName}");
                />
            </sun:button>
        </sun:panelGroup>
    </facet>