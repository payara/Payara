<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.

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

    Portions Copyright [2017-2018] [Payara Foundation and/or its affiliates]
-->

<!-- appServer/domainTabs.jsf -->

<sun:tabSet id="domainTabs" immediate="true" selected="#{sessionScope.domainTabs}">
    <sun:tab id="domainAttrs" immediate="true" text="$resource{i18n.headings.DomainAttributes}" toolTip="$resource{i18n.headings.DomainAttributesTab}">
        <!command
            setSessionAttribute(key="domainTabs" value="domainAttrs");
            gf.redirect(page="#{request.contextPath}/common/appServer/serverInstDomainAttrs.jsf");
        />
    </sun:tab>
    
    <sun:tab id="domainProps" immediate="true" text="$resource{i18nc.properties.TabTittle}" toolTip="$resource{i18nc.properties.TabTittleHelp}">
         <!command
            setSessionAttribute(key="domainTabs" value="domainProps");
            gf.redirect(page="#{request.contextPath}/common/appServer/serverInstConfigurationProps.jsf?tabSetName=domainTabs");
         />

        <sun:tab id="domainConfigurationProps" immediate="$boolean{true}" text="$resource{i18nc.properties.configurationPropertiesTabTittle}" 
                 toolTip="$resource{i18nc.properties.configurationPropertiesTabTittleHelp}" >
            <!command
                setSessionAttribute(key="domainTabs" value="domainConfigurationProps");
                gf.redirect(page="#{request.contextPath}/common/appServer/serverInstConfigurationProps.jsf?tabSetName=domainTabs");
            />
        </sun:tab>

        <sun:tab id="domainMicroProfileProps" immediate="$boolean{true}" text="$resource{i18nc.properties.microProfilePropertiesTabTittle}" 
                 toolTip="$resource{i18nc.properties.microProfilePropertiesTabTittleHelp}" >
            <!command
                setSessionAttribute(key="domainTabs" value="domainMicroProfileProps");
                gf.redirect(page="#{request.contextPath}/common/appServer/serverInstMicroProfileProps.jsf?tabSetName=domainTabs");
            />
        </sun:tab>
    </sun:tab>
    
    <sun:tab id="appConfig" immediate="true" text="$resource{i18n.headings.AppConfig}" toolTip="$resource{i18n.headings.AppConfigTab}" >
        <!command
            setSessionAttribute(key="domainTabs" value="appConfig");
            gf.redirect(page="#{request.contextPath}/common/appServer/serverInstAppsConfig.jsf");
        />
    </sun:tab>
    
    <sun:tab id="adminPassword" immediate="true" text="$resource{i18n.headings.AdminPassword}" toolTip="$resource{i18n.headings.AdminPasswordTab}" >
        <!command
            setSessionAttribute(key="domainTabs" value="adminPassword");
            gf.redirect(page="#{request.contextPath}/common/appServer/serverInstAdminPassword.jsf");
        />
    </sun:tab>
    
    <sun:tab id="pswdAliases" immediate="true" text="$resource{i18nc.pswdAlias.tab}" toolTip="$resource{i18nc.pswdAlias.tabTip}" >
        <!command
            setSessionAttribute(key="domainTabs" value="pswdAliases");
            gf.redirect(page="#{request.contextPath}/common/appServer/pswdAliases.jsf");
        />
    </sun:tab>
    
    <sun:tab id="domainLogs" immediate="true" text="$resource{i18nc.domainLogs.tab}" toolTip="$resource{i18nc.domainLogs.tabTip}" >
        <!command
            setSessionAttribute(key="domainTabs" value="domainLogs");
            gf.redirect(page="#{request.contextPath}/common/appServer/domainLogs.jsf");
        />
    </sun:tab>
</sun:tabSet>
<event>
    <!afterCreate
        includeIntegrations(type="org.glassfish.admingui:domainTab");
    />
</event>

