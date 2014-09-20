<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.

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

<!-- applications/resources/appScopedResourcesTable.inc -->

#include "/applications/applicationHandlers.inc"
<sun:table id="appScopedResources"  title="$resource{i18n.appScopedResourcesTable.tableTitle}" style="padding: 10pt; width:95%" >
    <!beforeCreate
       gfr.getAppScopedResourcesTable();
    />
     <sun:tableRowGroup id="rowGroup1"  data={"$pageSession{listOfRows}"} sourceVar="td">

        <sun:tableColumn headerText="$resource{i18n.ComponentTable.colApp}" sort="appName" rowHeader="$boolean{false}" id="colAppName">
            <staticText id="name" value="#{td.value.appName}" />
        </sun:tableColumn>
         <sun:tableColumn headerText="$resource{i18n.ComponentTable.colModule}" sort="moduleName" rowHeader="$boolean{true}" id="colModule">
            <staticText id="nameCol" value="#{td.value.moduleName}" />
        </sun:tableColumn>
        <sun:tableColumn headerText="$resource{i18n.appScopedResourcesTable.colResourceName}" sort="resName" rowHeader="$boolean{true}" id="resName">
            <sun:hyperlink id="resNameCol" text="#{td.value.resName}" url="#{request.contextPath}/#{td.value.link}&isAppScopedRes=#{true}" />
           </sun:tableColumn>
        <sun:tableColumn headerText="Resource Type" sort="resType" rowHeader="$boolean{true}" id="colResType">
            <staticText id="resTypeCol" value="#{td.value.resType}" />
        </sun:tableColumn>

    </sun:tableRowGroup>
</sun:table>
"<br /><br />
