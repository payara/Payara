<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.

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

<sun:property id="ctxInfoEnabled"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18ncon.contextInfoLabel}">
            <sun:checkbox id="ctxInfoEnabled"  label="$resource{i18n.common.Enabled}" selected="#{pageSession.valueMap['contextInfoEnabled']}" selectedValue="true"
                 //onChange="javascript:   webui.suntheme.dropDown.setDisabled('#{pageSession.ctxInfoCompId}', !this.checked); "
             />
        </sun:property>
        <sun:property id="contextInfo"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}"  helpText="$resource{i18ncon.contextInfoLabelHelp}">
            ### the beforeEncode is needed for 'loadDefault' since beforeCreate will not be called.
            <event>
                <!beforeEncode
                    getListBoxOptions(
                        availableList={ "Classloader", "JNDI", "Security", "WorkArea" },
                        selectedCommaString="#{pageSession.valueMap['contextInfo']}",
                        availableListResult="#{pageSession.ctxInfoOptions}",
                        selectedOptions="#{pageSession.contextInfo}"
                        addEmptyFirstChoice="false");
                />
            </event>
            <sun:listbox id="contextInfo" immediate="#{true}"  rows="$int{6}" multiple="#{true}"
                labels="#{pageSession.ctxInfoOptions}" selected="#{pageSession.contextInfo}" >
                <!beforeCreate
                    getListBoxOptions(
                        availableList={ "Classloader", "JNDI", "Security", "WorkArea" },
                        selectedCommaString="#{pageSession.valueMap['contextInfo']}",
                        availableListResult="#{pageSession.ctxInfoOptions}",
                        selectedOptions="#{pageSession.contextInfo}"
                        addEmptyFirstChoice="false");
                />
                <!afterCreate
                    getClientId(component="$this{component}" clientId="#{pageSession.ctxInfoCompId}");
                />
            </sun:listbox>
            <sun:staticText id="select" text="$resource{i18ncon.contextInfoSelection}" />
        </sun:property>