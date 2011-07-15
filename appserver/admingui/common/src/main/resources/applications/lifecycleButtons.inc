<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.

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

<!facet pageButtonsTop>
<sun:panelGroup id="topButtons">
     <sun:button id="saveButton" rendered="#{edit}" text="$resource{i18n.button.Save}" 
        onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
        <!command
            gf.mapValueNullToFalse(map="#{pageSession.valueMap}"  keys={"enabled", "failurefatal" } result="#{requestScope.attrMap}")
            setPageSessionAttribute(key="valueMap" value="#{requestScope.attrMap}");

            getUIComponent(clientId="$pageSession{propertyTableRowGroupId}", component=>$attribute{tableRowGroup});
            getAllSingleMapRows(TableRowGroup="$attribute{tableRowGroup}",  Rows=>$attribute{newList});

            setAttribute(key="tmp" value="#{pageSession.valueMap.classname}");
            gf.createAttributeMap(keys={"name" , "value" } ,values={"class-name", "$attribute{tmp}"} map="#{requestScope.map}");
            listAdd(list="#{requestScope.newList}" value="#{requestScope.map}");

            if (#{pageSession.valueMap.classpath}){
                setAttribute(key="tmp" value="#{pageSession.valueMap.classpath}");
                gf.createAttributeMap( keys={"name" , "value" } , values={"classpath", "$attribute{tmp}"}  map="#{requestScope.map}");
                listAdd(list="#{requestScope.newList}" value="#{requestScope.map}");
            }
            if (#{pageSession.valueMap.loadorder}){
                setAttribute(key="tmp" value="#{pageSession.valueMap.loadorder}");
                gf.createAttributeMap(keys={"name" , "value" } ,values={"load-order","$attribute{tmp}"}  map="#{requestScope.map}");
                listAdd(list="#{requestScope.newList}" value="#{requestScope.map}");
            }

            setAttribute(key="tmp" value="#{pageSession.valueMap.failurefatal}");
            gf.createAttributeMap(keys={"name" , "value" }, values={"is-failure-fatal", "$attribute{tmp}"}  map="#{requestScope.map}");
            listAdd(list="#{requestScope.newList}" value="#{requestScope.map}");

            gf.createAttributeMap(keys={"name" , "value" }, values={ "isLifecycle", "true"}  map="#{requestScope.map}");
            listAdd(list="#{requestScope.newList}" value="#{requestScope.map}");

            javaToJSON(obj="#{requestScope.newList}" json="#{requestScope.tmpJSON}");
            gf.restRequest(endpoint="#{pageSession.selfUrl}/property.json" method="POST" data="#{requestScope.tmpJSON}" result="#{requestScope.restResponse}");

            createMap(result="#{requestScope.aMap}")
            mapPut(map="#{requestScope.aMap}" key="description" value="#{pageSession.valueMap.description}");
            gf.restRequest(endpoint="#{pageSession.selfUrl}" attrs="#{requestScope.aMap}" method="POST" );

            if (#{pageSession.onlyDASExist}){
                createMap(result="#{requestScope.aMap}")
                mapPut(map="#{requestScope.aMap}" key="enabled" value="#{pageSession.valueMap.enabled}");
                gf.restRequest(endpoint="#{pageSession.appRefUrl}" attrs="#{requestScope.aMap}" method="POST" );
            }

            prepareSuccessfulMsg();
            gf.redirect(page="#{selfPage}&alertType=${alertType}&alertSummary=${alertSummary}&alertDetail=${alertDetail}");
        />
    </sun:button>

    <sun:button id="newButton" rendered="#{!edit}" text="$resource{i18n.button.OK}"
            onClick="if (guiValidate('#{reqMsg}','#{reqInt}','#{reqPort}')) {submitAndDisable(this, '$resource{i18n.button.Processing}');}; return false;" >
        <!command
            if (!#{pageSession.onlyDASExist}){
              // mapPut(map="#{pageSession.valueMap}" key="target" value="domain");
            }
            gf.mapValueNullToFalse(map="#{pageSession.valueMap}"  keys={"enabled", "failurefatal" } result="#{requestScope.attrMap}")
            gf.restRequest(
                endpoint="#{sessionScope.REST_URL}/applications/application/create-lifecycle-module"
                attrs="#{requestScope.attrMap}"
                method="POST");
            if (!#{pageSession.onlyDASExist}){
                gf.changeAppTargets(appName="#{pageSession.valueMap['name']}" targets="#{pageSession.selectedTargets}"  status="#{requestScope.attrMap['enabled']}");
            }
            gf.redirect(page="#{pageSession.listPageLink}")
        />
    </sun:button>
    <sun:button id="cancelButton" immediate="#{true}" text="$resource{i18n.button.Cancel}" primary="#{false}">
        <!command
            gf.redirect(page="#{listPageLink}")
        />
  </sun:button>
</sun:panelGroup>
</facet>
