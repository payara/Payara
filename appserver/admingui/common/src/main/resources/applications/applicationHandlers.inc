<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.

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

<!-- applications/applicationHandlers.inc -->

<!-- Input : snifferType     - required
     Input : isFullName
     Output: snifferTypeApplicationsList
-->
<handler id="gfr.getApplicationsBySnifferType">
    setPageSessionAttribute(key="snifferTypeApplicationsList" value={});
    if ("#{pageSession.isFullName}=#{null}") {
        setPageSessionAttribute(key="isFullName" value="#{true}");
    }
    setAttribute(key="applicationsURL" value="#{sessionScope.REST_URL}/applications/application");
    setAttribute(key="sniffersHideList" value={"security"});
    gf.getChildrenNamesList(endpoint="#{applicationsURL}" result="#{requestScope.applicationsList}");
    foreach (var="app" list="#{applicationsList}") {
        urlencode(value="#{app}" encoding="UTF-8" result="#{requestScope.encodedAppName}");
        setAttribute(key="moduleURL" value="#{applicationsURL}/#{encodedAppName}/module");
        gf.getChildrenNamesList(endpoint="#{moduleURL}" result="#{requestScope.modulesList}");
        foreach (var="module" list="#{modulesList}") {
            urlencode(value="#{module}" encoding="UTF-8" result="#{requestScope.encodedModuleName}");
            gf.getChildrenNamesList(endpoint="#{moduleURL}/#{encodedModuleName}/engine" result="#{requestScope.sniffersList}");
            gf.containedIn(list="#{requestScope.sniffersList}" testStr="#{pageSession.snifferType}" contain="#{requestScope.isSnifferType}");
            gf.containedIn(list="#{sniffersHideList}" testStr="#{pageSession.snifferType}" contain="#{requestScope.isSnifferHideType}");
            if(!#{requestScope.isSnifferHideType}&#{requestScope.isSnifferType}) {
                if(#{pageSession.isFullName}) {
                    gf.checkIfEndPointExist(endpoint="#{applicationsURL}/#{encodedAppName}/engine/ear" exists="#{requestScope.isEarSniffer}");
                    if(!#{isEarSniffer}) {
                        listAdd(list="#{snifferTypeApplicationsList}" value="#{app}");
                    }
                    if(#{isEarSniffer}) {
                        listAdd(list="#{snifferTypeApplicationsList}" value="#{app}##{module}");
                    }
                }
                if(!#{pageSession.isFullName}) {
                    listAdd(list="#{snifferTypeApplicationsList}" value="#{app}");
                }
            }
        }
    }
</handler>

<handler id="gfr.listAppScopedResources">
    mapPut(map="#{pageSession.argsMap}" key="appname" value="#{pageSession.encodedAppName}");
    gf.restRequest(endpoint="#{sessionScope.REST_URL}/_list-resources"
                    attrs="#{pageSession.argsMap}"
                    method="get"
                    throwException="$boolean{false}"
                    result="#{requestScope.appScopedResourcesTable}");
    if("#{requestScope.appScopedResourcesTable.data.properties}") {
        gf.addToAppScopedResourcesTable(appName="#{pageSession.appName}"
            moduleName="#{pageSession.moduleName}"
            resources="#{requestScope.appScopedResourcesTable.data.properties}"
            listOfRows="#{pageSession.listOfRows}"
            result="#{pageSession.listOfRows}");
    }
</handler>

<handler id="gfr.getAppScopedResourcesTable">
    //Application scoped Resources
    createMap(result="#{pageSession.argsMap}");
    gfr.listAppScopedResources();
    //Modules list of an application
    createMap(result="#{pageSession.argsMap}");
    //mapPut(map="#{pageSession.argsMap}" key="id" value="#{pageSession.encodedAppName}");
    //mapPut(map="#{pageSession.argsMap}" key="resources" value="true");
    gf.restRequest(endpoint="#{sessionScope.REST_URL}/applications/application/#{pageSession.encodedAppName}/list-sub-components"
                   attrs="#{pageSession.argsMap}"
                   method="get"
                   result="#{requestScope.result}");
    //Module scoped Resources
    if("#{requestScope.result.data.properties}") {
        gf.getMapKeys(Map="#{requestScope.result.data.properties}" Keys=>$attribute{moduleKeys});
        foreach(var="module" list="#{moduleKeys}") {
            setPageSessionAttribute(key="moduleName" value="#{module}");
            urlencode(value="#{module}" encoding="UTF-8" result="#{encodedModuleName}");
            createMap(result="#{pageSession.argsMap}");
            mapPut(map="#{pageSession.argsMap}" key="modulename" value="#{encodedModuleName}");
            gfr.listAppScopedResources();
        }
    }   
    if(!#{pageSession.listOfRows}) {
        setPageSessionAttribute(key="appScopedResLength" value="$int{0}");
    }
    if(#{pageSession.listOfRows}) {
        setPageSessionAttribute(key="appScopedResLength" value="#{pageSession.listOfRows.size()}");
    }
    setPageSessionAttribute(key="appScopedResExists" value="$boolean{false}");
    if(#{pageSession.appScopedResLength} > $int{0}) {
        setPageSessionAttribute(key="appScopedResExists" value="$boolean{true}");
    }
</handler>