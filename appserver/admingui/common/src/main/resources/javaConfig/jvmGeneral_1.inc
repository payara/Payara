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

<!-- jvmGeneral_1.inc -->
<!-- when including this page, should followed by including jvmGeneral_2.inc to make a complete page -->

<!initPage
    setResourceBundle(key="i18nc" bundle="org.glassfish.common.admingui.Strings")
    setResourceBundle(key="help_common" bundle="org.glassfish.common.admingui.Helplinks");
/>

<!composition template="/templates/default.layout"  guiTitle="$resource{i18nc.jvm.GeneralTitle}" >
<!define name="content">
    <event>
    <!beforeCreate
            # for EE, the following session attribute will just be ignored.
            setSessionAttribute(key="eeJvmSettingsTab" value="eeJvmGeneral");
            setSessionAttribute(key="javaConfigTab" value="jvmGeneral");
            getRequestValue(key="configName" value=>$page{configName} default="server-config");
            setPageSessionAttribute(key="parentUrl", value="#{sessionScope.REST_URL}/configs/config/#{pageSession.configName}");
            setPageSessionAttribute(key="selfUrl", value="#{pageSession.parentUrl}/java-config");
            setPageSessionAttribute(key="rest-api" value="true");
            gf.getEntityAttrs(endpoint="#{pageSession.selfUrl}.json", valueMap="#{pageSession.valueMap}");
            
            setPageSessionAttribute(key="convertToFalseList" value={"debugEnabled"});
            setPageSessionAttribute(key="childType" value="java-config");
            gf.restRequest(endpoint="#{pageSession.selfUrl}/property.json" method="GET" result="#{requestScope.propTable}");
            setPageSessionAttribute(key="tableList" value="#{requestScope.propTable.data.extraProperties.properties}");
            setPageSessionAttribute(key="selfPage" value="#{request.contextPath}/javaConfig/serverInstJvmGeneral.jsf?configName=#{pageSession.configName}");
            setPageSessionAttribute(key="skipAttrsList", value={"envClasspathIgnored" "serverClasspath" "systemClasspath" "classpathPrefix" "classpathSuffix" "nativeLibraryPathPrefix" "nativeLibraryPathSuffix"});
             //set the following for including buttons.inc
            setPageSessionAttribute(key="edit" value="#{true}" );
            setPageSessionAttribute(key="showDefaultButton" value="#{true}" );
            setPageSessionAttribute(key="showCancelButton" value="#{false}" );
            setPageSessionAttribute(key="hasPropertySheet" value="#{true}" );
             setPageSessionAttribute(key="hasPropertyTable" value="#{true}" );
    />
    </event>
            <sun:form id="propertyForm">
