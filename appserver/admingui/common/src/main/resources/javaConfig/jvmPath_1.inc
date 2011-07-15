<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.

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

<!-- jvmPath_1.inc -->
<!-- when including this page, should followed by including jvmPath_2.inc to make a complete page -->
<!initPage
    setResourceBundle(key="i18nc" bundle="org.glassfish.common.admingui.Strings")
    setResourceBundle(key="help_common" bundle="org.glassfish.common.admingui.Helplinks");
/>
/>

<!composition template="/templates/default.layout"  guiTitle="$resource{i18nc.jvm.PathSettingsTitle}" >
<!define name="content">
    <event>
        <!beforeCreate
            setSessionAttribute(key="javaConfigTab" value="pathSettings");
            getRequestValue(key="configName" value=>$page{configName} default="server-config");
            setPageSessionAttribute(key="parentUrl", value="#{sessionScope.REST_URL}/configs/config/#{pageSession.configName}");
            setPageSessionAttribute(key="selfUrl", value="#{pageSession.parentUrl}/java-config");
            setPageSessionAttribute(key="rest-api" value="true");
            gf.getEntityAttrs(endpoint="#{pageSession.selfUrl}.json", valueMap="#{pageSession.valueMap}");

            formatStringsforDisplay(string="#{pageSession.valueMap['serverClasspath']}"  formattedString="#{requestScope.scp}");
            mapPut(map="#{pageSession.valueMap}" key="serverClasspath" value="$attribute{scp}");
            formatStringsforDisplay(string="#{pageSession.valueMap['systemClasspath']}"  formattedString="#{requestScope.syscp}");
            mapPut(map="#{pageSession.valueMap}" key="systemClasspath" value="$attribute{syscp}");
            formatStringsforDisplay(string="#{pageSession.valueMap['classpathPrefix']}"  formattedString="#{requestScope.cpp}");
            mapPut(map="#{pageSession.valueMap}" key="classpathPrefix" value="$attribute{cpp}");
            formatStringsforDisplay(string="#{pageSession.valueMap['classpathSuffix']}"  formattedString="#{requestScope.cps}");
            mapPut(map="#{pageSession.valueMap}" key="classpathSuffix" value="$attribute{cps}");

            formatStringsforDisplay(string="#{pageSession.valueMap['nativeLibraryPathPrefix']}"  formattedString="#{requestScope.npp}");
            mapPut(map="#{pageSession.valueMap}" key="nativeLibraryPathPrefix" value="$attribute{npp}");
            formatStringsforDisplay(string="#{pageSession.valueMap['nativeLibraryPathSuffix']}"  formattedString="#{requestScope.nps}");
            mapPut(map="#{pageSession.valueMap}" key="nativeLibraryPathSuffix" value="$attribute{nps}");

            setPageSessionAttribute(key="convertToFalseList" value={"envClasspathIgnored"});
            setPageSessionAttribute(key="selfPage" value="#{request.contextPath}/javaConfig/serverInstJvmPathSettings.jsf?name=#{pageSession.Name}&configName=#{pageSession.configName}");
            setPageSessionAttribute(key="skipAttrsList", 
                    value={"envClasspathIgnored" "serverClasspath" "systemClasspath" "classpathPrefix" "classpathSuffix"
                        "rmicOptions" "javacOptions" "javaHome" "debugOptions" "debugEnabled" "bytecodePreprocessors"});
        />   
    </event> 
            <sun:form id="propertyForm">
