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

<!-- jvmProfiler_1.inc -->
<!-- when including this page, should followed by including jvmProfiler_2.inc to make a complete page -->
<!initPage
    setResourceBundle(key="i18nc" bundle="org.glassfish.common.admingui.Strings")
    setResourceBundle(key="help_common" bundle="org.glassfish.common.admingui.Helplinks");
/>
<!composition template="/templates/default.layout"  guiTitle="$resource{i18nc.jvm.ProfilerPageName}" >
<!define name="content">
    <event>
    <!beforeCreate
        setSessionAttribute(key="javaConfigTab" value="profiler");
        getRequestValue(key="configName" value=>$page{configName}  );
    
        urlencode(value="#{pageSession.configName}" encoding="UTF-8" result="#{pageSession.encodedConfigName}");
        setPageSessionAttribute(key="parentUrl", value="#{sessionScope.REST_URL}/configs/config/#{pageSession.encodedConfigName}");
        setPageSessionAttribute(key="selfUrl", value="#{pageSession.parentUrl}/java-config");
        setPageSessionAttribute(key="rest-api" value="true");
        setPageSessionAttribute(key="selfPage" value="#{request.contextPath}/javaConfig/serverInstProfiler.jsf");
        setPageSessionAttribute(key="childType" value="profiler");
        setPageSessionAttribute(key="confirmDeleteMsg" value="$resource{i18nc.msg.JS.confirmDeleteProfiler}");
        setPageSessionAttribute(key="profilerUrl", value="#{pageSession.parentUrl}/java-config/profiler");
        setPageSessionAttribute(key="createProfilerUrl", value="#{pageSession.parentUrl}/java-config/create-profiler");
        setPageSessionAttribute(key="deleteProfilerUrl", value="#{pageSession.parentUrl}/java-config/profiler/delete-profiler");
        gf.checkIfEndPointExist(endpoint="#{pageSession.profilerUrl}/jvm-options" exists="#{pageSession.edit}");

        if(#{edit}) {
            gf.getEntityAttrs(endpoint="#{pageSession.parentUrl}/java-config/profiler.json", valueMap="#{pageSession.valueMap}");
            getJvmOptionsValues(endpoint="#{pageSession.profilerUrl}/jvm-options.json"  result=>$attribute{tableList});
            gf.listCombine(list="#{null}" list2="$attribute{tableList}", result="#{pageSession.origList}");
        }
        //there is no way to get the default value of profiler when one doesn't exist.  just hard code 'enabled' to true
        if ("#{edit}=false"){
            createMap(result="#{pageSession.valueMap}");
            mapPut(map="#{pageSession.valueMap}" key="enabled" value="true");
        }
        setPageSessionAttribute(key="onlyUseAttrs" value={"name", "target", "classpath", "enabled","nativeLibraryPath"})
        setPageSessionAttribute(key="convertToFalseList" value={"enabled"});
    />
    </event>
            <sun:form id="propertyForm">