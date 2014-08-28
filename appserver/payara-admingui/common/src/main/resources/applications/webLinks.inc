<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.

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
<sun:html id="html2">
    <sun:head id="propertyhead" title="#{pageSession.pageTitle}" />
    <sun:body id="body3">
        <sun:form id="propertyForm">
            <sun:title id="propertyContentPage" title="#{pageSession.pageTitle}" helpText="#{pageSession.pageTitleHelp}" />
                <sun:propertySheet id="propertySheet">
                    <sun:propertySheetSection id="propertySectionTextField">
                        <sun:property id="nameProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.deploy.ApplicationName}">
                            <sun:staticText id="name" text="#{pageSession.appName}" />
                        </sun:property>
                        <sun:property id="linkProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.common.Links}">

                            "<table>
                            <foreach key="list" list="#{pageSession.urlList}">
                               "<tr>
                                   "<td>
                                       <sun:staticText id="linkTarget" text="[#{list.target}] " />
                                   "</td>
                                   "<td>
                                        <sun:hyperlink text="#{list.url}" url="#{list.url}"/>
                                   "</td>
                                "</tr>
                            </foreach>
                            "</table>

                        </sun:property>
                    </sun:propertySheetSection>
                </sun:propertySheet>
            <sun:title id="title2">
                <!facet pageButtonsBottom>
                <sun:panelGroup id="topButtons">
                    <sun:button id="closeButton"
                        onClick="javascript: window.close(); return false;"
                        primary="#{true}"
                        text="$resource{i18n.button.Close}">
                    </sun:button>
                </sun:panelGroup>
                </facet>
            </sun:title>
        <sun:hidden id="helpKey" value="#{pageSession.helpKey}" />
        </sun:form>
    </sun:body>
</sun:html>